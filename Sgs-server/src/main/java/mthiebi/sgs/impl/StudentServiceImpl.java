package mthiebi.sgs.impl;

import mthiebi.sgs.ExceptionKeys;
import mthiebi.sgs.SGSException;
import mthiebi.sgs.SGSExceptionCode;
import mthiebi.sgs.models.AcademyClass;
import mthiebi.sgs.models.Student;
import mthiebi.sgs.models.SystemUser;
import mthiebi.sgs.repository.StudentRepository;
import mthiebi.sgs.repository.SystemUserRepository;
import mthiebi.sgs.service.StudentService;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StudentServiceImpl implements StudentService {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private SystemUserRepository systemUserRepository;

    @PersistenceContext
    private EntityManager em;


    @Override
    public Student createStudent(Student student) {
        String encodedPassword = DigestUtils.md5Hex(student.getPassword().getBytes()).toUpperCase();
        student.setPassword(encodedPassword);
        return studentRepository.save(student);
        // todo validations - exceptions
    }

    @Override
    public Student updateStudent(Student student) throws SGSException {
        Student oldStudent = studentRepository.findById(student.getId())
                .orElseThrow(() -> new SGSException(SGSExceptionCode.BAD_REQUEST, ExceptionKeys.STUDENT_NOT_FOUNT));
        oldStudent.setFirstName(student.getFirstName());
        oldStudent.setLastName(student.getLastName());
        oldStudent.setAge(student.getAge());
        oldStudent.setPersonalNumber(student.getPersonalNumber());
        oldStudent.setUsername(student.getUsername());
        String encodedPassword = DigestUtils.md5Hex(student.getPassword().getBytes()).toUpperCase();
        oldStudent.setPassword(encodedPassword);
        oldStudent.setOwnerMail(student.getOwnerMail());
        studentRepository.save(oldStudent);
        return oldStudent;
    }

    @Override
    public void deleteStudent(Long id) {
        studentRepository.deleteById(id);
    }

    @Override
    public List<Student> getStudents(int limit,
                                     int page,
                                     Long id,
                                     String firstName,
                                     String lastName,
                                     String personalNumber) {
        return studentRepository.findAllStudent(limit, page, id, firstName, lastName, personalNumber).stream()
                .sorted(Comparator.comparing(Student::getLastName))
                .collect(Collectors.toList());
    }

    @Override
    public List<Student> findByNameAndSurname(String username, String queryKey) throws SGSException {
        SystemUser systemUser = systemUserRepository.findSystemUserByUsername(username);
        if (systemUser == null) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, ExceptionKeys.SYSTEM_USER_NOT_FOUND);
        }
        List<AcademyClass> academyClassList = systemUser.getAcademyClassList();
        return studentRepository.findByNameAndSurname(academyClassList, queryKey);
    }

    @Override
    public List<Student> findByNameAndSurname(String queryKey) throws SGSException {
        return studentRepository.findByNameAndSurname(queryKey);
    }

    @Override
    public Student findStudentById(Long studentId) throws SGSException {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new SGSException(SGSExceptionCode.BAD_REQUEST, ExceptionKeys.STUDENT_NOT_FOUNT));
    }
}
