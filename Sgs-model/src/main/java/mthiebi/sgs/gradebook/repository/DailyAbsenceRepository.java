package mthiebi.sgs.gradebook.repository;

import mthiebi.sgs.gradebook.model.DailyAbsence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Reading and clearing daily absence.
 * <p>
 * Every query here is a date range. That is the point of the table: the same
 * questions used to be a walk down the period tree - "the days beneath this
 * month", "the days beneath this year, three levels past trimesters that hold
 * nothing" - and each caller walked it its own way.
 */
@Repository
public interface DailyAbsenceRepository extends JpaRepository<DailyAbsence, Long> {

    /**
     * A month of the register for a whole class, in one query.
     */
    @Query("select a from DailyAbsence a "
            + "where a.enrollment.id in :enrollmentIds "
            + "  and a.absenceDate between :from and :to "
            + "order by a.absenceDate")
    List<DailyAbsence> findInRange(@Param("enrollmentIds") List<Long> enrollmentIds,
                                   @Param("from") LocalDate from,
                                   @Param("to") LocalDate to);

    /**
     * One cell.
     * <p>
     * A list rather than an Optional even though the unique constraint makes a
     * second row impossible: an Optional would throw if one ever appeared, and
     * it would throw on every subsequent mark rather than on the anomaly.
     */
    @Query("select a from DailyAbsence a "
            + "where a.enrollment.id = :enrollmentId and a.absenceDate = :date")
    List<DailyAbsence> findCell(@Param("enrollmentId") Long enrollmentId,
                                @Param("date") LocalDate date);

    /**
     * Days absent in a range - what the yearly total used to be a rollup for.
     * <p>
     * As a journal this was a DERIVED column summing a DESCENDANTS term three
     * levels down, recomputed on every mark and stored. It is a count.
     */
    @Query("select count(a) from DailyAbsence a "
            + "where a.enrollment.id = :enrollmentId "
            + "  and a.absenceDate between :from and :to")
    long countInRange(@Param("enrollmentId") Long enrollmentId,
                      @Param("from") LocalDate from,
                      @Param("to") LocalDate to);

    @Modifying
    @Query("delete from DailyAbsence a "
            + "where a.enrollment.id = :enrollmentId and a.absenceDate = :date")
    int clearCell(@Param("enrollmentId") Long enrollmentId, @Param("date") LocalDate date);
}
