package mthiebi.sgs.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import mthiebi.sgs.models.AcademyClass;
import mthiebi.sgs.models.QAcademyClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AcademyClassRepositoryCustomImpl implements mthiebi.sgs.repository.AcademyClassRepositoryCustom {

    private static final QAcademyClass qAcademyClass = QAcademyClass.academyClass;
    @Autowired
    private JPAQueryFactory qf;

    @Override
    public List<AcademyClass> getAcademyClasses(List<AcademyClass> academyClassList, String queryKey) {
        BooleanExpression likeName = mthiebi.sgs.repository.QueryUtils.stringLike(qAcademyClass.className, queryKey);
        return qf.selectFrom(qAcademyClass)
                .where(likeName)
                .where(qAcademyClass.in(academyClassList))
                .orderBy(qAcademyClass.createTime.desc())
                .fetch();
    }
}