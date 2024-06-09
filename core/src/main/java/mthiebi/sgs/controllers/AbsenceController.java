package mthiebi.sgs.controllers;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.dto.*;
import mthiebi.sgs.models.AbsenceGrade;
import mthiebi.sgs.models.AbsenceGradeType;
import mthiebi.sgs.models.Student;
import mthiebi.sgs.service.AbsenceService;
import mthiebi.sgs.service.TotalAbsenceService;
import mthiebi.sgs.utils.AuthConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Positive;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/absence")
public class AbsenceController {

    @Autowired
    private TotalAbsenceService totalAbsenceService;

    @Autowired
    private TotalAbsenceMapper totalAbsenceMapper;

    @Autowired
    private AbsenceService absenceService;

    @Autowired
    private AbsenceGradeMapper absenceGradeMapper;

    @PostMapping("/add-absence-grade")
    @Secured({AuthConstants.ADD_GRADES})
    public AbsenceGradeDto addAbsenceGrade(@RequestBody AbsenceGradeDto absenceGradeDto) {
        return absenceGradeMapper.absenceGradeDTO(absenceService.addAbsenceGrade(absenceGradeDto.getStudent().getId(),
                absenceGradeDto.getAcademyClass().getId(), AbsenceGradeType.valueOf(absenceGradeDto.getGradeType()),
                absenceGradeDto.getValue(), absenceGradeDto.getExactMonth()));

    }


    @GetMapping("/find-absence-grade")
    @Secured({AuthConstants.MANAGE_GRADES})
    public List<AbsenceGradeComponentWrapper> findAbsenceGrade(@RequestParam(required = false) Long classId,
                                                               @RequestParam(required = false) Long studentId,
                                                               @RequestParam(required = false) String yearRange) {
        Map<Student, List<AbsenceGrade>> absenceGradeByStudent = absenceService.findAbsenceGrade(classId, studentId, yearRange);
        List<AbsenceGradeComponentWrapper> result = new ArrayList<>();
        for (Student student : absenceGradeByStudent.keySet()) {
            AbsenceGradeComponentWrapper gradeComponentWrapper = new AbsenceGradeComponentWrapper();
            gradeComponentWrapper.setStudent(student);
            List<AbsenceGrade> a = absenceGradeByStudent.get(student);
            gradeComponentWrapper.setGradeList(a.stream().map(absenceGradeMapper::absenceGradeDTO).collect(Collectors.toList()));
            result.add(gradeComponentWrapper);
        }
        return result;
    }

    @PostMapping("/create")
    @Secured({AuthConstants.MANAGE_TOTAL_ABSENCE}) //TODO
    public void createTotalAbsences(@RequestBody TotalAbsenceCreateRequest createRequest) throws SGSException {
        totalAbsenceService.addTotalAbsenceToAcademyClasses(createRequest.getAcademyClasses(),
                createRequest.getTotalAcademyHour(),
                createRequest.getActivePeriod());
    }

    @GetMapping("/filter")
    @Secured({AuthConstants.MANAGE_TOTAL_ABSENCE}) //TODO
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
    @Secured({AuthConstants.MANAGE_TOTAL_ABSENCE}) //TODO
    ResponseEntity<Void> deleteCardRequest(@Positive @PathVariable long id) throws SGSException {
        totalAbsenceService.deleteById(id);
        return ResponseEntity.ok().build();
    }

}
