package mthiebi.sgs.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import mthiebi.sgs.models.QStudent;
import mthiebi.sgs.models.Student;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class StudentRepositoryCustomImpl implements StudentRepositoryCustom {

    private static final QStudent qStudent = QStudent.student;
    @Autowired
    private JPAQueryFactory qf;

    @Override
    public Student authStudent(String username, String password) {
        return qf.select(qStudent)
                .from(qStudent)
                .where(qStudent.username.eq(username))
                .where(qStudent.password.eq(password))
                .fetchOne();
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
