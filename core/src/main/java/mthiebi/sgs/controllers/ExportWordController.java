package mthiebi.sgs.controllers;

import mthiebi.sgs.models.Student;
import mthiebi.sgs.models.Subject;
import mthiebi.sgs.service.ExportWordService;
import mthiebi.sgs.service.GradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

@RestController
@RequestMapping("/export")
public class ExportWordController {

    @Autowired
    private GradeService gradeService;

    @Autowired
    private ExportWordService exportWordService;

    @GetMapping("/semester-word")
    public void calculateGradeMonthly(@RequestParam Long classId,
                                      @RequestParam(required = false) Long studentId,
                                      @RequestParam(required = false) String yearRange,
                                      @RequestParam(required = false) String createDate,
                                      @RequestParam String component) throws Exception {
        Date date = new Date();
        if (createDate != null) {
            date.setTime(Long.parseLong(createDate));
        }
        Map<Student, Map<Subject, Map<Integer, BigDecimal>>> map = (Map<Student, Map<Subject, Map<Integer, BigDecimal>>>) gradeService.getGradeByComponent(classId, studentId, yearRange, date, component);
        exportWordService.exportSemesterGrades(map, component);
    }

}
