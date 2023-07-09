package mthiebi.sgs.repository;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.EnumPath;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.jpa.impl.JPAQueryFactory;
import mthiebi.sgs.models.Grade;
import mthiebi.sgs.models.QGrade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Repository
public class GradeRepositoryCustomImpl implements mthiebi.sgs.repository.GradeRepositoryCustom {

    private static final QGrade qGrade = QGrade.grade;
    @Autowired
    private JPAQueryFactory qf;

    @Override
    public List<Grade> findGradeByAcademyClassIdAndSubjectIdAndCreateTime(Long academyClassId,
                                                                          Long subjectId,
                                                                          Long studentId,
                                                                          Date createTime) {
        Predicate academyClassIdPredicate = academyClassId == null ? qGrade.academyClass.id.isNotNull() : qGrade.academyClass.id.eq(academyClassId);
        Predicate subjectIdPredicate = subjectId != null ? qGrade.subject.id.eq(subjectId) : qGrade.subject.id.isNull();
        Predicate studentIdPredicate = studentId != null ? qGrade.student.id.eq(studentId) : qGrade.student.id.isNotNull();
        Predicate datePredicate;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(createTime);
        datePredicate = qGrade.createTime.month().eq(calendar.get(Calendar.MONTH) + 1).and(qGrade.createTime.year().eq(calendar.get(Calendar.YEAR)));
        //        StringExpression prefixPredicate = qGrade.gradeType.stringValue();
//        BooleanExpression booleanExpression = prefixPredicate.startsWith("GENERAL_");

        return qf.selectFrom(qGrade)
                .where(academyClassIdPredicate)
                .where(subjectIdPredicate)
                .where(studentIdPredicate)
                .where(datePredicate)
//                .where(booleanExpression)
                .orderBy(qGrade.createTime.desc())
                .fetch();
    }
}