package mthiebi.sgs.repository;

import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQueryFactory;
import mthiebi.sgs.models.Grade;
import mthiebi.sgs.models.QGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import java.util.Date;
import java.util.List;

@Repository
public interface GradeRepository extends JpaRepository<Grade, Long> {

    List<Grade> findGradeByStudentId(Long studentId);

    List<Grade> findGradeByAcademyClassId(Long academyClassId);

    List<Grade> findGradeByAcademyClassIdAndSubjectId(Long academyClassId, Long subjectId);

    default List<Grade> findGradeByAcademyClassIdAndSubjectIdAndCreateTime(Long academyClassId, Long subjectId, Date createTime, EntityManager em){

        QGrade qGrade = QGrade.grade;
        Predicate academyClassIdPredicate = academyClassId == 0 ? qGrade.academyClass.id.isNotNull() : qGrade.academyClass.id.eq(academyClassId);
        Predicate subjectIdPredicate = subjectId != 0 ? qGrade.subject.id.eq(subjectId) : qGrade.subject.id.isNotNull();
        Predicate datePredicate;
        if (createTime != null) {
            datePredicate = qGrade.createTime.month().eq(createTime.getMonth() + 1).and(qGrade.createTime.year().eq(createTime.getYear()));
        } else {
            datePredicate = qGrade.createTime.isNotNull();
        }

        final JPAQueryFactory query = new JPAQueryFactory(em);

        return query.selectFrom(qGrade)
                .where(academyClassIdPredicate)
                .where(subjectIdPredicate)
                .where(datePredicate)
                .orderBy(qGrade.createTime.desc())
                .fetch();
    }
}
