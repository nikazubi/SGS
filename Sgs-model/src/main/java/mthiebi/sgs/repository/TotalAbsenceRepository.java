package mthiebi.sgs.repository;

import mthiebi.sgs.models.SystemUser;
import mthiebi.sgs.models.TotalAbsence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TotalAbsenceRepository extends JpaRepository<TotalAbsence, Long>,
                                                    TotalAbsenceRepositoryCustom{
}
