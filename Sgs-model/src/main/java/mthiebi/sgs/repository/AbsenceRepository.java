package mthiebi.sgs.repository;

import mthiebi.sgs.models.AbsenceGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AbsenceRepository extends JpaRepository<AbsenceGrade, Long>,
        PagingAndSortingRepository<AbsenceGrade, Long>,
        AbsenceRepositoryCustom {

}
