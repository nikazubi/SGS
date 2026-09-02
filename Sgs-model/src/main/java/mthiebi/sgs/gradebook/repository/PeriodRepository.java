package mthiebi.sgs.gradebook.repository;

import mthiebi.sgs.gradebook.model.Period;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PeriodRepository extends JpaRepository<Period, Long> {

    @Query("select p from Period p where p.scheme.id = :schemeId order by p.depth, p.ordinal")
    List<Period> findByScheme(@Param("schemeId") Long schemeId);

    /**
     * A period's children, in order.
     * <p>
     * The absence register's columns: a month's days, or the year's months.
     */
    @Query("select p from Period p where p.parent.id = :parentId order by p.ordinal")
    List<Period> findChildren(@Param("parentId") Long parentId);
}
