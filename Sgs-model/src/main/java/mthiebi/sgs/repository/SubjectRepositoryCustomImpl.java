package mthiebi.sgs.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import mthiebi.sgs.models.QSubject;
import mthiebi.sgs.models.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class SubjectRepositoryCustomImpl implements SubjectRepositoryCustom {

    private static final QSubject qSubject = QSubject.subject;

    @Autowired
    private JPAQueryFactory qf;


    @Override
    public List<Subject> findByIds(List<Long> ids) {
        return qf.select(qSubject)
                .from(qSubject)
                .where(QueryUtils.longIn(qSubject.id, ids))
                .fetch();
    }
}
