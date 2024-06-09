package mthiebi.sgs.service;

import mthiebi.sgs.models.AbsenceGrade;
import mthiebi.sgs.models.AbsenceGradeType;
import mthiebi.sgs.models.Student;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

public interface AbsenceService {

    Map<Student, List<AbsenceGrade>> findAbsenceGrade(Long academyClassId, Long studentId, String yearRange);

    AbsenceGrade addAbsenceGrade(Long studentId, Long academyClassId, AbsenceGradeType gradeType, BigDecimal value, Date exactMonth);
}
