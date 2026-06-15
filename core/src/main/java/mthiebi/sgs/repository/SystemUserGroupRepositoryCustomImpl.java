package mthiebi.sgs.repository;
import lombok.RequiredArgsConstructor;

import com.querydsl.jpa.impl.JPAQueryFactory;
import mthiebi.sgs.models.QSystemUserGroup;
import mthiebi.sgs.models.SystemUserGroup;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SystemUserGroupRepositoryCustomImpl implements SystemUserGroupRepositoryCustom {

    private static final QSystemUserGroup qSystemUserGroup = QSystemUserGroup.systemUserGroup;


    private final JPAQueryFactory qf;


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
