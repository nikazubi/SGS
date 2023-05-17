package mthiebi.sgs.repository;

import mthiebi.sgs.models.Grade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GradeRepository extends JpaRepository<Grade, Long> {

    List<Grade> findGradeByStudentId(Long studentId);

    List<Grade> findGradeByAcademyClassId(Long academyClassId);

    List<Grade> findGradeByAcademyClassIdAndStudentId(Long academyClassId, Long studentId);
}
