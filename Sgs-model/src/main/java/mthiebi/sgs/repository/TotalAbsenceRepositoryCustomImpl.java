package mthiebi.sgs.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import mthiebi.sgs.models.QAcademyClass;
import mthiebi.sgs.models.QTotalAbsence;
import mthiebi.sgs.models.TotalAbsence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;

@Repository
public class TotalAbsenceRepositoryCustomImpl implements TotalAbsenceRepositoryCustom {

    private static final QTotalAbsence qTotalAbsence = QTotalAbsence.totalAbsence;
    @Autowired
    private JPAQueryFactory qf;

    @Override
    public List<TotalAbsence> filter(Long academyClassId, Date activePeriod) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(activePeriod);
        BooleanExpression datePredicate = qTotalAbsence.activePeriod.month().eq(calendar.get(Calendar.MONTH) + 1).and(qTotalAbsence.activePeriod.year().eq(calendar.get(Calendar.YEAR)));
        return qf.select(qTotalAbsence)
                .from(qTotalAbsence)
                .where(QueryUtils.longEq(qTotalAbsence.academyClass.id, academyClassId))
                .where(datePredicate)
                .fetch();
    }
}
