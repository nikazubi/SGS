package mthiebi.sgs.repository;

import mthiebi.sgs.models.ClosedPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ClosedPeriodRepository extends JpaRepository<ClosedPeriod, Long>,
                                                QuerydslPredicateExecutor<ClosedPeriod>,
                                                ClosedPeriodRepositoryCustom {
}
