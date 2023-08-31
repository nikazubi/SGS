package mthiebi.sgs.service;

import mthiebi.sgs.models.Student;
import mthiebi.sgs.models.Subject;

import java.math.BigDecimal;
import java.util.Map;

public interface ExportWordService {

    void exportSemesterGrades(Map<Student, Map<Subject, Map<Integer, BigDecimal>>> grades, boolean semester);
}
