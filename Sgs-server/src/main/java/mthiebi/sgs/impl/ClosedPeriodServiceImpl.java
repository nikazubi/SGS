package mthiebi.sgs.impl;

import mthiebi.sgs.ExceptionKeys;
import mthiebi.sgs.SGSException;
import mthiebi.sgs.SGSExceptionCode;
import mthiebi.sgs.SMTP.EmailDetails;
import mthiebi.sgs.SMTP.EmailService;
import mthiebi.sgs.models.*;
import mthiebi.sgs.repository.ClosedPeriodRepository;
import mthiebi.sgs.repository.GradeRepository;
import mthiebi.sgs.repository.SystemUserRepository;
import mthiebi.sgs.service.ClosedPeriodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClosedPeriodServiceImpl implements ClosedPeriodService {

    @Autowired
    private ClosedPeriodRepository closedPeriodRepository;

    @Autowired
    private GradeRepository gradeRepository;

    @Autowired
    private SystemUserRepository systemUserRepository;

    @Autowired
    private EmailService emailService;

    @Override
    //TODO change!!
    public ClosedPeriod createClosedPeriod(String username, List<Long> ids) throws SGSException {
        SystemUser systemUser = systemUserRepository.findSystemUserByUsername(username);
        if (systemUser == null) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, ExceptionKeys.SYSTEM_USER_NOT_FOUND);
        }
        List<AcademyClass> toClose = systemUser.getAcademyClassList()
                                                .stream()
                                                .filter(academyClass -> ids.contains(academyClass.getId()))
                                                .collect(Collectors.toList());
        for (AcademyClass academyClass : toClose) {
            createOrUpdateClosedPeriodByPrefix(academyClass.getId(), "GENERAL");
            createOrUpdateClosedPeriodByPrefix(academyClass.getId(), "BEHAVIOUR");
            for(Student student : academyClass.getStudentList()){
                if(student.getOwnerMail() == null){
                    continue;
                }
                EmailDetails  emailDetails = EmailDetails.builder()
                                                        .subject("IB მთიები - ნიშნების განახლება")
                                                        .msgBody("გაცნობებთ რომ, https://www.ibmthiebistudentrating.edu.ge პორტალზე ატვირთულია ახალი ნიშნები")
                                                        .recipient(student.getOwnerMail())
                                                        .build();
                emailService.sendSimpleMail(emailDetails);
            }
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
        ClosedPeriod closedPeriod = closedPeriodRepository.findClosedPeriodByAcademyClassIdAndPrefix(id, gradePrefix, grade.getExactMonth());
        return closedPeriod != null;
    }

    @Override
    public List<ClosedPeriod> getClosedPeriods() {
        return closedPeriodRepository.findAll();
    }

    private void createOrUpdateClosedPeriodByPrefix(long academyClassId, String prefix) {
        ClosedPeriod currClosedPeriod = closedPeriodRepository.findClosedPeriodByAcademyClassIdAndPrefix(academyClassId, prefix);
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
