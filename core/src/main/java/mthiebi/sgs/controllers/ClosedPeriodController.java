package mthiebi.sgs.controllers;

import mthiebi.sgs.models.ClosedPeriod;
import mthiebi.sgs.service.ClosedPeriodService;
import mthiebi.sgs.utils.AuthConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
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
                                               @RequestParam(required = false) Long gradeId){
        return closedPeriodService.getClosedPeriodByClassId(academyClassId, gradePrefix, gradeId);
    }

    @GetMapping("/create-closed-period")
    @Secured({AuthConstants.MANAGE_CLOSED_PERIOD})
    public ClosedPeriod createclosedPeriod(@RequestParam long academyClassId){
        return closedPeriodService.createClosedPeriod(academyClassId);
    }

}
