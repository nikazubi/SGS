package mthiebi.sgs.impl;

import mthiebi.sgs.ExceptionKeys;
import mthiebi.sgs.SGSException;
import mthiebi.sgs.SGSExceptionCode;
import mthiebi.sgs.models.AcademyClass;
import mthiebi.sgs.models.Student;
import mthiebi.sgs.models.Subject;
import mthiebi.sgs.models.SystemUser;
import mthiebi.sgs.repository.AcademyClassRepository;
import mthiebi.sgs.repository.StudentRepository;
import mthiebi.sgs.repository.SubjectRepository;
import mthiebi.sgs.repository.SystemUserRepository;
import mthiebi.sgs.service.SubjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Service
public class SubjectServiceImpl implements SubjectService {

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private SystemUserRepository systemUserRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private AcademyClassRepository academyClassRepository;

    @PersistenceContext
    private EntityManager em;

    @Override
    public Subject createSubject(Subject subject) {
        return subjectRepository.save(subject);
    }

    @Override
    public Subject updateSubject(Subject subject) throws SGSException {
        Subject oldSubject = subjectRepository.findById(subject.getId())
                .orElseThrow(() -> new SGSException(SGSExceptionCode.BAD_REQUEST, ExceptionKeys.SUBJECT_NOT_FOUND));
        oldSubject.setName(subject.getName());
        oldSubject.setTeacher(subject.getTeacher());
        return subjectRepository.save(oldSubject);
    }

    @Override
    public void deleteSubject(Long id) {
        subjectRepository.deleteById(id);
    }

    @Override
    public List<Subject> getSubjects(int limit,
                                     int page,
                                     Long id,
                                     String name,
                                     String username) throws SGSException {

        SystemUser systemUser = systemUserRepository.findSystemUserByUsername(username);
        if (systemUser == null) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, ExceptionKeys.SYSTEM_USER_NOT_FOUND);
        }
        List<AcademyClass> academyClassList = systemUser.getAcademyClassList();
        return subjectRepository.findAllSubject(limit, page, id, name, academyClassList, em);
    }

    @Override
    public List<Subject> getSubjectsForStudent(String username) throws SGSException {
        Student student = studentRepository.findByUsername(username).orElseThrow(
                () -> new SGSException(SGSExceptionCode.BAD_REQUEST, "სტუდენტი ვერ მოიძებნა"));
        AcademyClass academyClass = academyClassRepository.getAcademyClassByStudent(student.getId()).orElseThrow(
                () -> new SGSException(SGSExceptionCode.BAD_REQUEST, "კლასი ვერ მოიძებნა"));

        return academyClass.getSubjectList();
    }

    @Override
    public List<Subject> getSubjects(int limit,
                                     int page,
                                     Long id,
                                     String name) throws SGSException {
        return subjectRepository.findAllSubject(limit, page, id, name, em);
    }

    @Override
    public Subject findSubjectById(Long id) throws SGSException {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new SGSException(SGSExceptionCode.BAD_REQUEST, ExceptionKeys.SUBJECT_NOT_FOUND));
    }

}
