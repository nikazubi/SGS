package mthiebi.sgs.service;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.models.Student;

import java.util.List;

public interface StudentService {

    Student createStudent(Student student);

    Student updateStudent(Student student) throws SGSException;

    void deleteStudent(Long id);

    List<Student> getStudents(int limit,
                              int page,
                              Long id,
                              String firstName,
                              String lastName,
                              String personalNumber);

    List<Student> findByNameAndSurname(String username, String queryKey) throws SGSException;

    Student findStudentById(Long studentId) throws SGSException;

}
