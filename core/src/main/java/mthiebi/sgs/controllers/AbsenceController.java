package mthiebi.sgs.controllers;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.dto.AcademyClassDTO;
import mthiebi.sgs.dto.TotalAbsenceCreateRequest;
import mthiebi.sgs.dto.TotalAbsenceDto;
import mthiebi.sgs.dto.TotalAbsenceMapper;
import mthiebi.sgs.models.AcademyClass;
import mthiebi.sgs.models.TotalAbsence;
import mthiebi.sgs.service.AcademyClassService;
import mthiebi.sgs.service.TotalAbsenceService;
import mthiebi.sgs.utils.AuthConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Positive;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/absence")
public class AbsenceController {

    @Autowired
    private TotalAbsenceService totalAbsenceService;

    @Autowired
    private TotalAbsenceMapper totalAbsenceMapper;

    @PostMapping("/create")
    @Secured({AuthConstants.MANAGE_ACADEMY_CLASS}) //TODO
    public void createTotalAbsences(@RequestBody TotalAbsenceCreateRequest createRequest) throws SGSException {
        totalAbsenceService.addTotalAbsenceToAcademyClasses(createRequest.getAcademyClasses(),
                createRequest.getTotalAcademyHour(),
                createRequest.getActivePeriod());
    }

    @GetMapping("/filter")
    public List<TotalAbsenceDto> getTotalAbsences(@RequestParam(required = false) Long academyClassId,
                                                  @RequestParam(required = false) String activePeriod) {
        Date date1 = new Date();;
        if (!activePeriod.equals("NaN")) {
            date1.setTime(Long.parseLong(activePeriod));
        }
        return totalAbsenceService.filter(academyClassId, date1).stream()
                .map(totalAbsence -> totalAbsenceMapper.totalAbsenceDto(totalAbsence))
                .collect(Collectors.toList());

    }

    @DeleteMapping("{id}")
    @Secured({AuthConstants.MANAGE_ACADEMY_CLASS}) //TODO
    ResponseEntity<Void> deleteCardRequest(@Positive @PathVariable long id) throws SGSException {
        totalAbsenceService.deleteById(id);
        return ResponseEntity.ok().build();
    }

}
