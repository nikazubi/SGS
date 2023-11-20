package mthiebi.sgs.repository;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.models.Grade;
import mthiebi.sgs.models.GradeType;
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

    List<Grade> findGradeByAcademyClassIdAndSubjectIdAndCreateTime(Long academyClassId,
                                                                   Long subjectId,
                                                                   Long studentId,
                                                                   Date createTime,
                                                                   Date closedPeriod);

    Map<Student, Map<Subject, Map<Integer, BigDecimal>>> findGradeBySemester(Long classId, int year, boolean firstSemester);

    Map<Student, Map<Subject, Map<Integer, BigDecimal>>> findGradeBySemester(Long classId, int year, boolean firstSemester, Date closedPeriod);

    Map<Student, List<Grade>> findGradeByMonth(Long classId, Date createDate) throws SGSException;

    Integer getMinYear();

    Integer getMaxYear();

    List<Grade> findGradeByAcademyClassIdAndSubjectIdAndGradeTypeAndYear(Long academyClassId, Long subjectId, Long studentId, GradeType gradeType, int maxYear);

    List<Grade> findGradeByAcademyClassIdAndSubjectIdAndGradeTypeAndYear(Long academyClassId, Long subjectId, Long studentId, GradeType gradeType, int maxYear, Date latest);

    Grade findGradeByAcademyClassIdAndSubjectIdAndGradeTypeAndExactMonth(Long academyClassId, Long subjectId, Long studentId, GradeType gradeType, Date exactMonth);

    List<Grade> findGradeByAcademyClassIdAndSubjectIdAndExactMonthAndYear(Long academyClassId, Long subjectId, Long studentId, int month, int year, Date latest);

    List<Grade> findGradeByAcademyClassIdAndSubjectIdAndGradeTypeAndExactMonthAndYear(Long academyClassId, Long subjectId, Long studentId, GradeType gradeType, int month, int year, Date latest);

    List<Grade> findGradeByAcademyClassIdAndSubjectIdAndGradeTypeAndExactMonthAndYear(Long academyClassId, Long studentId, GradeType gradeType, Long month, int startYear, int endYear, Date latest);

    BigDecimal findTotalAbsenceHours(long id, Date createDate);

    BigDecimal findBehaviourMonth(long id, Date createDate);
}
