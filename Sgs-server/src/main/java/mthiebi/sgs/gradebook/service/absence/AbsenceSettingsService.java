package mthiebi.sgs.gradebook.service.absence;

import mthiebi.sgs.gradebook.model.ClassGroup;
import mthiebi.sgs.gradebook.model.ClassPeriodSetting;
import mthiebi.sgs.gradebook.model.Period;
import mthiebi.sgs.gradebook.repository.ClassPeriodSettingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.math.BigDecimal;

/**
 * The brief's two numbers: the month's academic hours, and how many a student
 * may miss.
 * <p>
 * ClassPeriodSetting was built in phase 1 with exactly these keys, per class per
 * period, so this is only the service that reads and writes them.
 * <p>
 * The permitted figure is what turns the absence chart from green to red.
 */
@Service
public class AbsenceSettingsService {

    @Autowired
    private ClassPeriodSettingRepository classPeriodSettingRepository;

    @PersistenceContext
    private EntityManager em;

    /**
     * A null value clears the setting rather than storing a null one, so
     * "not set" has one representation instead of two.
     */
    @Transactional(rollbackFor = Exception.class)
    public void save(Long classGroupId, Long periodId,
                     BigDecimal totalAcademicHours, BigDecimal permittedMissedHours) {
        put(classGroupId, periodId, AbsenceSettings.TOTAL_ACADEMIC_HOURS, totalAcademicHours);
        put(classGroupId, periodId, AbsenceSettings.PERMITTED_MISSED_HOURS, permittedMissedHours);
    }

    private void put(Long classGroupId, Long periodId, String key, BigDecimal value) {
        ClassPeriodSetting existing = classPeriodSettingRepository
                .findOne(classGroupId, periodId, key).orElse(null);

        if (value == null) {
            if (existing != null) {
                classPeriodSettingRepository.delete(existing);
            }
            return;
        }
        if (existing == null) {
            existing = new ClassPeriodSetting();
            existing.setClassGroup(em.getReference(ClassGroup.class, classGroupId));
            existing.setPeriod(em.getReference(Period.class, periodId));
            existing.setSettingKey(key);
        }
        existing.setSettingValue(value);
        classPeriodSettingRepository.save(existing);
    }
}
