package mthiebi.sgs.gradebook.repository;

import mthiebi.sgs.gradebook.model.TemplateAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TemplateAssignmentRepository extends JpaRepository<TemplateAssignment, Long> {

    /**
     * Assignments for a class in one scope, most specific first, so a
     * subject-level override is picked ahead of the class-wide default.
     */
    @Query("select a from TemplateAssignment a "
            + "where a.classGroup.id = :classGroupId and a.template.id = :templateId "
            + "  and (a.subject.id = :subjectId or a.subject is null) "
            + "order by case when a.subject is null then 1 else 0 end")
    List<TemplateAssignment> findForClassAndSubject(@Param("classGroupId") Long classGroupId,
                                                    @Param("subjectId") Long subjectId,
                                                    @Param("templateId") Long templateId);
}
