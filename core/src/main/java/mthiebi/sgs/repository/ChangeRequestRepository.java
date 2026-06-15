package mthiebi.sgs.repository;

import mthiebi.sgs.models.ChangeRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ChangeRequestRepository extends JpaRepository<ChangeRequest, Long>,
                                                    QuerydslPredicateExecutor<ChangeRequest>,
                                                    ChangeRequestRepositoryCustom {

}
