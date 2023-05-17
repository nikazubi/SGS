package mthiebi.sgs.controllers;

import mthiebi.sgs.dto.GradeDTO;
import mthiebi.sgs.dto.GradeMapper;
import mthiebi.sgs.models.Grade;
import mthiebi.sgs.service.GradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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



}
