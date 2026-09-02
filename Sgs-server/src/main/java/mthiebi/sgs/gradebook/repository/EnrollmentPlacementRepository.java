package mthiebi.sgs.gradebook.repository;

import mthiebi.sgs.gradebook.model.EnrollmentPlacement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentPlacementRepository extends JpaRepository<EnrollmentPlacement, Long> {

    /**
     * The class this enrollment is in now. Absent only for a child who has left.
     */
    @Query("select p from EnrollmentPlacement p join fetch p.classGroup "
            + "where p.enrollment.id = :enrollmentId and p.toDate is null")
    Optional<EnrollmentPlacement> findOpen(@Param("enrollmentId") Long enrollmentId);

    /**
     * Where they have been, oldest first.
     */
    @Query("select p from EnrollmentPlacement p join fetch p.classGroup "
            + "where p.enrollment.id = :enrollmentId order by p.fromDate")
    List<EnrollmentPlacement> findHistory(@Param("enrollmentId") Long enrollmentId);

    /**
     * Who was in this class on a given day.
     * <p>
     * The date range is inclusive at both ends, which is why the open row is
     * matched separately rather than with a coalesce - a coalesce over a nullable
     * column cannot use ix_placement_class_dates.
     */
    @Query("select p from EnrollmentPlacement p "
            + "join fetch p.enrollment e join fetch e.student "
            + "where p.classGroup.id = :classGroupId "
            + "and p.fromDate <= :on and (p.toDate is null or p.toDate >= :on)")
    List<EnrollmentPlacement> findInClassOn(@Param("classGroupId") Long classGroupId,
                                            @Param("on") LocalDate on);
}
