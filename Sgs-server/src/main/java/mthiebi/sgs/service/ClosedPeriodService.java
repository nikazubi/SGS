package mthiebi.sgs.service;


import mthiebi.sgs.models.ClosedPeriod;

import java.util.List;

public interface ClosedPeriodService {

    ClosedPeriod createClosedPeriod(ClosedPeriod closedPeriod);

    void deleteClosedPeriod(Long closedPeriodId);

    List<ClosedPeriod> getClosedPeriods();

}
