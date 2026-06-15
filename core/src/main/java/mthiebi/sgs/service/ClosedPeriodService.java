package mthiebi.sgs.service;


import mthiebi.sgs.SGSException;
import mthiebi.sgs.models.ClosedPeriod;

import java.util.Date;
import java.util.List;

public interface ClosedPeriodService {

    ClosedPeriod createClosedPeriod(String username, List<Long> ids) throws SGSException;

    void deleteClosedPeriod(Long closedPeriodId);

    boolean getClosedPeriodByClassId(Long id, String gradePrefix, Long gradeId) throws SGSException;

    List<ClosedPeriod> getClosedPeriods();

    List<ClosedPeriod> findAllOrdered(Long academyClass, Date dateFrom, Date dateTo);

    Date getLatestClosedPeriodBy(Long academyClassId);

}
