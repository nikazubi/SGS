package mthiebi.sgs.impl;

import mthiebi.sgs.models.*;
import mthiebi.sgs.repository.AcademyClassRepository;
import mthiebi.sgs.repository.GradeRepository;
import mthiebi.sgs.repository.SubjectRepository;
import mthiebi.sgs.service.GradeCalculationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GradeCalculationServiceImpl implements GradeCalculationService {

    @Autowired
    private GradeRepository gradeRepository;

    @Autowired
    private AcademyClassRepository academyClassRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Override
    public void calculateGradeMonthly(long academyClassId, long subjectId, Date date) {
        AcademyClass academyClass = academyClassRepository.findById(academyClassId).orElseThrow();
        Subject subject = subjectRepository.findById(subjectId).orElseThrow();
        List<Student> studentList = academyClass.getStudentList();
        for (Student student : studentList) {
            List<Grade> gradeList = gradeRepository.findGradeByAcademyClassIdAndSubjectIdAndCreateTime(academyClassId, subjectId, student.getId(), date);
            List<Grade> withoutMonthly = gradeList.stream()
                    .filter(grade -> grade.getGradeType() != GradeType.GENERAL_COMPLETE_MONTHLY && grade.getGradeType().name().startsWith("GENERAL"))
                    .collect(Collectors.toList());
            if(withoutMonthly.size() == 0){
                continue; //TODO throw exception
            }
            Long sum = withoutMonthly.stream().map(Grade::getValue).reduce(0L, (first, second) -> BigDecimal.valueOf(first).add(BigDecimal.valueOf(second)).longValue());
            Long monthly = BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(withoutMonthly.size()), RoundingMode.HALF_UP).longValue();
            Grade grade = Grade.builder()
                                .gradeType(GradeType.GENERAL_COMPLETE_MONTHLY)
                                .subject(subject)
                                .academyClass(academyClass)
                                .student(student)
                                .value(monthly)
                                .build();
            gradeRepository.save(grade);
        }
    }

    @Override
    public void calculateBehaviourMonthly(long academyClassId, Date date) {
        AcademyClass academyClass = academyClassRepository.findById(academyClassId).orElseThrow();
        List<Student> studentList = academyClass.getStudentList();
        for (Student student : studentList) {
            List<Grade> gradeList = gradeRepository.findGradeByAcademyClassIdAndSubjectIdAndCreateTime(academyClassId, null, student.getId(), date);
            List<Grade> withoutMonthly = gradeList.stream().filter(grade -> grade.getGradeType() != GradeType.ABSENCE_MONTHLY && grade.getGradeType().name().startsWith("ABSENT")).collect(Collectors.toList());
            Long sum = withoutMonthly.stream().map(Grade::getValue).reduce(0L, (first, second) -> BigDecimal.valueOf(first).add(BigDecimal.valueOf(second)).longValue());
            Long monthly = BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(withoutMonthly.size()), RoundingMode.HALF_UP).longValue();
            Grade grade = Grade.builder()
                    .gradeType(GradeType.GENERAL_COMPLETE_MONTHLY)
                    .subject(null)
                    .academyClass(academyClass)
                    .student(student)
                    .value(monthly)
                    .build();
            gradeRepository.save(grade);
        }
    }

    @Override
    public void calculateAbsenceMonthly(long academyClassId, long subjectId, Date date) {
        //TODO
    }
}
