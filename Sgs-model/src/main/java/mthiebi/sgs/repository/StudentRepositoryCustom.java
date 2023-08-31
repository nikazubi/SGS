package mthiebi.sgs.repository;

import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQueryFactory;
import mthiebi.sgs.models.AcademyClass;
import mthiebi.sgs.models.QStudent;
import mthiebi.sgs.models.Student;
import org.springframework.data.domain.PageRequest;

import javax.persistence.EntityManager;
import java.util.List;

public interface StudentRepositoryCustom {

    List<Student> findAllStudent(int limit,
                                 int page,
                                 Long id,
                                 String firstName,
                                 String lastName,
                                 String personalNumber);

    List<Student> findByNameAndSurname(List<AcademyClass> academyClassList, String queryKey);

    List<Student> findAllByAcademyClass(long academyClassId);

    Student authStudent(String username, String password);

}
