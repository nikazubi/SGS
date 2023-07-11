package mthiebi.sgs.service;

import mthiebi.sgs.SGSException;

import java.util.Date;

public interface GradeCalculationService {
    void calculateGradeMonthly(long academyClassId, long subjectId, Date date) throws SGSException;

    void calculateBehaviourMonthly(long academyClassId, Date date);

    void calculateAbsenceMonthly(long academyClassId, long subjectId, Date date);
}
