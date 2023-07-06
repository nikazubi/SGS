package mthiebi.sgs.service;


import mthiebi.sgs.models.ClosedPeriod;

import java.util.List;

public interface ClosedPeriodService {

    ClosedPeriod createClosedPeriod(String username);

    void deleteClosedPeriod(Long closedPeriodId);

    boolean getClosedPeriodByClassId(Long id, String gradePrefix, Long gradeId);

    List<ClosedPeriod> getClosedPeriods();

}
