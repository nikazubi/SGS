package mthiebi.sgs.repository;

import mthiebi.sgs.models.Grade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GradeRepository extends JpaRepository<Grade, Long>,
                                            PagingAndSortingRepository<Grade, Long>,
                                            GradeRepositoryCustom {

    List<Grade> findGradeByStudentId(Long studentId);

    List<Grade> findGradeByAcademyClassId(Long academyClassId);

    List<Grade> findGradeByAcademyClassIdAndSubjectId(Long academyClassId, Long subjectId);

}
