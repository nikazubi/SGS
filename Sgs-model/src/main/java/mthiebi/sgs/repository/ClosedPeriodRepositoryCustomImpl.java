package mthiebi.sgs.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import mthiebi.sgs.models.ClosedPeriod;
import mthiebi.sgs.models.QClosedPeriod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Calendar;
import java.util.Date;

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
                .fetchOne();
    }

    @Override
    public ClosedPeriod findClosedPeriodByAcademyClassIdAndPrefix(Long academyClassId, String gradePrefix) {
        return qf.selectFrom(qClosedPeriod)
                .where(qClosedPeriod.academyClassId.eq(academyClassId))
                .where(qClosedPeriod.gradePrefix.eq(gradePrefix))
                .orderBy(qClosedPeriod.lastUpdateTime.desc())
                .fetchOne();
    }
}
