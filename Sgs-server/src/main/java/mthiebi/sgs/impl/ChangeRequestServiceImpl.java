package mthiebi.sgs.impl;

import mthiebi.sgs.ExceptionKeys;
import mthiebi.sgs.SGSException;
import mthiebi.sgs.SGSExceptionCode;
import mthiebi.sgs.SMTP.EmailDetails;
import mthiebi.sgs.SMTP.EmailService;
import mthiebi.sgs.models.*;
import mthiebi.sgs.repository.*;
import mthiebi.sgs.service.ChangeRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class ChangeRequestServiceImpl implements ChangeRequestService {

    @Autowired
    private ChangeRequestRepository changeRequestRepository;

    @Autowired
    private GradeRepository gradeRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private SystemUserRepository systemUserRepository;

    @Autowired
    private AcademyClassRepository academyClassRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private EmailService emailService;

    @Override
    public ChangeRequest createChangeRequest(ChangeRequest changeRequest, String username) throws SGSException {
        SystemUser systemUser = systemUserRepository.findSystemUserByUsername(username);
        if (systemUser == null) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, ExceptionKeys.SYSTEM_USER_NOT_FOUND);
        }
        Grade prevGrade = gradeRepository.findById(changeRequest.getId()).orElseThrow(); //TODO incorrect code

        changeRequest.setIssuer(systemUser);
        changeRequest.setPrevGrade(prevGrade);
        changeRequest.setPrevValue(prevGrade.getValue());
        changeRequest.setStatus(ChangeRequestStatus.PENDING);
        return changeRequestRepository.save(changeRequest);
    }

    @Override
    @Transactional
    public void changeRequestStatus(Long changeRequestId, ChangeRequestStatus changeRequestStatus, String description) throws SGSException {
        ChangeRequest changeRequest = changeRequestRepository.findById(changeRequestId)
                .orElseThrow(() -> new SGSException(SGSExceptionCode.BAD_REQUEST, ExceptionKeys.CHANGE_REQUEST_NOT_FOUND));
        if (changeRequestStatus == ChangeRequestStatus.APPROVED) {
            Grade prevGrade = changeRequest.getPrevGrade();
            prevGrade.setValue(changeRequest.getNewValue());
            gradeRepository.save(prevGrade);
            if (prevGrade.getStudent().getOwnerMail() != null && prevGrade.getStudent().getOwnerMail().length() > 5 ) {
                if (description != null && !description.equalsIgnoreCase("")) {
                    emailService.sendSimpleMail(EmailDetails.builder()
                            .recipient(prevGrade.getStudent().getOwnerMail())
                            .subject("IB მთიები - ნიშნის ცვლილება")
                            .msgBody(description)
                            .build());
                }
            }
        }
        changeRequest.setDirectorDescription(description);
        changeRequest.setStatus(changeRequestStatus);
        changeRequestRepository.save(changeRequest);
    }

    @Override
    public List<ChangeRequest> getChangeRequests(String username, Long classId, Long studentId, Date date) throws SGSException {
        SystemUser systemUser = systemUserRepository.findSystemUserByUsername(username);
        if (systemUser == null) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, ExceptionKeys.SYSTEM_USER_NOT_FOUND);
        }
        List<AcademyClass> academyClassList = systemUser.getAcademyClassList();
        return changeRequestRepository.getChangeRequests(academyClassList, classId, studentId, date);
    }

    @Override
    public Date getLastUpdateTime() {
        return changeRequestRepository.getLastUpdateDate();
    }

}
