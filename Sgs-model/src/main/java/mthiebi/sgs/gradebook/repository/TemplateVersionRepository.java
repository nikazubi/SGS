package mthiebi.sgs.gradebook.repository;

import mthiebi.sgs.gradebook.model.TemplateVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TemplateVersionRepository extends JpaRepository<TemplateVersion, Long> {

    /**
     * Every version of a journal, newest last.
     */
    @org.springframework.data.jpa.repository.Query(
            "select v from TemplateVersion v where v.template.id = :templateId "
                    + "order by v.versionNo")
    java.util.List<TemplateVersion> findByTemplate(
            @org.springframework.data.repository.query.Param("templateId") Long templateId);
}
