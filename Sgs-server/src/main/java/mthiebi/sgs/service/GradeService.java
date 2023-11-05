package mthiebi.sgs.service;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.models.AcademyClass;
import mthiebi.sgs.models.Grade;
import mthiebi.sgs.models.Student;
import mthiebi.sgs.models.Subject;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

public interface GradeService {

    Grade insertStudentGrade(Grade grade);

    Grade updateStudentGrade(Grade grade);

    void deleteGrade(Long id);

    List<Grade> getStudentGradeByStudentId(Long studentId);

    List<Grade> getStudentGradeByClassId(Long classId);

    List<Grade> getStudentGradeByClassAndSubjectId(Long classId, Long subjectId);

    List<Grade> getStudentGradeByClassAndSubjectIdAndCreateTime(Long classId,
                                                                Long subjectId,
                                                                Long studentId,
                                                                Date createTime,
                                                                String gradeTypePrefix);
    Object getGradeByComponent(Long classId,
                                                               Long studentId,
                                                               String yearRange,
                                                               Date createDate,
                                                               String component) throws SGSException;

    List<String> getGradeYearGrouped();

    byte[] exportPdfMonthlyGrade(Long classId, Long studentId, Date month);

    List<Grade> findGradeByAcademyClassIdAndSubjectIdAndGradeTypeAndExactMonthAndYear(String studentUsername,
                                                                                      Long subjectId,
                                                                                      Long month,
                                                                                      Long year,
                                                                                      String gradeTypePrefix);

    List<Grade> findAllMonthlyGradesForSubjectInYear(String studentUsername, Long subjectId, Long year);

    List<Grade> findAllMonthlyGradesForMonthAndYear(String studentUsername, Long month, Long year);

    List<Grade> findAllBehaviourGradesForMonthAndYear(String studentUsername, Long month, Long year);

    Object getGradeByComponent(String userName, String yearRange, Date date, String component) throws SGSException;
}
