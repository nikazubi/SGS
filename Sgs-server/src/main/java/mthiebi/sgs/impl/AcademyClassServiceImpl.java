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
import mthiebi.sgs.service.AcademyClassService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AcademyClassServiceImpl implements AcademyClassService {

    @Autowired
    private AcademyClassRepository academyClassRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private SystemUserRepository systemUserRepository;

    @Override
    public AcademyClass createAcademyClass(AcademyClass academyClass) {
        return academyClassRepository.save(academyClass);
        // todo validations - exceptions
    }

    @Override
    public AcademyClass updateAcademyClass(AcademyClass academyClass) throws SGSException {
        AcademyClass oldAcademyClass = academyClassRepository.findById(academyClass.getId())
                .orElseThrow(() -> new SGSException(SGSExceptionCode.BAD_REQUEST, ExceptionKeys.ACADEMY_CLASS_NOT_FOUND));
        oldAcademyClass.setClassLevel(academyClass.getClassLevel());
        oldAcademyClass.setClassName(academyClass.getClassName());
        oldAcademyClass.setStudentList(academyClass.getStudentList());
        academyClassRepository.save(oldAcademyClass);
        return oldAcademyClass;
    }

    @Override
    public void deleteAcademyClass(Long id) {
        academyClassRepository.deleteById(id);
    }

    @Override
    public List<AcademyClass> getAcademyClasses() {
        return academyClassRepository.findAll();
    }

    @Override
    public List<AcademyClass> getAcademyClasses(String username, String queryKey) throws SGSException {
        SystemUser systemUser = systemUserRepository.findSystemUserByUsername(username);
        if (systemUser == null) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, ExceptionKeys.SYSTEM_USER_NOT_FOUND);
        }
        List<AcademyClass> academyClassList = systemUser.getAcademyClassList();

        return academyClassRepository.getAcademyClasses(academyClassList, queryKey);
    }

    @Override
    public AcademyClass findAcademyClassById(Long id) {
        return academyClassRepository.findById(id).orElseThrow();
    }

    @Override
    public void attachStudentsToAcademyClass(Long academyClassId, List<Long> studentIdList) throws SGSException {
        AcademyClass academyClass = academyClassRepository.findById(academyClassId)
                .orElseThrow(() -> new SGSException(SGSExceptionCode.BAD_REQUEST, ExceptionKeys.ACADEMY_CLASS_NOT_FOUND));

        List<Student> studentList = new ArrayList<>();
        for (Long studentId : studentIdList) {
            Student student = studentRepository.findById(studentId)
                    .orElseThrow(() -> new SGSException(SGSExceptionCode.BAD_REQUEST, ExceptionKeys.STUDENT_NOT_FOUNT));
            studentList.add(student);
        }
        academyClass.setStudentList(studentList);
        academyClassRepository.save(academyClass);
    }

    @Override
    public void attachSubjectsToAcademyClass(Long academyClassId, List<Long> subjectIdList) throws SGSException {
        AcademyClass academyClass = academyClassRepository.findById(academyClassId)
                .orElseThrow(() -> new SGSException(SGSExceptionCode.BAD_REQUEST, ExceptionKeys.ACADEMY_CLASS_NOT_FOUND));

        List<Subject> subjectList = new ArrayList<>();
        for (Long subjectId : subjectIdList) {
            Subject subject = subjectRepository.findById(subjectId)
                    .orElseThrow(() -> new SGSException(SGSExceptionCode.BAD_REQUEST, ExceptionKeys.SUBJECT_NOT_FOUND));
        }
        academyClass.setSubjectList(subjectList);
        academyClassRepository.save(academyClass);
    }
}
