package mthiebi.sgs.controllers;

import mthiebi.sgs.dto.*;
import mthiebi.sgs.models.Grade;
import mthiebi.sgs.service.GradeService;
import mthiebi.sgs.utils.AuthConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
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
    @Secured({AuthConstants.ADD_GRADES})
    public GradeDTO insertGrade(@RequestBody GradeDTO gradeDTO){
        return gradeMapper.gradeDTO(gradeService.insertStudentGrade(gradeMapper.grade(gradeDTO)));
    }

    @GetMapping("/get-grades")
    @Secured({AuthConstants.MANAGE_GRADES})
    public List<GradeDTO> getGradeByClassAndSubject(@RequestParam(required = false) Long classId,
                                                    @RequestParam(required = false) Long subjectId,
                                                    @RequestParam(required = false) Long studentId,
                                                    @RequestParam(required = false) Date date){
        return gradeService.getStudentGradeByClassAndSubjectIdAndCreateTime(classId, subjectId, studentId, date, null)
                                                                                .stream()
                                                                                .map(gradeMapper::gradeDTO)
                                                                                .collect(Collectors.toList());
    }

    @GetMapping("/get-grades-grouped")
    @Secured({AuthConstants.MANAGE_GRADES})
    public List<GradeWrapperDto> getGradeGrouped(@RequestParam(required = false) Long classId,
                                                 @RequestParam(required = false) Long subjectId,
                                                 @RequestParam(required = false) Long studentId,
                                                 @RequestParam(required = false) String date,
                                                 @RequestParam GradeGroupByClause groupByClause,
                                                 @RequestParam(defaultValue = "GENERAL") String gradeTypePrefix){
        Date date1 = new Date();
        date1.setTime(Long.parseLong(date));
        if (groupByClause == GradeGroupByClause.STUDENT ){
            return gradeService.getStudentGradeByClassAndSubjectIdAndCreateTime(classId, subjectId, studentId, date1, gradeTypePrefix)
                    .stream()
                    .map(gradeMapper::gradeDTO)
                    .collect(Collectors.groupingBy(GradeDTO::getStudent))
                    .entrySet().stream()
                    .map(k -> GradeWrapperByStudent.builder()
                                                    .student(k.getKey())
                                                    .grades(k.getValue())
                                                    .build())
                    .collect(Collectors.toList());
        } else {
            return gradeService.getStudentGradeByClassAndSubjectIdAndCreateTime(classId, subjectId, studentId, date1, gradeTypePrefix)
                    .stream()
                    .map(gradeMapper::gradeDTO)
                    .collect(Collectors.groupingBy(GradeDTO::getSubject))
                    .entrySet().stream()
                    .map(k -> GradeWrapperBySubject.builder()
                            .subject(k.getKey())
                            .grades(k.getValue())
                            .build())
                    .collect(Collectors.toList());
        }
    }
}
