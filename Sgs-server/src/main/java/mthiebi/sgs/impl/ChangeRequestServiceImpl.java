package mthiebi.sgs.impl;

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

    @Override
    public ChangeRequest createChangeRequest(ChangeRequest changeRequest, String username) {
        SystemUser systemUser = systemUserRepository.findSystemUserByUsername(username);
        Grade prevGrade = gradeRepository.findById(changeRequest.getId()).orElseThrow();

        changeRequest.setIssuer(systemUser);
        changeRequest.setPrevGrade(prevGrade);
        changeRequest.setPrevValue(prevGrade.getValue());
        changeRequest.setStatus(ChangeRequestStatus.PENDING);
        return changeRequestRepository.save(changeRequest);
    }

    @Override
    @Transactional
    public void changeRequestStatus(Long changeRequestId, ChangeRequestStatus changeRequestStatus) {
        ChangeRequest changeRequest = changeRequestRepository.findById(changeRequestId).orElseThrow();
        if (changeRequestStatus == ChangeRequestStatus.APPROVED){
            Grade prevGrade = changeRequest.getPrevGrade();
            prevGrade.setValue(changeRequest.getNewValue());
            gradeRepository.save(prevGrade);
        }
        changeRequest.setStatus(changeRequestStatus);
        changeRequestRepository.save(changeRequest);
    }

    @Override
    public List<ChangeRequest> getChangeRequests(String username, Long classId, Long studentId, Date date) {
        SystemUser systemUser = systemUserRepository.findSystemUserByUsername(username);
        List<AcademyClass> academyClassList = systemUser.getAcademyClassList();
        return changeRequestRepository.getChangeRequests(academyClassList, classId, studentId, date);
    }
}
