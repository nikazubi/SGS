package mthiebi.sgs.repository;

import mthiebi.sgs.models.TotalAbsence;

import java.util.Date;
import java.util.List;

public interface TotalAbsenceRepositoryCustom {

    List<TotalAbsence> filter(Long academyClassId, Date activePeriod);
}
