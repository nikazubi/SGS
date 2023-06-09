package mthiebi.sgs.impl;

import mthiebi.sgs.ExceptionKeys;
import mthiebi.sgs.SGSException;
import mthiebi.sgs.SGSExceptionCode;
import mthiebi.sgs.models.AcademyClass;
import mthiebi.sgs.models.ClosedPeriod;
import mthiebi.sgs.models.Grade;
import mthiebi.sgs.models.SystemUser;
import mthiebi.sgs.repository.ClosedPeriodRepository;
import mthiebi.sgs.repository.GradeRepository;
import mthiebi.sgs.repository.SystemUserRepository;
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

    @Autowired
    private SystemUserRepository systemUserRepository;

    @Override
    //TODO change!!
    public ClosedPeriod createClosedPeriod(String username) throws SGSException {
        SystemUser systemUser = systemUserRepository.findSystemUserByUsername(username);
        if (systemUser == null) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, ExceptionKeys.SYSTEM_USER_NOT_FOUND);
        }
        for (AcademyClass academyClass : systemUser.getAcademyClassList()) {
            createOrUpdateClosedPeriodByPrefix(academyClass.getId(), "GENERAL");
            createOrUpdateClosedPeriodByPrefix(academyClass.getId(), "BEHAVIOUR");
            createOrUpdateClosedPeriodByPrefix(academyClass.getId(), "ABSENT");
        }
        return null;
    }

    @Override
    public void deleteClosedPeriod(Long closedPeriodId) {
        closedPeriodRepository.deleteById(closedPeriodId);
    }

    @Override
    public boolean getClosedPeriodByClassId(Long id, String gradePrefix, Long gradeId) throws SGSException {
        Grade grade = gradeRepository.findById(gradeId)
                .orElseThrow(() -> new SGSException(SGSExceptionCode.BAD_REQUEST, ExceptionKeys.GRADE_REQUEST_NOT_FOUND));
        ClosedPeriod closedPeriod = closedPeriodRepository.findClosedPeriodByAcademyClassIdAndPrefix(id, gradePrefix, grade.getCreateTime());
        return closedPeriod != null;
    }

    @Override
    public List<ClosedPeriod> getClosedPeriods() {
        return closedPeriodRepository.findAll();
    }

    private void createOrUpdateClosedPeriodByPrefix(long academyClassId, String prefix) {
        ClosedPeriod currClosedPeriod = closedPeriodRepository.findClosedPeriodByAcademyClassIdAndPrefix(academyClassId, "GENERAL");
        if (currClosedPeriod == null) {
            currClosedPeriod = ClosedPeriod.builder()
                    .id(0L)
                    .gradePrefix(prefix)
                    .academyClassId(academyClassId)
                    .build();
        } else {
            currClosedPeriod.setLastUpdateTime(new Date());
        }
        closedPeriodRepository.save(currClosedPeriod);
    }
}
