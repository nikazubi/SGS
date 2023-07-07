package mthiebi.sgs.service;

import java.util.Date;

public interface GradeCalculationService {
    void calculateGradeMonthly(long academyClassId, long subjectId, Date date);

    void calculateBehaviourMonthly(long academyClassId, Date date);

    void calculateAbsenceMonthly(long academyClassId, long subjectId, Date date);
}
