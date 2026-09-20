package mthiebi.sgs.gradebook.service.parent;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.SGSExceptionCode;
import mthiebi.sgs.gradebook.model.*;
import mthiebi.sgs.gradebook.repository.*;
import mthiebi.sgs.gradebook.service.TemplateVersionResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
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

    /**
     * The one column a register has.
     * <p>
     * A fixed code rather than a component's, because which component fills it
     * changes from row to row - the months carry theirs and the year carries the
     * roll-up. The console addresses cells by column code and does not care
     * which component produced one.
     */
    private static final String VALUE_COLUMN = "VALUE";

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
    @Autowired
    private ParentMenuItemRepository parentMenuItemRepository;

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

    /**
     * The landing page's buttons.
     *
     * <p>Three of the school's five parent screens are one journal - per-subject
     * detail, the summary across subjects and the year view are the trimester
     * journal at different settings - so a button cannot be a journal. It is a
     * {@link ParentMenuItem} row: a journal, a tier of period, one subject or all,
     * a chart, and the school's own wording.
     *
     * <p>Same school rule as {@link #journals}, applied to the view's journal.
     * The two cannot drift because this delegates to it rather than repeating
     * the test.
     *
     * <p><b>A journal with no rows stands in for itself.</b> One button, the
     * journal's name, its own chart, and no opinion about period or subject -
     * which is exactly today's behaviour. So seeding is not a prerequisite: an
     * unseeded database gets the old landing page, a seeded one gets the
     * school's five, and a journal invented next year appears without a
     * migration.
     */
    @Transactional(readOnly = true)
    public List<ParentLandingView> views(Long studentId) throws SGSException {

        Map<String, ParentJournal> eligible = new java.util.LinkedHashMap<>();
        for (ParentJournal journal : journals(studentId)) {
            eligible.put(journal.getUuid(), journal);
        }

        List<ParentLandingView> views = new ArrayList<>();
        java.util.Set<String> configured = new java.util.HashSet<>();

        for (ParentMenuItem row : parentMenuItemRepository.findAllOrdered()) {
            ParentJournal journal = eligible.get(row.getTemplate().getUuid());
            if (journal == null) {
                // The child's school does not get this journal, so it does not
                // get a door to it either.
                continue;
            }
            configured.add(journal.getUuid());
            views.add(new ParentLandingView(
                    row.getLabel(),
                    journal.getUuid(),
                    journal.getName(),
                    row.getPeriodKind() == null ? null : row.getPeriodKind().name(),
                    row.getSubjectMode().name(),
                    row.getChartKey(),
                    row.getChartColumn(),
                    journal.isSubjectScoped()));
        }

        for (ParentJournal journal : eligible.values()) {
            if (configured.contains(journal.getUuid())) {
                continue;
            }
            views.add(new ParentLandingView(
                    journal.getName(), journal.getUuid(), journal.getName(),
                    null, "ALL", journal.getChartKey(), null, journal.isSubjectScoped()));
        }

        return views;
    }

    @Transactional(readOnly = true)
    public ParentView view(Long studentId, String journalUuid, Long periodId,
                           Long subjectId) throws SGSException {
        return view(studentId, journalUuid, periodId, subjectId, null, false);
    }

    /**
     * @param periodKind which tier to open on when the parent has not chosen a
     *                   period yet - the landing page's menu items name one, so
     *                   "the annual view" and "a trimester" are the same journal
     *                   entered at different levels. Ignored once periodId is
     *                   set, which is what the picker sets.
     */
    @Transactional(readOnly = true)
    public ParentView view(Long studentId, String journalUuid, Long periodId,
                           Long subjectId, PeriodKind periodKind) throws SGSException {
        return view(studentId, journalUuid, periodId, subjectId, periodKind, false);
    }

    // ---- per-subject journals: rows are subjects, one period at a time ----

    /**
     * @param summary only the columns marked for a summary, which is what a menu
     *                item listing every subject wants: twelve columns by five
     *                subjects is a grid, not a summary. The school had two
     *                screens over one trimester - one subject with all of its
     *                columns, and every subject with the single mark the rest
     *                add up to - and this is the difference between them.
     */
    @Transactional(readOnly = true)
    public ParentView view(Long studentId, String journalUuid, Long periodId,
                           Long subjectId, PeriodKind periodKind, boolean summary)
            throws SGSException {

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
        view.setTitle(menuLabelFor(journal, periodKind, summary));
        view.setChartKey(journal.getChartKey());
        view.setSubjectScoped(journal.isSubjectScoped());

        List<Period> levelPeriods = periodsAtJournalLevel(enrollment, journal);
        if (levelPeriods.isEmpty()) {
            return view;
        }

        if (journal.isSubjectScoped()) {
            buildSubjectRows(view, enrollment, journal, levelPeriods, periodId, subjectId,
                    periodKind, summary);
        } else {
            buildPeriodRows(view, enrollment, journal, levelPeriods, periodId, periodKind,
                    summary);
        }
        return view;
    }

    // ---- class-wide journals: rows are the periods themselves -------------

    private void buildSubjectRows(ParentView view, Enrollment enrollment, GradingTemplate journal,
                                  List<Period> levelPeriods, Long periodId, Long subjectId,
                                  PeriodKind periodKind, boolean summary)
            throws SGSException {

        levelPeriods.forEach(p -> view.getPeriods().add(new ParentPeriod(p.getId(), p.getLabel())));

        // What the parent picked, else the tier the menu item asked for, else
        // the first. The year sorts first among these - it is depth 0 - so
        // without a tier every trimester journal opens on the annual columns,
        // which is right for one of the school's three buttons and wrong for
        // the other two.
        Period period = levelPeriods.stream()
                .filter(p -> p.getId().equals(periodId))
                .findFirst()
                .orElseGet(() -> levelPeriods.stream()
                        .filter(p -> periodKind != null && p.getKind() == periodKind)
                        .findFirst()
                        .orElse(levelPeriods.get(0)));
        view.setSelectedPeriodId(period.getId());

        TemplateVersion version = templateVersionResolver
                .resolve(enrollment.getClassGroup().getId(), null, period.getId(), journal.getId())
                .getVersion();

        // Summary when the menu item asks for one, and always at the year:
        // the year is a summary by definition and nothing else is drawn there.
        List<SummaryColumn> columns = summary || period.getKind() == PeriodKind.YEAR
                ? summaryColumns(enrollment, version, journal, period)
                : visibleColumns(version, journal, period).stream()
                .map(c -> new SummaryColumn(c, period, false))
                .collect(Collectors.toList());

        columns.forEach(c -> view.getColumns().add(new ParentColumn(
                c.code(), c.label(), c.groupLabel(), c.component.getDecimals())));

        List<ClassSubject> subjects = classGroupRepository
                .findClassSubjectsOf(enrollment.getClassGroup().getId());

        Map<String, GradeEntry> cells = publishedCells(enrollment.getId(),
                columns.stream().map(c -> c.period.getId()).distinct()
                        .collect(Collectors.toList()));

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
            for (SummaryColumn column : columns) {
                row.getValues().put(column.code(), render(
                        cells.get(key(id, column.period.getId(), column.component.getCode())),
                        column.component));
            }
            view.getRows().add(row);
        }
    }

    private void buildPeriodRows(ParentView view, Enrollment enrollment, GradingTemplate journal,
                                 List<Period> levelPeriods, Long periodId,
                                 PeriodKind periodKind, boolean summary) throws SGSException {

        TemplateVersion version = templateVersionResolver
                .resolve(enrollment.getClassGroup().getId(), null,
                        levelPeriods.get(0).getId(), journal.getId())
                .getVersion();

        // One component per tier, because the rows are periods of more than one:
        // the months and, above them, the year. Asking for the first row's
        // columns asked for the year's, and then filled every month from it.
        Map<PeriodKind, GradeComponent> byKind = new java.util.EnumMap<>(PeriodKind.class);
        for (Period period : levelPeriods) {
            for (GradeComponent component : visibleColumns(version, journal, period)) {
                byKind.putIfAbsent(period.getKind(), component);
            }
        }
        if (byKind.isEmpty()) {
            return;
        }

        // A single column. Every row carries one figure and says which period it
        // belongs to, so a column per tier would be one value and one blank on
        // every line. Named for the tier most of the rows are on - the months -
        // falling back to whatever exists for a journal shaped differently.
        GradeComponent naming = byKind.containsKey(PeriodKind.REPORTING)
                ? byKind.get(PeriodKind.REPORTING)
                : byKind.values().iterator().next();
        view.getColumns().add(new ParentColumn(
                VALUE_COLUMN, naming.getLabel(), null, naming.getDecimals()));

        List<Long> periodIds = levelPeriods.stream().map(Period::getId).collect(Collectors.toList());
        Map<String, GradeEntry> cells = publishedCells(enrollment.getId(), periodIds);
        Map<Long, java.math.BigDecimal> thresholds =
                permittedByPeriod(enrollment.getClassGroup().getId(), periodIds);

        // Every period at once, or one at a time with a picker. The register
        // wants the whole year on screen; the ethics screen is one month's mark
        // and has been for as long as the school has had it.
        // The year last. It sorts first because it is depth 0, which says where
        // it is in the tree and nothing about where it should be read: a total
        // belongs at the end of the figures it totals.
        List<Period> ordered = new ArrayList<>(levelPeriods);
        ordered.sort(java.util.Comparator.comparingInt(
                p -> p.getKind() == PeriodKind.YEAR ? 1 : 0));

        List<Period> shown = ordered;
        if (!summary) {
            ordered.forEach(p ->
                    view.getPeriods().add(new ParentPeriod(p.getId(), p.getLabel())));

            Period chosen = ordered.stream()
                    .filter(p -> p.getId().equals(periodId))
                    .findFirst()
                    .orElseGet(() -> ordered.stream()
                            .filter(p -> periodKind != null && p.getKind() == periodKind)
                            .findFirst()
                            .orElse(ordered.get(0)));

            view.setSelectedPeriodId(chosen.getId());
            shown = Collections.singletonList(chosen);
        }

        for (Period period : shown) {
            ParentRow row = new ParentRow();
            row.setLabel(period.getLabel());
            row.setPeriodId(period.getId());
            row.setPeriodKind(period.getKind() == null ? null : period.getKind().name());
            row.setThreshold(thresholds.get(period.getId()));

            GradeComponent component = byKind.get(period.getKind());
            row.getValues().put(VALUE_COLUMN, component == null ? "" : render(
                    cells.get(key(null, period.getId(), component.getCode())), component));

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
     * The words on the button that opens this journal at these settings.
     * <p>
     * Matched on what the link carried rather than on an id, because the link is
     * a starting position and a parent who changes the period picker is still on
     * the screen they opened. Null when nothing matches, and the journal's own
     * name stands.
     */
    private String menuLabelFor(GradingTemplate journal, PeriodKind periodKind, boolean summary) {
        String wanted = summary ? "ALL" : "ONE";
        return parentMenuItemRepository.findAllOrdered().stream()
                .filter(m -> m.getTemplate().getId().equals(journal.getId()))
                .filter(m -> periodKind == null || m.getPeriodKind() == periodKind)
                .filter(m -> m.getSubjectMode().name().equals(wanted)
                        || m.getTemplate().isSubjectScoped() == false)
                .map(ParentMenuItem::getLabel)
                .findFirst()
                .orElse(null);
    }

    /**
     * The report card: every column marked for a summary, at every period of its
     * own tier inside the year.
     * <p>
     * A journal that marks none falls back to the year's own columns, which is
     * what every journal did before db/034 and what the absence register still
     * does.
     */
    private List<SummaryColumn> summaryColumns(Enrollment enrollment, TemplateVersion version,
                                               GradingTemplate journal, Period at) {

        List<GradeComponent> marked = componentRepository.findByTemplateVersion(version.getId())
                .stream()
                .filter(GradeComponent::isParentVisible)
                .filter(c -> c.isSubjectScoped() == journal.isSubjectScoped())
                .filter(GradeComponent::isSummaryColumn)
                .collect(Collectors.toList());

        // A journal that marks none has no summary to show, so it shows what it
        // would have shown anyway - every journal before db/034, and the absence
        // register still.
        if (marked.isEmpty()) {
            return visibleColumns(version, journal, at).stream()
                    .map(c -> new SummaryColumn(c, at, false))
                    .collect(Collectors.toList());
        }

        List<Period> scheme = periodRepository.findByScheme(
                enrollment.getClassGroup().getPeriodScheme().getId());

        List<SummaryColumn> columns = new ArrayList<>();
        for (GradeComponent component : marked) {
            if (component.getPeriodKind() == at.getKind()) {
                // The tier being looked at: one column, right here.
                columns.add(new SummaryColumn(component, at, false));
                continue;
            }
            // A tier below: one column per period of it inside this one. The
            // trimester assessment is one column within a trimester and three
            // across the year, and that follows from this alone.
            for (Period period : scheme) {
                if (period.getKind() == component.getPeriodKind()
                        && within(period, at, scheme)) {
                    columns.add(new SummaryColumn(component, period, true));
                }
            }
        }
        return columns;
    }

    /**
     * Whether a period sits underneath another in the scheme.
     */
    private boolean within(Period period, Period ancestor, List<Period> scheme) {
        Map<Long, Period> byId = new HashMap<>();
        scheme.forEach(p -> byId.put(p.getId(), p));
        Period walk = period;
        while (walk != null) {
            if (walk.getId().equals(ancestor.getId())) {
                return true;
            }
            walk = walk.getParent() == null ? null : byId.get(walk.getParent().getId());
        }
        return false;
    }

    /**
     * One drawn column: a component, and the period it is read at.
     * <p>
     * The pair is the identity, not the component alone - the trimester
     * assessment appears three times in the summary, once per trimester, and
     * they are three columns of one component rather than three components.
     */
    private static final class SummaryColumn {
        private final GradeComponent component;
        private final Period period;
        /**
         * Whether this component supplies more than one column.
         * <p>
         * Not derivable from its tier: the trimester assessment is one column
         * inside a trimester and three across the year, and it is the same
         * component both times. Only the caller knows which it is building.
         */
        private final boolean repeated;

        private SummaryColumn(GradeComponent component, Period period, boolean repeated) {
            this.component = component;
            this.period = period;
            this.repeated = repeated;
        }

        /**
         * Unique per drawn column: the component's own code, qualified by the
         * period only where that code would otherwise appear more than once.
         * The console addresses cells by a single string either way.
         */
        private String code() {
            return repeated ? component.getCode() + "@" + period.getId() : component.getCode();
        }

        /**
         * The period names the column where one component supplies several.
         */
        private String label() {
            return repeated ? period.getLabel() : component.getLabel();
        }

        /**
         * And those several sit together under the component's own name.
         */
        private String groupLabel() {
            return repeated ? component.getLabel() : component.getGroupLabel();
        }
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
