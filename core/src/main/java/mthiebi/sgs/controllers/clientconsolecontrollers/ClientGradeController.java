package mthiebi.sgs.controllers.clientconsolecontrollers;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.dto.GradeComponentWrapper;
import mthiebi.sgs.dto.GradeDTO;
import mthiebi.sgs.dto.GradeMapper;
import mthiebi.sgs.dto.SubjectComponentWrapper;
import mthiebi.sgs.models.Grade;
import mthiebi.sgs.models.Student;
import mthiebi.sgs.models.Subject;
import mthiebi.sgs.service.GradeService;
import mthiebi.sgs.utils.UtilsJwt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
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
    public List<GradeComponentWrapper> getGradesForMonth(@RequestHeader("authorization") String authHeader,
                                                         @RequestParam Long month,
                                                         @RequestParam(required = false) Long year) throws Exception {

        String userName = utilsJwt.getUsernameFromHeader(authHeader);
        List<Grade> monthlyGrades = gradeService.findAllMonthlyGradesForMonthAndYear(userName, month, year);
        List<SubjectComponentWrapper> list = new ArrayList<>();
        GradeComponentWrapper gradeComponentWrapper = new GradeComponentWrapper();

        for (Grade grade : monthlyGrades) {
            SubjectComponentWrapper subjectComponentWrapper = new SubjectComponentWrapper();
            subjectComponentWrapper.setSubject(grade.getSubject());
            subjectComponentWrapper.setValue(grade.getValue());
            list.add(subjectComponentWrapper);
        }

        gradeComponentWrapper.setGradeList(list);
        gradeComponentWrapper.setStudent(monthlyGrades.get(0).getStudent());

        return List.of(gradeComponentWrapper);
    }

    @GetMapping("/get-grades-years-grouped")
    public List<String> getGradeYear() throws SGSException {
        return gradeService.getGradeYearGrouped();
    }

    @GetMapping("/get-grades-by-semester")
    public List<GradeComponentWrapper> getGradesBySemester(@RequestHeader("authorization") String authHeader,
                                                           @RequestParam(required = false) String yearRange,
                                                           @RequestParam(required = false) String createDate,
                                                           @RequestParam String component) throws Exception {
        String userName = utilsJwt.getUsernameFromHeader(authHeader);


        List<GradeComponentWrapper> list = new ArrayList<>();
        Date date = new Date();
        if (createDate != null) {
            date.setTime(Long.parseLong(createDate));
        }
        Map<Student, Map<Subject, Map<Integer, BigDecimal>>> map = (Map<Student, Map<Subject, Map<Integer, BigDecimal>>>) gradeService.getGradeByComponent(userName, yearRange, date, component);

        for (Student student : map.keySet()) {
            GradeComponentWrapper gradeComponentWrapper = new GradeComponentWrapper();
            gradeComponentWrapper.setStudent(student);
            List<SubjectComponentWrapper> gradeList = new ArrayList<>();
            Map<Subject, Map<Integer, BigDecimal>> map2 = map.get(student);
            for (Subject subject : map2.keySet()) {
                SubjectComponentWrapper subjectComponentWrapper = new SubjectComponentWrapper();
                subjectComponentWrapper.setSubject(subject);
                subjectComponentWrapper.setValue(map2.get(subject));
                gradeList.add(subjectComponentWrapper);
            }
            gradeComponentWrapper.setGradeList(gradeList);
            list.add(gradeComponentWrapper);
        }
        return list;
    }

    @GetMapping("/get-grades-behaviour")
    public List<GradeDTO> getGradesBehaviour(@RequestHeader("authorization") String authHeader,
                                             @RequestParam Long month,
                                             @RequestParam(required = false) Long year) throws Exception {

        String userName = utilsJwt.getUsernameFromHeader(authHeader);
        return gradeService.findAllBehaviourGradesForMonthAndYear(userName, month, year)
                .stream()
                .map(grade -> gradeMapper.gradeDTOWithoutAcademyClass(grade))
                .collect(Collectors.toList());
    }

    @GetMapping("/get-grades-year")
    public List<GradeComponentWrapper> getGradesYear(@RequestHeader("authorization") String authHeader,
                                                     @RequestParam(required = false) String yearRange,
                                                     @RequestParam(required = false) String createDate) throws Exception {

        String userName = utilsJwt.getUsernameFromHeader(authHeader);


        List<GradeComponentWrapper> list = new ArrayList<>();
        Date date = new Date();
        if (createDate != null) {
            date.setTime(Long.parseLong(createDate));
        }
        Map<Student, Map<Subject, Map<Integer, BigDecimal>>> map = (Map<Student, Map<Subject, Map<Integer, BigDecimal>>>) gradeService.getGradeByComponent(userName, yearRange, date, "anual");

        for (Student student : map.keySet()) {
            GradeComponentWrapper gradeComponentWrapper = new GradeComponentWrapper();
            gradeComponentWrapper.setStudent(student);
            List<SubjectComponentWrapper> gradeList = new ArrayList<>();
            Map<Subject, Map<Integer, BigDecimal>> map2 = map.get(student);
            for (Subject subject : map2.keySet()) {
                SubjectComponentWrapper subjectComponentWrapper = new SubjectComponentWrapper();
                subjectComponentWrapper.setSubject(subject);
                subjectComponentWrapper.setValue(map2.get(subject));
                gradeList.add(subjectComponentWrapper);
            }
            gradeComponentWrapper.setGradeList(gradeList);
            list.add(gradeComponentWrapper);
        }
        return list;
    }

    @GetMapping("/get-grades-absence")
    public List<GradeDTO> getGradesAbsence(@RequestHeader("authorization") String authHeader,
                                           @RequestParam(required = false) String yearRange,
                                           @RequestParam(required = false) Long month) throws Exception {
        String userName = utilsJwt.getUsernameFromHeader(authHeader);
        return gradeService.getAbsenceGrades(userName, yearRange, month)
                .stream()
                .map(grade -> gradeMapper.gradeDTOWithoutAcademyClass(grade))
                .collect(Collectors.toList());
    }
}
