package mthiebi.sgs.gradebook.repository;

import mthiebi.sgs.gradebook.model.ClassPeriodSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassPeriodSettingRepository extends JpaRepository<ClassPeriodSetting, Long> {

    @Query("select s from ClassPeriodSetting s "
            + "where s.classGroup.id = :classGroupId and s.period.id = :periodId")
    List<ClassPeriodSetting> findFor(@Param("classGroupId") Long classGroupId,
                                     @Param("periodId") Long periodId);

    /**
     * A whole grid's worth in one query.
     * <p>
     * The register's two figures - the month's academic hours and the permitted
     * absence - are per class *per month*, so a grid spanning a year needs nine
     * pairs. Reading them a period at a time would be nine queries to draw one
     * header.
     */
    @Query("select s from ClassPeriodSetting s "
            + "where s.classGroup.id = :classGroupId and s.period.id in :periodIds")
    List<ClassPeriodSetting> findForPeriods(@Param("classGroupId") Long classGroupId,
                                            @Param("periodIds") List<Long> periodIds);

    @Query("select s from ClassPeriodSetting s "
            + "where s.classGroup.id = :classGroupId and s.period.id = :periodId "
            + "and s.settingKey = :key")
    Optional<ClassPeriodSetting> findOne(@Param("classGroupId") Long classGroupId,
                                         @Param("periodId") Long periodId,
                                         @Param("key") String key);
}
