package mthiebi.sgs.gradebook.repository;

import mthiebi.sgs.gradebook.model.GradingTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GradingTemplateRepository extends JpaRepository<GradingTemplate, Long> {

    /**
     * The menu, in order. Archived journals are kept but not shown.
     */
    @Query("select t from GradingTemplate t where t.archived = false order by t.sortIndex, t.name")
    List<GradingTemplate> findActive();

    /**
     * Including archived, for the index page's restore action.
     */
    @Query("select t from GradingTemplate t order by t.archived, t.sortIndex, t.name")
    List<GradingTemplate> findAllOrdered();

    Optional<GradingTemplate> findByUuid(String uuid);

    @Query("select coalesce(max(t.sortIndex), -1) from GradingTemplate t")
    int maxSortIndex();
}
