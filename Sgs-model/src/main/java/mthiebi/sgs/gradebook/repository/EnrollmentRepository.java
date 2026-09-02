package mthiebi.sgs.gradebook.repository;

import mthiebi.sgs.gradebook.model.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    @Query("select e from Enrollment e join fetch e.student s "
            + "where e.classGroup.id = :classGroupId and e.leftOn is null "
            + "order by s.lastName, s.firstName")
    List<Enrollment> findActiveByClassGroup(@Param("classGroupId") Long classGroupId);
}
