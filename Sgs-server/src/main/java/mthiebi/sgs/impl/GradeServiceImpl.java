package mthiebi.sgs.impl;

import mthiebi.sgs.ExceptionKeys;
import mthiebi.sgs.SGSException;
import mthiebi.sgs.SGSExceptionCode;
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
        Date exactDate = grade.getExactMonth();
        if (exactDate.getMonth() == Calendar.FEBRUARY) {
            exactDate.setMonth(Calendar.JANUARY);
        } else if (exactDate.getMonth() == Calendar.OCTOBER) {
            exactDate.setMonth(Calendar.SEPTEMBER);
        }
        Student student = studentRepository.findById(grade.getStudent().getId()).orElseThrow();
        AcademyClass academyClass = academyClassRepository.getAcademyClassByStudent(student.getId()).orElseThrow();
        Subject subject = grade.getSubject() == null ? null : subjectRepository.findById(grade.getSubject().getId()).orElseThrow();


        Grade existing = gradeRepository.findGradeByAcademyClassIdAndSubjectIdAndGradeTypeAndExactMonth(academyClass.getId(),
                subject == null ? null : subject.getId(), student.getId(), grade.getGradeType(), exactDate);

        if (existing != null) {
            existing.setValue(grade.getValue());
            gradeRepository.save(existing);
            return existing;
        }

        grade.setExactMonth(exactDate);
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
        Subject currSubject = subjectId == null ? null : subjectRepository.findById(subjectId).orElse(null);
        AcademyClass academyClass = academyClassRepository.findById(classId).orElse(null);

        List<Grade> existingGrades = gradeRepository.findGradeByAcademyClassIdAndSubjectIdAndCreateTime(classId, subjectId, studentId, createTime)
                .stream()
                .filter(grade -> grade.getGradeType().toString().startsWith(gradeTypePrefix))
                .collect(Collectors.toList());
        if (studentId != null) {
            Student student = studentRepository.findById(studentId).orElseThrow();
            return fillWithEmptyGradeListOfGradeType(List.of(student), gradeTypePrefix, academyClass, currSubject, existingGrades);
        }
        return fillWithEmptyGradeListOfGradeType(allStudentsInAcademyClass, gradeTypePrefix, academyClass, currSubject, existingGrades);
    }

    @Override
    public Object getGradeByComponent(Long classId, Long studentId, String yearRange, Date createDate, String component) throws SGSException {
        int startYear = 2023, endYear = 2023;
        if (yearRange != null) {
            String[] arr = yearRange.split("-");
            startYear = Integer.parseInt(arr[0]);
            endYear = Integer.parseInt(arr[1]);
        }
        Object gradeByStudent = new HashMap<>();
        switch (component) {
            case "firstSemester":
                gradeByStudent = fillMissingSubjects(classId, gradeRepository.findGradeBySemester(classId, startYear, true), true, studentId);
                break;
            case "secondSemester":
                gradeByStudent = fillMissingSubjects(classId, gradeRepository.findGradeBySemester(classId, endYear, false), false, studentId);
                break;
            case "anual":
                gradeByStudent = fillMissingSubjectsAnual(classId, getAnualGrades(classId, startYear, endYear), studentId);
                break;
            case "monthly":
                gradeByStudent = getMonthlyGrades(classId, createDate, studentId);
                break;
        }
        return gradeByStudent;
    }

    @Override
    public List<String> getGradeYearGrouped() {
        Integer minYear = gradeRepository.getMinYear();
        Integer maxYear = gradeRepository.getMaxYear();
        List<String> result = new ArrayList<>();
        if (Objects.equals(minYear, maxYear)) return List.of(minYear + "-" + (minYear + 1));
        for (; minYear < maxYear; minYear++) {
            result.add(minYear + "-" + (minYear + 1));
        }
        return result;
    }

    private BigDecimal getFinalExamValueByStudentIdAndSubjectId(long academyClassId, long subjectId, Long studentId, int maxYear) {
        List<Grade> grades = gradeRepository.findGradeByAcademyClassIdAndSubjectIdAndGradeTypeAndYear(academyClassId, subjectId, studentId, GradeType.FINAL_EXAM, maxYear);
        return grades == null || grades.isEmpty() ? BigDecimal.ZERO : grades.get(0).getValue();
    }

    private Map<Student, Map<Subject, BigDecimal>> getMonthlyGrades(Long classId, Date createDate, Long studentId) throws SGSException {
        Map<Student, List<Grade>> gradelist = gradeRepository.findGradeByMonth(classId, createDate);
        Map<Student, Map<Subject, BigDecimal>> result = new HashMap<>();
        for (Student student : gradelist.keySet()) {
            Map<Subject, BigDecimal> value = gradelist.get(student).stream().collect(Collectors.toMap(
                    Grade::getSubject,
                    Grade::getValue
            ));
            result.put(student, value);
        }
        return fillMissingSubjects(classId, result, studentId);
    }

    @Override
    public byte[] exportPdfMonthlyGrade(Long classId, Long studentId, Date month) {

        return new byte[0];
    }

    @Override
    public List<Grade> findGradeByAcademyClassIdAndSubjectIdAndGradeTypeAndExactMonthAndYear(String studentUsername,
                                                                                             Long subjectId,
                                                                                             Long month,
                                                                                             Long year,
                                                                                             String gradeTypePrefix) {
        Student student = studentRepository.findByUsername(studentUsername).orElseThrow();
        Subject currSubject = subjectId == null ? null : subjectRepository.findById(subjectId).orElseThrow();
        AcademyClass academyClass = academyClassRepository.getAcademyClassByStudent(student.getId()).orElseThrow();
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MONTH, month.intValue());
        if (year != null) {
            calendar.set(Calendar.YEAR, year.intValue());
        }

        List<Grade> existingGrades = gradeRepository.findGradeByAcademyClassIdAndSubjectIdAndExactMonthAndYear(academyClass.getId(),
                        subjectId, student.getId(), calendar.get(Calendar.MONTH), calendar.get(Calendar.YEAR))
                .stream()
                .filter(grade -> grade.getGradeType().toString().startsWith(gradeTypePrefix))
                .collect(Collectors.toList());

        return fillWithEmptyGradeListOfGradeType(List.of(student), gradeTypePrefix, academyClass, currSubject, existingGrades);
    }

    @Override
    public List<Grade> findAllMonthlyGradesForSubjectInYear(String studentUsername, Long subjectId, Long year) {
        Student student = studentRepository.findByUsername(studentUsername).orElseThrow();
        Subject currSubject = subjectId == null ? null : subjectRepository.findById(subjectId).orElseThrow();
        AcademyClass academyClass = academyClassRepository.getAcademyClassByStudent(student.getId()).orElseThrow();
        Calendar calendar = Calendar.getInstance();
        if(year != null){
            calendar.set(Calendar.YEAR, year.intValue());
        }
        List<Grade> existingGrades = gradeRepository.findGradeByAcademyClassIdAndSubjectIdAndGradeTypeAndYear(academyClass.getId(),
                subjectId, student.getId(), GradeType.GENERAL_COMPLETE_MONTHLY, calendar.get(Calendar.YEAR));
        return fillWithEmptyGradeListOfGradeType(List.of(student), "GENERAL_COMPLETE_MONTHLY", academyClass, currSubject, existingGrades);
    }

    @Override
    public List<Grade> findAllMonthlyGradesForMonthAndYear(String studentUsername, Long month, Long year) {
        Student student = studentRepository.findByUsername(studentUsername).orElseThrow();
        AcademyClass academyClass = academyClassRepository.getAcademyClassByStudent(student.getId()).orElseThrow();
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MONTH, month.intValue());
        if (year != null) {
            calendar.set(Calendar.YEAR, year.intValue());
        }
        List<Grade> existingGrades = gradeRepository.findGradeByAcademyClassIdAndSubjectIdAndGradeTypeAndExactMonthAndYear(academyClass.getId(),
                null, student.getId(), GradeType.GENERAL_COMPLETE_MONTHLY, calendar.get(Calendar.MONTH), calendar.get(Calendar.YEAR));
        return existingGrades;

    }

    private List<Grade> fillWithEmptyGradeListOfGradeType(List<Student> students,
                                                          String gradeTypePrefix,
                                                          AcademyClass academyClass,
                                                          Subject subject,
                                                          List<Grade> existingGrades) {
        List<GradeType> gradeTypes = Arrays.stream(GradeType.values())
                .filter(gradeType -> gradeType.toString().startsWith(gradeTypePrefix))
                .collect(Collectors.toList());
        List<Grade> result = new ArrayList<>(existingGrades);
        for (Student student : students) {
            for (GradeType gradeType : gradeTypes) {
                if (existingGrades.stream().filter(grade -> grade.getGradeType() == gradeType && student.getId() == grade.getStudent().getId()).findAny().isEmpty()) {
                    Grade grade = buildGradeOfGradeType(gradeType, academyClass, student, subject);
                    result.add(grade);
                }
            }
        }
        return result;
    }

    private Grade buildGradeOfGradeType(GradeType gradeType, AcademyClass academyClass, Student student, Subject subject) {
        return Grade.builder()
                .gradeType(gradeType)
                .academyClass(academyClass)
                .student(student)
                .subject(subject)
                .value(null)
                .build();
    }

    private static BigDecimal calculateAverage(BigDecimal value1, BigDecimal value2, BigDecimal value3) {
        BigDecimal sum = value1.add(value2).add(value3);
        return BigDecimal.ZERO.equals(sum) ? BigDecimal.ZERO : sum.divide(BigDecimal.valueOf(3), RoundingMode.HALF_UP); // Assuming you want the average of two values
    }

    private Map<Student, Map<Subject, Map<Integer, BigDecimal>>> getAnualGrades(Long classId, int startYear, int endYear) throws SGSException {
        Map<Student, Map<Subject, Map<Integer, BigDecimal>>> first = gradeRepository.findGradeBySemester(classId, startYear, true);
        Map<Student, Map<Subject, Map<Integer, BigDecimal>>> second = gradeRepository.findGradeBySemester(classId, endYear, false);
//        if (first == null || first.isEmpty() || second == null || second.isEmpty()) {
//            throw new SGSException("საჭიროა დასრულდეს 2-ვე სემესტრი ნიშნების სნახავად");
//        }
        Set<Student> allStudents = new HashSet<>(first.keySet());
        allStudents.addAll(second.keySet());
        return allStudents.stream()
                .collect(Collectors.toMap(
                        student -> student,
                        student -> {
                            Set<Subject> allSubject = new HashSet<>(first.getOrDefault(student, new HashMap<>()).keySet());
                            allSubject.addAll(second.getOrDefault(student, new HashMap<>()).keySet());
                            return allSubject.stream().collect(Collectors.toMap(
                                    subject -> subject,
                                    subject -> {
                                        Map<Integer, BigDecimal> temporaryMap = new HashMap<>();
                                        BigDecimal finalExamGrade = getFinalExamValueByStudentIdAndSubjectId(classId, subject.getId(), student.getId(), endYear);
                                        BigDecimal firstValue = first.getOrDefault(student, new HashMap<>()).getOrDefault(subject, new HashMap<>()).getOrDefault(-1, BigDecimal.ZERO);
                                        temporaryMap.put(1, firstValue);
                                        BigDecimal secondValue = second.getOrDefault(student, new HashMap<>()).getOrDefault(subject, new HashMap<>()).getOrDefault(-1, BigDecimal.ZERO);
                                        temporaryMap.put(2, secondValue);
                                        temporaryMap.put(3, finalExamGrade);
                                        temporaryMap.put(4, calculateAverage(firstValue, secondValue, finalExamGrade));
                                        return temporaryMap;
                                    }
                            ));
                        }
                ));
    }

    private Map<Student, Map<Subject, Map<Integer, BigDecimal>>> fillMissingSubjectsAnual(Long classId, Map<Student, Map<Subject, Map<Integer, BigDecimal>>> map, Long studentId) throws SGSException {
        AcademyClass academyClass = academyClassRepository.findById(classId).orElseThrow(() -> new SGSException(SGSExceptionCode.BAD_REQUEST, ExceptionKeys.ACADEMY_CLASS_NOT_FOUND));
        List<Subject> subjectList = academyClass.getSubjectList();
        List<Student> studentList = academyClass.getStudentList();
        for (Student student : studentList) {
            Map<Subject, Map<Integer, BigDecimal>> existMap = map.get(student);
            Map<Subject, Map<Integer, BigDecimal>> newMap = new HashMap<>();
            for (Subject subject : subjectList) {
                if (existMap != null && existMap.get(subject) != null) {
                    newMap.put(subject, existMap.get(subject));
                } else {
                    Map<Integer, BigDecimal> map1 = new HashMap<>();
                    map1.put(1, BigDecimal.ZERO);
                    map1.put(2, BigDecimal.ZERO);
                    map1.put(3, BigDecimal.ZERO);
                    newMap.put(subject, map1);
                }
            }
            if (studentId != null && student.getId() != studentId) {
                map.remove(student);
                continue;
            }
            map.put(student, newMap);
        }
        return map;
    }

    private Map<Student, Map<Subject, Map<Integer, BigDecimal>>> fillMissingSubjects(Long classId, Map<Student, Map<Subject, Map<Integer, BigDecimal>>> map, boolean firstSemester, Long studentId) throws SGSException {
        AcademyClass academyClass = academyClassRepository.findById(classId).orElseThrow(() -> new SGSException(SGSExceptionCode.BAD_REQUEST, ExceptionKeys.ACADEMY_CLASS_NOT_FOUND));
        List<Subject> subjectList = academyClass.getSubjectList();
        List<Student> studentList = academyClass.getStudentList();
        List<Integer> mapKeys = firstSemester ? List.of(-4, -3, -2, -1, 9, 11, 12) : List.of(-2, -1, 1, 3, 4, 5, 6);

        for (Student student : studentList) {
            Map<Subject, Map<Integer, BigDecimal>> existMap = map.get(student);
            Map<Subject, Map<Integer, BigDecimal>> newMap = new HashMap<>();
            for (Subject subject : subjectList) {
                if (existMap != null && existMap.get(subject) != null) {
                    newMap.put(subject, existMap.get(subject));
                } else {
                    Map<Integer, BigDecimal> emptyMap = new HashMap<>();
                    for (Integer semesterKey : mapKeys) {
                        emptyMap.put(semesterKey, BigDecimal.ZERO);
                    }
//                    mapKeys.stream().map(key -> emptyMap.put(key, BigDecimal.ZERO));
                    newMap.put(subject, emptyMap);
                }
            }
            if (studentId != null && student.getId() != studentId) {
                map.remove(student);
                continue;
            }
            map.put(student, newMap);
        }
        return map;
    }

    private Map<Student, Map<Subject, BigDecimal>> fillMissingSubjects(Long classId, Map<Student, Map<Subject, BigDecimal>> map, Long studentId) throws SGSException {
        AcademyClass academyClass = academyClassRepository.findById(classId).orElseThrow(() -> new SGSException(SGSExceptionCode.BAD_REQUEST, ExceptionKeys.ACADEMY_CLASS_NOT_FOUND));
        List<Subject> subjectList = academyClass.getSubjectList();
        List<Student> studentList = academyClass.getStudentList();
        Map<Subject, BigDecimal> sums = new HashMap<>();
        for (Student student : studentList) {
            Map<Subject, BigDecimal> existMap = map.get(student);
            Map<Subject, BigDecimal> newMap = new HashMap<>();
            for (Subject subject : subjectList) {
                if (existMap != null && existMap.get(subject) != null) {
                    BigDecimal val = existMap.get(subject);
                    newMap.put(subject, val);
                    BigDecimal currSum = sums.get(subject);
                    if (currSum == null) {
                        currSum = val;
                    } else {
                        currSum = currSum.add(val);
                    }
                    sums.put(subject, currSum);
                } else {
                    newMap.put(subject, BigDecimal.ZERO);
                }
            }
            if (studentId != null && student.getId() != studentId) {
                map.remove(student);
                continue;
            }
            map.put(student, newMap);
        }
        for (Subject subject : sums.keySet()) {
            BigDecimal sum = sums.get(subject);
            BigDecimal average = sum.divide(BigDecimal.valueOf(studentList.size()), RoundingMode.HALF_UP);
            sums.put(subject, average);
        }
        Student average = Student.builder().id(-5).firstName("საშუალო").lastName("ქულა").build();
        map.put(average, sums);
        Student teacher = Student.builder().id(-6).firstName("მასწავლებელი").lastName("").build();
        map.put(teacher, sums);
        return map;
    }
}
