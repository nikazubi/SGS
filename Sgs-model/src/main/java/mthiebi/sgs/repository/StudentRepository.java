package mthiebi.sgs.repository;

import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQueryFactory;
import mthiebi.sgs.models.QStudent;
import mthiebi.sgs.models.QSubject;
import mthiebi.sgs.models.Student;
import mthiebi.sgs.models.Subject;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import java.util.List;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long>, QuerydslPredicateExecutor<Student> {

    default List<Student> findAllStudent(int limit,
                                         int page,
                                         Long id,
                                         String firstName,
                                         String lastName,
                                         String personalNumber,
                                         EntityManager em){

        QStudent qStudent = QStudent.student;
        PageRequest converted = PageRequest.of(page, limit);

        Predicate idPredicate = id == 0 ? qStudent.id.isNotNull() : qStudent.id.eq(id);
        Predicate firstNamePredicate = firstName != null ? qStudent.firstName.contains(firstName) : qStudent.firstName.isNotNull();
        Predicate lastNamePredicate = lastName != null ? qStudent.lastName.contains(lastName) : qStudent.lastName.isNotNull();
        Predicate personalNumberPredicate = personalNumber != null ? qStudent.personalNumber.contains(lastName) : qStudent.personalNumber.isNotNull();
        final JPAQueryFactory query = new JPAQueryFactory(em);

        return query.selectFrom(qStudent)
                .where(idPredicate)
                .where(firstNamePredicate)
                .where(lastNamePredicate)
                .where(personalNumberPredicate)
                .offset(converted.getOffset())
                .limit(converted.getPageSize())
                .orderBy(qStudent.createTime.desc())
                .fetch();
    }

}
