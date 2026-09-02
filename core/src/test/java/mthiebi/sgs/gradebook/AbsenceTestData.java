package mthiebi.sgs.gradebook;

import mthiebi.sgs.gradebook.model.AcademicYear;
import mthiebi.sgs.gradebook.model.ClassGroup;
import mthiebi.sgs.gradebook.model.ComponentKind;
import mthiebi.sgs.gradebook.model.DailyAbsence;
import mthiebi.sgs.gradebook.model.DerivationRule;
import mthiebi.sgs.gradebook.model.DerivationSource;
import mthiebi.sgs.gradebook.model.DerivationTerm;
import mthiebi.sgs.gradebook.model.Enrollment;
import mthiebi.sgs.gradebook.model.GradeComponent;
import mthiebi.sgs.gradebook.model.GradingTemplate;
import mthiebi.sgs.gradebook.model.GridMode;
import mthiebi.sgs.gradebook.model.JournalFrequency;
import mthiebi.sgs.gradebook.model.NullPolicy;
import mthiebi.sgs.gradebook.model.Period;
import mthiebi.sgs.gradebook.model.PeriodKind;
import mthiebi.sgs.gradebook.model.PeriodRef;
import mthiebi.sgs.gradebook.model.PeriodScheme;
import mthiebi.sgs.gradebook.model.ReduceType;
import mthiebi.sgs.gradebook.model.RuleType;
import mthiebi.sgs.gradebook.model.School;
import mthiebi.sgs.gradebook.model.SourceKind;
import mthiebi.sgs.gradebook.model.Student;
import mthiebi.sgs.gradebook.model.TemplateAssignment;
import mthiebi.sgs.gradebook.model.TemplateVersion;
import mthiebi.sgs.gradebook.model.TemplateVersionStatus;

import javax.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A class, two months, and the two absence registers - which are no longer the
 * same kind of thing.
 *
 * <b>Daily</b> is rows in {@code daily_absence}, keyed by enrollment and date.
 * No journal, no component, and no period beneath the month: the tree stops at
 * depth 2, which is the point of the extraction.
 *
 * <b>Monthly</b> is still a journal - typed academic hours with a yearly total -
 * and it is what keeps DESCENDANTS exercised: the year is two levels above the
 * months, with a trimester in between that holds nothing.
 */
public class AbsenceTestData {

    private static final int STUDENT_COUNT = 5;

    private final EntityManager em;

    public PeriodScheme scheme;
    public ClassGroup classGroup;
    public Period year;
    public Period trimester;
    /**
     * September. Bounded properly: the daily grid cuts its columns from these dates.
     */
    public Period month;
    public final List<Period> months = new ArrayList<>();
    public final List<Enrollment> enrollments = new ArrayList<>();

    public GradingTemplate monthlyJournal;
    public TemplateVersion monthlyVersion;
    public GradeComponent hoursColumn;
    public GradeComponent hoursYearColumn;

    /**
     * Whether the tree is built around today rather than around 2025/26.
     * <p>
     * The fixed year keeps every other assertion stable - column counts, school
     * days, the September/October split. But a guard about "today" cannot be
     * tested against a year that has passed, so one test asks for a tree that
     * contains the current date instead.
     * <p>
     * Only the year and the trimester move. The two months stay at September and
     * October 2025, so under this flag they sit outside their own year - fine
     * for the one test that uses it, which marks daily absence and never touches
     * a month, and not worth branching a fixture twenty other tests depend on.
     * If a second caller ever needs it, move the months too rather than working
     * around the inconsistency.
     */
    private boolean aroundToday;

    public AbsenceTestData(EntityManager em) {
        this.em = em;
    }

    public AbsenceTestData aroundToday() {
        this.aroundToday = true;
        return this;
    }

    public AbsenceTestData build(String suffix) {
        School school = new School();
        school.setCode("PRIMARY-" + suffix);
        school.setName("დაწყებითი სკოლა");
        school.setOrdinal(1);
        em.persist(school);

        LocalDate yearFrom = aroundToday
                ? LocalDate.now().minusMonths(6) : LocalDate.of(2025, 9, 1);
        LocalDate yearTo = aroundToday
                ? LocalDate.now().plusMonths(6) : LocalDate.of(2026, 6, 30);

        AcademicYear academicYear = new AcademicYear();
        // The column is short; the suffix alone keeps it unique per run.
        academicYear.setCode("A" + suffix);
        academicYear.setStartsOn(yearFrom);
        academicYear.setEndsOn(yearTo);
        // Not the current year: the fixture builds its own tree and must not
        // compete with the seeded one for "the" current scheme.
        academicYear.setCurrent(false);
        em.persist(academicYear);

        scheme = new PeriodScheme();
        scheme.setName("გაცდენები " + suffix);
        scheme.setAcademicYear(academicYear);
        em.persist(scheme);

        // Real spans, not one date each. The daily register's columns are the
        // weekdays between a month's own two dates, so a month that started and
        // ended on the same day would produce a one-column grid.
        year = period(null, "YEAR", "წელი", 0, 0, PeriodKind.YEAR, yearFrom, yearTo);
        trimester = period(year, "T1", "I ტრიმესტრი", 0, 1, PeriodKind.ROLLUP,
                yearFrom, aroundToday ? yearTo : LocalDate.of(2025, 12, 31));

        // Two months under the trimester, so "months under the year" has more
        // than one thing to find and the grandchild walk is actually exercised.
        month = period(trimester, "M09", "სექტემბერი", 0, 2, PeriodKind.REPORTING,
                LocalDate.of(2025, 9, 1), LocalDate.of(2025, 9, 30));
        months.add(month);
        months.add(period(trimester, "M10", "ოქტომბერი", 1, 2, PeriodKind.REPORTING,
                LocalDate.of(2025, 10, 1), LocalDate.of(2025, 10, 31)));

        classGroup = new ClassGroup();
        classGroup.setSchool(school);
        classGroup.setAcademicYear(academicYear);
        classGroup.setPeriodScheme(scheme);
        classGroup.setLevel((short) 3);
        classGroup.setName("3A-" + suffix);
        em.persist(classGroup);

        for (int i = 1; i <= STUDENT_COUNT; i++) {
            Student student = new Student();
            student.setFirstName("ბავშვი" + i);
            student.setLastName("გვარი" + String.format("%02d", i));
            student.setUsername("abs-" + suffix + "-" + i);
            student.setPasswordHash("{noop}x");
            student.setGuardianEmail("guardian" + i + "@example.edu.ge");
            student.setActive(true);
            em.persist(student);

            Enrollment enrollment = new Enrollment();
            enrollment.setStudent(student);
            enrollment.setClassGroup(classGroup);
            enrollment.setAcademicYear(academicYear);
            enrollment.setJoinedOn(LocalDate.of(2025, 9, 1));
            em.persist(enrollment);
            enrollments.add(enrollment);
        }

        buildMonthly(suffix);
        em.flush();
        return this;
    }

    /**
     * Academic hours, typed, with a yearly total two levels above them.
     * <p>
     * Independent of the daily register by design - it counts hours where daily
     * counts days, and converting between them would need an hours-per-day
     * figure nobody has.
     * <p>
     * The journal does <b>not</b> lock on publish: hours accumulate through a
     * month and the coordinator republishes as they do, so an approval per
     * top-up would sit on the normal path rather than on an exception to it.
     */
    private void buildMonthly(String suffix) {
        monthlyJournal = journal("გაცდენილი საათები " + suffix, JournalFrequency.MONTH);
        monthlyJournal.setLocksOnPublish(false);
        monthlyVersion = version(monthlyJournal);
        // REPORTING, because that is the kind of period the hours are typed
        // against. It said ROLLUP while period_kind was read as a binary - YEAR
        // or "the journal's own level" - where any non-YEAR value meant the
        // same thing. It now names the level, so saying ROLLUP here would put
        // this column on the trimester and leave the months empty.
        hoursColumn = component(monthlyVersion, "HOURS_MISSED", "გაცდენილი საათები", 0,
                ComponentKind.INPUT, PeriodKind.REPORTING, new BigDecimal("500"));

        // The yearly sum, two levels above the months it adds up - which only
        // DESCENDANTS reaches, because the year's children are trimesters and
        // no hours are ever written on one.
        hoursYearColumn = component(monthlyVersion, "HOURS_YEAR", "წლიური ჯამი", 1,
                ComponentKind.DERIVED, PeriodKind.YEAR, new BigDecimal("5000"));
        hoursYearColumn.setAllowOverride(true);
        sumOfMonths(hoursYearColumn, hoursColumn);

        assign(monthlyJournal, monthlyVersion);
    }

    /**
     * Give the monthly journal a report card: a trimester total, and the year.
     * <p>
     * The register's own columns are deliberately not in it - the summary is
     * the roll-ups, which is what makes it a different view of the same journal
     * rather than the same grid twice.
     */
    public void markSummaryColumns() {
        GradeComponent trimester = component(monthlyVersion, "HOURS_TRIMESTER",
                "ტრიმესტრის ჯამი", 2, ComponentKind.DERIVED, PeriodKind.ROLLUP,
                new BigDecimal("1500"));
        trimester.setSummaryColumn(true);
        hoursYearColumn.setSummaryColumn(true);
        em.flush();
    }

    /**
     * SUM over every descendant at the journal's own level.
     */
    private void sumOfMonths(GradeComponent total, GradeComponent source) {
        DerivationRule rule = new DerivationRule();
        rule.setComponent(total);
        rule.setChainOrder(0);
        rule.setType(RuleType.SUM);
        rule.setNullPolicy(NullPolicy.IGNORE);
        rule.setRenormalizeWeights(false);
        rule.setRoundingMode(java.math.RoundingMode.HALF_UP);
        rule.setDecimals(0);
        em.persist(rule);

        DerivationTerm term = new DerivationTerm();
        term.setRule(rule);
        term.setOrdinal(0);
        term.setWeight(BigDecimal.ONE);
        term.setSourceKind(SourceKind.COMPONENT);
        term.setReduce(ReduceType.SUM);
        term.setPeriodRef(PeriodRef.DESCENDANTS);
        term.setLabel("თვეები");
        em.persist(term);

        DerivationSource src = new DerivationSource();
        src.setTerm(term);
        src.setComponent(source);
        em.persist(src);
    }

    /**
     * A child absent on a day.
     * <p>
     * One row, no value. This used to be a GradeEntry carrying the number 1,
     * against a day period, with a template version and a row version.
     */
    public DailyAbsence absent(Enrollment enrollment, LocalDate date) {
        DailyAbsence absence = new DailyAbsence();
        absence.setEnrollment(enrollment);
        absence.setAbsenceDate(date);
        absence.setMarkedAt(Instant.now());
        absence.setMarkedBy(1L);
        em.persist(absence);
        return absence;
    }

    /**
     * The nth weekday of September 2025, counting from zero.
     */
    public LocalDate schoolDay(int index) {
        LocalDate day = LocalDate.of(2025, 9, 1);
        int seen = 0;
        while (true) {
            if (day.getDayOfWeek().getValue() <= 5) {
                if (seen == index) {
                    return day;
                }
                seen++;
            }
            day = day.plusDays(1);
        }
    }

    private GradingTemplate journal(String name, JournalFrequency frequency) {
        GradingTemplate t = new GradingTemplate();
        t.setUuid(UUID.randomUUID().toString());
        t.setName(name);
        t.setFrequency(frequency);
        // Absence is a class matter, not a subject one.
        t.setSubjectScoped(false);
        t.setGridMode(GridMode.PERIODS);
        em.persist(t);
        return t;
    }

    private TemplateVersion version(GradingTemplate journal) {
        TemplateVersion v = new TemplateVersion();
        v.setTemplate(journal);
        v.setVersionNo(1);
        v.setStatus(TemplateVersionStatus.ACTIVE);
        v.setPeriodScheme(scheme);
        em.persist(v);
        return v;
    }

    private void assign(GradingTemplate journal, TemplateVersion version) {
        TemplateAssignment assignment = new TemplateAssignment();
        assignment.setClassGroup(classGroup);
        assignment.setSubject(null);
        assignment.setTemplate(journal);
        assignment.setTemplateVersion(version);
        em.persist(assignment);
    }

    private GradeComponent component(TemplateVersion version, String code, String label,
                                     int ordinal, ComponentKind kind, PeriodKind periodKind,
                                     BigDecimal scaleMax) {
        GradeComponent c = new GradeComponent();
        c.setTemplateVersion(version);
        c.setCode(code);
        c.setLabel(label);
        c.setOrdinal(ordinal);
        c.setKind(kind);
        c.setPeriodKind(periodKind);
        c.setSubjectScoped(false);
        c.setDecimals(0);
        c.setScaleMin(BigDecimal.ZERO);
        c.setScaleMax(scaleMax);
        c.setAllowSpecialValues(false);
        c.setParentVisible(true);
        em.persist(c);
        return c;
    }

    private Period period(Period parent, String code, String label, int ordinal, int depth,
                          PeriodKind kind, LocalDate from, LocalDate to) {
        Period p = new Period();
        p.setScheme(scheme);
        p.setParent(parent);
        p.setCode(code);
        p.setLabel(label);
        p.setOrdinal(ordinal);
        p.setDepth(depth);
        p.setKind(kind);
        p.setStartsOn(from);
        p.setEndsOn(to);
        em.persist(p);
        return p;
    }
}
