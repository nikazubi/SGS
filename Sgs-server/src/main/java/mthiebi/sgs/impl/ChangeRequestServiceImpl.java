package mthiebi.sgs.impl;

import mthiebi.sgs.models.*;
import mthiebi.sgs.repository.*;
import mthiebi.sgs.service.ChangeRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        AcademyClass academyClass = academyClassRepository.findById(changeRequest.getAcademyClass().getId()).orElseThrow();
        Student student = studentRepository.findById(changeRequest.getStudent().getId()).orElseThrow();
        Grade prevGrade = gradeRepository.findById(changeRequest.getPrevGrade().getId()).orElseThrow();
        Subject subject = subjectRepository.findById(changeRequest.getSubject().getId()).orElseThrow();

        changeRequest.setIssuer(systemUser);
        changeRequest.setAcademyClass(academyClass);
        changeRequest.setStudent(student);
        changeRequest.setPrevGrade(prevGrade);
        changeRequest.setSubject(subject);
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
    public List<ChangeRequest> getChangeRequests(String username) {
        SystemUser systemUser = systemUserRepository.findSystemUserByUsername(username);
        List<AcademyClass> academyClassList = systemUser.getAcademyClassList();
        return changeRequestRepository.getChangeRequests(academyClassList);
    }
}
