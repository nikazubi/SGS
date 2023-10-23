package mthiebi.sgs.controllers.clientconsolecontrollers;

import mthiebi.sgs.dto.GradeDTO;
import mthiebi.sgs.dto.GradeMapper;
import mthiebi.sgs.service.GradeService;
import mthiebi.sgs.utils.UtilsJwt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/client/grade")
public class ClientGradeController {

    @Autowired
    private GradeService gradeService;

    @Autowired
    private UtilsJwt utilsJwt;

    @Autowired
    private GradeMapper gradeMapper;

    @GetMapping("/get-grades-for-student")
    public List<GradeDTO> getGrades(@RequestHeader("authorization") String authHeader,
                                    @RequestParam(defaultValue = "GENERAL") String gradeTypePrefix,
                                    @RequestParam Long subjectId,
                                    @RequestParam Long month,
                                    @RequestParam(required = false) Long year) throws Exception {

        String userName = utilsJwt.getUsernameFromHeader(authHeader);
        return gradeService.findGradeByAcademyClassIdAndSubjectIdAndGradeTypeAndExactMonthAndYear(userName, subjectId, month, year, gradeTypePrefix)
                .stream()
                .map(grade -> gradeMapper.gradeDTOWithoutAcademyClass(grade))
                .collect(Collectors.toList());
    }


    @GetMapping("/get-grades-for-subject-monthly")
    public List<GradeDTO> getGradesForSubjectMonthly(@RequestHeader("authorization") String authHeader,
                                                     @RequestParam Long subjectId,
                                                     @RequestParam(required = false) Long year) throws Exception {

        String userName = utilsJwt.getUsernameFromHeader(authHeader);
        return gradeService.findAllMonthlyGradesForSubjectInYear(userName, subjectId, year)
                .stream()
                .map(grade -> gradeMapper.gradeDTOWithoutAcademyClass(grade))
                .collect(Collectors.toList());
    }

    @GetMapping("/get-grades-for-month")
    public List<GradeDTO> getGradesForMonth(@RequestHeader("authorization") String authHeader,
                                                     @RequestParam Long month,
                                                     @RequestParam(required = false) Long year) throws Exception {

        String userName = utilsJwt.getUsernameFromHeader(authHeader);
        return gradeService.findAllMonthlyGradesForMonthAndYear(userName, month, year)
                .stream()
                .map(grade -> gradeMapper.gradeDTOWithoutAcademyClass(grade))
                .collect(Collectors.toList());
    }
}
