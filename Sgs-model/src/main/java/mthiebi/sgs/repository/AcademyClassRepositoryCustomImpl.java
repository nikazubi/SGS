package mthiebi.sgs.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import mthiebi.sgs.models.AcademyClass;
import mthiebi.sgs.models.QAcademyClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class AcademyClassRepositoryCustomImpl implements mthiebi.sgs.repository.AcademyClassRepositoryCustom {

    private static final QAcademyClass qAcademyClass = QAcademyClass.academyClass;
    @Autowired
    private JPAQueryFactory qf;

    /**
     * The classes this user may act on, narrowed by the grant they hold.
     * <p>
     * An empty grant means unrestricted, which is how {@code ClassScopeGuard}
     * reads it too: a restriction is something somebody adds, so nobody having
     * added one cannot be the same as being shut out of everything.
     * <p>
     * It used to mean the opposite, and the cost was circular. Classes only ever
     * landed in a grant as a side effect of creating one through this console,
     * so a school whose classes arrived any other way had nobody holding any -
     * and this list, which is what the form offers when granting classes to a
     * user, came back empty for the very administrator who needed to grant them.
     */
    @Override
    public List<AcademyClass> getAcademyClasses(List<AcademyClass> academyClassList, String queryKey) {
        BooleanExpression likeName = mthiebi.sgs.repository.QueryUtils.stringLike(qAcademyClass.className, queryKey);
        return qf.selectFrom(qAcademyClass)
                .where(likeName)
                .where(academyClassList == null || academyClassList.isEmpty()
                        ? null : qAcademyClass.in(academyClassList))
                .orderBy(qAcademyClass.createTime.desc())
                .fetch();
    }

    @Override
    public List<AcademyClass> getAcademyClasses(String queryKey) {
        BooleanExpression likeName = mthiebi.sgs.repository.QueryUtils.stringLike(qAcademyClass.className, queryKey);
        return qf.selectFrom(qAcademyClass)
                .where(likeName)
                .orderBy(qAcademyClass.createTime.desc())
                .fetch();
    }

    @Override
    public Optional<AcademyClass> getAcademyClassByStudent(long studentId) {
        return Optional.ofNullable(qf.selectFrom(qAcademyClass)
                .where(qAcademyClass.studentList.any().id.eq(studentId))
                .fetchOne());
    }
}