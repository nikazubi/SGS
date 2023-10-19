package mthiebi.sgs.repository;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import mthiebi.sgs.models.AcademyClass;
import mthiebi.sgs.models.QAcademyClass;
import mthiebi.sgs.models.QStudent;
import mthiebi.sgs.models.Student;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static mthiebi.sgs.repository.QueryUtils.True;

@Repository
public class StudentRepositoryCustomImpl implements StudentRepositoryCustom {

    private static final QStudent qStudent = QStudent.student;
    private static final QAcademyClass qAcademyClass = QAcademyClass.academyClass;
    @Autowired
    private JPAQueryFactory qf;

    @Override
    public List<Student> findAllStudent(int limit, int page, Long id, String firstName, String lastName, String personalNumber) {
        QStudent qStudent = QStudent.student;
        PageRequest converted = PageRequest.of(page, limit);

        Predicate idPredicate = id == 0 ? qStudent.id.isNotNull() : qStudent.id.eq(id);
        Predicate firstNamePredicate = StringUtils.isNotBlank(firstName) ? qStudent.firstName.contains(firstName) : qStudent.firstName.isNotNull();
        Predicate lastNamePredicate = StringUtils.isNotBlank(lastName) ? qStudent.lastName.contains(lastName) : qStudent.lastName.isNotNull();
        Predicate personalNumberPredicate = StringUtils.isNotBlank(personalNumber) ? qStudent.personalNumber.contains(personalNumber) : qStudent.personalNumber.isNotNull();

        return qf.selectFrom(qStudent)
                .where(idPredicate)
                .where(firstNamePredicate)
                .where(lastNamePredicate)
                .where(personalNumberPredicate)
//                .offset(converted.getOffset())
                .limit(converted.getPageSize())
                .orderBy(qStudent.createTime.desc())
                .fetch();
    }

    @Override
    public List<Student> findByNameAndSurname(List<AcademyClass> academyClassList, String queryKey) {

        QAcademyClass qAcademyClass = QAcademyClass.academyClass;
        BooleanExpression likeNameAndSurname = qStudent.firstName.concat(" " + qStudent.lastName)
                .likeIgnoreCase(Expressions.asString("%") + queryKey + Expressions.asString("%"));

        return qf.selectFrom(qStudent)
                .join(qAcademyClass)
                .on(qStudent.in(qAcademyClass.studentList))
                .where(qAcademyClass.in(academyClassList))
                .where(likeNameAndSurname)
                .orderBy(qStudent.createTime.desc())
                .fetch();
    }

    @Override
    public List<Student> findByNameAndSurname(String queryKey) {

        BooleanExpression likeNameAndSurname = qStudent.firstName.concat(" " + qStudent.lastName)
                .likeIgnoreCase(Expressions.asString("%") + queryKey + Expressions.asString("%"));

        if (queryKey == null) {
            likeNameAndSurname = True();
        }

        return qf.selectFrom(qStudent)
                .where(likeNameAndSurname)
                .orderBy(qStudent.createTime.desc())
                .fetch();
    }

    @Override
    public List<Student> findAllByAcademyClass(long academyClassId) {
        return qf.select(qStudent)
                .from(qStudent)
                .join(qAcademyClass)
                .on(qStudent.in(qAcademyClass.studentList))
                .where(qAcademyClass.id.eq(academyClassId))
                .fetch();
    }

    @Override
    public Student authStudent(String username, String password) {
        return qf.select(qStudent)
                .from(qStudent)
                .where(qStudent.username.eq(username))
                .where(qStudent.password.eq(password))
                .fetchOne();
    }

    @Override
    public List<Student> findByIds(List<Long> ids) {
        return qf.select(qStudent)
                .from(qStudent)
                .where(QueryUtils.longIn(qStudent.id, ids))
                .fetch();
    }

    @Override
    public Optional<Student> findByUsername(String username) {
        return Optional.ofNullable(
                qf.select(qStudent)
                        .from(qStudent)
                        .where(QueryUtils.stringEq(qStudent.username, username))
                        .fetchOne()
        );
    }
}
