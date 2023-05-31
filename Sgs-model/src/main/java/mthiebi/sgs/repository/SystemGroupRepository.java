package mthiebi.sgs.repository;

import mthiebi.sgs.models.SystemUserGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemGroupRepository extends JpaRepository<SystemUserGroup, Long> {
}
