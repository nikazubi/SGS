package mthiebi.sgs.repository;

import mthiebi.sgs.models.Grade;
import mthiebi.sgs.models.Student;
import mthiebi.sgs.models.Subject;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

public interface GradeRepositoryCustom {

    List<Grade> findGradeByAcademyClassIdAndSubjectIdAndCreateTime(Long academyClassId,
                                                                   Long subjectId,
                                                                   Long studentId,
                                                                   Date createTim);

    Map<Student, Map<Subject, BigDecimal>> findGradeBySemester(Long classId, int year, boolean firstSemester);

    Map<Student, List<Grade>> findGradeByMonth(Long classId, Date createDate);

    Integer getMinYear();

    Integer getMaxYear();
}
