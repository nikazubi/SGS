package mthiebi.sgs.controllers;

import mthiebi.sgs.dto.GradeDTO;
import mthiebi.sgs.dto.GradeGroupByClause;
import mthiebi.sgs.dto.GradeMapper;
import mthiebi.sgs.models.Grade;
import mthiebi.sgs.service.GradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/grade")
public class GradeController {

    @Autowired
    private GradeService gradeService;

    @Autowired
    private GradeMapper gradeMapper;

    @PostMapping("/insert-student-grade")
    public Grade insertGrade(@RequestBody GradeDTO gradeDTO){
        return gradeService.insertStudentGrade(gradeMapper.grade(gradeDTO));
    }

    @GetMapping("/get-grades")
    public List<GradeDTO> getGradeByClassAndSubject(@RequestParam(required = false) Long classId,
                                                    @RequestParam(required = false) Long subject,
                                                    @RequestParam(required = false) Long student,
                                                    @RequestParam(required = false) Date date){
        return gradeService.getStudentGradeByClassAndSubjectIdAndCreateTime(classId, subject, student, date, null)
                                                                                .stream()
                                                                                .map(gradeMapper::gradeDTO)
                                                                                .collect(Collectors.toList());
    }

    @GetMapping("/get-grades-grouped")
    public Map<Object, List<GradeDTO>> getGradeGrouped(@RequestParam(required = false) Long classId,
                                                       @RequestParam(required = false) Long subject,
                                                       @RequestParam(required = false) Long student,
                                                       @RequestParam(required = false) Date date,
                                                       @RequestParam String gradeTypePrefix,
                                                       @RequestParam GradeGroupByClause groupByClause){
        if (groupByClause == GradeGroupByClause.STUDENT ){
            return gradeService.getStudentGradeByClassAndSubjectIdAndCreateTime(classId, subject, student, date, gradeTypePrefix)
                    .stream()
                    .map(gradeMapper::gradeDTO)
                    .collect(Collectors.groupingBy(grade -> grade.getStudent().getId()));
        } else {
            return gradeService.getStudentGradeByClassAndSubjectIdAndCreateTime(classId, subject, student, date, gradeTypePrefix)
                    .stream()
                    .map(gradeMapper::gradeDTO)
                    .collect(Collectors.groupingBy(grade -> grade.getSubject().getId()));
        }
    }

}
