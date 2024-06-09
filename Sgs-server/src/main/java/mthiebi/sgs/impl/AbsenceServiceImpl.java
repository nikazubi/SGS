package mthiebi.sgs.impl;

import mthiebi.sgs.models.AbsenceGrade;
import mthiebi.sgs.models.AbsenceGradeType;
import mthiebi.sgs.models.AcademyClass;
import mthiebi.sgs.models.Student;
import mthiebi.sgs.repository.AbsenceRepository;
import mthiebi.sgs.repository.AcademyClassRepository;
import mthiebi.sgs.repository.StudentRepository;
import mthiebi.sgs.service.AbsenceService;
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
}