package mthiebi.sgs.impl;

import mthiebi.sgs.models.*;
import mthiebi.sgs.repository.AcademyClassRepository;
import mthiebi.sgs.repository.GradeRepository;
import mthiebi.sgs.repository.StudentRepository;
import mthiebi.sgs.repository.SubjectRepository;
import mthiebi.sgs.service.GradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GradeServiceImpl implements GradeService {

    @Autowired
    private GradeRepository gradeRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private AcademyClassRepository academyClassRepository;

    @Autowired
    private SubjectRepository subjectRepository;


    @Override
    public Grade insertStudentGrade(Grade grade) {
        Student student = studentRepository.findById(grade.getStudent().getId()).orElseThrow();
        AcademyClass academyClass = academyClassRepository.getAcademyClassByStudent(student.getId()).orElseThrow();
        Subject subject = grade.getSubject() == null ? null : subjectRepository.findById(grade.getSubject().getId()).orElseThrow();
        grade.setStudent(student);
        grade.setAcademyClass(academyClass);
        grade.setSubject(subject);
        return gradeRepository.save(grade);
    }

    @Override
    public Grade updateStudentGrade(Grade grade) {
        Grade oldGrade = gradeRepository.findById(grade.getId()).orElseThrow();
        oldGrade.setValue(grade.getValue());
        return gradeRepository.save(oldGrade);
    }

    @Override
    public void deleteGrade(Long id) {
        gradeRepository.deleteById(id);
    }

    @Override
    public List<Grade> getStudentGradeByStudentId(Long studentId) {
        return gradeRepository.findGradeByStudentId(studentId);
    }

    @Override
    public List<Grade> getStudentGradeByClassId(Long classId) {
        return gradeRepository.findGradeByAcademyClassId(classId);
    }

    @Override
    public List<Grade> getStudentGradeByClassAndSubjectId(Long classId, Long studentId) {
        return gradeRepository.findGradeByAcademyClassIdAndSubjectId(classId, studentId);
    }

    @Override
    public List<Grade> getStudentGradeByClassAndSubjectIdAndCreateTime(Long classId,
                                                                       Long subjectId,
                                                                       Long studentId,
                                                                       Date createTime,
                                                                       String gradeTypePrefix) {
        List<Student> allStudentsInAcademyClass = studentRepository.findAllByAcademyClass(classId);
        Subject currSubject = subjectId == null? null : subjectRepository.findById(subjectId).orElse(null);
        AcademyClass academyClass = academyClassRepository.findById(classId).orElse(null);

        List<Grade> existingGrades =  gradeRepository.findGradeByAcademyClassIdAndSubjectIdAndCreateTime(classId, subjectId, studentId, createTime)
                                                        .stream()
                                                        .filter(grade -> grade.getGradeType().toString().startsWith(gradeTypePrefix))
                                                        .collect(Collectors.toList());
        return fillWithEmptyGradeListOfGradeType(allStudentsInAcademyClass, gradeTypePrefix, academyClass, currSubject, existingGrades);
    }

    @Override
    public Map<Student, Map<Subject, BigDecimal>> getGradeByComponent(Long classId, Long studentId, String yearRange, Date createDate, String component) {
        String[] arr = yearRange.split("-");
        int startYear = Integer.parseInt(arr[0]), endYear = Integer.parseInt(arr[1]);
        Map<Student, Map<Subject, BigDecimal>> gradeByStudent = new HashMap<>();
        switch (component) {
            case "firstSemester":
                gradeByStudent = gradeRepository.findGradeBySemester(classId, startYear, true);
                break;
            case "secondSemester":
                gradeByStudent = gradeRepository.findGradeBySemester(classId, endYear, false);
                break;
            case "anual":
                gradeByStudent = getAnualGrades(classId, startYear, endYear);
                break;
            case "monthly":
                gradeByStudent = getMonthlyGrades(classId, createDate);
                break;
        }
        return gradeByStudent;

    }

    private Map<Student, Map<Subject, BigDecimal>> getMonthlyGrades(Long classId, Date createDate) {
        Map<Student, List<Grade>> gradelist = gradeRepository.findGradeByMonth(classId, createDate);
        Map<Student, Map<Subject, BigDecimal>> result = new HashMap<>();
        for (Student student : gradelist.keySet()) {
            Map<Subject, BigDecimal> value = gradelist.get(student).stream().collect(Collectors.toMap(
                    Grade::getSubject,
                    entry -> new BigDecimal(entry.getValue())
            ));
            result.put(student, value);
        }
        return result;
    }

    private List<Grade> fillWithEmptyGradeListOfGradeType(List<Student> students,
                                                          String gradeTypePrefix,
                                                          AcademyClass academyClass,
                                                          Subject subject,
                                                          List<Grade> existingGrades){
       List<GradeType> gradeTypes = Arrays.stream(GradeType.values())
                                                    .filter(gradeType -> gradeType.toString().startsWith(gradeTypePrefix))
                                                    .collect(Collectors.toList());
        List<Grade> result = new ArrayList<>(existingGrades);
        for (Student student : students) {
            for(GradeType gradeType : gradeTypes){
                if (existingGrades.stream().filter(grade -> grade.getGradeType() == gradeType && student.getId() == grade.getStudent().getId()).findAny().isEmpty()){
                    Grade grade = buildGradeOfGradeType(gradeType, academyClass, student, subject);
                    result.add(grade);
                }
            }
        }
        return result;
    }

    private Grade buildGradeOfGradeType(GradeType gradeType, AcademyClass academyClass, Student student, Subject subject){
        return  Grade.builder()
                    .gradeType(gradeType)
                    .academyClass(academyClass)
                    .student(student)
                    .subject(subject)
                    .value(null)
                    .build();
    }

    private static BigDecimal calculateAverage(BigDecimal value1, BigDecimal value2) {
        BigDecimal sum = value1.add(value2);
        return sum.divide(BigDecimal.valueOf(2), RoundingMode.HALF_UP); // Assuming you want the average of two values
    }

    private Map<Student, Map<Subject, BigDecimal>> getAnualGrades(Long classId, int startYear, int endYear) {
        Map<Student, Map<Subject, BigDecimal>> first = gradeRepository.findGradeBySemester(classId, startYear, true);
        Map<Student, Map<Subject, BigDecimal>> second = gradeRepository.findGradeBySemester(classId, endYear, false);
        Set<Student> allStudents = new HashSet<>(first.keySet());
        allStudents.addAll(second.keySet());
        return allStudents.stream()
                .collect(Collectors.toMap(
                        student -> student,
                        student -> {
                            Set<Subject> allSubject = new HashSet<>(first.get(student).keySet());
                            allSubject.addAll(second.get(student).keySet());
                            return allSubject.stream().collect(Collectors.toMap(
                                    subject -> subject,
                                    subject -> {
                                        BigDecimal firstValue = first.get(student).getOrDefault(subject, BigDecimal.ZERO);
                                        BigDecimal secondValue = second.get(student).getOrDefault(subject, BigDecimal.ZERO);
                                        return calculateAverage(firstValue, secondValue);
                                    }
                            ));
                        }
                ));
    }

}
