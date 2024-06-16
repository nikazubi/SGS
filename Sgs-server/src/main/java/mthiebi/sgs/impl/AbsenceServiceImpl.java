package mthiebi.sgs.impl;

import mthiebi.sgs.models.AbsenceGrade;
import mthiebi.sgs.models.AbsenceGradeType;
import mthiebi.sgs.models.AcademyClass;
import mthiebi.sgs.models.Student;
import mthiebi.sgs.repository.AbsenceRepository;
import mthiebi.sgs.repository.AcademyClassRepository;
import mthiebi.sgs.repository.StudentRepository;
import mthiebi.sgs.service.AbsenceService;
import mthiebi.sgs.service.ClosedPeriodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AbsenceServiceImpl implements AbsenceService {

    @Autowired
    private AbsenceRepository absenceRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private AcademyClassRepository academyClassRepository;

    @Autowired
    private ClosedPeriodService closedPeriodService;


    @Override
    public Map<Student, List<AbsenceGrade>> findAbsenceGrade(Long academyClassId, Long studentId, String yearRange) {
        int startYear = 2023, endYear = 2023;
        if (yearRange != null) {
            String[] arr = yearRange.split("-");
            startYear = Integer.parseInt(arr[0]);
            endYear = Integer.parseInt(arr[1]);
        }
        Map<Student, List<AbsenceGrade>> grades = absenceRepository.findAbsenceGrade(academyClassId, studentId, startYear, endYear).stream()
                .collect(Collectors.groupingBy(AbsenceGrade::getStudent));
        List<Student> students = (studentId == null) ? studentRepository.findAllByAcademyClass(academyClassId) : new ArrayList<>(grades.keySet());

        Map<Student, List<AbsenceGrade>> result = new HashMap<>();

        for (Student student : students) {
            Set<AbsenceGradeType> gradesOfStudent = grades.get(student) == null ? Collections.emptySet() : grades.get(student).stream().map(AbsenceGrade::getGradeType).collect(Collectors.toSet());
            List<AbsenceGrade> gradesForThisStudent = grades.getOrDefault(student, new ArrayList<>());
            for (AbsenceGradeType type : AbsenceGradeType.values()) {
                if (!gradesOfStudent.contains(type)) {
                    AbsenceGrade dummy = new AbsenceGrade();
                    dummy.setGradeType(type);
                    dummy.setValue(BigDecimal.ZERO);
                    gradesForThisStudent.add(dummy);
                }
            }
            result.put(student, gradesForThisStudent);
        }
        return result;
    }

    @Override
    public AbsenceGrade addAbsenceGrade(Long studentId, Long academyClassId, AbsenceGradeType gradeType, BigDecimal value, Date exactMonth) {

        Student student = studentRepository.findById(studentId).orElseThrow();
        AcademyClass academyClass = academyClassRepository.findById(academyClassId).orElseThrow();

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(exactMonth);

        AbsenceGrade absenceGrade = absenceRepository.findAbsenceGrade(academyClassId, studentId, calendar.get(Calendar.YEAR), gradeType);
        if (absenceGrade != null) {
            if (value == null) {
                AbsenceGrade toReturn = new AbsenceGrade();
                toReturn.setGradeType(gradeType);
                toReturn.setValue(value);
                toReturn.setAcademyClass(academyClass);
                toReturn.setStudent(student);
                toReturn.setExactMonth(exactMonth);
                absenceRepository.delete(absenceGrade);
                return toReturn;
            }
            absenceGrade.setValue(value);
            return absenceRepository.save(absenceGrade);
        }

        AbsenceGrade toAdd = new AbsenceGrade();
        toAdd.setGradeType(gradeType);
        toAdd.setValue(value);
        toAdd.setAcademyClass(academyClass);
        toAdd.setStudent(student);
        toAdd.setExactMonth(exactMonth);
        return absenceRepository.save(toAdd);
    }

    @Override
    public AbsenceGrade findAbsenceGrade(Long academyClassId, long studentId, Date time) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(time);

        return absenceRepository.findAbsenceGradeByMonthAndYear(academyClassId, studentId, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH));
    }

    @Override
    public AbsenceGradeType getGradeType(Date time) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(time);
        int month = calendar.get(Calendar.MONTH);
        switch (month) {
            case 0:
                return AbsenceGradeType.JANUARY_FEBRUARY;
            case 1:
                return AbsenceGradeType.JANUARY_FEBRUARY;
            case 2:
                return AbsenceGradeType.MARCH;
            case 3:
                return AbsenceGradeType.APRIL;
            case 4:
                return AbsenceGradeType.MAY;
            case 8:
                return AbsenceGradeType.SEPTEMBER_OCTOBER;
            case 9:
                return AbsenceGradeType.SEPTEMBER_OCTOBER;
            case 10:
                return AbsenceGradeType.NOVEMBER;
            case 11:
                return AbsenceGradeType.DECEMBER;
            default:
                return AbsenceGradeType.JANUARY_FEBRUARY;
        }
    }
}