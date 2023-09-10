package mthiebi.sgs.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import mthiebi.sgs.models.QSystemUserGroup;
import mthiebi.sgs.models.SystemUserGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class SystemUserGroupRepositoryCustomImpl implements SystemUserGroupRepositoryCustom {

    private static final QSystemUserGroup qSystemUserGroup = QSystemUserGroup.systemUserGroup;


    @Autowired
    private JPAQueryFactory qf;


    @Override
    public List<SystemUserGroup> findByNameAndPermission(String name, String permission) {
        return qf.select(qSystemUserGroup)
                .from(qSystemUserGroup)
                .where(QueryUtils.stringLike(qSystemUserGroup.name, name)
                        .and(QueryUtils.stringLike(qSystemUserGroup.permissions, permission)))
                .orderBy(qSystemUserGroup.id.asc())
                .fetch();
    }
}
