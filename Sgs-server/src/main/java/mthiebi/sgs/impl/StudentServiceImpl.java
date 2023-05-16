package mthiebi.sgs.impl;

import mthiebi.sgs.models.Student;
import mthiebi.sgs.repository.StudentRepository;
import mthiebi.sgs.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudentServiceImpl implements StudentService {

    @Autowired
    private StudentRepository studentRepository;

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
    public List<Student> getStudents() {
        return studentRepository.findAll();
    }
}
