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
import java.util.Calendar;
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
                saveGrade(subject, academyClass, student, BigDecimal.valueOf(Math.round(monthlyGeneralSummery.doubleValue())), GradeType.GENERAL_SUMMARY_ASSIGMENT_MONTH, date);

                BigDecimal monthlyGeneralSummeryPercent = BigDecimal.ZERO.equals(monthlyGeneralSummery) ? BigDecimal.ZERO : monthlyGeneralSummery.divide(BigDecimal.valueOf(2), RoundingMode.HALF_UP);
                saveGrade(subject, academyClass, student, monthlyGeneralSummeryPercent, GradeType.GENERAL_SUMMARY_ASSIGMENT_PERCENT, date);

                BigDecimal monthlySchoolWork = calculateSimpleAverageOfPrefix(gradeList, "GENERAL_SCHOOL_WORK");
                saveGrade(subject, academyClass, student, BigDecimal.valueOf(Math.round(monthlySchoolWork.doubleValue())), GradeType.GENERAL_SCHOOL_WORK_MONTH, date);

                BigDecimal monthlySchoolWorkPercent = BigDecimal.ZERO.equals(monthlySchoolWork) ? BigDecimal.ZERO : monthlySchoolWork.divide(BigDecimal.valueOf(2), RoundingMode.HALF_UP);
                saveGrade(subject, academyClass, student, monthlySchoolWorkPercent, GradeType.GENERAL_SCHOOL_WORK_PERCENT, date);

                BigDecimal sum = (monthlyGeneralSummeryPercent).add(monthlySchoolWorkPercent);
                BigDecimal monthly = BigDecimal.ZERO.equals(sum) ? BigDecimal.ZERO : sum.divide(BigDecimal.valueOf(1L), RoundingMode.HALF_UP);
                saveGrade(subject, academyClass, student, BigDecimal.valueOf(Math.round(monthly.doubleValue())),
                        GradeType.GENERAL_COMPLETE_MONTHLY, date);
            }
        } else {
            for (Student student : studentList) {
                List<Grade> gradeList = gradeRepository.findGradeByAcademyClassIdAndSubjectIdAndCreateTime(academyClassId, subjectId, student.getId(), date);
                BigDecimal monthlyGeneralSummery = calculateGeneralSummeryMonthlyGrade(gradeList).setScale(1, RoundingMode.HALF_UP);
                saveGrade(subject, academyClass, student, BigDecimal.valueOf(Math.round(monthlyGeneralSummery.doubleValue())), GradeType.GENERAL_SUMMARY_ASSIGMENT_MONTH, date);

                BigDecimal monthlyGeneralSummeryPercent = BigDecimal.ZERO.equals(monthlyGeneralSummery) ? BigDecimal.ZERO : monthlyGeneralSummery.divide(BigDecimal.valueOf(2));
                saveGrade(subject, academyClass, student, monthlyGeneralSummeryPercent, GradeType.GENERAL_SUMMARY_ASSIGMENT_PERCENT, date);

                BigDecimal monthlyHomework = calculateSimpleAverageOfPrefix(gradeList, "GENERAL_HOMEWORK");
                saveGrade(subject, academyClass, student, BigDecimal.valueOf(Math.round(monthlyHomework.doubleValue())), GradeType.GENERAL_HOMEWORK_MONTHLY, date);

                BigDecimal monthlyHomeworkPercent = BigDecimal.ZERO.equals(monthlyHomework) ? BigDecimal.ZERO : monthlyHomework.divide(BigDecimal.valueOf(4));
                saveGrade(subject, academyClass, student, monthlyHomeworkPercent, GradeType.GENERAL_HOMEWORK_PERCENT, date);

                BigDecimal monthlySchoolWork = calculateSimpleAverageOfPrefix(gradeList, "GENERAL_SCHOOL_WORK");
                saveGrade(subject, academyClass, student, BigDecimal.valueOf(Math.round(monthlySchoolWork.doubleValue())), GradeType.GENERAL_SCHOOL_WORK_MONTH, date);

                BigDecimal monthlySchoolWorkPercent = BigDecimal.ZERO.equals(monthlySchoolWork) ? BigDecimal.ZERO : monthlySchoolWork.divide(BigDecimal.valueOf(4));
                saveGrade(subject, academyClass, student, monthlySchoolWorkPercent, GradeType.GENERAL_SCHOOL_WORK_PERCENT, date);

                BigDecimal sum = monthlyHomeworkPercent.add(monthlyGeneralSummeryPercent).add(monthlySchoolWorkPercent);
                BigDecimal monthly = BigDecimal.ZERO.equals(sum) ? BigDecimal.ZERO : sum.divide(BigDecimal.valueOf(1L), RoundingMode.HALF_UP);
                saveGrade(subject, academyClass, student, BigDecimal.valueOf(Math.round(monthly.doubleValue())),
                        GradeType.GENERAL_COMPLETE_MONTHLY, date);
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
            saveGrade(null, academyClass, student, BigDecimal.valueOf(Math.round(monthlyUniform.doubleValue())),
                    GradeType.BEHAVIOUR_APPEARING_IN_UNIFORM_MONTHLY, date);
            BigDecimal delays = calculateSimpleAverageOfPrefix(gradeList, "BEHAVIOUR_STUDENT_DELAYS");
            saveGrade(null, academyClass, student, BigDecimal.valueOf(Math.round(delays.doubleValue())),
                    GradeType.BEHAVIOUR_STUDENT_DELAYS_MONTHLY, date);
            BigDecimal inventory = calculateSimpleAverageOfPrefix(gradeList, "BEHAVIOUR_CLASSROOM_INVENTORY");
            saveGrade(null, academyClass, student, BigDecimal.valueOf(Math.round(inventory.doubleValue())),
                    GradeType.BEHAVIOUR_CLASSROOM_INVENTORY_MONTHLY, date);
            BigDecimal hygiene = calculateSimpleAverageOfPrefix(gradeList, "BEHAVIOUR_STUDENT_HYGIENE");
            saveGrade(null, academyClass, student, BigDecimal.valueOf(Math.round(hygiene.doubleValue())),
                    GradeType.BEHAVIOUR_STUDENT_HYGIENE_MONTHLY, date);
            BigDecimal behaviourStudentBehavior = calculateSimpleAverageOfPrefix(gradeList, "BEHAVIOUR_STUDENT_BEHAVIOR");
            saveGrade(null, academyClass, student, BigDecimal.valueOf(Math.round(behaviourStudentBehavior.doubleValue())),
                    GradeType.BEHAVIOUR_STUDENT_BEHAVIOR_MONTHLY, date);
            for (int i = 1; i <= 6; i++) {
                BigDecimal behaviorWeek = calculateSimpleAverageOfPrefixAndWeek(gradeList, "BEHAVIOUR", i);
                String enumName = "BEHAVIOUR_WEEK_AVERAGE_";
                saveGrade(null, academyClass, student, BigDecimal.valueOf(Math.round(behaviorWeek.doubleValue())),
                        GradeType.valueOf(enumName + i), date);
            }
            BigDecimal sum = monthlyUniform.add(delays).add(inventory).add(hygiene).add(behaviourStudentBehavior);
            BigDecimal monthly = BigDecimal.ZERO.equals(sum) ? BigDecimal.ZERO : sum.divide(BigDecimal.valueOf(5L), RoundingMode.HALF_UP);
            saveGrade(null, academyClass, student, BigDecimal.valueOf(Math.round(monthly.doubleValue())),
                    GradeType.BEHAVIOUR_MONTHLY, date);
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
                Optional<Grade> generalSummeryAssignmentRestoration = gradeList.stream().findFirst().filter(grade -> grade.getGradeType().equals(GradeType.GENERAL_SUMMARY_ASSIGMENT_RESTORATION));
                if (generalSummeryAssignmentRestoration.isEmpty() || generalSummeryAssignmentRestoration.get().getValue() == null) {
                    throw new SGSException(SGSExceptionCode.BAD_REQUEST, ExceptionKeys.GENERAL_SUMMERY_GRADES_NOT_PRESENT);
                }
                return generalSummeryAssignmentRestoration.get().getValue();
            } else {
                return getRestorationGrade(gradeList, generalSummeryAssignment2.get(0));
            }
        } else {
            if (generalSummeryAssignment2.isEmpty() || generalSummeryAssignment2.get(0).getValue() == null) {
                return getRestorationGrade(gradeList, generalSummeryAssignment1.get(0));
            } else {
                BigDecimal sum = generalSummeryAssignment2.get(0).getValue()
                        .add(generalSummeryAssignment1.get(0).getValue());
                return BigDecimal.ZERO.equals(sum) ? BigDecimal.ZERO : sum.divide(BigDecimal.valueOf(2L), RoundingMode.HALF_UP);
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
                Optional<Grade> generalSummeryAssignmentRestoration = gradeList.stream().findFirst().filter(grade -> grade.getGradeType().equals(GradeType.GENERAL_SUMMARY_ASSIGMENT_RESTORATION));
                if (generalSummeryAssignmentRestoration.isEmpty() || generalSummeryAssignmentRestoration.get().getValue() == null) {
                    throw new SGSException(SGSExceptionCode.BAD_REQUEST, ExceptionKeys.GENERAL_SUMMERY_GRADES_NOT_PRESENT);
                }
                return generalSummeryAssignmentRestoration.get().getValue();
            } else {
                return getRestorationGradeTransit(gradeList, generalSummeryAssignment2.get(0));
            }
        } else {
            if (generalSummeryAssignment2.isEmpty() || generalSummeryAssignment2.get(0).getValue() == null) {
                return getRestorationGradeTransit(gradeList, generalSummeryAssignment1.get(0));
            } else {
                BigDecimal sum = generalSummeryAssignment2.get(0).getValue()
                        .add(generalSummeryAssignment1.get(0).getValue());
                return BigDecimal.ZERO.equals(sum) ? BigDecimal.ZERO : sum.divide(BigDecimal.valueOf(2L), RoundingMode.HALF_UP);
            }
        }
    }

    private BigDecimal getRestorationGrade(List<Grade> gradeList, Grade generalSummeryAssignment2) throws SGSException {
        Optional<Grade> generalSummeryAssignmentRestoration = gradeList.stream().findFirst().filter(grade -> grade.getGradeType().equals(GradeType.GENERAL_SUMMARY_ASSIGMENT_RESTORATION));
        if (generalSummeryAssignmentRestoration.isEmpty() || generalSummeryAssignmentRestoration.get().getValue() == null) {
            return generalSummeryAssignment2.getValue();
        }
        BigDecimal sum = generalSummeryAssignment2.getValue()
                .add(generalSummeryAssignmentRestoration.get().getValue());
        return BigDecimal.ZERO.equals(sum) ? BigDecimal.ZERO : sum.divide(BigDecimal.valueOf(2L), RoundingMode.HALF_UP);
    }

    private BigDecimal getRestorationGradeTransit(List<Grade> gradeList, Grade generalSummeryAssignment2) throws SGSException {
        Optional<Grade> generalSummeryAssignmentRestoration = gradeList.stream().findFirst().filter(grade -> grade.getGradeType().equals(GradeType.TRANSIT_SUMMARY_ASSIGMENT_RESTORATION));
        if (generalSummeryAssignmentRestoration.isEmpty() || generalSummeryAssignmentRestoration.get().getValue() == null) {
            return generalSummeryAssignment2.getValue();
        }
        BigDecimal sum = generalSummeryAssignment2.getValue()
                .add(generalSummeryAssignmentRestoration.get().getValue());
        return BigDecimal.ZERO.equals(sum) ? BigDecimal.ZERO : sum.divide(BigDecimal.valueOf(2L), RoundingMode.HALF_UP);
    }

    private BigDecimal calculateSimpleAverageOfPrefix(List<Grade> gradeList, String prefix) {
        List<Grade> eligibleGrades = gradeList.stream()
                .filter(grade -> grade.getGradeType().name().startsWith(prefix) &&
                        !grade.getGradeType().name().contains("MONTHLY") && !grade.getGradeType().name().contains("PERCENT"))
                .collect(Collectors.toList());
        BigDecimal sum = eligibleGrades.stream().map(Grade::getValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        return BigDecimal.ZERO.equals(sum) ? BigDecimal.ZERO : sum.divide(BigDecimal.valueOf(eligibleGrades.size()), RoundingMode.HALF_UP);
    }

    private BigDecimal calculateSimpleAverageOfPrefixAndWeek(List<Grade> gradeList, String prefix, int index) {
        List<Grade> eligibleGrades = gradeList.stream()
                .filter(grade -> grade.getGradeType().name().startsWith(prefix) &&
                        grade.getGradeType().name().contains(String.valueOf(index)) &&
                        !grade.getGradeType().name().contains("MONTHLY") && !grade.getGradeType().name().contains("PERCENT"))
                .collect(Collectors.toList());
        BigDecimal sum = eligibleGrades.stream().map(Grade::getValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        return BigDecimal.ZERO.equals(sum) ? BigDecimal.ZERO : sum.divide(BigDecimal.valueOf(eligibleGrades.size()), RoundingMode.HALF_UP);
    }

    private void saveGrade(Subject subject, AcademyClass academyClass, Student student, BigDecimal value, GradeType gradeType, Date exactMonth) {
        if (value.equals(BigDecimal.ZERO)) {
            return;
        }

        Grade existing;
        if (subject == null) {
            existing = gradeRepository.findGradeByAcademyClassIdAndSubjectIdAndGradeTypeAndExactMonth(academyClass.getId(),
                    null, student.getId(), gradeType, exactMonth);
        } else {
            existing = gradeRepository.findGradeByAcademyClassIdAndSubjectIdAndGradeTypeAndExactMonth(academyClass.getId(),
                    subject.getId(), student.getId(), gradeType, exactMonth);
        }
        if (existing != null) {
            existing.setValue(value);
            gradeRepository.save(existing);
            return;
        }

        if (exactMonth.getMonth() == Calendar.FEBRUARY) {
            exactMonth.setMonth(Calendar.JANUARY);
        } else if (exactMonth.getMonth() == Calendar.OCTOBER) {
            exactMonth.setMonth(Calendar.SEPTEMBER);
        }

        Grade grade = Grade.builder()
                .gradeType(gradeType)
                .subject(subject)
                .academyClass(academyClass)
                .student(student)
                .value(value)
                .exactMonth(exactMonth)
                .build();
        gradeRepository.save(grade);
    }
}
