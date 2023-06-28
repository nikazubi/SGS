package mthiebi.sgs.impl;

import mthiebi.sgs.models.ClosedPeriod;
import mthiebi.sgs.models.Grade;
import mthiebi.sgs.repository.ClosedPeriodRepository;
import mthiebi.sgs.repository.GradeRepository;
import mthiebi.sgs.service.ClosedPeriodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClosedPeriodServiceImpl implements ClosedPeriodService {

    @Autowired
    private ClosedPeriodRepository closedPeriodRepository;

    @Autowired
    private GradeRepository gradeRepository;

    @Override
    public ClosedPeriod createClosedPeriod(ClosedPeriod closedPeriod) {
        return closedPeriodRepository.save(closedPeriod);
    }

    @Override
    public void deleteClosedPeriod(Long closedPeriodId) {
        closedPeriodRepository.deleteById(closedPeriodId);
    }

    @Override
    public ClosedPeriod getClosedPeriodByClassId(Long id, String gradePrefix, Long gradeId) {
        Grade grade = gradeRepository.findById(gradeId).orElseThrow();
        return closedPeriodRepository.findClosedPeriodByAcademyClassIdAndPrefix(id, gradePrefix, grade.getLastUpdateTime());
    }

    @Override
    public List<ClosedPeriod> getClosedPeriods() {
        return closedPeriodRepository.findAll();
    }
}
