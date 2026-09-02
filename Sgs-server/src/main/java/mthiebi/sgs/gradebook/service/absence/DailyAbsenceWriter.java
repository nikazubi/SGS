package mthiebi.sgs.gradebook.service.absence;

import mthiebi.sgs.gradebook.model.DailyAbsence;
import mthiebi.sgs.gradebook.model.Enrollment;
import mthiebi.sgs.gradebook.repository.DailyAbsenceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

/**
 * One cell of the register, in its own transaction.
 * <p>
 * A separate bean rather than a method on {@link DailyAbsenceService}, and not
 * for tidiness: Spring's transaction proxy is bypassed by a self-call, so an
 * annotation on a private neighbour would silently do nothing.
 * <p>
 * Why per cell at all. A register is saved a column at a time - thirty children
 * on one day. Two coordinators marking the same child at the same moment is
 * rare but possible, and it violates {@code uq_daily_absence}. Under one
 * transaction for the batch that violation rolls back the whole column: the
 * twenty-nine marks that were fine are lost to a collision on the thirtieth,
 * over a state both requests agreed on.
 * <p>
 * It also has to be a separate transaction, not merely a caught exception. A
 * constraint violation leaves the Hibernate session unusable, so catching it and
 * carrying on inside the same session is not recoverable however wide the catch
 * is - an earlier version of this code caught {@code
 * DataIntegrityViolationException} around a bare {@code save()} and did nothing
 * at all, because a SEQUENCE-generated id means {@code save()} only queues the
 * insert and the violation surfaces later, at flush.
 * <p>
 * REQUIRED rather than REQUIRES_NEW, deliberately, and for the same reason as
 * {@link AbsenceNoticeSender}: the caller is not transactional, so REQUIRED
 * already opens a fresh transaction per call. REQUIRES_NEW would additionally
 * be invisible to a test's uncommitted rows and block on its locks until the
 * suite times out.
 */
@Service
public class DailyAbsenceWriter {

    @Autowired
    private DailyAbsenceRepository dailyAbsenceRepository;

    /**
     * Mark a child absent on a day.
     *
     * @return true if this created the row. False means it was already there,
     * so nothing changed and nobody needs telling a second time.
     */
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public boolean markAbsent(Enrollment enrollment, LocalDate date, Long actorUserId) {
        if (!dailyAbsenceRepository.findCell(enrollment.getId(), date).isEmpty()) {
            return false;
        }
        DailyAbsence absence = new DailyAbsence();
        absence.setEnrollment(enrollment);
        absence.setAbsenceDate(date);
        absence.setMarkedAt(Instant.now());
        absence.setMarkedBy(actorUserId);
        // saveAndFlush, not save: the insert has to reach the database inside
        // this method so that a lost race fails here, in a transaction of its
        // own, rather than at the caller's commit.
        dailyAbsenceRepository.saveAndFlush(absence);
        return true;
    }

    /**
     * Clear a mark. Deleting nothing is success: the child was present either way.
     */
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void clear(Long enrollmentId, LocalDate date) {
        dailyAbsenceRepository.clearCell(enrollmentId, date);
    }
}
