package mthiebi.sgs.controllers;

import mthiebi.sgs.models.ClosedPeriod;
import mthiebi.sgs.service.ClosedPeriodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/close-period")
public class ClosedPeriodController {

    @Autowired
    private ClosedPeriodService closedPeriodService;

    //todo: create DTO
    @GetMapping("/get-period-by-class")
    public ClosedPeriod getClosedPeriodByClass(@RequestParam Long academyClassId,
                                               @RequestParam String gradePrefix,
                                               @RequestParam Long gradeId){
        return closedPeriodService.getClosedPeriodByClassId(academyClassId, gradePrefix, gradeId);
    }

    @GetMapping("/create-closed-period")
    public ClosedPeriod createclosedPeriod(@RequestBody ClosedPeriod closedPeriod){
        return closedPeriodService.createClosedPeriod(closedPeriod);
    }

}
