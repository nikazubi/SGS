package mthiebi.sgs.gradebook;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.gradebook.model.AcademicYear;
import mthiebi.sgs.gradebook.model.ClassGroup;
import mthiebi.sgs.gradebook.model.Enrollment;
import mthiebi.sgs.gradebook.model.EnrollmentPlacement;
import mthiebi.sgs.gradebook.model.School;
import mthiebi.sgs.gradebook.model.Student;
import mthiebi.sgs.gradebook.repository.EnrollmentPlacementRepository;
import mthiebi.sgs.db.QueryFactoryProvider;
import mthiebi.sgs.gradebook.service.roster.EnrollmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Moving a child between classes without cutting their year in half.
 * <p>
 * The design under test: one enrollment per student per year, with the class
 * history in enrollment_placement beside it. The alternative - a second
 * enrollment per move - is what these tests exist to make unnecessary, so the
 * one that matters most is {@link #marksSurviveAMove}: it is the whole reason
 * the year is not split.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
// QueryFactoryProvider: the slice picks up the legacy QueryDSL repositories
// too, and they need a JPAQueryFactory to be constructible at all.
@Import({EnrollmentService.class, QueryFactoryProvider.class})
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.SQLServer2012Dialect",
        "spring.jpa.hibernate.ddl-auto=none"
})
class EnrollmentServiceIT {

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private EnrollmentPlacementRepository placements;

    @PersistenceContext
    private EntityManager em;

    private GradebookTestData data;
    private ClassGroup otherClass;
    private Student newcomer;

    private static final LocalDate SEPTEMBER = LocalDate.of(2025, 9, 1);
    private static final LocalDate OCTOBER = LocalDate.of(2025, 10, 14);
    private static final LocalDate JANUARY = LocalDate.of(2026, 1, 15);

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        data = new GradebookTestData(em).build(suffix);

        // A second class in the same year to move between.
        otherClass = new ClassGroup();
        otherClass.setSchool(data.classGroup.getSchool());
        otherClass.setAcademicYear(data.classGroup.getAcademicYear());
        otherClass.setPeriodScheme(data.classGroup.getPeriodScheme());
        otherClass.setLevel((short) 9);
        otherClass.setName("9B-" + suffix);
        em.persist(otherClass);

        // The fixture persists its enrollments directly, so they have no
        // placements. A child enrolled through the service is what this tests.
        newcomer = new Student();
        newcomer.setFirstName("ახალი");
        newcomer.setLastName("მოსწავლე");
        newcomer.setUsername("newcomer-" + suffix);
        newcomer.setPasswordHash("{noop}x");
        newcomer.setActive(true);
        em.persist(newcomer);
        em.flush();
    }

    // ---- enrolling ----------------------------------------------------------

    @Test
    @DisplayName("enrolling opens a placement")
    void enrolOpensAPlacement() throws Exception {
        Enrollment enrollment = enrollmentService.enrol(
                newcomer.getId(), data.classGroup.getId(), SEPTEMBER);

        List<EnrollmentPlacement> history = placements.findHistory(enrollment.getId());
        assertEquals(1, history.size());
        assertEquals(data.classGroup.getId(), history.get(0).getClassGroup().getId());
        assertEquals(SEPTEMBER, history.get(0).getFromDate());
        assertNull(history.get(0).getToDate(), "the current placement is the open one");
    }

    @Test
    @DisplayName("a second enrollment in one year is refused")
    void oneEnrollmentPerYear() throws Exception {
        enrollmentService.enrol(newcomer.getId(), data.classGroup.getId(), SEPTEMBER);

        // The database would refuse it too. Caught in the service so the answer
        // is a sentence rather than a constraint name.
        assertThrows(SGSException.class, () ->
                enrollmentService.enrol(newcomer.getId(), otherClass.getId(), OCTOBER));
    }

    @Test
    @DisplayName("a date outside the academic year is refused")
    void datesOutsideTheYearAreRefused() {
        assertThrows(SGSException.class, () -> enrollmentService.enrol(
                newcomer.getId(), data.classGroup.getId(), LocalDate.of(2024, 5, 1)));
    }

    // ---- moving -------------------------------------------------------------

    @Test
    @DisplayName("moving closes the old placement the day before and opens a new one")
    void movingSplitsTheYearIntoTwoPlacements() throws Exception {
        Enrollment enrollment = enrollmentService.enrol(
                newcomer.getId(), data.classGroup.getId(), SEPTEMBER);

        enrollmentService.move(enrollment.getId(), otherClass.getId(), JANUARY);
        em.flush();
        em.clear();

        List<EnrollmentPlacement> history = placements.findHistory(enrollment.getId());
        assertEquals(2, history.size());

        // Inclusive dates: no day belongs to two classes, and none to neither.
        assertEquals(data.classGroup.getId(), history.get(0).getClassGroup().getId());
        assertEquals(JANUARY.minusDays(1), history.get(0).getToDate());
        assertEquals(otherClass.getId(), history.get(1).getClassGroup().getId());
        assertEquals(JANUARY, history.get(1).getFromDate());
        assertNull(history.get(1).getToDate());

        // And the pointer every other query reads has followed.
        assertEquals(otherClass.getId(),
                em.find(Enrollment.class, enrollment.getId()).getClassGroup().getId());
    }

    @Test
    @DisplayName("the October register still knows where they sat")
    void historyAnswersWhoWasInAClassOnADay() throws Exception {
        Enrollment enrollment = enrollmentService.enrol(
                newcomer.getId(), data.classGroup.getId(), SEPTEMBER);
        enrollmentService.move(enrollment.getId(), otherClass.getId(), JANUARY);
        em.flush();
        em.clear();

        // This is what the whole table is for: a child who left in January was
        // still in the old class in October, and no column on `enrollment` can
        // say so.
        assertTrue(inClassOn(data.classGroup, OCTOBER).contains(enrollment.getId()));
        assertFalse(inClassOn(otherClass, OCTOBER).contains(enrollment.getId()));

        LocalDate march = LocalDate.of(2026, 3, 1);
        assertFalse(inClassOn(data.classGroup, march).contains(enrollment.getId()));
        assertTrue(inClassOn(otherClass, march).contains(enrollment.getId()));
    }

    @Test
    @DisplayName("the boundary days land in the right class")
    void boundariesAreInclusive() throws Exception {
        Enrollment enrollment = enrollmentService.enrol(
                newcomer.getId(), data.classGroup.getId(), SEPTEMBER);
        enrollmentService.move(enrollment.getId(), otherClass.getId(), JANUARY);
        em.flush();
        em.clear();

        assertTrue(inClassOn(data.classGroup, JANUARY.minusDays(1)).contains(enrollment.getId()),
                "the last day in the old class is still in the old class");
        assertTrue(inClassOn(otherClass, JANUARY).contains(enrollment.getId()),
                "the day of the move is the first day in the new class");
    }

    @Test
    @DisplayName("moving to the class they are already in changes nothing")
    void movingNowhereIsSilent() throws Exception {
        Enrollment enrollment = enrollmentService.enrol(
                newcomer.getId(), data.classGroup.getId(), SEPTEMBER);

        // Two people pressing the same button agree; the outcome is the one
        // that was wanted, so this is not an error to report.
        enrollmentService.move(enrollment.getId(), data.classGroup.getId(), JANUARY);
        assertEquals(1, placements.findHistory(enrollment.getId()).size());
    }

    @Test
    @DisplayName("a move dated before the current placement began is refused")
    void backwardsMovesAreRefused() throws Exception {
        Enrollment enrollment = enrollmentService.enrol(
                newcomer.getId(), data.classGroup.getId(), OCTOBER);

        // It would close the placement before it opened, and the day lookup
        // would then match neither class.
        assertThrows(SGSException.class, () ->
                enrollmentService.move(enrollment.getId(), otherClass.getId(), SEPTEMBER));
    }

    @Test
    @DisplayName("a class from another year is refused")
    void crossYearMovesAreRefused() throws Exception {
        Enrollment enrollment = enrollmentService.enrol(
                newcomer.getId(), data.classGroup.getId(), SEPTEMBER);

        assertThrows(SGSException.class, () ->
                enrollmentService.move(enrollment.getId(), nextYearClass().getId(), JANUARY));
    }

    // ---- leaving ------------------------------------------------------------

    @Test
    @DisplayName("leaving closes the placement and takes them off the class list")
    void leavingClosesThePlacement() throws Exception {
        Enrollment enrollment = enrollmentService.enrol(
                newcomer.getId(), data.classGroup.getId(), SEPTEMBER);
        enrollmentService.leave(enrollment.getId(), JANUARY);
        em.flush();
        em.clear();

        List<EnrollmentPlacement> history = placements.findHistory(enrollment.getId());
        assertEquals(1, history.size());
        assertEquals(JANUARY, history.get(0).getToDate());
        assertEquals(JANUARY, em.find(Enrollment.class, enrollment.getId()).getLeftOn());

        assertFalse(inClassOn(data.classGroup, LocalDate.of(2026, 3, 1))
                .contains(enrollment.getId()));
        assertTrue(inClassOn(data.classGroup, OCTOBER).contains(enrollment.getId()),
                "a child who left in January was still there in October");
    }

    @Test
    @DisplayName("a child who has left cannot be moved")
    void leftChildrenCannotMove() throws Exception {
        Enrollment enrollment = enrollmentService.enrol(
                newcomer.getId(), data.classGroup.getId(), SEPTEMBER);
        enrollmentService.leave(enrollment.getId(), OCTOBER);

        assertThrows(SGSException.class, () ->
                enrollmentService.move(enrollment.getId(), otherClass.getId(), JANUARY));
    }

    // ---- the reason for all of it -------------------------------------------

    @Test
    @DisplayName("marks survive a move, because the enrollment does")
    void marksSurviveAMove() throws Exception {
        Enrollment enrollment = enrollmentService.enrol(
                newcomer.getId(), data.classGroup.getId(), SEPTEMBER);
        Long enrollmentId = enrollment.getId();

        mthiebi.sgs.gradebook.model.GradeEntry mark = new mthiebi.sgs.gradebook.model.GradeEntry();
        mark.setEnrollment(enrollment);
        mark.setSubject(data.subject);
        mark.setPeriod(data.trimester1);
        mark.setComponent(data.components.get("ONGOING_1"));
        mark.setTemplateVersion(data.version);
        mark.setValue(new java.math.BigDecimal("8.00"));
        mark.setSource(mthiebi.sgs.gradebook.model.GradeSource.MANUAL);
        em.persist(mark);
        em.flush();

        enrollmentService.move(enrollmentId, otherClass.getId(), JANUARY);
        em.flush();
        em.clear();

        // Nothing had to move with them. This is the difference between this
        // design and a second enrollment per move, where the first trimester
        // would now hang off a row the annual assessment no longer looks at.
        List<mthiebi.sgs.gradebook.model.GradeEntry> after = em.createQuery(
                        "select g from GradeEntry g where g.enrollment.id = :e",
                        mthiebi.sgs.gradebook.model.GradeEntry.class)
                .setParameter("e", enrollmentId)
                .getResultList();

        assertEquals(1, after.size());
        assertNotNull(after.get(0).getValue());
        assertEquals(0, after.get(0).getValue().compareTo(new java.math.BigDecimal("8.00")));
    }

    // ---- helpers ------------------------------------------------------------

    private List<Long> inClassOn(ClassGroup classGroup, LocalDate on) {
        return placements.findInClassOn(classGroup.getId(), on).stream()
                .map(p -> p.getEnrollment().getId())
                .collect(java.util.stream.Collectors.toList());
    }

    private ClassGroup nextYearClass() {
        AcademicYear next = new AcademicYear();
        next.setCode("2026-27-" + UUID.randomUUID().toString().substring(0, 8));
        next.setStartsOn(LocalDate.of(2026, 9, 1));
        next.setEndsOn(LocalDate.of(2027, 6, 30));
        next.setCurrent(false);
        em.persist(next);

        mthiebi.sgs.gradebook.model.PeriodScheme scheme =
                new mthiebi.sgs.gradebook.model.PeriodScheme();
        scheme.setName("ტრიმესტრები");
        scheme.setAcademicYear(next);
        em.persist(scheme);

        School school = data.classGroup.getSchool();
        ClassGroup nextClass = new ClassGroup();
        nextClass.setSchool(school);
        nextClass.setAcademicYear(next);
        nextClass.setPeriodScheme(scheme);
        nextClass.setLevel((short) 10);
        nextClass.setName("10A-" + UUID.randomUUID().toString().substring(0, 8));
        em.persist(nextClass);
        em.flush();
        return nextClass;
    }
}
