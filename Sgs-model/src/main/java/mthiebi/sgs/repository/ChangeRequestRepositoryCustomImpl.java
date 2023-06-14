package mthiebi.sgs.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import mthiebi.sgs.models.AcademyClass;
import mthiebi.sgs.models.ChangeRequest;
import mthiebi.sgs.models.QChangeRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ChangeRequestRepositoryCustomImpl implements ChangeRequestRepositoryCustom{

    private static final QChangeRequest qChangeRequest = QChangeRequest.changeRequest;

    @Autowired
    private JPAQueryFactory qf;


    @Override
    public List<ChangeRequest> getChangeRequests(List<AcademyClass> academyClassList) {
        return qf.selectFrom(qChangeRequest)
                .where(qChangeRequest.academyClass.in(academyClassList))
                .orderBy(qChangeRequest.createTime.desc())
                .fetch();
    }
}
