package mthiebi.sgs.gradebook.service.parent;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.SGSExceptionCode;
import mthiebi.sgs.gradebook.model.ClassSubject;
import mthiebi.sgs.gradebook.model.Enrollment;
import mthiebi.sgs.gradebook.model.GradeComponent;
import mthiebi.sgs.gradebook.model.GradeEntry;
import mthiebi.sgs.gradebook.model.GradingTemplate;
import mthiebi.sgs.gradebook.model.Period;
import mthiebi.sgs.gradebook.model.PeriodKind;
import mthiebi.sgs.gradebook.model.TemplateVersion;
import mthiebi.sgs.gradebook.repository.ClassGroupRepository;
import mthiebi.sgs.gradebook.repository.GradeComponentRepository;
import mthiebi.sgs.gradebook.repository.GradeEntryRepository;
import mthiebi.sgs.gradebook.repository.GradingTemplateRepository;
import mthiebi.sgs.gradebook.repository.PeriodRepository;
import mthiebi.sgs.gradebook.service.TemplateVersionResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * What a parent sees.
 * <p>
 * Every value comes from {@code published_value}. A parent never reads the
 * working column - that is the entire point of publication, and taking the
 * working value here would quietly undo the flow the school runs on.
 * <p>
 * One service serves every journal. What a row is comes from the journal's own
 * shape: a per-subject journal has a row per subject within a chosen period, a
 * class-wide one has a row per period. The console then draws a single row as
 * cards and several as a table, because a one-row table is an ugly way to show
 * one thing - so the layout follows from the data rather than from a setting.
 */
@Service
public class ParentViewService {

    @Autowired
    private GradingTemplateRepository journalRepository;

    @Autowired
    private GradeComponentRepository componentRepository;

    @Autowired
    private GradeEntryRepository gradeEntryRepository;

    @Autowired
    private ClassGroupRepository classGroupRepository;

    @Autowired
    private PeriodRepository periodRepository;

    @Autowired
    private TemplateVersionResolver templateVersionResolver;

    @Autowired
    private mthiebi.sgs.gradebook.service.SpecialValueRegistry specialValueRegistry;

    @Autowired
    private mthiebi.sgs.gradebook.repository.ClassPeriodSettingRepository
            classPeriodSettingRepository;

    @PersistenceContext
    private EntityManager em;

    /**
     * The boxes on the landing page: journals the school has released to
     * parents, and that this child's school shows at all.
     * <p>
     * Scoped, which it was not. This returned every parent-visible journal to
     * every parent without ever looking at the student - so a primary-school
     * parent was offered the trimester gradebook, which their school does not
     * grade on.
     * <p>
     * The rule is the brief's: primary school does not grade academically, so
     * its parents get homework, news, meals, the daily schedule, the child's
     * description and absence - and no gradebook.
     * <p>
     * Absence is the subtlety. It is delivered as a journal like any other, so
     * "primary has no grades" cannot be implemented as "primary has no
     * journals" - that was the first version of this method and it silently
     * took the absence register away from the school that most needs it.
     * <p>
     * Keyed on the school rather than on the class or the level, because that is
     * the axis the brief uses and the one the data already carries. Levels move -
     * the school is adding grades 1 to 4 - and nothing here has to change when
     * they do.
     */
    @Transactional(readOnly = true)
    public List<ParentJournal> journals(Long studentId) throws SGSException {
        boolean primary = PRIMARY.equals(schoolCodeOf(studentId));

        return journalRepository.findActive().stream()
                .filter(GradingTemplate::isParentVisible)
                .filter(j -> !primary || isRegister(j))
                .map(j -> new ParentJournal(j.getUuid(), j.getName(), j.getDescription(),
                        j.getFrequency(), j.isSubjectScoped(), j.getChartKey()))
                .collect(Collectors.toList());
    }

    /**
     * A register rather than a gradebook.
     * <p>
     * Read off the grid shape, which is the only thing today that separates the
     * two: PERIODS is the transposed grid - students down, periods across - and
     * it exists for registers. No gradebook uses it.
     * <p>
     * A proxy, and worth knowing it is one. It holds while "transposed" and
     * "not academic" mean the same thing; a transposed *gradebook* would arrive
     * on the primary landing page uninvited. That fails visibly - a box appears
     * where none should - rather than silently, which is why this is preferred
     * to a fourth boolean on the template that the journal editor cannot set
     * and that somebody would have to remember to tick.
     */
    private boolean isRegister(GradingTemplate journal) {
        return journal.getGridMode() == mthiebi.sgs.gradebook.model.GridMode.PERIODS;
    }

    /**
     * db/006 seeds exactly three, and this is the one that differs.
     */
    private static final String PRIMARY = "PRIMARY";

    private String schoolCodeOf(Long studentId) throws SGSException {
        return enrollmentOf(studentId).getClassGroup().getSchool().getCode();
    }

    @Transactional(readOnly = true)
    public ParentView view(Long studentId, String journalUuid, Long periodId,
                           Long subjectId) throws SGSException {

        Enrollment enrollment = enrollmentOf(studentId);
        GradingTemplate journal = journalRepository.findByUuid(journalUuid)
                .orElseThrow(() -> new SGSException(SGSExceptionCode.BAD_REQUEST,
                        "ჟურნალი ვერ მოიძებნა"));

        // Checked here rather than trusted from the request: a uuid is easy to
        // guess at and a journal the school has not released is staff-only.
        if (!journal.isParentVisible() || journal.isArchived()) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, "ჟურნალი ხელმისაწვდომი არ არის");
        }

        ParentView view = new ParentView();
        view.setJournalName(journal.getName());
        view.setChartKey(journal.getChartKey());
        view.setSubjectScoped(journal.isSubjectScoped());

        List<Period> levelPeriods = periodsAtJournalLevel(enrollment, journal);
        if (levelPeriods.isEmpty()) {
            return view;
        }

        if (journal.isSubjectScoped()) {
            buildSubjectRows(view, enrollment, journal, levelPeriods, periodId, subjectId);
        } else {
            buildPeriodRows(view, enrollment, journal, levelPeriods);
        }
        return view;
    }

    // ---- per-subject journals: rows are subjects, one period at a time ----

    private void buildSubjectRows(ParentView view, Enrollment enrollment, GradingTemplate journal,
                                  List<Period> levelPeriods, Long periodId, Long subjectId)
            throws SGSException {

        levelPeriods.forEach(p -> view.getPeriods().add(new ParentPeriod(p.getId(), p.getLabel())));

        Period period = levelPeriods.stream()
                .filter(p -> p.getId().equals(periodId))
                .findFirst()
                .orElse(levelPeriods.get(0));
        view.setSelectedPeriodId(period.getId());

        TemplateVersion version = templateVersionResolver
                .resolve(enrollment.getClassGroup().getId(), null, period.getId(), journal.getId())
                .getVersion();

        List<GradeComponent> columns = visibleColumns(version, journal, period);
        columns.forEach(c -> view.getColumns().add(toColumn(c)));

        List<ClassSubject> subjects = classGroupRepository
                .findClassSubjectsOf(enrollment.getClassGroup().getId());

        Map<String, GradeEntry> cells = publishedCells(
                enrollment.getId(), Collections.singletonList(period.getId()));

        for (ClassSubject classSubject : subjects) {
            Long id = classSubject.getSubject().getId();
            // Drilling into one subject is the same view filtered to one row -
            // which is what turns it into cards.
            if (subjectId != null && !subjectId.equals(id)) {
                continue;
            }
            ParentRow row = new ParentRow();
            row.setLabel(classSubject.getSubject().getName());
            row.setSubjectId(id);
            row.setPeriodId(period.getId());
            fill(row, columns, cells, id, period.getId());
            view.getRows().add(row);
        }
    }

    // ---- class-wide journals: rows are the periods themselves -------------

    private void buildPeriodRows(ParentView view, Enrollment enrollment, GradingTemplate journal,
                                 List<Period> levelPeriods) throws SGSException {

        TemplateVersion version = templateVersionResolver
                .resolve(enrollment.getClassGroup().getId(), null,
                        levelPeriods.get(0).getId(), journal.getId())
                .getVersion();

        List<GradeComponent> columns = visibleColumns(version, journal, levelPeriods.get(0));
        columns.forEach(c -> view.getColumns().add(toColumn(c)));

        List<Long> periodIds = levelPeriods.stream().map(Period::getId).collect(Collectors.toList());
        Map<String, GradeEntry> cells = publishedCells(enrollment.getId(), periodIds);
        Map<Long, java.math.BigDecimal> thresholds =
                permittedByPeriod(enrollment.getClassGroup().getId(), periodIds);

        for (Period period : levelPeriods) {
            ParentRow row = new ParentRow();
            row.setLabel(period.getLabel());
            row.setPeriodId(period.getId());
            row.setThreshold(thresholds.get(period.getId()));
            fill(row, columns, cells, null, period.getId());
            view.getRows().add(row);
        }
    }

    /**
     * The permitted-missed-hours figure, per period.
     * <p>
     * The brief's one visual rule: the absence diagram is green until the child
     * passes the allowance, then red. The allowance is entered by the
     * coordinator per class per month, so it is looked up per row rather than
     * once for the grid - a child may be inside September's allowance and past
     * October's, and one number for the year could not say so.
     * <p>
     * Empty for every other journal, because no other journal has a ceiling.
     * The chart colours only where it finds one.
     */
    private Map<Long, java.math.BigDecimal> permittedByPeriod(Long classGroupId,
                                                              List<Long> periodIds) {
        Map<Long, java.math.BigDecimal> byPeriod = new java.util.HashMap<>();
        if (periodIds.isEmpty()) {
            return byPeriod;
        }
        for (mthiebi.sgs.gradebook.model.ClassPeriodSetting setting
                : classPeriodSettingRepository.findForPeriods(classGroupId, periodIds)) {
            if (mthiebi.sgs.gradebook.service.absence.AbsenceSettings.PERMITTED_MISSED_HOURS
                    .equals(setting.getSettingKey())) {
                byPeriod.put(setting.getPeriod().getId(), setting.getSettingValue());
            }
        }
        return byPeriod;
    }

    // ---- shared -----------------------------------------------------------

    /**
     * Every column is listed, whether or not it holds a value.
     * <p>
     * A trimester in progress has empty columns, and showing them tells a parent
     * what is still to come rather than hiding the fact that anything is
     * missing.
     */
    private void fill(ParentRow row, List<GradeComponent> columns,
                      Map<String, GradeEntry> cells, Long subjectId, Long periodId) {
        for (GradeComponent column : columns) {
            GradeEntry entry = cells.get(key(subjectId, periodId, column.getCode()));
            row.getValues().put(column.getCode(), render(entry, column));
        }
    }

    /**
     * Only what has been published, and only for this student.
     * <p>
     * The published column, never the working one - a cell edited since the last
     * release still shows a parent what they were last shown.
     */
    private Map<String, GradeEntry> publishedCells(Long enrollmentId, List<Long> periodIds) {
        Map<String, GradeEntry> byKey = new HashMap<>();
        for (GradeEntry entry : gradeEntryRepository.loadPublishedForStudent(
                enrollmentId, periodIds)) {
            byKey.put(key(entry.getSubject() == null ? null : entry.getSubject().getId(),
                    entry.getPeriod().getId(), entry.getComponent().getCode()), entry);
        }
        return byKey;
    }

    private String render(GradeEntry entry, GradeComponent column) {
        if (entry == null) {
            return "";
        }
        if (entry.getPublishedSpecialValue() != null) {
            // The stored form is a code (CHT); a parent reads ჩთ.
            return specialValueRegistry.labelOf(entry.getPublishedSpecialValue());
        }
        BigDecimal value = entry.getPublishedValue();
        if (value == null) {
            return "";
        }
        // Never converted. The converted scale exists so the school can report
        // to the government out of 10; a parent reads the mark the school
        // actually gave, on the scale the school actually grades on.
        return value.setScale(column.getDecimals(), RoundingMode.HALF_UP).toPlainString();
    }

    /**
     * Columns a parent may see.
     * <p>
     * {@code parentVisible} has existed on a component since phase 1 and was
     * read by nothing, so a column marked staff-only was shown anyway. A journal
     * can be released while an internal working column inside it is not.
     */
    private List<GradeComponent> visibleColumns(TemplateVersion version, GradingTemplate journal,
                                                Period period) {
        // Matched on the kind of period the column names, the same way the
        // staff grid matches, so a parent sees the columns the school entered.
        return componentRepository.findByTemplateVersion(version.getId()).stream()
                .filter(GradeComponent::isParentVisible)
                .filter(c -> c.isSubjectScoped() == journal.isSubjectScoped())
                .filter(c -> c.getPeriodKind() == period.getKind())
                .collect(Collectors.toList());
    }

    /**
     * The journal's own level, plus the year when it has columns that roll up
     * there.
     * <p>
     * Without the year the annual grade is published and then invisible - the
     * legacy portal's "ტრიმესტრული და წლიური" view would be unreachable for
     * every trimester journal. The staff picker already includes it.
     */
    private List<Period> periodsAtJournalLevel(Enrollment enrollment, GradingTemplate journal) {
        int depth = journal.getFrequency().getDepth();
        boolean hasYearRollup = hasYearLevelColumns(enrollment, journal);
        return periodRepository.findByScheme(enrollment.getClassGroup().getPeriodScheme().getId())
                .stream()
                .filter(p -> p.getDepth() == depth || (hasYearRollup && p.getDepth() == 0))
                .collect(Collectors.toList());
    }

    private boolean hasYearLevelColumns(Enrollment enrollment, GradingTemplate journal) {
        try {
            TemplateVersion version = templateVersionResolver.resolve(
                    enrollment.getClassGroup().getId(), null,
                    periodRepository.findByScheme(
                                    enrollment.getClassGroup().getPeriodScheme().getId()).stream()
                            .filter(p -> p.getDepth() == 0).findFirst()
                            .map(Period::getId).orElse(null),
                    journal.getId()).getVersion();
            return componentRepository.findByTemplateVersion(version.getId()).stream()
                    .anyMatch(c -> c.getPeriodKind() == PeriodKind.YEAR && c.isParentVisible());
        } catch (Exception e) {
            return false;
        }
    }

    private ParentColumn toColumn(GradeComponent c) {
        return new ParentColumn(c.getCode(), c.getLabel(), c.getGroupLabel(), c.getDecimals());
    }

    private String key(Long subjectId, Long periodId, String code) {
        return subjectId + ":" + periodId + ":" + code;
    }

    /**
     * By id, never by username.
     * <p>
     * Two children may share a username - only the pair with the password is
     * unique - so looking a student up by name and taking the first match would
     * quietly serve one family another's child.
     */
    private Enrollment enrollmentOf(Long studentId) throws SGSException {
        List<Enrollment> found = em.createQuery(
                        "select e from Enrollment e "
                                + "join fetch e.classGroup c join fetch c.periodScheme "
                                + "join fetch e.student s "
                                + "where s.id = :id and e.academicYear.current = true",
                        Enrollment.class)
                .setParameter("id", studentId)
                .getResultList();
        if (found.isEmpty()) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, "მოსწავლე ვერ მოიძებნა");
        }
        return found.get(0);
    }

    private List<ParentJournal> emptyJournals() {
        return new ArrayList<>();
    }
}
