package mthiebi.sgs.gradebook.repository;

import mthiebi.sgs.gradebook.model.GradeComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface GradeComponentRepository extends JpaRepository<GradeComponent, Long> {

    @Query("select c from GradeComponent c "
            + "where c.templateVersion.id = :versionId order by c.ordinal")
    List<GradeComponent> findByTemplateVersion(@Param("versionId") Long versionId);

    /**
     * Which versions the given components belong to.
     * <p>
     * A formula may name a column in another journal, so validating one version
     * means loading every version it reaches - otherwise a cross-journal
     * reference looks like a dangling one, and a cycle spanning two journals is
     * never seen at all.
     */
    @Query("select distinct c.templateVersion.id from GradeComponent c where c.id in :ids")
    List<Long> findVersionIdsOf(@Param("ids") Collection<Long> ids);

    /**
     * Every column of every live journal, for the cross-journal picker.
     */
    @Query("select c from GradeComponent c "
            + "join fetch c.templateVersion v join fetch v.template t "
            + "where t.archived = false and v.status <> mthiebi.sgs.gradebook.model.TemplateVersionStatus.ARCHIVED "
            + "order by t.sortIndex, c.ordinal")
    List<GradeComponent> findAllLive();
}
