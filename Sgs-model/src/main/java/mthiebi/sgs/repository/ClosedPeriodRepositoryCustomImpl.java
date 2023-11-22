package mthiebi.sgs.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import mthiebi.sgs.models.ClosedPeriod;
import mthiebi.sgs.models.QClosedPeriod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Repository
public class ClosedPeriodRepositoryCustomImpl implements ClosedPeriodRepositoryCustom{

    private static final QClosedPeriod qClosedPeriod = QClosedPeriod.closedPeriod;

    @Autowired
    private JPAQueryFactory qf;


    @Override
    public ClosedPeriod findClosedPeriodByAcademyClassIdAndPrefix(Long academyClassId, String gradePrefix, Date lastUpdate) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(lastUpdate);
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        return qf.selectFrom(qClosedPeriod)
                .where(qClosedPeriod.academyClassId.eq(academyClassId))
                .where(qClosedPeriod.gradePrefix.eq(gradePrefix))
                .where(qClosedPeriod.lastUpdateTime.after(calendar.getTime()))
                .orderBy(qClosedPeriod.lastUpdateTime.desc())
                .fetchFirst();
    }

    @Override
    public ClosedPeriod findClosedPeriodByAcademyClassIdAndPrefix(Long academyClassId, String gradePrefix) {
        return qf.selectFrom(qClosedPeriod)
                .where(qClosedPeriod.academyClassId.eq(academyClassId))
                .where(qClosedPeriod.gradePrefix.eq(gradePrefix))
                .orderBy(qClosedPeriod.lastUpdateTime.desc())
                .fetchOne();
    }

    @Override
    public List<ClosedPeriod> findAllOrderedByLastUpdateTime(Long academyClass, Date dateFrom, Date dateTo) {
        return qf.selectFrom(qClosedPeriod)
                .where(QueryUtils.longEq(qClosedPeriod.academyClassId, academyClass))
                .where(QueryUtils.dateTimeMore(qClosedPeriod.lastUpdateTime, dateFrom))
                .where(QueryUtils.dateTimeLess(qClosedPeriod.lastUpdateTime, dateTo))
                .orderBy(qClosedPeriod.lastUpdateTime.desc())
                .fetch();
    }

    @Override
    public Date findCreateTimeOfLatestClosePeriodByClassId(Long academyClassId) {
        return qf.select(qClosedPeriod.lastUpdateTime.max())
                .from(qClosedPeriod)
                .where(QueryUtils.longEq(qClosedPeriod.academyClassId, academyClassId))
                .fetchOne();
    }
}
