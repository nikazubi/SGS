package mthiebi.sgs.impl;

import mthiebi.sgs.ExceptionKeys;
import mthiebi.sgs.SGSException;
import mthiebi.sgs.SGSExceptionCode;
import mthiebi.sgs.models.*;
import mthiebi.sgs.repository.AcademyClassRepository;
import mthiebi.sgs.repository.GradeRepository;
import mthiebi.sgs.repository.SubjectRepository;
import mthiebi.sgs.service.GradeCalculationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.Optional;
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
    @Transactional
    public void calculateGradeMonthly(long academyClassId, long subjectId, Date date) throws SGSException {
        AcademyClass academyClass = academyClassRepository.findById(academyClassId).orElseThrow();
        Subject subject = subjectRepository.findById(subjectId).orElseThrow();
        List<Student> studentList = academyClass.getStudentList();
        if (academyClass.getIsTransit()) {
            for (Student student : studentList) {
                List<Grade> gradeList = gradeRepository.findGradeByAcademyClassIdAndSubjectIdAndCreateTime(academyClassId, subjectId, student.getId(), date);
                Long monthlyGeneralSummery = calculateTransitSummeryMonthlyGrade(gradeList);
                saveGrade(subject, academyClass, student, monthlyGeneralSummery, GradeType.TRANSIT_SUMMARY_ASSIGMENT_MONTH, date);
                BigDecimal monthlyGeneralSummeryPercent = BigDecimal.valueOf(monthlyGeneralSummery).divide(BigDecimal.valueOf(2), RoundingMode.HALF_UP);
                saveGrade(subject, academyClass, student, monthlyGeneralSummeryPercent.longValue(), GradeType.TRANSIT_SUMMARY_ASSIGMENT_PERCENT, date);
                Long monthlySchoolWork = calculateSimpleAverageOfPrefix(gradeList, "TRANSIT_SCHOOL_WORK");
                saveGrade(subject, academyClass, student, monthlySchoolWork, GradeType.TRANSIT_SCHOOL_WORK_MONTH, date);
                BigDecimal monthlySchoolWorkPercent = BigDecimal.valueOf(monthlySchoolWork).divide(BigDecimal.valueOf(2), RoundingMode.HALF_UP);
                saveGrade(subject, academyClass, student, monthlySchoolWorkPercent.longValue(), GradeType.TRANSIT_SCHOOL_WORK_MONTH_PERCENT, date);

                BigDecimal sum = (monthlyGeneralSummeryPercent).add(monthlySchoolWorkPercent);
                Long monthly = sum.divide(BigDecimal.valueOf(1L), RoundingMode.HALF_UP).longValue();
                Grade grade = Grade.builder()
                        .gradeType(GradeType.GENERAL_COMPLETE_MONTHLY)
                        .subject(subject)
                        .academyClass(academyClass)
                        .student(student)
                        .value(monthly)
                        .exactMonth(date)
                        .build();
                gradeRepository.save(grade);
            }
        } else {
            for (Student student : studentList) {
                List<Grade> gradeList = gradeRepository.findGradeByAcademyClassIdAndSubjectIdAndCreateTime(academyClassId, subjectId, student.getId(), date);
                Long monthlyGeneralSummery = calculateGeneralSummeryMonthlyGrade(gradeList);
                saveGrade(subject, academyClass, student, monthlyGeneralSummery, GradeType.GENERAL_SUMMARY_ASSIGMENT_MONTH, date);
                BigDecimal monthlyGeneralSummeryPercent = BigDecimal.valueOf(monthlyGeneralSummery).divide(BigDecimal.valueOf(2), RoundingMode.HALF_UP);
                saveGrade(subject, academyClass, student, monthlyGeneralSummeryPercent.longValue(), GradeType.GENERAL_SUMMARY_ASSIGMENT_PERCENT, date);
                Long monthlyHomework = calculateSimpleAverageOfPrefix(gradeList, "GENERAL_HOMEWORK");
                saveGrade(subject, academyClass, student, monthlyGeneralSummeryPercent.longValue(), GradeType.GENERAL_HOMEWORK_MONTHLY, date);
                BigDecimal monthlyHomeworkPercent = BigDecimal.valueOf(monthlyHomework).multiply(BigDecimal.valueOf(3)).divide(BigDecimal.valueOf(10), RoundingMode.HALF_UP);
                saveGrade(subject, academyClass, student, monthlyHomeworkPercent.longValue(), GradeType.GENERAL_HOMEWORK_PERCENT, date);
                Long monthlySchoolWork = calculateSimpleAverageOfPrefix(gradeList, "GENERAL_SCHOOL_WORK");
                saveGrade(subject, academyClass, student, monthlySchoolWork, GradeType.GENERAL_SCHOOL_WORK_MONTH, date);
                BigDecimal monthlySchoolWorkPercent = BigDecimal.valueOf(monthlySchoolWork).divide(BigDecimal.valueOf(5), RoundingMode.HALF_UP);
                saveGrade(subject, academyClass, student, monthlySchoolWorkPercent.longValue(), GradeType.GENERAL_SCHOOL_WORK_PERCENT, date);

                BigDecimal sum = monthlyHomeworkPercent.add(monthlyGeneralSummeryPercent).add(monthlySchoolWorkPercent);
                Long monthly = sum.divide(BigDecimal.valueOf(1L), RoundingMode.HALF_UP).longValue();
                Grade grade = Grade.builder()
                        .gradeType(GradeType.GENERAL_COMPLETE_MONTHLY)
                        .subject(subject)
                        .academyClass(academyClass)
                        .student(student)
                        .value(monthly)
                        .exactMonth(date)
                        .build();
                gradeRepository.save(grade);
            }
        }
    }

    @Override
    public void calculateBehaviourMonthly(long academyClassId, Date date) {
        AcademyClass academyClass = academyClassRepository.findById(academyClassId).orElseThrow();
        List<Student> studentList = academyClass.getStudentList();
        for (Student student : studentList) {
            List<Grade> gradeList = gradeRepository.findGradeByAcademyClassIdAndSubjectIdAndCreateTime(academyClassId, null, student.getId(), date);
            BigDecimal monthlyUniform = BigDecimal.valueOf(calculateSimpleAverageOfPrefix(gradeList, "BEHAVIOUR_APPEARING_IN_UNIFORM"));
            Grade gradeUniform = Grade.builder()
                    .gradeType(GradeType.BEHAVIOUR_APPEARING_IN_UNIFORM_MONTHLY)
                    .subject(null)
                    .academyClass(academyClass)
                    .student(student)
                    .value(monthlyUniform.longValue())
                    .exactMonth(date)
                    .build();
            gradeRepository.save(gradeUniform);
            BigDecimal delays = BigDecimal.valueOf(calculateSimpleAverageOfPrefix(gradeList, "BEHAVIOUR_STUDENT_DELAYS"));
            Grade delaysMonthly = Grade.builder()
                    .gradeType(GradeType.BEHAVIOUR_STUDENT_DELAYS_MONTHLY)
                    .subject(null)
                    .academyClass(academyClass)
                    .student(student)
                    .value(monthlyUniform.longValue())
                    .exactMonth(date)
                    .build();
            gradeRepository.save(delaysMonthly);
            BigDecimal inventory = BigDecimal.valueOf(calculateSimpleAverageOfPrefix(gradeList, "BEHAVIOUR_CLASSROOM_INVENTORY"));
            Grade inventoryMonthly = Grade.builder()
                    .gradeType(GradeType.BEHAVIOUR_CLASSROOM_INVENTORY_MONTHLY)
                    .subject(null)
                    .academyClass(academyClass)
                    .student(student)
                    .value(inventory.longValue())
                    .exactMonth(date)
                    .build();
            gradeRepository.save(inventoryMonthly);
            BigDecimal hygiene = BigDecimal.valueOf(calculateSimpleAverageOfPrefix(gradeList, "BEHAVIOUR_STUDENT_HYGIENE"));
            Grade hygieneMonthly = Grade.builder()
                    .gradeType(GradeType.BEHAVIOUR_STUDENT_HYGIENE_MONTHLY)
                    .subject(null)
                    .academyClass(academyClass)
                    .student(student)
                    .value(hygiene.longValue())
                    .exactMonth(date)
                    .build();
            gradeRepository.save(hygieneMonthly);
            BigDecimal sum = monthlyUniform.add(delays).add(inventory).add(hygiene);
            Long monthly = sum.divide(BigDecimal.valueOf(5L), RoundingMode.HALF_UP).longValue();
            Grade grade = Grade.builder()
                    .gradeType(GradeType.GENERAL_COMPLETE_MONTHLY)
                    .subject(null)
                    .academyClass(academyClass)
                    .student(student)
                    .value(monthly)
                    .exactMonth(date)
                    .build();
            gradeRepository.save(grade);
        }
    }

    @Override
    public void calculateAbsenceMonthly(long academyClassId, long subjectId, Date date) {
        //TODO
    }

    private Long calculateGeneralSummeryMonthlyGrade(List<Grade> gradeList) throws SGSException {
        List<Grade> generalSummeryAssignment1 = gradeList.stream()
                .filter(grade -> grade.getGradeType().equals(GradeType.GENERAL_SUMMARY_ASSIGMENT_1))
                .collect(Collectors.toList());
        List<Grade> generalSummeryAssignment2 = gradeList.stream()
                .filter(grade -> grade.getGradeType().equals(GradeType.GENERAL_SUMMARY_ASSIGMENT_2))
                .collect(Collectors.toList());
        if (generalSummeryAssignment1.isEmpty()) {
            if (generalSummeryAssignment2.isEmpty()) {
                throw new SGSException(SGSExceptionCode.BAD_REQUEST, ExceptionKeys.GENERAL_SUMMERY_GRADES_NOT_PRESENT);
            } else {
                return getRestorationGrade(gradeList, generalSummeryAssignment2.get(0));
            }
        } else {
            if (generalSummeryAssignment2.isEmpty() || generalSummeryAssignment2.get(0).getValue() == null) {
                return getRestorationGrade(gradeList, generalSummeryAssignment2.get(0));
            } else {
                BigDecimal sum = BigDecimal.valueOf(generalSummeryAssignment2.get(0).getValue())
                        .add(BigDecimal.valueOf(generalSummeryAssignment1.get(0).getValue()));
                return sum.divide(BigDecimal.valueOf(2L), RoundingMode.HALF_UP).longValue();
            }
        }
    }

    private Long calculateTransitSummeryMonthlyGrade(List<Grade> gradeList) throws SGSException {
        List<Grade> generalSummeryAssignment1 = gradeList.stream()
                .filter(grade -> grade.getGradeType().equals(GradeType.TRANSIT_SUMMARY_ASSIGMENT_1))
                .collect(Collectors.toList());
        List<Grade> generalSummeryAssignment2 = gradeList.stream()
                .filter(grade -> grade.getGradeType().equals(GradeType.TRANSIT_SUMMARY_ASSIGMENT_2))
                .collect(Collectors.toList());
        if (generalSummeryAssignment1.isEmpty()) {
            if (generalSummeryAssignment2.isEmpty()) {
                throw new SGSException(SGSExceptionCode.BAD_REQUEST, ExceptionKeys.GENERAL_SUMMERY_GRADES_NOT_PRESENT);
            } else {
                return getRestorationGradeTransit(gradeList, generalSummeryAssignment2.get(0));
            }
        } else {
            if (generalSummeryAssignment2.isEmpty() || generalSummeryAssignment2.get(0).getValue() == null) {
                return getRestorationGradeTransit(gradeList, generalSummeryAssignment2.get(0));
            } else {
                BigDecimal sum = BigDecimal.valueOf(generalSummeryAssignment2.get(0).getValue())
                        .add(BigDecimal.valueOf(generalSummeryAssignment1.get(0).getValue()));
                return sum.divide(BigDecimal.valueOf(2L), RoundingMode.HALF_UP).longValue();
            }
        }
    }

    private Long getRestorationGrade(List<Grade> gradeList, Grade generalSummeryAssignment2) throws SGSException {
        Optional<Grade> generalSummeryAssignmentRestoration = gradeList.stream().findFirst().filter(grade -> grade.getGradeType().equals(GradeType.GENERAL_SUMMARY_ASSIGMENT_RESTORATION));
        if (generalSummeryAssignmentRestoration.isEmpty() || generalSummeryAssignmentRestoration.get().getValue() == null) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, ExceptionKeys.GENERAL_SUMMERY_GRADES_NOT_PRESENT);
        }
        BigDecimal sum = BigDecimal.valueOf(generalSummeryAssignment2.getValue())
                .add(BigDecimal.valueOf(generalSummeryAssignmentRestoration.get().getValue()));
        return sum.divide(BigDecimal.valueOf(2L), RoundingMode.HALF_UP).longValue();
    }

    private Long getRestorationGradeTransit(List<Grade> gradeList, Grade generalSummeryAssignment2) throws SGSException {
        Optional<Grade> generalSummeryAssignmentRestoration = gradeList.stream().findFirst().filter(grade -> grade.getGradeType().equals(GradeType.TRANSIT_SUMMARY_ASSIGMENT_RESTORATION));
        if (generalSummeryAssignmentRestoration.isEmpty() || generalSummeryAssignmentRestoration.get().getValue() == null) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, ExceptionKeys.GENERAL_SUMMERY_GRADES_NOT_PRESENT);
        }
        BigDecimal sum = BigDecimal.valueOf(generalSummeryAssignment2.getValue())
                .add(BigDecimal.valueOf(generalSummeryAssignmentRestoration.get().getValue()));
        return sum.divide(BigDecimal.valueOf(2L), RoundingMode.HALF_UP).longValue();
    }

    private Long calculateSimpleAverageOfPrefix(List<Grade> gradeList, String prefix) {
        List<Grade> eligibleGrades = gradeList.stream()
                .filter(grade -> grade.getGradeType().name().startsWith(prefix) &&
                        !grade.getGradeType().name().contains("MONTHLY") && !grade.getGradeType().name().contains("PERCENT"))
                .collect(Collectors.toList());
        Long sum = eligibleGrades.stream().map(Grade::getValue).reduce(0L, (first, second) -> BigDecimal.valueOf(first).add(BigDecimal.valueOf(second)).longValue());
        return BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(eligibleGrades.size()), RoundingMode.HALF_UP).longValue();
    }

    private Grade saveGrade(Subject subject, AcademyClass academyClass, Student student, Long value, GradeType gradeType, Date exactMonth) {
        Grade grade = Grade.builder()
                .gradeType(gradeType)
                .subject(subject)
                .academyClass(academyClass)
                .student(student)
                .value(value)
                .exactMonth(exactMonth)
                .build();
        return gradeRepository.save(grade);
    }
}
