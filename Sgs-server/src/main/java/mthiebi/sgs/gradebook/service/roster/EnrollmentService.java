package mthiebi.sgs.gradebook.service.roster;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.SGSExceptionCode;
import mthiebi.sgs.gradebook.model.AcademicYear;
import mthiebi.sgs.gradebook.model.ClassGroup;
import mthiebi.sgs.gradebook.model.Enrollment;
import mthiebi.sgs.gradebook.model.EnrollmentPlacement;
import mthiebi.sgs.gradebook.model.Student;
import mthiebi.sgs.gradebook.repository.ClassGroupRepository;
import mthiebi.sgs.gradebook.repository.EnrollmentPlacementRepository;
import mthiebi.sgs.gradebook.repository.EnrollmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.List;

/**
 * Placing a child in a class, moving them, and taking them off the roll.
 *
 * <h3>The one rule this class exists to hold</h3>
 * <p>
 * A child's current class is recorded in two places - {@code enrollment
 * .class_group_id}, which every existing query reads, and the open
 * {@link EnrollmentPlacement}, which is what makes the history answerable. They
 * are allowed to be two only because <b>nothing else assigns either of them</b>.
 * Every path that changes where a child sits goes through {@link #move}, and
 * every path that creates one goes through {@link #enrol}.
 * <p>
 * If a second writer ever appears, the duplication stops being a design and
 * starts being a bug - so it is worth saying plainly: this is the writer.
 *
 * <h3>Dates are inclusive</h3>
 * <p>
 * A placement runs from {@code fromDate} to {@code toDate} and the child was in
 * that class on both of those days. Moving on the 15th therefore closes the old
 * placement on the 14th; there is no day belonging to two classes and no gap
 * between them. Half-open ranges would avoid the arithmetic and read worse on a
 * screen the school actually looks at - "5ა, 1 Sep to 14 Jan" is what they say
 * out loud.
 */
@Service
public class EnrollmentService {

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private EnrollmentPlacementRepository placementRepository;

    @Autowired
    private ClassGroupRepository classGroupRepository;

    @PersistenceContext
    private EntityManager em;

    /**
     * Put a child in a class for a year.
     * <p>
     * The year comes from the class rather than the caller: a class belongs to
     * exactly one academic year, so asking for both invites them to disagree.
     */
    @Transactional(rollbackFor = Exception.class)
    public Enrollment enrol(Long studentId, Long classGroupId, LocalDate joinedOn)
            throws SGSException {

        ClassGroup classGroup = classGroup(classGroupId);
        AcademicYear year = classGroup.getAcademicYear();
        LocalDate from = joinedOn != null ? joinedOn : year.getStartsOn();

        requireWithinYear(year, from);

        // uq_enrollment_student_year makes this impossible to get past anyway;
        // caught here so the answer is a sentence rather than a constraint name.
        if (!existing(studentId, year.getId()).isEmpty()) {
            throw new SGSException(SGSExceptionCode.CONFLICT,
                    "მოსწავლე უკვე ჩარიცხულია ამ სასწავლო წელს");
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(em.getReference(Student.class, studentId));
        enrollment.setClassGroup(classGroup);
        enrollment.setAcademicYear(year);
        enrollment.setJoinedOn(from);
        enrollmentRepository.saveAndFlush(enrollment);

        openPlacement(enrollment, classGroup, from);
        return enrollment;
    }

    /**
     * Move a child to another class, keeping the year whole.
     * <p>
     * Their marks do not move and do not need to: they hang off the enrollment,
     * which is the same row before and after. What changes is where the child
     * sits, and the register for October still knows they sat somewhere else.
     */
    @Transactional(rollbackFor = Exception.class)
    public void move(Long enrollmentId, Long toClassGroupId, LocalDate on) throws SGSException {
        Enrollment enrollment = enrollment(enrollmentId);
        ClassGroup target = classGroup(toClassGroupId);

        if (!target.getAcademicYear().getId().equals(enrollment.getAcademicYear().getId())) {
            // Otherwise the enrollment's year and its class's year disagree, and
            // every screen that joins through one of them shows a different answer.
            throw new SGSException(SGSExceptionCode.BAD_REQUEST,
                    "კლასი სხვა სასწავლო წელს ეკუთვნის");
        }
        if (enrollment.getLeftOn() != null) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST,
                    "მოსწავლე სკოლიდან გასულია");
        }

        LocalDate moveDate = on != null ? on : LocalDate.now();
        requireWithinYear(enrollment.getAcademicYear(), moveDate);

        EnrollmentPlacement open = placementRepository.findOpen(enrollmentId)
                .orElseThrow(() -> new SGSException(SGSExceptionCode.INTERNAL_SEVER_ERROR,
                        "ჩარიცხვას მიმდინარე კლასი არ აქვს"));

        if (open.getClassGroup().getId().equals(toClassGroupId)) {
            // Already there. Silent rather than an error: two people pressing
            // the same button agree, and the outcome is the one that was wanted.
            return;
        }
        if (!moveDate.isAfter(open.getFromDate())) {
            // A placement that ends before it begins is not a record of
            // anything, and it would break the "which class on day D" lookup.
            throw new SGSException(SGSExceptionCode.BAD_REQUEST,
                    "გადაყვანის თარიღი წინა კლასში ჩარიცხვის შემდეგ უნდა იყოს");
        }

        open.setToDate(moveDate.minusDays(1));
        placementRepository.saveAndFlush(open);

        openPlacement(enrollment, target, moveDate);

        enrollment.setClassGroup(target);
        enrollmentRepository.saveAndFlush(enrollment);
    }

    /**
     * Off the roll.
     * <p>
     * The enrollment stays, and so does everything hanging off it - a child who
     * leaves in March still has a first trimester. Only the placement closes and
     * leftOn is set, which is what takes them out of the class lists.
     */
    @Transactional(rollbackFor = Exception.class)
    public void leave(Long enrollmentId, LocalDate on) throws SGSException {
        Enrollment enrollment = enrollment(enrollmentId);
        LocalDate date = on != null ? on : LocalDate.now();
        requireWithinYear(enrollment.getAcademicYear(), date);

        enrollment.setLeftOn(date);
        enrollmentRepository.saveAndFlush(enrollment);

        placementRepository.findOpen(enrollmentId).ifPresent(open -> {
            open.setToDate(date);
            placementRepository.saveAndFlush(open);
        });
    }

    /**
     * Where this child has been this year, oldest first.
     */
    @Transactional(readOnly = true)
    public List<EnrollmentPlacement> history(Long enrollmentId) {
        return placementRepository.findHistory(enrollmentId);
    }

    // ---- helpers ------------------------------------------------------------

    private void openPlacement(Enrollment enrollment, ClassGroup classGroup, LocalDate from) {
        EnrollmentPlacement placement = new EnrollmentPlacement();
        placement.setEnrollment(enrollment);
        placement.setClassGroup(classGroup);
        placement.setFromDate(from);
        placementRepository.saveAndFlush(placement);
    }

    /**
     * A date outside the year is a typo, and one that would be invisible: the
     * placement would be written, indexed, and matched by no screen that shows a
     * month.
     */
    private void requireWithinYear(AcademicYear year, LocalDate date) throws SGSException {
        if (date.isBefore(year.getStartsOn()) || date.isAfter(year.getEndsOn())) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST,
                    "თარიღი სასწავლო წლის ფარგლებს გარეთაა");
        }
    }

    private List<Enrollment> existing(Long studentId, Long yearId) {
        return em.createQuery(
                        "select e from Enrollment e "
                                + "where e.student.id = :s and e.academicYear.id = :y", Enrollment.class)
                .setParameter("s", studentId)
                .setParameter("y", yearId)
                .getResultList();
    }

    private Enrollment enrollment(Long id) throws SGSException {
        return enrollmentRepository.findById(id)
                .orElseThrow(() -> new SGSException(SGSExceptionCode.NOT_FOUND,
                        "ჩარიცხვა ვერ მოიძებნა"));
    }

    private ClassGroup classGroup(Long id) throws SGSException {
        return classGroupRepository.findById(id)
                .orElseThrow(() -> new SGSException(SGSExceptionCode.NOT_FOUND,
                        "კლასი ვერ მოიძებნა"));
    }
}
