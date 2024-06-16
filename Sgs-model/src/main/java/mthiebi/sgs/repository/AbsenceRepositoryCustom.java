package mthiebi.sgs.repository;

import mthiebi.sgs.models.AbsenceGrade;
import mthiebi.sgs.models.AbsenceGradeType;

import java.util.List;

public interface AbsenceRepositoryCustom {

    List<AbsenceGrade> findAbsenceGrade(Long academyClassId, Long studentId, int year, int endYear);

    AbsenceGrade findAbsenceGradeByMonthAndYear(Long academyClassId, Long studentId, int year, int month);

    AbsenceGrade findAbsenceGrade(Long academyClassId, Long studentId, int year, AbsenceGradeType gradeType);

    List<AbsenceGrade> findAbsenceGrade(Long studentId, int startYear, int endYear, Long month);

    AbsenceGrade findAbsenceGrade(Long studentId, int month);
}
