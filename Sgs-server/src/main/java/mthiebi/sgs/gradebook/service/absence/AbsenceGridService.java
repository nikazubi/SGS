package mthiebi.sgs.gradebook.service.absence;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.SGSExceptionCode;
import mthiebi.sgs.gradebook.model.ClassPeriodSetting;
import mthiebi.sgs.gradebook.model.Enrollment;
import mthiebi.sgs.gradebook.model.GradeComponent;
import mthiebi.sgs.gradebook.model.GradeEntry;
import mthiebi.sgs.gradebook.model.GradingTemplate;
import mthiebi.sgs.gradebook.model.Period;
import mthiebi.sgs.gradebook.model.PeriodKind;
import mthiebi.sgs.gradebook.model.Student;
import mthiebi.sgs.gradebook.model.TemplateVersion;
import mthiebi.sgs.gradebook.repository.ClassPeriodSettingRepository;
import mthiebi.sgs.gradebook.repository.EnrollmentRepository;
import mthiebi.sgs.gradebook.repository.GradeComponentRepository;
import mthiebi.sgs.gradebook.repository.GradeEntryRepository;
import mthiebi.sgs.gradebook.repository.PeriodRepository;
import mthiebi.sgs.gradebook.engine.PeriodReach;
import mthiebi.sgs.gradebook.service.TemplateVersionResolver;
import mthiebi.sgs.gradebook.service.grid.GridStudent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The monthly absence register: students down, months across.
 * <p>
 * The transpose of the ordinary grid, expressed as a journal property
 * ({@code GridMode.PERIODS}) rather than a hardcoded screen.
 * <p>
 * It served both registers until the daily one moved to its own table. What is
 * left here is the journal-shaped half: academic hours missed, a real number
 * with a scale, a yearly total and a publication. See DailyAbsenceService for
 * the other, which needs none of that.
 * <p>
 * **Read only.** Writes go through GradeWriteService exactly as the ordinary
 * grid's do: it is addressed by cell coordinates and has no opinion about
 * layout, so publication and recomputation work unchanged.
 */
@Service
public class AbsenceGridService {

    @Autowired
    private TemplateVersionResolver templateVersionResolver;

    @Autowired
    private PeriodRepository periodRepository;

    @Autowired
    private mthiebi.sgs.gradebook.service.PeriodTreeLoader periodTreeLoader;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private GradeComponentRepository gradeComponentRepository;

    @Autowired
    private GradeEntryRepository gradeEntryRepository;

    @Autowired
    private ClassPeriodSettingRepository classPeriodSettingRepository;

    @PersistenceContext
    private EntityManager em;

    /**
     * @param parentPeriodId the period whose children become the columns - a
     *                       month for the daily register, the year for the
     *                       monthly one
     */
    @Transactional(readOnly = true)
    public AbsenceGrid grid(Long classGroupId, Long parentPeriodId, String journalUuid,
                            boolean canEdit) throws SGSException {
        return grid(classGroupId, parentPeriodId, journalUuid, canEdit, null, false);
    }

    /**
     * @param subjectId only for a subject-scoped journal; null otherwise
     * @param summary   the report card - every column marked summary_column, at
     *                  whatever level it lives on - rather than the register's
     *                  one column per level
     */
    @Transactional(readOnly = true)
    public AbsenceGrid grid(Long classGroupId, Long parentPeriodId, String journalUuid,
                            boolean canEdit, Long subjectId, boolean summary)
            throws SGSException {

        GradingTemplate journal = templateVersionResolver.journalByUuid(journalUuid);
        Period parent = periodRepository.findById(parentPeriodId)
                .orElseThrow(() -> new SGSException(SGSExceptionCode.BAD_REQUEST,
                        "პერიოდი ვერ მოიძებნა"));

        // Every period under the chosen one, in the order the brief prints them:
        // the reporting periods of a trimester, then that trimester, and the
        // year last. A post-order walk gives exactly that.
        List<Period> subtree = subtreeOf(parent);
        if (subtree.isEmpty()) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST,
                    "ამ პერიოდში ქვეპერიოდები არ არის");
        }

        TemplateVersion version = templateVersionResolver
                .resolve(classGroupId, null, subtree.get(0).getId(), journal.getId())
                .getVersion();

        // The register shows one column per level, because its periods are the
        // columns and a second component per level would need a second row of
        // headers. The summary shows every column marked for it, which is how
        // the report card gets three trimester marks and four year columns in
        // one row.
        Map<PeriodKind, List<GradeComponent>> byLevel = componentsByLevel(version, summary);
        List<ColumnSpec> columns = new ArrayList<>();
        for (Period period : subtree) {
            for (GradeComponent on : byLevel.getOrDefault(period.getKind(),
                    Collections.emptyList())) {
                columns.add(new ColumnSpec(period, on));
            }
        }
        if (columns.isEmpty()) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST,
                    "ამ პერიოდში ჟურნალის დონის ქვეპერიოდები არ არის");
        }

        GradeComponent component = summary ? columns.get(0).component : inputColumnOf(version);
        List<Enrollment> enrollments = enrollmentRepository.findActiveByClassGroup(classGroupId);

        AbsenceGrid grid = new AbsenceGrid();
        grid.setParentPeriodId(parent.getId());
        grid.setParentPeriodLabel(parent.getLabel());
        grid.setComponentCode(component.getCode());
        grid.setComponentLabel(component.getLabel());
        // A one-point scale is a tick and a cross; anything wider is typed.
        grid.setToggle(component.getScaleMax() != null
                && component.getScaleMax().compareTo(BigDecimal.ONE) == 0);
        grid.setCanEdit(canEdit);

        for (ColumnSpec column : columns) {
            Period period = column.period;
            GradeComponent on = column.component;
            grid.getColumns().add(new AbsenceGrid.AbsenceColumn(
                    period.getId(), period.getCode(),
                    // In the summary a column is named for its column, not its
                    // period: "Annual" rather than "Year". In the register the
                    // period is the name - September, not "hours missed".
                    summary ? on.getLabel() : period.getLabel(),
                    period.getStartsOn() == null ? null : period.getStartsOn().toString(),
                    on.getCode(), on.getLabel(), period.getDepth(),
                    on.getKind() == mthiebi.sgs.gradebook.model.ComponentKind.INPUT,
                    on.getDecimals()));
        }

        appendStudents(grid, enrollments);
        appendCells(grid, enrollments, columns, subjectId);
        appendSettings(grid, classGroupId,
                columns.stream().map(c -> c.period).distinct().collect(Collectors.toList()));
        return grid;
    }

    /**
     * The periods that become columns: everything at {@code depth} beneath
     * {@code ancestor}, in order.
     *
     * The reach itself comes from {@link PeriodReach}, which is the only place
     * that walks the tree. This was one of six independent walks; they had to
     * agree and nothing checked that they did.
     */
    /**
     * The chosen period and everything under it, in printing order.
     * <p>
     * Post-order: a trimester's reporting periods, then the trimester, and the
     * year last - which is the order the brief's tables read left to right.
     * Children are taken in ordinal order rather than whatever the map yields,
     * because calendar order is the whole point of the row.
     */
    private List<Period> subtreeOf(Period root) {
        List<Period> all = periodRepository.findByScheme(root.getScheme().getId());
        Map<Long, List<Period>> children = new java.util.LinkedHashMap<>();
        for (Period p : all) {
            if (p.getParent() != null) {
                children.computeIfAbsent(p.getParent().getId(), k -> new ArrayList<>()).add(p);
            }
        }
        children.values().forEach(list ->
                list.sort(java.util.Comparator.comparingInt(Period::getOrdinal)));

        List<Period> ordered = new ArrayList<>();
        walk(root, children, ordered);
        return ordered;
    }

    private void walk(Period node, Map<Long, List<Period>> children, List<Period> into) {
        for (Period child : children.getOrDefault(node.getId(), Collections.emptyList())) {
            walk(child, children, into);
        }
        into.add(node);
    }

    /**
     * The one component to show at each level of the tree.
     * <p>
     * A component names the kind of period it lives on, so this is a grouping
     * rather than a decision - except where a journal has two at the same level,
     * where the first by ordinal wins. See the note at the call site.
     */
    private Map<PeriodKind, List<GradeComponent>> componentsByLevel(
            TemplateVersion version, boolean summary) {

        Map<PeriodKind, List<GradeComponent>> byLevel = new java.util.EnumMap<>(PeriodKind.class);
        gradeComponentRepository.findByTemplateVersion(version.getId()).stream()
                .filter(c -> !summary || c.isSummaryColumn())
                .sorted(java.util.Comparator.comparingInt(GradeComponent::getOrdinal))
                .forEach(c -> byLevel.computeIfAbsent(c.getPeriodKind(),
                        k -> new ArrayList<>()).add(c));

        // The register draws one column per level; a journal with two at the
        // same level would otherwise put both on every period and double its
        // own width.
        if (!summary) {
            byLevel.replaceAll((k, list) -> list.subList(0, 1));
        }
        return byLevel;
    }

    /**
     * One column: the period it sits on, and the value it carries.
     */
    private static final class ColumnSpec {
        private final Period period;
        private final GradeComponent component;

        private ColumnSpec(Period period, GradeComponent component) {
            this.period = period;
            this.component = component;
        }
    }

    private List<Period> columnsUnder(Period ancestor, int depth) {
        Set<Long> wanted = new java.util.LinkedHashSet<>(
                PeriodReach.of(periodTreeLoader.treeOf(ancestor.getScheme().getId()))
                        .atDepth(ancestor.getId(), depth));
        if (wanted.isEmpty()) {
            return Collections.emptyList();
        }
        return periodRepository.findByScheme(ancestor.getScheme().getId()).stream()
                .filter(p -> wanted.contains(p.getId()))
                .sorted(java.util.Comparator.comparingInt(Period::getOrdinal))
                .collect(Collectors.toList());
    }

    /**
     * The one column a person fills in.
     * <p>
     * Both absence journals have a single input and some rollups. The rollups
     * belong to other periods - a trimester total is not a column of the day
     * grid - so the register shows the input and nothing else.
     */
    private GradeComponent inputColumnOf(TemplateVersion version) throws SGSException {
        List<GradeComponent> inputs = gradeComponentRepository
                .findByTemplateVersion(version.getId()).stream()
                .filter(c -> c.getKind() == mthiebi.sgs.gradebook.model.ComponentKind.INPUT)
                .filter(c -> c.getPeriodKind() != mthiebi.sgs.gradebook.model.PeriodKind.YEAR)
                .collect(Collectors.toList());
        if (inputs.isEmpty()) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST,
                    "ჟურნალს შესავსები სვეტი არ აქვს");
        }
        return inputs.get(0);
    }

    private void appendStudents(AbsenceGrid grid, List<Enrollment> enrollments) {
        List<Enrollment> ordered = new ArrayList<>(enrollments);
        ordered.sort((a, b) -> {
            Student x = a.getStudent();
            Student y = b.getStudent();
            int byLast = nullSafe(x.getLastName()).compareTo(nullSafe(y.getLastName()));
            return byLast != 0 ? byLast
                    : nullSafe(x.getFirstName()).compareTo(nullSafe(y.getFirstName()));
        });
        int index = 1;
        for (Enrollment e : ordered) {
            grid.getStudents().add(new GridStudent(e.getId(), e.getStudent().getId(),
                    e.getStudent().getFirstName(), e.getStudent().getLastName(), index++));
        }
    }

    private void appendCells(AbsenceGrid grid, List<Enrollment> enrollments,
                             List<ColumnSpec> columns, Long subjectId) {

        List<Long> enrollmentIds = enrollments.stream()
                .map(Enrollment::getId).collect(Collectors.toList());
        List<Long> periodIds = columns.stream()
                .map(c -> c.period.getId()).distinct().collect(Collectors.toList());

        // One query for the whole grid rather than one per column.
        List<GradeEntry> entries = gradeEntryRepository.loadGrid(
                enrollmentIds, periodIds, subjectId);

        // The pairs this grid draws. A cell on one of these periods for some
        // other column belongs to a different view of the same journal.
        Set<String> drawn = columns.stream()
                .map(c -> c.period.getId() + ":" + c.component.getId())
                .collect(Collectors.toSet());

        Set<Long> disputed = pendingRequestEntryIds(entries);

        for (GradeEntry entry : entries) {
            if (!drawn.contains(entry.getPeriod().getId() + ":"
                    + entry.getComponent().getId())) {
                continue;
            }
            grid.getCells().add(new AbsenceGrid.AbsenceCell(
                    entry.getId(),
                    entry.getEnrollment().getId(),
                    entry.getPeriod().getId(),
                    entry.getComponent().getCode(),
                    entry.getValue(),
                    entry.getRowVersion(),
                    entry.isPublished(),
                    entry.getPublishedValue(),
                    changedSincePublication(entry),
                    disputed.contains(entry.getId())));
        }
    }

    /**
     * Each month's academic hours and permitted absence.
     * <p>
     * Per column, not one pair for the grid. They used to be read against the
     * period the user had chosen - the *year* - so one entry stood for all nine
     * months, and the permitted figure that turns a parent's chart red had the
     * wrong granularity to do it. ClassPeriodSetting was keyed per class per
     * period from phase 1; only the reading was wrong.
     */
    private void appendSettings(AbsenceGrid grid, Long classGroupId, List<Period> columns) {
        List<Long> periodIds = columns.stream().map(Period::getId).collect(Collectors.toList());
        if (periodIds.isEmpty()) {
            return;
        }
        Map<Long, BigDecimal> totals = new HashMap<>();
        Map<Long, BigDecimal> permitted = new HashMap<>();

        for (ClassPeriodSetting setting
                : classPeriodSettingRepository.findForPeriods(classGroupId, periodIds)) {
            Long periodId = setting.getPeriod().getId();
            if (AbsenceSettings.TOTAL_ACADEMIC_HOURS.equals(setting.getSettingKey())) {
                totals.put(periodId, setting.getSettingValue());
            } else if (AbsenceSettings.PERMITTED_MISSED_HOURS.equals(setting.getSettingKey())) {
                permitted.put(periodId, setting.getSettingValue());
            }
        }
        for (Long periodId : periodIds) {
            if (totals.containsKey(periodId) || permitted.containsKey(periodId)) {
                grid.getSettings().add(new AbsenceGrid.PeriodSetting(
                        periodId, totals.get(periodId), permitted.get(periodId)));
            }
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
        return new HashSet<>(em.createQuery(
                        "select r.gradeEntry.id from GradeChangeRequest r "
                                + "where r.status = mthiebi.sgs.gradebook.model.ChangeRequestStatus.PENDING "
                                + "and r.gradeEntry.id in :ids", Long.class)
                .setParameter("ids", published)
                .getResultList());
    }

    private boolean changedSincePublication(GradeEntry entry) {
        if (!entry.isPublished()) {
            return false;
        }
        BigDecimal a = entry.getValue();
        BigDecimal b = entry.getPublishedValue();
        if (a == null || b == null) {
            return a != b;
        }
        return a.compareTo(b) != 0;
    }

    private String nullSafe(String s) {
        return s == null ? "" : s;
    }
}
