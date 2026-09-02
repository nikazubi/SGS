package mthiebi.sgs.gradebook.repository;

import mthiebi.sgs.gradebook.model.DerivationSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface DerivationSourceRepository extends JpaRepository<DerivationSource, Long> {

    /**
     * Which components read the given ones - the reverse of a formula's own
     * references.
     * <p>
     * Needed because following only what a version reads walks upstream: if
     * ethics reads academic, then starting from academic never reaches ethics,
     * and saving an academic mark would leave the ethics column stale.
     */
    @org.springframework.data.jpa.repository.Query(
            "select distinct r.component.id from DerivationSource s "
                    + "join s.term t join t.rule r "
                    + "where s.component.id in :ids "
                    + "  and r.component.templateVersion.status in "
                    + "      (mthiebi.sgs.gradebook.model.TemplateVersionStatus.ACTIVE, "
                    + "       mthiebi.sgs.gradebook.model.TemplateVersionStatus.LOCKED)")
    java.util.List<Long> findComponentIdsReading(
            @org.springframework.data.repository.query.Param("ids")
            java.util.Collection<Long> ids);

    @Query("select s from DerivationSource s where s.term.id in :termIds order by s.term.id, s.id")
    List<DerivationSource> findByTermIds(@Param("termIds") Collection<Long> termIds);
}
