package mthiebi.sgs.repository;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import mthiebi.sgs.models.AcademyClass;
import mthiebi.sgs.models.ChangeRequest;
import mthiebi.sgs.models.QChangeRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Repository
public class ChangeRequestRepositoryCustomImpl implements ChangeRequestRepositoryCustom{

    private static final QChangeRequest qChangeRequest = QChangeRequest.changeRequest;

    @Autowired
    private JPAQueryFactory qf;


    @Override
    public List<ChangeRequest> getChangeRequests(List<AcademyClass> academyClassList, Long classId, Long StudentId, Date date) {
        Predicate classPredicate = classId != null ? qChangeRequest.prevGrade.academyClass.id.eq(classId) : qChangeRequest.prevGrade.academyClass.id.isNotNull();
        Predicate studentPredicate = StudentId != null ? qChangeRequest.student.id.eq(StudentId) : qChangeRequest.student.id.isNotNull();
        Predicate datePredicate;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        datePredicate = qChangeRequest.createTime.month().eq(calendar.get(Calendar.MONTH) + 1).and(qChangeRequest.createTime.year().eq(calendar.get(Calendar.YEAR)));

        return qf.selectFrom(qChangeRequest)
                .where(qChangeRequest.prevGrade.academyClass.in(academyClassList))
                .where(classPredicate)
                .where(studentPredicate)
                .where(datePredicate)
                .orderBy(qChangeRequest.createTime.desc())
                .fetch();
    }
}
