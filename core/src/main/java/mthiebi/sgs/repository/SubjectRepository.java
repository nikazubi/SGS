package mthiebi.sgs.repository;

import com.querydsl.core.types.Predicate;
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
import java.util.stream.Collectors;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long>, SubjectRepositoryCustom, QuerydslPredicateExecutor<Subject> {

    default List<Subject> findAllSubject(int limit,
                                         int page,
                                         Long id,
                                         String name,
                                         List<AcademyClass> academyClassList,
                                         EntityManager em) {

        QSubject qSubject = QSubject.subject;
        QAcademyClass qAcademyClass = QAcademyClass.academyClass;

        //todo
        List<Long> uniqueSubjects = academyClassList.stream()
                .flatMap(academyClass -> academyClass.getSubjectList().stream().map(Subject::getId))
                .distinct()
                .collect(Collectors.toList());


        PageRequest converted = PageRequest.of(page, limit);
        Predicate idPredicate = id == 0 ? qSubject.id.isNotNull() : qSubject.id.eq(id);
        Predicate namePredicate = name != null ? qSubject.name.contains(name) : qSubject.name.isNotNull();
        final JPAQueryFactory query = new JPAQueryFactory(em);

        return query.selectDistinct(qSubject)
                .from(qSubject)
                        .where(idPredicate)
                        .where(namePredicate)
                        .where(qSubject.id.in(uniqueSubjects))
                        .limit(converted.getPageSize())
                        .orderBy(qSubject.createTime.desc())
                        .fetch();
    }

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

        return query.selectDistinct(qSubject)
                .from(qSubject)
                .where(idPredicate)
                .where(namePredicate)
                .limit(converted.getPageSize())
                .orderBy(qSubject.createTime.desc())
                .fetch();
    }
}
