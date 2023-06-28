package mthiebi.sgs.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import mthiebi.sgs.models.ClosedPeriod;
import mthiebi.sgs.models.QClosedPeriod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Date;

@Repository
public class ClosedPeriodRepositoryCustomImpl implements ClosedPeriodRepositoryCustom{

    private static final QClosedPeriod qClosedPeriod = QClosedPeriod.closedPeriod;

    @Autowired
    private JPAQueryFactory qf;


    @Override
    public ClosedPeriod findClosedPeriodByAcademyClassIdAndPrefix(Long academyClassId, String gradePrefix, Date lastUpdate) {
        return qf.selectFrom(qClosedPeriod)
                .where(qClosedPeriod.AcademyClassId.eq(academyClassId))
                .where(qClosedPeriod.gradePrefix.eq(gradePrefix))
                .where(qClosedPeriod.lastUpdateTime.after(lastUpdate))
                .orderBy(qClosedPeriod.lastUpdateTime.desc())
                .fetchOne();
    }
}
