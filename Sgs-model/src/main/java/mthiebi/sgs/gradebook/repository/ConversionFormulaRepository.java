package mthiebi.sgs.gradebook.repository;

import mthiebi.sgs.gradebook.model.ConversionFormula;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversionFormulaRepository extends JpaRepository<ConversionFormula, Long> {

    /**
     * There is one formula. Returned as a list rather than Optional because
     * nothing at the database level enforces the single row, and a repository
     * that throws on an unexpected second one would take the grid down over a
     * display setting.
     */
    List<ConversionFormula> findAllByOrderByIdAsc();
}
