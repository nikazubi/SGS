package mthiebi.sgs.repository;

import mthiebi.sgs.models.SystemUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemUserRepository extends JpaRepository<SystemUser, Long>,
        SystemUserRepositoryCustom, QuerydslPredicateExecutor<SystemUser> {

    SystemUser findSystemUserByUsername(String userName);
}
