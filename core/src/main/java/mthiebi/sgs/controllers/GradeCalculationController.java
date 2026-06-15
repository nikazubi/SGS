package mthiebi.sgs.controllers;
import lombok.RequiredArgsConstructor;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.service.GradeCalculationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@RestController
@RequestMapping("/calculate-grade")
@RequiredArgsConstructor
public class GradeCalculationController {

    private final GradeCalculationService gradeCalculationService;

    @GetMapping("/grades-monthly")
    public void calculateGradeMonthly(@RequestParam long academyClassId,
                                      @RequestParam long subjectId,
                                      @RequestParam String date) throws SGSException {
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
