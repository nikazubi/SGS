package mthiebi.sgs.controllers.clientconsolecontrollers;

import mthiebi.sgs.dto.TotalAbsenceDto;
import mthiebi.sgs.dto.TotalAbsenceMapper;
import mthiebi.sgs.service.TotalAbsenceService;
import mthiebi.sgs.utils.UtilsJwt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/client/absence")
public class ClientAbsenceController {

    @Autowired
    private TotalAbsenceService totalAbsenceService;

    @Autowired
    private TotalAbsenceMapper totalAbsenceMapper;

    @Autowired
    private UtilsJwt utilsJwt;


    @GetMapping("/filter")
    public List<TotalAbsenceDto> getTotalAbsences(@RequestHeader("authorization") String authHeader,
                                                  @RequestParam Long month,
                                                  @RequestParam(required = false) Long year) throws Exception {

        String userName = utilsJwt.getUsernameFromHeader(authHeader);
        Calendar calendar = Calendar.getInstance();
        if (month != null) {
            calendar.set(Calendar.MONTH, month.intValue());
        }
        if (year != null) {
            calendar.set(Calendar.YEAR, year.intValue());
        }
        return totalAbsenceService.filter(userName, calendar.getTime()).stream()
                .map(totalAbsence -> totalAbsenceMapper.totalAbsenceDto(totalAbsence))
                .collect(Collectors.toList());

    }
}
