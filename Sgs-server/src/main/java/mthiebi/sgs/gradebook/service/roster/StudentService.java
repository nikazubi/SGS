package mthiebi.sgs.gradebook.service.roster;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.SGSExceptionCode;
import mthiebi.sgs.gradebook.model.Enrollment;
import mthiebi.sgs.gradebook.model.EnrollmentPlacement;
import mthiebi.sgs.gradebook.model.Student;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The roster: who the children are, and which class each is in this year.
 *
 * <h3>Identity is a pair</h3>
 * <p>
 * Two rules, both enforced here as well as in the database, so that the console
 * can say what is wrong instead of showing a constraint name:
 *
 * <ul>
 *   <li>the personal number identifies one child, when it is given at all;</li>
 *   <li>the <b>(username, password)</b> pair identifies one child.</li>
 * </ul>
 * <p>
 * Duplicate usernames are fine and are not a mistake - siblings share one, and
 * the live data has eight such pairs. It is the combination that must pick out
 * a single row, because the parent portal authenticates on the pair and a
 * second match would mean showing somebody another family's child. That is not
 * hypothetical: the legacy portal looked a student up by username alone.
 *
 * <h3>Passwords</h3>
 * <p>
 * Unsalted uppercase MD5, matching {@code ParentAuthController} - inherited,
 * weak, and deliberately not changed here, because a roster screen is the wrong
 * place to migrate 913 families' credentials. See FOLLOW-UPS.md section 2.
 */
@Service
public class StudentService {

    @Autowired
    private EnrollmentService enrollmentService;

    @PersistenceContext
    private EntityManager em;

    // ---- reading ------------------------------------------------------------

    /**
     * @param academicYearId which year's enrollment to show against each child.
     *                       Required: without it "which class" has no answer.
     */
    @Transactional(readOnly = true)
    public List<RosterView.StudentRow> list(Long academicYearId, Long classGroupId,
                                            String search, boolean includeInactive) {

        StringBuilder jpql = new StringBuilder(
                "select s, e from Student s "
                        + "left join Enrollment e on e.student.id = s.id and e.academicYear.id = :year "
                        + "where 1 = 1 ");
        if (!includeInactive) {
            jpql.append("and s.active = true ");
        }
        if (classGroupId != null) {
            jpql.append("and e.classGroup.id = :class ");
        }
        String term = SubjectService.trimmed(search);
        if (!term.isEmpty()) {
            jpql.append("and (lower(s.firstName) like :q or lower(s.lastName) like :q "
                    + "or s.personalNumber like :raw or lower(s.username) like :q) ");
        }
        jpql.append("order by s.lastName, s.firstName");

        javax.persistence.TypedQuery<Object[]> query =
                em.createQuery(jpql.toString(), Object[].class).setParameter("year", academicYearId);
        if (classGroupId != null) {
            query.setParameter("class", classGroupId);
        }
        if (!term.isEmpty()) {
            query.setParameter("q", "%" + term.toLowerCase() + "%");
            query.setParameter("raw", "%" + term + "%");
        }

        List<RosterView.StudentRow> rows = new ArrayList<>();
        for (Object[] pair : query.getResultList()) {
            Student student = (Student) pair[0];
            Enrollment enrollment = (Enrollment) pair[1];
            rows.add(new RosterView.StudentRow(
                    student.getId(), student.getFirstName(), student.getLastName(),
                    student.getPersonalNumber(), student.getUsername(),
                    student.getGuardianEmail(), student.isActive(),
                    enrollment == null ? null : enrollment.getId(),
                    enrollment == null ? null : enrollment.getClassGroup().getId(),
                    enrollment == null ? null : enrollment.getClassGroup().getName(),
                    enrollment == null ? null : enrollment.getLeftOn()));
        }
        return rows;
    }

    /**
     * Where this child has sat this year, oldest first.
     */
    @Transactional(readOnly = true)
    public List<RosterView.PlacementRow> history(Long studentId, Long academicYearId) {
        List<Enrollment> enrollments = em.createQuery(
                        "select e from Enrollment e where e.student.id = :s "
                                + "and (:y is null or e.academicYear.id = :y)", Enrollment.class)
                .setParameter("s", studentId).setParameter("y", academicYearId)
                .getResultList();

        List<RosterView.PlacementRow> rows = new ArrayList<>();
        for (Enrollment enrollment : enrollments) {
            rows.addAll(enrollmentService.history(enrollment.getId()).stream()
                    .map(this::toRow).collect(Collectors.toList()));
        }
        return rows;
    }

    // ---- writing ------------------------------------------------------------

    @Transactional(rollbackFor = Exception.class)
    public Student save(RosterDraft.Student draft, Long academicYearId) throws SGSException {
        boolean creating = draft.getId() == null;
        Student student = creating ? new Student() : find(draft.getId());

        String firstName = SubjectService.trimmed(draft.getFirstName());
        String lastName = SubjectService.trimmed(draft.getLastName());
        String username = SubjectService.trimmed(draft.getUsername());
        if (firstName.isEmpty() || lastName.isEmpty()) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, "სახელი და გვარი სავალდებულოა");
        }
        if (username.isEmpty()) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, "მომხმარებელი სავალდებულოა");
        }

        String personalNumber = normalisePersonalNumber(draft.getPersonalNumber());
        requirePersonalNumberFree(personalNumber, draft.getId());

        String hash = resolvePassword(draft, student, creating);
        requireLoginFree(username, hash, draft.getId());

        student.setFirstName(firstName);
        student.setLastName(lastName);
        student.setUsername(username);
        student.setPasswordHash(hash);
        student.setPersonalNumber(personalNumber);
        student.setGuardianEmail(SubjectService.blankToNull(draft.getGuardianEmail()));
        if (draft.getActive() != null) {
            student.setActive(draft.getActive());
        }
        em.persist(student);
        em.flush();

        applyClass(student, draft, academicYearId);
        return student;
    }

    /**
     * Deactivated, never deleted.
     * <p>
     * Their marks, absences and homework hang off an enrollment that hangs off
     * this row. A school that wants a child gone wants them off the lists, not
     * their first trimester erased.
     */
    @Transactional(rollbackFor = Exception.class)
    public void deactivate(Long studentId) throws SGSException {
        find(studentId).setActive(false);
        em.flush();
    }

    // ---- the class, which is really the enrollment ---------------------------

    /**
     * The student form shows one field called "class". Behind it is an
     * enrollment for the chosen year, and this is where the two are reconciled:
     * enrol if there is none, move if there is one and it points elsewhere, do
     * nothing if it already agrees.
     * <p>
     * Never writes class_group_id itself - {@link EnrollmentService} owns that,
     * because it is also what keeps the placement history in step.
     */
    private void applyClass(Student student, RosterDraft.Student draft, Long academicYearId)
            throws SGSException {

        if (draft.getClassGroupId() == null || academicYearId == null) {
            return;
        }
        List<Enrollment> existing = em.createQuery(
                        "select e from Enrollment e where e.student.id = :s and e.academicYear.id = :y",
                        Enrollment.class)
                .setParameter("s", student.getId()).setParameter("y", academicYearId)
                .getResultList();

        if (existing.isEmpty()) {
            enrollmentService.enrol(student.getId(), draft.getClassGroupId(), draft.getJoinedOn());
            return;
        }
        Enrollment enrollment = existing.get(0);
        if (!enrollment.getClassGroup().getId().equals(draft.getClassGroupId())) {
            enrollmentService.move(enrollment.getId(), draft.getClassGroupId(), draft.getJoinedOn());
        }
    }

    // ---- the identity rules --------------------------------------------------

    /**
     * Eight legacy numbers had lost a leading zero, so db/015 pads to eleven and
     * this does the same. Otherwise 1617064292 and 01617064292 are two children.
     */
    private String normalisePersonalNumber(String raw) {
        String trimmed = SubjectService.trimmed(raw);
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() == 10 ? "0" + trimmed : trimmed;
    }

    private void requirePersonalNumberFree(String personalNumber, Long selfId)
            throws SGSException {
        if (personalNumber == null) {
            return;
        }
        List<Long> clash = em.createQuery(
                        "select s.id from Student s where s.personalNumber = :p", Long.class)
                .setParameter("p", personalNumber)
                .getResultList();
        clash.remove(selfId);
        if (!clash.isEmpty()) {
            throw new SGSException(SGSExceptionCode.CONFLICT,
                    "ეს პირადი ნომერი სხვა მოსწავლეს უკვე აქვს");
        }
    }

    private void requireLoginFree(String username, String passwordHash, Long selfId)
            throws SGSException {
        List<Long> clash = em.createQuery(
                        "select s.id from Student s where s.username = :u and s.passwordHash = :p",
                        Long.class)
                .setParameter("u", username).setParameter("p", passwordHash)
                .getResultList();
        clash.remove(selfId);
        if (!clash.isEmpty()) {
            // Deliberately explains the rule. "Username taken" would be wrong -
            // it is not - and would send somebody off inventing a second name
            // for a child who is allowed to share their sibling's.
            throw new SGSException(SGSExceptionCode.CONFLICT,
                    "ასეთი მომხმარებლისა და პაროლის წყვილი უკვე გამოიყენება. "
                            + "მომხმარებელი შეიძლება დაემთხვეს, პაროლი კი განსხვავებული უნდა იყოს.");
        }
    }

    private String resolvePassword(RosterDraft.Student draft, Student student, boolean creating)
            throws SGSException {
        if (draft.getPassword() == null) {
            if (creating) {
                throw new SGSException(SGSExceptionCode.BAD_REQUEST, "პაროლი სავალდებულოა");
            }
            // Null means "leave it alone", which is what lets the edit form work
            // without ever showing the current password.
            return student.getPasswordHash();
        }
        String password = draft.getPassword().trim();
        if (password.isEmpty()) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, "პაროლი ცარიელი ვერ იქნება");
        }
        return DigestUtils.md5Hex(password).toUpperCase();
    }

    private RosterView.PlacementRow toRow(EnrollmentPlacement placement) {
        return new RosterView.PlacementRow(
                placement.getClassGroup().getId(), placement.getClassGroup().getName(),
                placement.getFromDate(), placement.getToDate());
    }

    private Student find(Long id) throws SGSException {
        Student student = em.find(Student.class, id);
        if (student == null) {
            throw new SGSException(SGSExceptionCode.NOT_FOUND, "მოსწავლე ვერ მოიძებნა");
        }
        return student;
    }
}
