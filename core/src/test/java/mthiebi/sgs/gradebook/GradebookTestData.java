package mthiebi.sgs.gradebook;

import mthiebi.sgs.gradebook.model.*;

import javax.persistence.EntityManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds a working gradebook directly through the EntityManager.
 * <p>
 * Shaped like the trimester grid the school uses now - seven ongoing marks, an
 * initial knowledge test, a progress test, a final test and the trimester
 * assessment - so the integration test exercises real proportions rather than
 * a toy.
 * <p>
 * The weights here are a stand-in. The school has not yet told us how the
 * trimester assessment is actually calculated, and since that is configuration
 * rather than code, guessing it in a production seeder would be worse than
 * leaving it to be entered once they answer.
 */
public class GradebookTestData {

    public static final int STUDENT_COUNT = 25;
    public static final int ONGOING_COUNT = 7;

    private final EntityManager em;

    public ClassGroup classGroup;
    public Subject subject;
    public TemplateVersion version;
    public GradingTemplate template;
    public PeriodScheme scheme;
    public Period year;
    public Period trimester1;
    public Period trimester2;
    public List<Enrollment> enrollments = new ArrayList<>();
    public Map<String, GradeComponent> components = new LinkedHashMap<>();

    public GradebookTestData(EntityManager em) {
        this.em = em;
    }

    public GradebookTestData build(String uniqueSuffix) {
        School school = new School();
        school.setCode("BASIC-" + uniqueSuffix);
        school.setName("საბაზო სკოლა");
        school.setOrdinal(2);
        em.persist(school);

        AcademicYear academicYear = new AcademicYear();
        academicYear.setCode("2025-26-" + uniqueSuffix);
        academicYear.setStartsOn(LocalDate.of(2025, 9, 1));
        academicYear.setEndsOn(LocalDate.of(2026, 6, 30));
        academicYear.setCurrent(true);
        em.persist(academicYear);

        scheme = new PeriodScheme();
        scheme.setName("ტრიმესტრები " + uniqueSuffix);
        scheme.setAcademicYear(academicYear);
        em.persist(scheme);

        year = period(scheme, null, "YEAR", "წელი", 0, 0, PeriodKind.YEAR);
        trimester1 = period(scheme, year, "T1", "I ტრიმესტრი", 0, 1, PeriodKind.ROLLUP);
        trimester2 = period(scheme, year, "T2", "II ტრიმესტრი", 1, 1, PeriodKind.ROLLUP);

        classGroup = new ClassGroup();
        classGroup.setSchool(school);
        classGroup.setAcademicYear(academicYear);
        classGroup.setPeriodScheme(scheme);
        classGroup.setLevel((short) 9);
        classGroup.setName("9A-" + uniqueSuffix);
        em.persist(classGroup);

        subject = new Subject();
        subject.setName("მათემატიკა " + uniqueSuffix);
        subject.setShortName("მათ");
        em.persist(subject);

        ClassSubject classSubject = new ClassSubject();
        classSubject.setClassGroup(classGroup);
        classSubject.setSubject(subject);
        classSubject.setSortIndex(1);
        em.persist(classSubject);

        for (int i = 1; i <= STUDENT_COUNT; i++) {
            Student student = new Student();
            student.setFirstName("მოსწავლე" + i);
            student.setLastName("გვარი" + String.format("%02d", i));
            student.setUsername("student-" + uniqueSuffix + "-" + i);
            student.setPasswordHash("{noop}x");
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

        buildTemplate(scheme, uniqueSuffix);
        em.flush();
        return this;
    }

    private void buildTemplate(PeriodScheme scheme, String uniqueSuffix) {
        template = new GradingTemplate();
        template.setName("ტრიმესტრული შეფასება " + uniqueSuffix);
        template.setFrequency(JournalFrequency.TRIMESTER);
        template.setSubjectScoped(true);
        em.persist(template);

        version = new TemplateVersion();
        version.setTemplate(template);
        version.setVersionNo(1);
        version.setStatus(TemplateVersionStatus.ACTIVE);
        version.setPeriodScheme(scheme);
        em.persist(version);

        int ordinal = 0;
        List<GradeComponent> ongoing = new ArrayList<>();
        for (int i = 1; i <= ONGOING_COUNT; i++) {
            GradeComponent c = component("ONGOING_" + i, String.valueOf(i), ordinal++,
                    ComponentKind.INPUT, PeriodKind.ROLLUP, true, 2);
            c.setGroupLabel("მიმდინარე შეფასება");
            ongoing.add(c);
        }

        GradeComponent ongoingAvg = component("ONGOING_AVG", "მიმდინარე საშუალო", ordinal++,
                ComponentKind.DERIVED, PeriodKind.ROLLUP, true, 2);
        GradeComponent initial = component("INITIAL_KNOWLEDGE", "საწყისი ცოდნის ტესტი", ordinal++,
                ComponentKind.INPUT, PeriodKind.ROLLUP, true, 2);
        GradeComponent progress = component("PROGRESS", "პროგრეს ტესტი", ordinal++,
                ComponentKind.INPUT, PeriodKind.ROLLUP, true, 2);
        GradeComponent finalTest = component("FINAL_TEST", "ფინალური ტესტი", ordinal++,
                ComponentKind.INPUT, PeriodKind.ROLLUP, true, 2);
        GradeComponent trimesterGrade = component("TRIMESTER_GRADE", "ტრიმესტრის შეფასება", ordinal++,
                ComponentKind.DERIVED, PeriodKind.ROLLUP, true, 1);
        GradeComponent annual = component("ANNUAL", "წლიური შეფასება", ordinal++,
                ComponentKind.DERIVED, PeriodKind.YEAR, true, 1);
        GradeComponent rating = component("RATING", "რეიტინგი", ordinal++,
                ComponentKind.DERIVED, PeriodKind.YEAR, false, 0);

        // ONGOING_AVG = average of the seven, blanks ignored
        DerivationRule avgRule = rule(ongoingAvg, 0, RuleType.AVERAGE, NullPolicy.IGNORE, false, 2);
        DerivationTerm avgTerm = term(avgRule, 0, BigDecimal.ONE, SourceKind.GROUP,
                ReduceType.AVERAGE, PeriodRef.SAME, null, "მიმდინარე 1-7");
        for (GradeComponent c : ongoing) {
            source(avgTerm, c);
        }

        // TRIMESTER_GRADE = 0.50 ongoing + 0.20 initial + 0.30 final
        DerivationRule trimRule = rule(trimesterGrade, 0, RuleType.WEIGHTED_SUM, NullPolicy.IGNORE, true, 1);
        source(term(trimRule, 0, new BigDecimal("0.50"), SourceKind.COMPONENT,
                ReduceType.FIRST_NON_NULL, PeriodRef.SAME, null, "მიმდინარე საშუალო"), ongoingAvg);
        source(term(trimRule, 1, new BigDecimal("0.20"), SourceKind.COMPONENT,
                ReduceType.FIRST_NON_NULL, PeriodRef.SAME, null, "საწყისი ცოდნის ტესტი"), initial);
        source(term(trimRule, 2, new BigDecimal("0.30"), SourceKind.COMPONENT,
                ReduceType.FIRST_NON_NULL, PeriodRef.SAME, null, "ფინალური ტესტი"), finalTest);

        // ANNUAL = average of the trimesters below the year
        DerivationRule annualRule = rule(annual, 0, RuleType.AVERAGE, NullPolicy.IGNORE, false, 1);
        source(term(annualRule, 0, BigDecimal.ONE, SourceKind.COMPONENT,
                ReduceType.AVERAGE, PeriodRef.CHILDREN, null, "ტრიმესტრები"), trimesterGrade);

        // RATING = the same, but across every subject the student takes
        DerivationRule ratingRule = rule(rating, 0, RuleType.AVERAGE, NullPolicy.IGNORE, false, 0);
        source(term(ratingRule, 0, BigDecimal.ONE, SourceKind.ALL_SUBJECTS,
                ReduceType.AVERAGE, PeriodRef.CHILDREN, null, "ყველა საგანი"), trimesterGrade);

        TemplateAssignment assignment = new TemplateAssignment();
        assignment.setClassGroup(classGroup);
        assignment.setSubject(null);
        assignment.setTemplate(template);
        assignment.setTemplateVersion(version);
        em.persist(assignment);

        // Unused here, but present so the fixture matches a real grid.
        progress.getCode();
    }

    // ------------------------------------------------------------- helpers

    private Period period(PeriodScheme scheme, Period parent, String code, String label,
                          int ordinal, int depth, PeriodKind kind) {
        Period p = new Period();
        p.setScheme(scheme);
        p.setParent(parent);
        p.setCode(code);
        p.setLabel(label);
        p.setOrdinal(ordinal);
        p.setDepth(depth);
        p.setKind(kind);
        em.persist(p);
        return p;
    }

    private GradeComponent component(String code, String label, int ordinal, ComponentKind kind,
                                     PeriodKind periodKind, boolean subjectScoped, int decimals) {
        GradeComponent c = new GradeComponent();
        c.setTemplateVersion(version);
        c.setCode(code);
        c.setLabel(label);
        c.setOrdinal(ordinal);
        c.setKind(kind);
        c.setPeriodKind(periodKind);
        c.setSubjectScoped(subjectScoped);
        c.setDecimals(decimals);
        c.setScaleMin(BigDecimal.ZERO);
        c.setScaleMax(new BigDecimal("10"));
        c.setAllowSpecialValues(true);
        c.setParentVisible(true);
        em.persist(c);
        components.put(code, c);
        return c;
    }

    private DerivationRule rule(GradeComponent component, int chainOrder, RuleType type,
                                NullPolicy nullPolicy, boolean renormalize, int decimals) {
        DerivationRule r = new DerivationRule();
        r.setComponent(component);
        r.setChainOrder(chainOrder);
        r.setType(type);
        r.setNullPolicy(nullPolicy);
        r.setRenormalizeWeights(renormalize);
        r.setRoundingMode(RoundingMode.HALF_UP);
        r.setDecimals(decimals);
        em.persist(r);
        return r;
    }

    private DerivationTerm term(DerivationRule rule, int ordinal, BigDecimal weight, SourceKind kind,
                                ReduceType reduce, PeriodRef ref, Period period, String label) {
        DerivationTerm t = new DerivationTerm();
        t.setRule(rule);
        t.setOrdinal(ordinal);
        t.setWeight(weight);
        t.setSourceKind(kind);
        t.setReduce(reduce);
        t.setPeriodRef(ref);
        t.setPeriod(period);
        t.setLabel(label);
        em.persist(t);
        return t;
    }

    private void source(DerivationTerm term, GradeComponent component) {
        DerivationSource s = new DerivationSource();
        s.setTerm(term);
        s.setComponent(component);
        em.persist(s);
    }

    public List<String> ongoingCodes() {
        List<String> codes = new ArrayList<>();
        for (int i = 1; i <= ONGOING_COUNT; i++) {
            codes.add("ONGOING_" + i);
        }
        return codes;
    }

    public List<Long> enrollmentIds() {
        List<Long> ids = new ArrayList<>();
        enrollments.forEach(e -> ids.add(e.getId()));
        return ids;
    }

    public static List<String> allCodes() {
        return Arrays.asList("ONGOING_AVG", "TRIMESTER_GRADE", "ANNUAL", "RATING");
    }
}
