package mthiebi.sgs.gradebook.repository;

import mthiebi.sgs.gradebook.model.AbsenceNotice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface AbsenceNoticeRepository extends JpaRepository<AbsenceNotice, Long> {

    /**
     * The *unresolved* notices for a student and day.
     * <p>
     * A list rather than an Optional: a filtered unique index (db/025) makes a
     * second pending row impossible, but an Optional would throw if one ever
     * appeared - and it would throw *after* the grade write had committed,
     * turning a data anomaly into a 500 on every subsequent mark.
     * <p>
     * Restricted to unsent deliberately. Matching any notice meant that once one
     * had been resolved - especially cancelled, because a mark was withdrawn
     * inside the window - the same student could never be reported absent again
     * on that date: a genuine absence later the same day silently reused the
     * dead row and told nobody.
     */
    @Query("select n from AbsenceNotice n "
            + "where n.enrollment.id = :enrollmentId and n.absenceDate = :date "
            + "and n.sentAt is null order by n.id")
    List<AbsenceNotice> findPending(@Param("enrollmentId") Long enrollmentId,
                                    @Param("date") LocalDate date);

    /**
     * Everything past the coalescing window that has not been resolved.
     * <p>
     * The job re-reads each cell before sending, so this is a candidate list
     * rather than a send list - a mark withdrawn inside the window is still
     * here, and is cancelled rather than sent.
     */
    @Query("select n from AbsenceNotice n "
            + "where n.sentAt is null and n.queuedAt <= :cutoff order by n.queuedAt")
    List<AbsenceNotice> findDue(@Param("cutoff") Instant cutoff);
}
