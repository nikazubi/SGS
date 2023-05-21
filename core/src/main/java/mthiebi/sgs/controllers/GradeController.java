package mthiebi.sgs.controllers;

import mthiebi.sgs.dto.GradeDTO;
import mthiebi.sgs.dto.GradeMapper;
import mthiebi.sgs.models.Grade;
import mthiebi.sgs.service.GradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
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

    @GetMapping("/get-grade-by-class-and-subject")
    public List<GradeDTO> getGradeByClassAndSubject(@RequestParam Long classId,
                                                    @RequestParam Long subject,
                                                    @RequestParam Date date){
        return gradeService.getStudentGradeByClassAndSubjectIdAndCreateTime(classId, subject, date)
                                                                                .stream()
                                                                                .map(gradeMapper::gradeDTO)
                                                                                .collect(Collectors.toList());
    }

}
