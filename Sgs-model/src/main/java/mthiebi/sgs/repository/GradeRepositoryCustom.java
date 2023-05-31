package mthiebi.sgs.repository;

import mthiebi.sgs.models.Grade;

import java.util.Date;
import java.util.List;

public interface GradeRepositoryCustom {

    List<Grade> findGradeByAcademyClassIdAndSubjectIdAndCreateTime(Long academyClassId, Long subjectId, Long studentId, Date createTim);
}
