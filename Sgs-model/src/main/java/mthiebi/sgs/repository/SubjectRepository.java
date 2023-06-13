package mthiebi.sgs.repository;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import mthiebi.sgs.models.AcademyClass;
import mthiebi.sgs.models.QAcademyClass;
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
                                         List<AcademyClass> academyClassList,
                                         EntityManager em){

        QSubject qSubject = QSubject.subject;
        QAcademyClass qAcademyClass = QAcademyClass.academyClass;

        PageRequest converted = PageRequest.of(page, limit);
        Predicate idPredicate = id == 0 ? qSubject.id.isNotNull() : qSubject.id.eq(id);
        Predicate namePredicate = name != null ? qSubject.name.contains(name) : qSubject.name.isNotNull();
        final JPAQueryFactory query = new JPAQueryFactory(em);

        return query.selectDistinct(qSubject)
                .from(qSubject)
                .join(qSubject.academyClassList, qAcademyClass)
                        .where(idPredicate)
                        .where(namePredicate)
                        .where(qAcademyClass.in(academyClassList))
                        .limit(converted.getPageSize())
                        .orderBy(qSubject.createTime.desc())
                        .fetch();
    }



}
