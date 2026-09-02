package mthiebi.sgs.gradebook.repository;

import mthiebi.sgs.gradebook.model.ClassGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassGroupRepository extends JpaRepository<ClassGroup, Long> {

    /**
     * The classes on offer in the current year. Fetches the school and scheme
     * because the toolbar shows the school and the grid needs the scheme; left
     * lazy they would each cost a query per row.
     */
    @Query("select c from ClassGroup c "
            + "join fetch c.school join fetch c.periodScheme "
            + "where c.academicYear.current = true "
            + "order by c.school.ordinal, c.level, c.name")
    List<ClassGroup> findForCurrentYear();

    /**
     * The class's subjects, as the join rows - the teacher lives on the join,
     * not on the subject, because the same subject is taught to different
     * classes by different people.
     */
    @Query("select cs from ClassSubject cs join fetch cs.subject s "
            + "where cs.classGroup.id = :classGroupId and s.active = true "
            + "order by cs.sortIndex")
    List<mthiebi.sgs.gradebook.model.ClassSubject> findClassSubjectsOf(
            @Param("classGroupId") Long classGroupId);
}
