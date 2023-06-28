package mthiebi.sgs.repository;

import mthiebi.sgs.models.ClosedPeriod;

import java.util.Date;

public interface ClosedPeriodRepositoryCustom {

    ClosedPeriod findClosedPeriodByAcademyClassIdAndPrefix(Long academyClassId, String gradePrefix, Date lastUpdateTime);
}
