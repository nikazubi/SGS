package mthiebi.sgs.gradebook;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.db.QueryFactoryProvider;
import mthiebi.sgs.gradebook.model.AcademicYear;
import mthiebi.sgs.gradebook.model.ClassGroup;
import mthiebi.sgs.gradebook.model.Student;
import mthiebi.sgs.gradebook.service.roster.AcademicYearService;
import mthiebi.sgs.gradebook.service.roster.ClassService;
import mthiebi.sgs.gradebook.service.roster.EnrollmentService;
import mthiebi.sgs.gradebook.service.roster.RosterDraft;
import mthiebi.sgs.gradebook.service.roster.RosterView;
import mthiebi.sgs.gradebook.service.roster.StudentService;
import mthiebi.sgs.gradebook.service.roster.SubjectService;
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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The roster screens' service layer.
 * <p>
 * Weighted towards the identity rules, because they are the ones a person can
 * break by typing, and towards refusing destructive edits, because those are
 * the ones nobody notices until a term's marks have gone.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({StudentService.class, ClassService.class, SubjectService.class,
        AcademicYearService.class, EnrollmentService.class, QueryFactoryProvider.class})
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.SQLServer2012Dialect",
        "spring.jpa.hibernate.ddl-auto=none"
})
class RosterServiceIT {

    @Autowired
    private StudentService students;
    @Autowired
    private ClassService classes;
    @Autowired
    private SubjectService subjects;
    @Autowired
    private AcademicYearService years;

    @PersistenceContext
    private EntityManager em;

    private GradebookTestData data;
    private Long yearId;
    private String suffix;

    @BeforeEach
    void setUp() {
        suffix = UUID.randomUUID().toString().substring(0, 8);
        data = new GradebookTestData(em).build(suffix);
        yearId = data.classGroup.getAcademicYear().getId();
        em.flush();
    }

    // ---- identity ------------------------------------------------------------

    @Test
    @DisplayName("a personal number belongs to one child")
    void personalNumberIsUnique() throws Exception {
        // The same number twice, deliberately - an earlier version of this test
        // generated a fresh one per call and so proved nothing at all.
        String shared = "01001" + digits();
        students.save(draft("ნინო", "ბერიძე", "beridze-" + suffix, "pw1", shared),
                yearId);

        RosterDraft.Student twin = draft("სხვა", "ბავშვი", "other-" + suffix, "pw2", shared);
        SGSException refused = assertThrows(SGSException.class, () -> students.save(twin, yearId));
        assertTrue(refused.getMessage().contains("პირადი ნომერი"));
    }

    @Test
    @DisplayName("siblings may share a username, with different passwords")
    void duplicateUsernamesAreAllowed() throws Exception {
        String shared = "beridze-" + suffix;
        Student nino = students.save(
                draft("ნინო", "ბერიძე", shared, "nino2025", "01001" + digits()), yearId);
        Student mariam = students.save(
                draft("მარიამ", "ბერიძე", shared, "mariam2025", "01001" + digits()), yearId);

        // Not an edge case being tolerated - it is the school's rule, and eight
        // pairs in the live data depend on it.
        assertNotNull(nino.getId());
        assertNotNull(mariam.getId());
        assertEquals(nino.getUsername(), mariam.getUsername());
    }

    @Test
    @DisplayName("the username and password pair may not repeat")
    void theLoginPairIsUnique() throws Exception {
        String shared = "beridze-" + suffix;
        students.save(draft("ნინო", "ბერიძე", shared, "samepw", "01001" + digits()), yearId);

        SGSException refused = assertThrows(SGSException.class, () -> students.save(
                draft("მარიამ", "ბერიძე", shared, "samepw", "01001" + digits()), yearId));

        // The message has to explain the rule: "username taken" would be wrong,
        // and would send somebody off renaming a child who may share.
        assertTrue(refused.getMessage().contains("პაროლი"),
                "the message must say the passwords have to differ, got: " + refused.getMessage());
    }

    @Test
    @DisplayName("a ten-digit personal number is padded, so it cannot become a second child")
    void personalNumbersArePadded() throws Exception {
        Student student = students.save(
                draft("ლუკა", "ჩხეიძე", "luka-" + suffix, "pw", "1617064292"), yearId);
        assertEquals("01617064292", student.getPersonalNumber());
    }

    @Test
    @DisplayName("editing without a password leaves the old one alone")
    void omittingThePasswordKeepsIt() throws Exception {
        Student student = students.save(
                draft("გიორგი", "მაისურაძე", "giorgi-" + suffix, "secret", "01001" + digits()),
                yearId);
        String before = student.getPasswordHash();

        RosterDraft.Student edit = new RosterDraft.Student();
        edit.setId(student.getId());
        edit.setFirstName("გიორგი");
        edit.setLastName("მაისურაძე");
        edit.setUsername("giorgi-" + suffix);
        edit.setPassword(null);
        students.save(edit, yearId);
        em.flush();

        assertEquals(before, em.find(Student.class, student.getId()).getPasswordHash());
    }

    @Test
    @DisplayName("an empty password is refused rather than stored")
    void emptyPasswordsAreRefused() throws Exception {
        Student student = students.save(
                draft("დავით", "ქურდაძე", "davit-" + suffix, "pw", "01001" + digits()), yearId);

        RosterDraft.Student edit = new RosterDraft.Student();
        edit.setId(student.getId());
        edit.setFirstName("დავით");
        edit.setLastName("ქურდაძე");
        edit.setUsername("davit-" + suffix);
        edit.setPassword("   ");
        assertThrows(SGSException.class, () -> students.save(edit, yearId));
    }

    // ---- the class field, which is an enrollment ------------------------------

    @Test
    @DisplayName("saving with a class enrols; saving with another moves")
    void theClassFieldWritesTheEnrollment() throws Exception {
        ClassGroup second = secondClass();

        RosterDraft.Student draft = draft("ანა", "წიკლაური", "ana-" + suffix, "pw",
                "01001" + digits());
        draft.setClassGroupId(data.classGroup.getId());
        draft.setJoinedOn(LocalDate.of(2025, 9, 1));
        Student student = students.save(draft, yearId);

        List<RosterView.StudentRow> rows = students.list(yearId, null, "წიკლაური", false);
        assertEquals(1, rows.size());
        assertEquals(data.classGroup.getId(), rows.get(0).getClassGroupId());

        // Same form, different class: a move, with the history to show for it.
        RosterDraft.Student edit = draft("ანა", "წიკლაური", "ana-" + suffix, null,
                rows.get(0).getPersonalNumber());
        edit.setId(student.getId());
        edit.setClassGroupId(second.getId());
        edit.setJoinedOn(LocalDate.of(2026, 1, 15));
        students.save(edit, yearId);
        em.flush();
        em.clear();

        List<RosterView.PlacementRow> history = students.history(student.getId(), yearId);
        assertEquals(2, history.size());
        assertEquals(LocalDate.of(2026, 1, 14), history.get(0).getToDate());
        assertNull(history.get(1).getToDate());
    }

    @Test
    @DisplayName("a student with no enrollment for the year is still listed")
    void unenrolledStudentsAreListed() throws Exception {
        students.save(draft("უკლასო", "მოსწავლე", "noclass-" + suffix, "pw",
                "01001" + digits()), yearId);

        // A student record outlives any one year, so "no class this year" is a
        // state to show rather than a row to hide.
        List<RosterView.StudentRow> rows = students.list(yearId, null, "უკლასო", false);
        assertEquals(1, rows.size());
        assertNull(rows.get(0).getClassGroupId());
    }

    // ---- classes and subjects -------------------------------------------------

    @Test
    @DisplayName("a class name is unique within its school and year")
    void classNamesAreUniquePerSchoolAndYear() {
        RosterDraft.ClassGroup clash = new RosterDraft.ClassGroup();
        clash.setName(data.classGroup.getName());
        clash.setLevel((short) 9);
        clash.setSchoolId(data.classGroup.getSchool().getId());
        clash.setAcademicYearId(yearId);
        assertThrows(SGSException.class, () -> classes.save(clash));
    }

    @Test
    @DisplayName("a class with children in it is not deleted")
    void populatedClassesAreNotDeleted() {
        // The fixture enrolled 25. Deleting the class would orphan every mark
        // hanging off their enrollments.
        assertThrows(SGSException.class, () -> classes.delete(data.classGroup.getId()));
    }

    @Test
    @DisplayName("subjects are added to a class in order, and reordered")
    void classSubjectsAreOrdered() throws Exception {
        ClassGroup target = secondClass();

        Long maths = addSubject(target, "მათემატიკა-" + suffix, "ირმა გელაშვილი");
        Long georgian = addSubject(target, "ქართული-" + suffix, "ნათელა ქავთარაძე");

        List<RosterView.ClassSubjectRow> rows = classes.subjects(target.getId());
        assertEquals(2, rows.size());
        assertEquals(maths, rows.get(0).getId());
        assertEquals("ირმა გელაშვილი", rows.get(0).getTeacherName());

        classes.reorder(target.getId(), Arrays.asList(georgian, maths));
        em.flush();
        em.clear();

        assertEquals(georgian, classes.subjects(target.getId()).get(0).getId());
    }

    @Test
    @DisplayName("the same subject is not added to a class twice")
    void noDuplicateClassSubjects() throws Exception {
        ClassGroup target = secondClass();
        addSubject(target, "სპორტი-" + suffix, "დავით ქურდაძე");

        RosterDraft.ClassSubject again = new RosterDraft.ClassSubject();
        again.setSubjectId(subjectIdByName("სპორტი-" + suffix));
        assertThrows(SGSException.class, () -> classes.addSubject(target.getId(), again));
    }

    @Test
    @DisplayName("a subject a class still takes is not deleted")
    void usedSubjectsAreNotDeleted() {
        // The legacy page deleted it and left class_subject pointing at nothing.
        assertThrows(SGSException.class, () -> subjects.delete(data.subject.getId()));
    }

    @Test
    @DisplayName("an unused subject is deleted")
    void unusedSubjectsGo() throws Exception {
        RosterDraft.Subject draft = new RosterDraft.Subject();
        draft.setName("გამოუყენებელი-" + suffix);
        Long id = subjects.save(draft).getId();

        subjects.delete(id);
        em.flush();
        assertNull(em.find(mthiebi.sgs.gradebook.model.Subject.class, id));
    }

    // ---- rollover --------------------------------------------------------------

    @Test
    @DisplayName("starting a year copies the period shape and shifts it by a year")
    void rolloverCopiesThePeriodTree() throws Exception {
        // GradebookTestData builds its period tree without dates, which is fine
        // for the engine but leaves nothing here to shift. Dated first, so the
        // assertion is about the copy rather than about the fixture.
        // A subquery, not p.scheme.academicYear.id: HQL turns a nested path in a
        // bulk update into an implicit join and SQL Server rejects the cross
        // apply it generates.
        em.createQuery("update Period p set p.startsOn = :from, p.endsOn = :to "
                        + "where p.depth = 0 and p.scheme.id in "
                        + "(select sc.id from PeriodScheme sc where sc.academicYear.id = :y)")
                .setParameter("from", LocalDate.of(2025, 9, 1))
                .setParameter("to", LocalDate.of(2026, 6, 30))
                .setParameter("y", yearId)
                .executeUpdate();
        em.flush();
        em.clear();

        RosterDraft.NewYear draft = new RosterDraft.NewYear();
        draft.setCode("2026-27-" + suffix);
        draft.setStartsOn(LocalDate.of(2026, 9, 1));
        draft.setEndsOn(LocalDate.of(2027, 6, 30));
        draft.setCopyClassesFromYearId(yearId);

        AcademicYear next = years.startYear(draft);
        em.flush();
        em.clear();

        Long copiedPeriods = em.createQuery(
                        "select count(p) from Period p where p.scheme.academicYear.id = :y", Long.class)
                .setParameter("y", next.getId()).getSingleResult();
        Long sourcePeriods = em.createQuery(
                        "select count(p) from Period p where p.scheme.academicYear.id = :y", Long.class)
                .setParameter("y", yearId).getSingleResult();
        assertEquals(sourcePeriods, copiedPeriods);

        LocalDate copiedStart = em.createQuery(
                        "select p.startsOn from Period p where p.scheme.academicYear.id = :y "
                                + "and p.depth = 0", LocalDate.class)
                .setParameter("y", next.getId()).getSingleResult();
        assertEquals(LocalDate.of(2026, 9, 1), copiedStart,
                "the year period's own dates shift by a year");
    }

    @Test
    @DisplayName("rollover copies classes a level up and enrols nobody")
    void rolloverCopiesClassesButNoChildren() throws Exception {
        RosterDraft.NewYear draft = new RosterDraft.NewYear();
        draft.setCode("2026-27-" + suffix);
        draft.setStartsOn(LocalDate.of(2026, 9, 1));
        draft.setEndsOn(LocalDate.of(2027, 6, 30));
        draft.setCopyClassesFromYearId(yearId);

        AcademicYear next = years.startYear(draft);
        em.flush();
        em.clear();

        List<RosterView.ClassRow> copied = classes.list(next.getId());
        assertFalse(copied.isEmpty());
        assertEquals(data.classGroup.getLevel() + 1, copied.get(0).getLevel());

        // The whole point: the school decides who goes where.
        assertEquals(0, copied.stream().mapToLong(RosterView.ClassRow::getStudentCount).sum());
    }

    @Test
    @DisplayName("only one year is current")
    void oneCurrentYear() throws Exception {
        RosterDraft.NewYear draft = new RosterDraft.NewYear();
        draft.setCode("2026-27-" + suffix);
        draft.setStartsOn(LocalDate.of(2026, 9, 1));
        draft.setEndsOn(LocalDate.of(2027, 6, 30));
        draft.setCopyClassesFromYearId(yearId);
        draft.setMakeCurrent(true);

        AcademicYear next = years.startYear(draft);
        em.flush();
        em.clear();

        Long currentCount = em.createQuery(
                        "select count(y) from AcademicYear y where y.current = true", Long.class)
                .getSingleResult();
        assertEquals(1L, currentCount);
        assertTrue(em.find(AcademicYear.class, next.getId()).isCurrent());
    }

    // ---- helpers ---------------------------------------------------------------

    private RosterDraft.Student draft(String first, String last, String username,
                                      String password, String personalNumber) {
        RosterDraft.Student draft = new RosterDraft.Student();
        draft.setFirstName(first);
        draft.setLastName(last);
        draft.setUsername(username);
        draft.setPassword(password);
        draft.setPersonalNumber(personalNumber);
        draft.setActive(true);
        return draft;
    }

    private ClassGroup secondClass() throws Exception {
        RosterDraft.ClassGroup draft = new RosterDraft.ClassGroup();
        draft.setName("9C-" + suffix);
        draft.setLevel((short) 9);
        draft.setSchoolId(data.classGroup.getSchool().getId());
        draft.setAcademicYearId(yearId);
        return classes.save(draft);
    }

    private Long addSubject(ClassGroup target, String name, String teacher) throws Exception {
        RosterDraft.Subject subject = new RosterDraft.Subject();
        subject.setName(name);
        Long subjectId = subjects.save(subject).getId();

        RosterDraft.ClassSubject draft = new RosterDraft.ClassSubject();
        draft.setSubjectId(subjectId);
        draft.setTeacherName(teacher);
        return classes.addSubject(target.getId(), draft).getId();
    }

    private Long subjectIdByName(String name) {
        return em.createQuery("select s.id from Subject s where s.name = :n", Long.class)
                .setParameter("n", name).getSingleResult();
    }

    /**
     * A personal number that is unique per call without colliding across runs.
     */
    private String digits() {
        return String.format("%06d", Math.abs(UUID.randomUUID().hashCode()) % 1000000);
    }
}
