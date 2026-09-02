package mthiebi.sgs.gradebook.service.grid;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.SGSExceptionCode;
import mthiebi.sgs.gradebook.engine.SpecialValueBehaviour;
import mthiebi.sgs.gradebook.engine.ComponentDef;
import mthiebi.sgs.gradebook.engine.TemplateGraph;
import mthiebi.sgs.gradebook.model.ClassGroup;
import mthiebi.sgs.gradebook.model.ComponentKind;
import mthiebi.sgs.gradebook.model.Enrollment;
import mthiebi.sgs.gradebook.model.GradeComponent;
import mthiebi.sgs.gradebook.model.GradeEntry;
import mthiebi.sgs.gradebook.model.Period;
import mthiebi.sgs.gradebook.model.PeriodKind;
import mthiebi.sgs.gradebook.model.Student;
import mthiebi.sgs.gradebook.model.TemplateVersion;
import mthiebi.sgs.gradebook.repository.EnrollmentRepository;
import mthiebi.sgs.gradebook.repository.GradeComponentRepository;
import mthiebi.sgs.gradebook.repository.GradeEntryRepository;
import mthiebi.sgs.gradebook.repository.GradeChangeRequestRepository;
import mthiebi.sgs.gradebook.repository.PeriodRepository;
import mthiebi.sgs.gradebook.service.SpecialValueRegistry;
import mthiebi.sgs.gradebook.service.TemplateGraphLoader;
import mthiebi.sgs.gradebook.service.TemplateVersionResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builds a grade entry screen.
 * <p>
 * Four queries regardless of class size: the students, the components, the
 * cells, and the period. The screen it replaces issued a grid fetch plus a
 * class fetch plus a subject fetch to draw itself, and then refetched the whole
 * grid after every single cell edit.
 */
@Service
public class GradeGridService {

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private GradeEntryRepository gradeEntryRepository;

    @Autowired
    private GradeComponentRepository gradeComponentRepository;

    @Autowired
    private PeriodRepository periodRepository;

    @Autowired
    private GradeChangeRequestRepository changeRequestRepository;

    @Autowired
    private TemplateVersionResolver templateVersionResolver;

    @Autowired
    private mthiebi.sgs.gradebook.service.conversion.GradeConversionService gradeConversionService;

    @Autowired
    private TemplateGraphLoader templateGraphLoader;

    @Autowired
    private SpecialValueRegistry specialValueRegistry;

    @Transactional(readOnly = true)
    public GradeGrid load(Long classGroupId, Long subjectId, Long periodId,
                          String journalUuid) throws SGSException {
        return load(classGroupId, subjectId, periodId, journalUuid,
                new GridCapabilities(true, true, true));
    }

    @Transactional(readOnly = true)
    public GradeGrid load(Long classGroupId, Long subjectId, Long periodId,
                          String journalUuid, GridCapabilities capabilities)
            throws SGSException {

        List<Enrollment> enrollments = enrollmentRepository.findActiveByClassGroup(classGroupId);
        if (enrollments.isEmpty()) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, "კლასში მოსწავლეები ვერ მოიძებნა");
        }
        ClassGroup classGroup = enrollments.get(0).getClassGroup();

        Period period = periodRepository.findById(periodId)
                .orElseThrow(() -> new SGSException(SGSExceptionCode.BAD_REQUEST,
                        "პერიოდი ვერ მოიძებნა"));

        // A period from another scheme would silently return an empty grid
        // rather than an error, which is the kind of thing that gets diagnosed
        // as "the marks disappeared".
        if (!period.getScheme().getId().equals(classGroup.getPeriodScheme().getId())) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST,
                    "პერიოდი არ ეკუთვნის კლასის სასწავლო სქემას");
        }

        mthiebi.sgs.gradebook.model.GradingTemplate journal =
                templateVersionResolver.journalByUuid(journalUuid);
        TemplateVersionResolver.Resolved resolved = templateVersionResolver.resolve(
                classGroupId, subjectId, periodId, journal.getId());
        TemplateVersion version = resolved.getVersion();
        TemplateGraph graph = templateGraphLoader.graphOf(version.getId());

        GradeGrid grid = new GradeGrid();
        grid.setPeriod(new GridPeriod(period.getId(), period.getCode(),
                period.getLabel(), period.getKind()));
        grid.setTemplateVersion(new GridTemplateVersion(version.getId(),
                version.getTemplate().getName(), version.getVersionNo(),
                version.getStatus(), resolved.isPinned()));

        List<GradeComponent> visible = visibleComponents(version, period, subjectId, journal);
        if (visible.isEmpty()) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST,
                    "ამ პერიოდისთვის შაბლონში სვეტები არ არის განსაზღვრული");
        }
        appendColumns(grid, visible, graph);
        appendStudents(grid, enrollments);
        appendCells(grid, enrollments, visible, periodId, subjectId);

        specialValueRegistry.behavioursFor(version.getId()).forEach((code, behaviour) ->
                grid.getSpecialValues().add(new GridSpecialValue(
                        code, specialValueRegistry.labelOf(code), behaviour)));

        grid.setCapabilities(capabilities);
        return grid;
    }

    /**
     * Which columns belong on this screen.
     * <p>
     * Matched on the period's depth rather than its kind. The journal declares
     * how often it is filled in, and that maps to a level of the period tree -
     * kind cannot do the job because months and weeks are both REPORTING, so a
     * monthly journal would have shown its columns on every week as well.
     * <p>
     * A column marked as living at the year is the exception: an annual grade
     * or a rating rolls the year's periods up, so it appears only there.
     */
    private List<GradeComponent> visibleComponents(TemplateVersion version, Period period,
                                                   Long subjectId,
                                                   mthiebi.sgs.gradebook.model.GradingTemplate journal) {
        boolean subjectGrid = subjectId != null;

        // A column belongs on this period when it names this kind of period.
        // Was a binary - year columns on the year, everything else on the
        // journal's own level - which could not put a trimester column on a
        // monthly journal. Three of the brief's four tables want one.
        return gradeComponentRepository.findByTemplateVersion(version.getId()).stream()
                .filter(c -> c.getPeriodKind() == period.getKind())
                .filter(c -> c.isSubjectScoped() == subjectGrid)
                .collect(Collectors.toList());
    }

    private void appendColumns(GradeGrid grid, List<GradeComponent> visible,
                               TemplateGraph graph) {
        boolean formulaExists = gradeConversionService.current() != null;
        Set<String> visibleCodes = visible.stream()
                .map(GradeComponent::getCode).collect(Collectors.toCollection(LinkedHashSet::new));

        Map<String, List<String>> groups = new LinkedHashMap<>();

        for (GradeComponent c : visible) {
            GridColumn column = new GridColumn();
            column.setCode(c.getCode());
            column.setLabel(c.getLabel());
            column.setOrdinal(c.getOrdinal());
            column.setKind(c.getKind());
            column.setGroupLabel(c.getGroupLabel());
            column.setDecimals(c.getDecimals());
            column.setScaleMin(c.getScaleMin());
            column.setScaleMax(c.getScaleMax());
            column.setAllowSpecialValues(c.isAllowSpecialValues());
            column.setAllowOverride(c.isAllowOverride());
            column.setHasConversion(formulaExists);

            // A calculated column is editable only where the template permits
            // it. The default is permissive - a formula is a convenience, not a
            // cage - so locking one is the deliberate exception.
            boolean derived = c.getKind() == ComponentKind.DERIVED;
            column.setEditable(!derived || c.isAllowOverride());

            for (Long dependentId : transitiveDependents(graph, c.getId())) {
                ComponentDef dependent = graph.byId(dependentId);
                if (dependent != null && visibleCodes.contains(dependent.getCode())) {
                    column.getDependents().add(dependent.getCode());
                }
            }

            ComponentDef def = graph.byCode(c.getCode());
            if (derived && def != null && def.getRule() != null) {
                for (Long sourceId : def.getRule().allSourceComponentIds()) {
                    ComponentDef source = graph.byId(sourceId);
                    if (source != null && visibleCodes.contains(source.getCode())
                            && !column.getDependsOn().contains(source.getCode())) {
                        column.getDependsOn().add(source.getCode());
                    }
                }
            }

            grid.getColumns().add(column);

            if (c.getGroupLabel() != null && !c.getGroupLabel().isEmpty()) {
                groups.computeIfAbsent(c.getGroupLabel(), k -> new ArrayList<>()).add(c.getCode());
            }
        }

        groups.forEach((label, codes) ->
                grid.getColumnGroups().add(new GridColumnGroup(label, codes)));
    }

    /**
     * Everything downstream, not just the next hop.
     * <p>
     * Entering an ongoing mark moves the ongoing average, which moves the
     * trimester grade. The console dims what a flush will change, so a
     * direct-dependents list would leave the trimester grade showing a stale
     * number while claiming to be current.
     */
    private Set<Long> transitiveDependents(TemplateGraph graph, Long componentId) {
        Set<Long> reached = new LinkedHashSet<>();
        Deque<Long> queue = new ArrayDeque<>(graph.dependentsOf(componentId));
        while (!queue.isEmpty()) {
            Long next = queue.poll();
            if (reached.add(next)) {
                queue.addAll(graph.dependentsOf(next));
            }
        }
        return reached;
    }

    private void appendStudents(GradeGrid grid, List<Enrollment> enrollments) {
        List<Enrollment> ordered = new ArrayList<>(enrollments);
        ordered.sort((a, b) -> {
            Student x = a.getStudent();
            Student y = b.getStudent();
            int byLast = nullSafe(x.getLastName()).compareTo(nullSafe(y.getLastName()));
            return byLast != 0
                    ? byLast
                    : nullSafe(x.getFirstName()).compareTo(nullSafe(y.getFirstName()));
        });

        int index = 1;
        for (Enrollment e : ordered) {
            Student s = e.getStudent();
            grid.getStudents().add(new GridStudent(e.getId(), s.getId(),
                    s.getFirstName(), s.getLastName(), index++));
        }
    }

    private void appendCells(GradeGrid grid, List<Enrollment> enrollments,
                             List<GradeComponent> visible, Long periodId, Long subjectId) {

        Map<Long, String> codeById = visible.stream()
                .collect(Collectors.toMap(GradeComponent::getId, GradeComponent::getCode));

        // One formula for the school, read once for the whole grid.
        mthiebi.sgs.gradebook.model.ConversionFormula formula = gradeConversionService.current();

        List<Long> enrollmentIds = enrollments.stream()
                .map(Enrollment::getId).collect(Collectors.toList());

        List<GradeEntry> entries = gradeEntryRepository.loadGrid(
                enrollmentIds, Collections.singletonList(periodId), subjectId);

        // One query for the whole grid rather than one per locked cell.
        Set<Long> disputed = pendingRequestEntryIds(entries);

        for (GradeEntry entry : entries) {
            String code = codeById.get(entry.getComponent().getId());
            if (code == null) {
                // Belongs to the period but not to this screen - a student-wide
                // column caught by the subject-less arm of the query.
                continue;
            }
            // A special value is a code, not a number. ჩთ means "not attested";
            // multiplying it is how it would silently become a mark.
            java.math.BigDecimal converted = entry.getSpecialValue() != null ? null
                    : gradeConversionService.convert(entry.getValue(), formula);

            grid.getCells().add(new GridCell(
                    entry.getId(),
                    entry.getEnrollment().getId(), code,
                    entry.getValue(), entry.getSpecialValue(),
                    entry.getSource(), entry.isOverride(), entry.getRowVersion(),
                    entry.isPublished(),
                    entry.getPublishedValue(), entry.getPublishedSpecialValue(),
                    changedSincePublication(entry),
                    disputed.contains(entry.getId()),
                    converted));
        }
    }

    private Set<Long> pendingRequestEntryIds(List<GradeEntry> entries) {
        List<Long> published = entries.stream()
                .filter(GradeEntry::isPublished)
                .map(GradeEntry::getId)
                .collect(Collectors.toList());
        if (published.isEmpty()) {
            return Collections.emptySet();
        }
        return changeRequestRepository.findPendingForEntries(published).stream()
                .map(r -> r.getGradeEntry().getId())
                .collect(Collectors.toSet());
    }

    private boolean changedSincePublication(GradeEntry entry) {
        if (!entry.isPublished()) {
            return false;
        }
        return !equalValues(entry.getValue(), entry.getPublishedValue())
                || !equalStrings(entry.getSpecialValue(), entry.getPublishedSpecialValue());
    }

    private boolean equalValues(java.math.BigDecimal a, java.math.BigDecimal b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.compareTo(b) == 0;
    }

    private boolean equalStrings(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private String nullSafe(String s) {
        return s == null ? "" : s;
    }
}
