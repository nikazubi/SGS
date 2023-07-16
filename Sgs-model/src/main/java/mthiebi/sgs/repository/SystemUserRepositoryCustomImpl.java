package mthiebi.sgs.repository;

import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQueryFactory;
import mthiebi.sgs.models.QSystemUser;
import mthiebi.sgs.models.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class SystemUserRepositoryCustomImpl implements SystemUserRepositoryCustom{

    private static final QSystemUser qSystemUser = QSystemUser.systemUser;

    @Autowired
    private JPAQueryFactory qf;

    @Override
    public List<SystemUser> filter(String username, String name, Boolean active) {
        Predicate usernamePredicate = username != null ? qSystemUser.username.contains(username) : qSystemUser.username.isNotNull();
        Predicate namePredicate = name != null ? qSystemUser.name.contains(name) : qSystemUser.name.isNotNull();
        Predicate activePredicate = active != null ? qSystemUser.active.eq(active) : qSystemUser.active.isNotNull();

        return qf.selectFrom(qSystemUser)
                .where(usernamePredicate)
                .where(namePredicate)
                .where(activePredicate)
                .orderBy(qSystemUser.createTime.desc())
                .fetch();
    }
}
