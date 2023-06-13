package mthiebi.sgs.repository;

import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQueryFactory;
import mthiebi.sgs.models.AcademyClass;
import mthiebi.sgs.models.QSubject;
import mthiebi.sgs.models.Subject;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import java.util.List;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long>, QuerydslPredicateExecutor<Subject> {

    default List<Subject> findAllSubject(int limit,
                                         int page,
                                         Long id,
                                         String name,
                                         EntityManager em){

        QSubject qSubject = QSubject.subject;
        PageRequest converted = PageRequest.of(page, limit);
        Predicate idPredicate = id == 0 ? qSubject.id.isNotNull() : qSubject.id.eq(id);
        Predicate namePredicate = name != null ? qSubject.name.contains(name) : qSubject.name.isNotNull();
        final JPAQueryFactory query = new JPAQueryFactory(em);

        return query.selectFrom(qSubject)
                        .where(idPredicate)
                        .where(namePredicate)
                        //.offset(converted.getOffset())
                        .limit(converted.getPageSize())
                        .orderBy(qSubject.createTime.desc())
                        .fetch();
    }



}
