package mthiebi.sgs.service;


import mthiebi.sgs.SGSException;
import mthiebi.sgs.models.ClosedPeriod;

import java.util.List;

public interface ClosedPeriodService {

    ClosedPeriod createClosedPeriod(String username) throws SGSException;

    void deleteClosedPeriod(Long closedPeriodId);

    boolean getClosedPeriodByClassId(Long id, String gradePrefix, Long gradeId) throws SGSException;

    List<ClosedPeriod> getClosedPeriods();

}
