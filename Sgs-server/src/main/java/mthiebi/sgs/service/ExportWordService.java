package mthiebi.sgs.service;

import mthiebi.sgs.models.Student;
import mthiebi.sgs.models.Subject;

import java.math.BigDecimal;
import java.util.Map;

public interface ExportWordService {

    byte[] exportSemesterGrades(Map<Student, Map<Subject, Map<Integer, BigDecimal>>> grades, boolean semester, boolean isDecimal, String semesterYears);
}
