package mthiebi.sgs.repository;

import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQueryFactory;
import mthiebi.sgs.models.Grade;
import mthiebi.sgs.models.GradeType;
import mthiebi.sgs.models.QGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import java.util.Date;
import java.util.List;

@Repository
public interface GradeRepository extends JpaRepository<Grade, Long>,
                                            PagingAndSortingRepository<Grade, Long>,
                                            GradeRepositoryCustom {

    List<Grade> findGradeByStudentId(Long studentId);

    List<Grade> findGradeByAcademyClassId(Long academyClassId);

    List<Grade> findGradeByAcademyClassIdAndSubjectId(Long academyClassId, Long subjectId);

}
