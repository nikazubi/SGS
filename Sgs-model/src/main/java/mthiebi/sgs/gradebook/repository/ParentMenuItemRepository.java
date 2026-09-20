package mthiebi.sgs.gradebook.repository;

import mthiebi.sgs.gradebook.model.ParentMenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ParentMenuItemRepository extends JpaRepository<ParentMenuItem, Long> {

    /**
     * Every menu item, in landing-page order, with its journal already loaded.
     * <p>
     * Joined rather than lazy: the caller needs the journal of every row to
     * decide whether the child's school gets it, so leaving it lazy is one
     * query per button.
     */
    @Query("select m from ParentMenuItem m join fetch m.template order by m.ordinal, m.id")
    List<ParentMenuItem> findAllOrdered();
}
