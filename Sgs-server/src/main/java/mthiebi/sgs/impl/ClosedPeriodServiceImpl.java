package mthiebi.sgs.impl;

import mthiebi.sgs.models.ClosedPeriod;
import mthiebi.sgs.repository.ClosedPeriodRepository;
import mthiebi.sgs.service.ClosedPeriodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClosedPeriodServiceImpl implements ClosedPeriodService {

    @Autowired
    private ClosedPeriodRepository closedPeriodRepository;

    @Override
    public ClosedPeriod createClosedPeriod(ClosedPeriod closedPeriod) {
        return closedPeriodRepository.save(closedPeriod);
    }

    @Override
    public void deleteClosedPeriod(Long closedPeriodId) {
        closedPeriodRepository.deleteById(closedPeriodId);
    }

    @Override
    public List<ClosedPeriod> getClosedPeriods() {
        return closedPeriodRepository.findAll();
    }
}
