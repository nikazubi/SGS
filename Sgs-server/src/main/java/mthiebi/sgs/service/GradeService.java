package mthiebi.sgs.service;

import mthiebi.sgs.models.AcademyClass;
import mthiebi.sgs.models.Grade;

import java.util.Date;
import java.util.List;

public interface GradeService {

    Grade insertStudentGrade(Grade grade);

    Grade updateStudentGrade(Grade grade);

    void deleteGrade(Long id);

    List<Grade> getStudentGradeByStudentId(Long studentId);

    List<Grade> getStudentGradeByClassId(Long classId);

    List<Grade> getStudentGradeByClassAndSubjectId(Long classId, Long subjectId);

    List<Grade> getStudentGradeByClassAndSubjectIdAndCreateTime(Long classId, Long subjectId, Long studentId, Date createTime);

}
