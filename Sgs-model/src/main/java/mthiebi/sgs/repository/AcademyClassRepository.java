package mthiebi.sgs.repository;

import mthiebi.sgs.models.AcademyClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AcademyClassRepository extends JpaRepository<AcademyClass, Long> {
}
