package mthiebi.sgs.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import mthiebi.sgs.models.AcademyClass;
import mthiebi.sgs.models.QAcademyClass;
import mthiebi.sgs.models.QGrade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AcademyClassRepositoryCustomImpl implements AcademyClassRepositoryCustom {

    private static final QAcademyClass qAcademyClass = QAcademyClass.academyClass;
    @Autowired
    private JPAQueryFactory qf;

    @Override
    public List<AcademyClass> getAcademyClasses(String queryKey) {
        BooleanExpression likeName = QueryUtils.stringLike(qAcademyClass.className, queryKey);
        return qf.selectFrom(qAcademyClass)
                .where(likeName)
                .orderBy(qAcademyClass.createTime.desc())
                .fetch();
    }
}
