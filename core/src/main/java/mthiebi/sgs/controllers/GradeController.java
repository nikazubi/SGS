package mthiebi.sgs.controllers;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.dto.*;
import mthiebi.sgs.models.Student;
import mthiebi.sgs.models.Subject;
import mthiebi.sgs.service.GradeService;
import mthiebi.sgs.utils.AuthConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/grade")
public class GradeController {

    @Autowired
    private GradeService gradeService;

    @Autowired
    private GradeMapper gradeMapper;

    @PostMapping("/insert-student-grade")
    @Secured({AuthConstants.ADD_GRADES})
    public GradeDTO insertGrade(@RequestBody GradeDTO gradeDTO){
        return gradeMapper.gradeDTO(gradeService.insertStudentGrade(gradeMapper.grade(gradeDTO)));
    }

    @GetMapping("/get-grades")
    @Secured({AuthConstants.MANAGE_GRADES})
    public List<GradeDTO> getGradeByClassAndSubject(@RequestParam(required = false) Long classId,
                                                    @RequestParam(required = false) Long subjectId,
                                                    @RequestParam(required = false) Long studentId,
                                                    @RequestParam(required = false) Date date){
        return gradeService.getStudentGradeByClassAndSubjectIdAndCreateTime(classId, subjectId, studentId, date, null)
                                                                                .stream()
                                                                                .map(gradeMapper::gradeDTO)
                                                                                .collect(Collectors.toList());
    }

    @GetMapping("/get-grades-grouped")
    @Secured({AuthConstants.MANAGE_GRADES})
    public List<GradeWrapperDto> getGradeGrouped(@RequestParam(required = false) Long classId,
                                                 @RequestParam(required = false) Long subjectId,
                                                 @RequestParam(required = false) Long studentId,
                                                 @RequestParam(required = false) String date,
                                                 @RequestParam GradeGroupByClause groupByClause,
                                                 @RequestParam(defaultValue = "GENERAL") String gradeTypePrefix){
        Date date1 = new Date();
        date1.setTime(Long.parseLong(date));
        if (groupByClause == GradeGroupByClause.STUDENT ){
            return gradeService.getStudentGradeByClassAndSubjectIdAndCreateTime(classId, subjectId, studentId, date1, gradeTypePrefix)
                    .stream()
                    .map(gradeMapper::gradeDTO)
                    .collect(Collectors.groupingBy(GradeDTO::getStudent))
                    .entrySet().stream()
                    .map(k -> GradeWrapperByStudent.builder()
                                                    .student(k.getKey())
                                                    .grades(k.getValue())
                                                    .build())
                    .collect(Collectors.toList());
        } else {
            return gradeService.getStudentGradeByClassAndSubjectIdAndCreateTime(classId, subjectId, studentId, date1, gradeTypePrefix)
                    .stream()
                    .map(gradeMapper::gradeDTO)
                    .collect(Collectors.groupingBy(GradeDTO::getSubject))
                    .entrySet().stream()
                    .map(k -> GradeWrapperBySubject.builder()
                            .subject(k.getKey())
                            .grades(k.getValue())
                            .build())
                    .collect(Collectors.toList());
        }
    }

    @GetMapping("/get-grades-by-component")
    @Secured({AuthConstants.MANAGE_GRADES}) //todo
    public List<GradeComponentWrapper> getGradesByComponent(@RequestParam Long classId,
                                                           @RequestParam(required = false) Long studentId,
                                                           @RequestParam(required = false) String yearRange,
                                                           @RequestParam(required = false) String createDate,
                                                           @RequestParam String component) throws SGSException {
        List<GradeComponentWrapper> list = new ArrayList<>();
        Date date = new Date();
        if (createDate != null) {
            date.setTime(Long.parseLong(createDate));
        }
        Map<Student, Map<Subject, BigDecimal>> map = (Map<Student, Map<Subject, BigDecimal>>) gradeService.getGradeByComponent(classId, studentId, yearRange, date, component);
        for (Student student : map.keySet()) {
            GradeComponentWrapper gradeComponentWrapper = new GradeComponentWrapper();
            gradeComponentWrapper.setStudent(student);
            List<SubjectComponentWrapper> gradeList = new ArrayList<>();
            Map<Subject, BigDecimal> map2 = map.get(student);
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

    @GetMapping("/get-grades-by-semester")
    @Secured({AuthConstants.MANAGE_GRADES}) //todo
    public List<GradeComponentWrapper> getGradesBySemester(@RequestParam Long classId,
                                                            @RequestParam(required = false) Long studentId,
                                                            @RequestParam(required = false) String yearRange,
                                                            @RequestParam(required = false) String createDate,
                                                            @RequestParam String component) throws SGSException {
        List<GradeComponentWrapper> list = new ArrayList<>();
        Date date = new Date();
        if (createDate != null) {
            date.setTime(Long.parseLong(createDate));
        }
        Map<Student, Map<Subject, Map<Integer, BigDecimal>>> map = (Map<Student, Map<Subject, Map<Integer, BigDecimal>>>) gradeService.getGradeByComponent(classId, studentId, yearRange, date, component);
        for (Student student : map.keySet()) {
            GradeComponentWrapper gradeComponentWrapper = new GradeComponentWrapper();
            gradeComponentWrapper.setStudent(student);
            List<SubjectComponentWrapper> gradeList = new ArrayList<>();
            Map<Subject,  Map<Integer, BigDecimal>> map2 = map.get(student);
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

    @GetMapping("/get-grades-years-grouped")
    @Secured({AuthConstants.MANAGE_GRADES}) //todo
    public List<String> getGradeYear() throws SGSException {
        return gradeService.getGradeYearGrouped();
    }
}
