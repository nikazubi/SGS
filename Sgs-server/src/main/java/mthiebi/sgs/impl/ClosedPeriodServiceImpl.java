package mthiebi.sgs.impl;

import mthiebi.sgs.models.ClosedPeriod;
import mthiebi.sgs.models.Grade;
import mthiebi.sgs.repository.ClosedPeriodRepository;
import mthiebi.sgs.repository.GradeRepository;
import mthiebi.sgs.service.ClosedPeriodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class ClosedPeriodServiceImpl implements ClosedPeriodService {

    @Autowired
    private ClosedPeriodRepository closedPeriodRepository;

    @Autowired
    private GradeRepository gradeRepository;

    @Override
    //TODO change!!
    public ClosedPeriod createClosedPeriod(long academyClassId) {
//        if (closedPeriod.getGradePrefix() == null) {
            ClosedPeriod currClosedPeriodGeneral = ClosedPeriod.builder()
                                                        .id(0L)
                                                        .gradePrefix("GENERAL")
                                                        .academyClassId(academyClassId)
                                                        .build();
            ClosedPeriod currClosedPeriodBehaviour = ClosedPeriod.builder()
                    .id(0L)
                    .gradePrefix("BEHAVIOUR")
                    .academyClassId(academyClassId)
                    .build();
            ClosedPeriod currClosedPeriodAbsent = ClosedPeriod.builder()
                    .id(0L)
                    .gradePrefix("ABSENT")
                    .academyClassId(academyClassId)
                    .build();
            closedPeriodRepository.save(currClosedPeriodGeneral);
            closedPeriodRepository.save(currClosedPeriodBehaviour);
            closedPeriodRepository.save(currClosedPeriodAbsent);
            return null;
//        }
//        return closedPeriodRepository.save(closedPeriod);
    }

    @Override
    public void deleteClosedPeriod(Long closedPeriodId) {
        closedPeriodRepository.deleteById(closedPeriodId);
    }

    @Override
    public ClosedPeriod getClosedPeriodByClassId(Long id, String gradePrefix, Long gradeId) {
        if (gradeId == null) {
            return closedPeriodRepository.findClosedPeriodByAcademyClassIdAndPrefix(id, gradePrefix, new Date());
        }
        Grade grade = gradeRepository.findById(gradeId).orElseThrow();
        return closedPeriodRepository.findClosedPeriodByAcademyClassIdAndPrefix(id, gradePrefix, grade.getLastUpdateTime());
    }

    @Override
    public List<ClosedPeriod> getClosedPeriods() {
        return closedPeriodRepository.findAll();
    }
}
