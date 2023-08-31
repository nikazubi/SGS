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

                BigDecimal monthlyGeneralSummery = calculateTransitSummeryMonthlyGrade(gradeList);
                saveGrade(subject, academyClass, student, BigDecimal.valueOf(Math.round(monthlyGeneralSummery.doubleValue())), GradeType.TRANSIT_SUMMARY_ASSIGMENT_MONTH, date);

                BigDecimal monthlyGeneralSummeryPercent = monthlyGeneralSummery.divide(BigDecimal.valueOf(2), RoundingMode.HALF_UP);
                saveGrade(subject, academyClass, student, monthlyGeneralSummeryPercent, GradeType.TRANSIT_SUMMARY_ASSIGMENT_PERCENT, date);

                BigDecimal monthlySchoolWork = calculateSimpleAverageOfPrefix(gradeList, "TRANSIT_SCHOOL_WORK");
                saveGrade(subject, academyClass, student, BigDecimal.valueOf(Math.round(monthlySchoolWork.doubleValue())), GradeType.TRANSIT_SCHOOL_WORK_MONTH, date);

                BigDecimal monthlySchoolWorkPercent = monthlySchoolWork.divide(BigDecimal.valueOf(2), RoundingMode.HALF_UP);
                saveGrade(subject, academyClass, student, monthlySchoolWorkPercent, GradeType.TRANSIT_SCHOOL_WORK_MONTH_PERCENT, date);

                BigDecimal sum = (monthlyGeneralSummeryPercent).add(monthlySchoolWorkPercent);
                BigDecimal monthly = sum.divide(BigDecimal.valueOf(1L), RoundingMode.HALF_UP);
                Grade grade = Grade.builder()
                        .gradeType(GradeType.GENERAL_COMPLETE_MONTHLY)
                        .subject(subject)
                        .academyClass(academyClass)
                        .student(student)
                        .value(BigDecimal.valueOf(Math.round(monthly.doubleValue())))
                        .exactMonth(date)
                        .build();
                gradeRepository.save(grade);
            }
        } else {
            for (Student student : studentList) {
                List<Grade> gradeList = gradeRepository.findGradeByAcademyClassIdAndSubjectIdAndCreateTime(academyClassId, subjectId, student.getId(), date);
                BigDecimal monthlyGeneralSummery = calculateGeneralSummeryMonthlyGrade(gradeList).setScale(1, RoundingMode.HALF_UP);
                saveGrade(subject, academyClass, student, BigDecimal.valueOf(Math.round(monthlyGeneralSummery.doubleValue())), GradeType.GENERAL_SUMMARY_ASSIGMENT_MONTH, date);

                BigDecimal monthlyGeneralSummeryPercent = monthlyGeneralSummery.divide(BigDecimal.valueOf(2));
                saveGrade(subject, academyClass, student, monthlyGeneralSummeryPercent, GradeType.GENERAL_SUMMARY_ASSIGMENT_PERCENT, date);

                BigDecimal monthlyHomework = calculateSimpleAverageOfPrefix(gradeList, "GENERAL_HOMEWORK");
                saveGrade(subject, academyClass, student, BigDecimal.valueOf(Math.round(monthlyGeneralSummeryPercent.doubleValue())), GradeType.GENERAL_HOMEWORK_MONTHLY, date);

                BigDecimal monthlyHomeworkPercent = monthlyHomework.multiply(BigDecimal.valueOf(3)).divide(BigDecimal.valueOf(10));
                saveGrade(subject, academyClass, student, monthlyHomeworkPercent, GradeType.GENERAL_HOMEWORK_PERCENT, date);

                BigDecimal monthlySchoolWork = calculateSimpleAverageOfPrefix(gradeList, "GENERAL_SCHOOL_WORK");
                saveGrade(subject, academyClass, student, BigDecimal.valueOf(Math.round(monthlySchoolWork.doubleValue())), GradeType.GENERAL_SCHOOL_WORK_MONTH, date);

                BigDecimal monthlySchoolWorkPercent = monthlySchoolWork.divide(BigDecimal.valueOf(5));
                saveGrade(subject, academyClass, student, monthlySchoolWorkPercent, GradeType.GENERAL_SCHOOL_WORK_PERCENT, date);

                BigDecimal sum = monthlyHomeworkPercent.add(monthlyGeneralSummeryPercent).add(monthlySchoolWorkPercent);
                BigDecimal monthly = sum.divide(BigDecimal.valueOf(1L), RoundingMode.HALF_UP);
                Grade grade = Grade.builder()
                        .gradeType(GradeType.GENERAL_COMPLETE_MONTHLY)
                        .subject(subject)
                        .academyClass(academyClass)
                        .student(student)
                        .value(BigDecimal.valueOf(Math.round(monthly.doubleValue())))
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
            BigDecimal monthlyUniform = calculateSimpleAverageOfPrefix(gradeList, "BEHAVIOUR_APPEARING_IN_UNIFORM");
            Grade gradeUniform = Grade.builder()
                    .gradeType(GradeType.BEHAVIOUR_APPEARING_IN_UNIFORM_MONTHLY)
                    .subject(null)
                    .academyClass(academyClass)
                    .student(student)
                    .value(BigDecimal.valueOf(Math.round(monthlyUniform.doubleValue())))
                    .exactMonth(date)
                    .build();
            gradeRepository.save(gradeUniform);
            BigDecimal delays = calculateSimpleAverageOfPrefix(gradeList, "BEHAVIOUR_STUDENT_DELAYS");
            Grade delaysMonthly = Grade.builder()
                    .gradeType(GradeType.BEHAVIOUR_STUDENT_DELAYS_MONTHLY)
                    .subject(null)
                    .academyClass(academyClass)
                    .student(student)
                    .value(BigDecimal.valueOf(Math.round(delays.doubleValue())))
                    .exactMonth(date)
                    .build();
            gradeRepository.save(delaysMonthly);
            BigDecimal inventory = calculateSimpleAverageOfPrefix(gradeList, "BEHAVIOUR_CLASSROOM_INVENTORY");
            Grade inventoryMonthly = Grade.builder()
                    .gradeType(GradeType.BEHAVIOUR_CLASSROOM_INVENTORY_MONTHLY)
                    .subject(null)
                    .academyClass(academyClass)
                    .student(student)
                    .value(BigDecimal.valueOf(Math.round(inventory.doubleValue())))
                    .exactMonth(date)
                    .build();
            gradeRepository.save(inventoryMonthly);
            BigDecimal hygiene = calculateSimpleAverageOfPrefix(gradeList, "BEHAVIOUR_STUDENT_HYGIENE");
            Grade hygieneMonthly = Grade.builder()
                    .gradeType(GradeType.BEHAVIOUR_STUDENT_HYGIENE_MONTHLY)
                    .subject(null)
                    .academyClass(academyClass)
                    .student(student)
                    .value(BigDecimal.valueOf(Math.round(hygiene.doubleValue())))
                    .exactMonth(date)
                    .build();
            gradeRepository.save(hygieneMonthly);
            BigDecimal behaviourStudentBehavior = calculateSimpleAverageOfPrefix(gradeList, "BEHAVIOUR_STUDENT_BEHAVIOR");
            Grade behaviourStudentBehaviorMonthly = Grade.builder()
                    .gradeType(GradeType.BEHAVIOUR_STUDENT_HYGIENE_MONTHLY)
                    .subject(null)
                    .academyClass(academyClass)
                    .student(student)
                    .value(BigDecimal.valueOf(Math.round(behaviourStudentBehavior.doubleValue())))
                    .exactMonth(date)
                    .build();
            gradeRepository.save(behaviourStudentBehaviorMonthly);
            BigDecimal sum = monthlyUniform.add(delays).add(inventory).add(hygiene).add(behaviourStudentBehavior);
            BigDecimal monthly = sum.divide(BigDecimal.valueOf(5L), RoundingMode.HALF_UP);
            Grade grade = Grade.builder()
                    .gradeType(GradeType.BEHAVIOUR_MONTHLY)
                    .subject(null)
                    .academyClass(academyClass)
                    .student(student)
                    .value(BigDecimal.valueOf(Math.round(monthly.doubleValue())))
                    .exactMonth(date)
                    .build();

            gradeRepository.save(grade);
        }
    }

    @Override
    public void calculateAbsenceMonthly(long academyClassId, long subjectId, Date date) {
        //TODO
    }

    private BigDecimal calculateGeneralSummeryMonthlyGrade(List<Grade> gradeList) throws SGSException {
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
                BigDecimal sum = generalSummeryAssignment2.get(0).getValue()
                        .add(generalSummeryAssignment1.get(0).getValue());
                return sum.divide(BigDecimal.valueOf(2L), RoundingMode.HALF_UP);
            }
        }
    }

    private BigDecimal calculateTransitSummeryMonthlyGrade(List<Grade> gradeList) throws SGSException {
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
                BigDecimal sum = generalSummeryAssignment2.get(0).getValue()
                        .add(generalSummeryAssignment1.get(0).getValue());
                return sum.divide(BigDecimal.valueOf(2L), RoundingMode.HALF_UP);
            }
        }
    }

    private BigDecimal getRestorationGrade(List<Grade> gradeList, Grade generalSummeryAssignment2) throws SGSException {
        Optional<Grade> generalSummeryAssignmentRestoration = gradeList.stream().findFirst().filter(grade -> grade.getGradeType().equals(GradeType.GENERAL_SUMMARY_ASSIGMENT_RESTORATION));
        if (generalSummeryAssignmentRestoration.isEmpty() || generalSummeryAssignmentRestoration.get().getValue() == null) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, ExceptionKeys.GENERAL_SUMMERY_GRADES_NOT_PRESENT);
        }
        BigDecimal sum = generalSummeryAssignment2.getValue()
                .add(generalSummeryAssignmentRestoration.get().getValue());
        return sum.divide(BigDecimal.valueOf(2L), RoundingMode.HALF_UP);
    }

    private BigDecimal getRestorationGradeTransit(List<Grade> gradeList, Grade generalSummeryAssignment2) throws SGSException {
        Optional<Grade> generalSummeryAssignmentRestoration = gradeList.stream().findFirst().filter(grade -> grade.getGradeType().equals(GradeType.TRANSIT_SUMMARY_ASSIGMENT_RESTORATION));
        if (generalSummeryAssignmentRestoration.isEmpty() || generalSummeryAssignmentRestoration.get().getValue() == null) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, ExceptionKeys.GENERAL_SUMMERY_GRADES_NOT_PRESENT);
        }
        BigDecimal sum = generalSummeryAssignment2.getValue()
                .add(generalSummeryAssignmentRestoration.get().getValue());
        return sum.divide(BigDecimal.valueOf(2L), RoundingMode.HALF_UP);
    }

    private BigDecimal calculateSimpleAverageOfPrefix(List<Grade> gradeList, String prefix) {
        List<Grade> eligibleGrades = gradeList.stream()
                .filter(grade -> grade.getGradeType().name().startsWith(prefix) &&
                        !grade.getGradeType().name().contains("MONTHLY") && !grade.getGradeType().name().contains("PERCENT"))
                .collect(Collectors.toList());
        BigDecimal sum = eligibleGrades.stream().map(Grade::getValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(eligibleGrades.size()), RoundingMode.HALF_UP);
    }

    private Grade saveGrade(Subject subject, AcademyClass academyClass, Student student, BigDecimal value, GradeType gradeType, Date exactMonth) {
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
