package mthiebi.sgs.gradebook.repository;

import mthiebi.sgs.gradebook.model.ParentModule;
import mthiebi.sgs.gradebook.model.ParentModuleGrant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ParentModuleGrantRepository extends JpaRepository<ParentModuleGrant, Long> {

    /**
     * Every module granted to a class in this school at this level.
     * <p>
     * The school is an explicit left join. Writing {@code g.school.id = :school}
     * would make Hibernate inner-join the school, which silently drops the rows
     * with no school - the ones meant for everybody.
     */
    @Query("select distinct g.module from ParentModuleGrant g left join g.school s "
            + "where (s is null or s.id = :school) "
            + "and (g.levelFrom is null or g.levelFrom <= :level) "
            + "and (g.levelTo is null or g.levelTo >= :level)")
    List<ParentModule> findModulesFor(@Param("school") Long schoolId, @Param("level") short level);
}
