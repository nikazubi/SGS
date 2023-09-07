package mthiebi.sgs.controllers;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.dto.AcademyClassDTO;
import mthiebi.sgs.dto.AcademyClassListDto;
import mthiebi.sgs.dto.AcademyClassMapper;
import mthiebi.sgs.dto.ClosePeriodDto;
import mthiebi.sgs.models.ClosedPeriod;
import mthiebi.sgs.service.AcademyClassService;
import mthiebi.sgs.service.ClosedPeriodService;
import mthiebi.sgs.utils.AuthConstants;
import mthiebi.sgs.utils.UtilsJwt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/close-period")
public class ClosedPeriodController {

    @Autowired
    private ClosedPeriodService closedPeriodService;

    @Autowired
    private UtilsJwt utilsJwt;

    @Autowired
    private AcademyClassService academyClassService;

    @Autowired
    private AcademyClassMapper academyClassMapper;

    //todo: create DTO
    @GetMapping("/get-period-by-class")
    public boolean getClosedPeriodByClass(@RequestParam Long academyClassId,
                                          @RequestParam String gradePrefix,
                                          @RequestParam(required = false) Long gradeId) throws SGSException {
        return closedPeriodService.getClosedPeriodByClassId(academyClassId, gradePrefix, gradeId);
    }

    @PostMapping("/create-closed-period")
    @Secured({AuthConstants.MANAGE_CLOSED_PERIOD})
    public ClosedPeriod createclosedPeriod(@RequestHeader("authorization") String authHeader,
                                           @RequestBody AcademyClassListDto academyClasses) throws Exception {

        String username = utilsJwt.getUsernameFromHeader(authHeader);
        List<Long> ids = academyClasses.getAcademyClassDTOS() == null || academyClasses.getAcademyClassDTOS().isEmpty() ?
                null : academyClasses.getAcademyClassDTOS().stream().map(AcademyClassDTO::getId).collect(Collectors.toList());
        return closedPeriodService.createClosedPeriod(username, ids);
    }

    @GetMapping("/get-closed-period-ordered")
    public List<ClosePeriodDto> getClosedPeriods(@RequestParam(required = false) Long academyClassId,
                                                 @RequestParam(required = false) String dateFrom,
                                                 @RequestParam(required = false) String dateTo) {
        Date date1 = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date1);
        calendar.add(Calendar.MONTH, -1);
        date1 = calendar.getTime();
        if (dateFrom != null && !dateFrom.equals("NaN")) {
            date1.setTime(Long.parseLong(dateFrom));
        }

        Date date2 = new Date();
        if (dateTo != null && !dateTo.equals("NaN")) {
            date2.setTime(Long.parseLong(dateTo));
        }
        return closedPeriodService.findAllOrdered(academyClassId, date1, date2).stream()
                .map(closedPeriod -> ClosePeriodDto.builder()
                        .id(closedPeriod.getId())
                        .gradePrefix(closedPeriod.getGradePrefix())
                        .createTime(closedPeriod.getCreateTime())
                        .lastUpdateTime(closedPeriod.getLastUpdateTime())
                        .academyClass(academyClassMapper.academyClassDTO(academyClassService.findAcademyClassById(closedPeriod.getAcademyClassId())))
                        .build()).collect(Collectors.toList());
    }

}
