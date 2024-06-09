package mthiebi.sgs.repository;

import mthiebi.sgs.models.AbsenceGrade;
import mthiebi.sgs.models.AbsenceGradeType;

import java.util.List;

public interface AbsenceRepositoryCustom {

    List<AbsenceGrade> findAbsenceGrade(Long academyClassId, Long studentId, int year, int endYear);

    AbsenceGrade findAbsenceGrade(Long academyClassId, Long studentId, int year, AbsenceGradeType gradeType);
}
