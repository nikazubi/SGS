package mthiebi.sgs.repository;

import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQueryFactory;
import mthiebi.sgs.models.Grade;
import mthiebi.sgs.models.QGrade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import java.util.Date;
import java.util.List;

@Repository
public class GradeRepositoryCustomImpl implements GradeRepositoryCustom {

    private static final QGrade qGrade = QGrade.grade;
    @Autowired
    private JPAQueryFactory qf;

    @Override
    public List<Grade> findGradeByAcademyClassIdAndSubjectIdAndCreateTime(Long academyClassId,
                                                                          Long subjectId,
                                                                          Long studentId,
                                                                          Date createTime,
                                                                          String gradeTypePrefix) {
        Predicate academyClassIdPredicate = academyClassId == null ? qGrade.academyClass.id.isNotNull() : qGrade.academyClass.id.eq(academyClassId);
        Predicate subjectIdPredicate = subjectId != null ? qGrade.subject.id.eq(subjectId) : qGrade.subject.id.isNotNull();
        Predicate studentIdPredicate = studentId != null ? qGrade.subject.id.eq(studentId) : qGrade.student.id.isNotNull();
        Predicate datePredicate;
        if (createTime != null) {
            datePredicate = qGrade.createTime.month().eq(createTime.getMonth() + 1).and(qGrade.createTime.year().eq(createTime.getYear()));
        } else {
            datePredicate = qGrade.createTime.isNotNull();
        }
        Predicate prefixPredicate = qGrade.gradeType.stringValue().startsWith(gradeTypePrefix);

        return qf.selectFrom(qGrade)
                .where(academyClassIdPredicate)
                .where(subjectIdPredicate)
                .where(studentIdPredicate)
                .where(datePredicate)
                .where(prefixPredicate)
                .orderBy(qGrade.createTime.desc())
                .fetch();
    }
}