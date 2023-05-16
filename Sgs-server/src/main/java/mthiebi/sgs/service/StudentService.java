package mthiebi.sgs.service;

import mthiebi.sgs.models.AcademyClass;
import mthiebi.sgs.models.Student;

import java.util.List;

public interface StudentService {

    Student createStudent(Student student);

    Student updateStudent(Student student);

    void deleteStudent(Long id);

    List<Student> getStudents();

}
