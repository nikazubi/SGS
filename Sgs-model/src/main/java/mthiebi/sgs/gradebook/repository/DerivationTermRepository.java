package mthiebi.sgs.gradebook.repository;

import mthiebi.sgs.gradebook.model.DerivationTerm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface DerivationTermRepository extends JpaRepository<DerivationTerm, Long> {

    @Query("select t from DerivationTerm t where t.rule.id in :ruleIds order by t.rule.id, t.ordinal")
    List<DerivationTerm> findByRuleIds(@Param("ruleIds") Collection<Long> ruleIds);
}
