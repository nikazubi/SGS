package mthiebi.sgs.repository;

import mthiebi.sgs.models.AcademyClass;
import mthiebi.sgs.models.Student;

import java.util.List;

public interface StudentRepositoryCustom {

    List<Student> findAllStudent(int limit,
                                 int page,
                                 Long id,
                                 String firstName,
                                 String lastName,
                                 String personalNumber);

    List<Student> findByNameAndSurname(List<AcademyClass> academyClassList, String queryKey);

    List<Student> findByNameAndSurname(String queryKey);

    List<Student> findAllByAcademyClass(long academyClassId);

    Student authStudent(String username, String password);

    List<Student> findByIds(List<Long> ids);

}
