package mthiebi.sgs.controllers;

import mthiebi.sgs.service.GradeCalculationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@RestController
@RequestMapping("/calculate-grade")
public class GradeCalculationController {

    @Autowired
    private GradeCalculationService gradeCalculationService;

    @GetMapping("/grades-monthly")
    public void calculateGradeMonthly(@RequestParam long academyClassId,
                                      @RequestParam long subjectId,
                                      @RequestParam String date) throws Exception {
        Date date1 = new Date();
        date1.setTime(Long.parseLong(date));
        gradeCalculationService.calculateGradeMonthly(academyClassId, subjectId, date1);
    }

    @GetMapping("/behaviour-monthly")
    public void calculateBehaviourMonthly(@RequestParam long academyClassId,
                                          @RequestParam String date){
        Date date1 = new Date();
        date1.setTime(Long.parseLong(date));
        gradeCalculationService.calculateBehaviourMonthly(academyClassId, date1);
    }

    @GetMapping("/absence-monthly")
    public void calculateAbsenceMonthly(){

    }
}
