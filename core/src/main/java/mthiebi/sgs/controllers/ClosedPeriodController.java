package mthiebi.sgs.controllers;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.models.ClosedPeriod;
import mthiebi.sgs.models.SystemUser;
import mthiebi.sgs.service.ClosedPeriodService;
import mthiebi.sgs.service.SystemUserService;
import mthiebi.sgs.utils.AuthConstants;
import mthiebi.sgs.utils.UtilsJwt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@RestController
@RequestMapping("/close-period")
public class ClosedPeriodController {

    @Autowired
    private ClosedPeriodService closedPeriodService;

    @Autowired
    private UtilsJwt utilsJwt;

    //todo: create DTO
    @GetMapping("/get-period-by-class")
    public boolean getClosedPeriodByClass(@RequestParam Long academyClassId,
                                           @RequestParam String gradePrefix,
                                           @RequestParam(required = false) Long gradeId) throws SGSException {
        return closedPeriodService.getClosedPeriodByClassId(academyClassId, gradePrefix, gradeId);
    }

    @GetMapping("/create-closed-period")
    @Secured({AuthConstants.MANAGE_CLOSED_PERIOD})
    public ClosedPeriod createclosedPeriod(@RequestHeader("authorization") String authHeader) throws Exception {

        String username = utilsJwt.getUsernameFromHeader(authHeader);
        return closedPeriodService.createClosedPeriod(username);
    }

}
