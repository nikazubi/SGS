package mthiebi.sgs.service;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.models.AcademyClass;
import mthiebi.sgs.models.TotalAbsence;

import java.util.Date;
import java.util.List;

public interface TotalAbsenceService {

    List<TotalAbsence> filter(Long academyClass, Date activePeriod);

    List<TotalAbsence> filter(String username, Date activePeriod);

    List<TotalAbsence> getCurrentAbsencesForEveryClass();

    void addTotalAbsenceToAcademyClass(long academyClassId, TotalAbsence totalAbsence) throws SGSException;

    void addTotalAbsenceToAcademyClasses(List<AcademyClass> academyClasses, Long totalAcademicHour, Date activePeriod ) throws SGSException;

    void deleteById(long id) throws SGSException;
}
