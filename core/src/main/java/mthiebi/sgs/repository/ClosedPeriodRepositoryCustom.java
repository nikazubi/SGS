package mthiebi.sgs.repository;

import mthiebi.sgs.models.ClosedPeriod;

import java.util.Date;
import java.util.List;

public interface ClosedPeriodRepositoryCustom {

    ClosedPeriod findClosedPeriodByAcademyClassIdAndPrefix(Long academyClassId, String gradePrefix, Date lastUpdateTime);

    ClosedPeriod findClosedPeriodByAcademyClassIdAndPrefix(Long academyClassId, String gradePrefix);

    List<ClosedPeriod> findAllOrderedByLastUpdateTime(Long academyClass, Date dateFrom, Date dateTo);

    Date findCreateTimeOfLatestClosePeriodByClassId(Long academyClassId);
}
