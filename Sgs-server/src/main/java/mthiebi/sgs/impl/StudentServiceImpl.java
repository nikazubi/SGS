package mthiebi.sgs.impl;

import mthiebi.sgs.models.AcademyClass;
import mthiebi.sgs.models.Student;
import mthiebi.sgs.models.SystemUser;
import mthiebi.sgs.repository.StudentRepository;
import mthiebi.sgs.repository.SystemUserRepository;
import mthiebi.sgs.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

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
        return studentRepository.save(student);
        // todo validations - exceptions
    }

    @Override
    public Student updateStudent(Student student) {
        Student oldStudent = studentRepository.findById(student.getId()).orElseThrow();
        oldStudent.setFirstName(student.getFirstName());
        oldStudent.setLastName(student.getLastName());
        oldStudent.setAge(student.getAge());
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
        return studentRepository.findAllStudent(limit, page, id, firstName, lastName, personalNumber);
    }

    @Override
    public List<Student> findByNameAndSurname(String username, String queryKey) {
        SystemUser systemUser = systemUserRepository.findSystemUserByUsername(username);
        List<AcademyClass> academyClassList = systemUser.getAcademyClassList();
        return studentRepository.findByNameAndSurname(academyClassList, queryKey);
    }

    @Override
    public Student findStudentById(Long studentId) {
        return studentRepository.findById(studentId).orElseThrow();
    }
}
