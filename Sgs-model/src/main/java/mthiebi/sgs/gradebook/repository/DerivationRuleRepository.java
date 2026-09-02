package mthiebi.sgs.gradebook.repository;

import mthiebi.sgs.gradebook.model.DerivationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DerivationRuleRepository extends JpaRepository<DerivationRule, Long> {

    /**
     * Every rule in a version, grouped by component and in chain order.
     */
    @Query("select r from DerivationRule r "
            + "where r.component.templateVersion.id = :versionId "
            + "order by r.component.id, r.chainOrder")
    List<DerivationRule> findByTemplateVersion(@Param("versionId") Long versionId);
}
