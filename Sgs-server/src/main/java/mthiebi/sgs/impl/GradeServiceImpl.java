package mthiebi.sgs.impl;

import mthiebi.sgs.ExceptionKeys;
import mthiebi.sgs.SGSException;
import mthiebi.sgs.SGSExceptionCode;
import mthiebi.sgs.models.*;
import mthiebi.sgs.repository.*;
import mthiebi.sgs.service.AbsenceService;
import mthiebi.sgs.service.ClosedPeriodService;
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

    @Autowired
    private ClosedPeriodService closedPeriodService;

    @Autowired
    private AbsenceService absenceService;

    @Autowired
    private AbsenceRepository absenceRepository;


    @Override
    public Grade insertStudentGrade(Grade grade, String semester) {
        Date exactDate = grade.getExactMonth();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(exactDate);
        if (calendar.get(Calendar.MONTH) == Calendar.FEBRUARY) {
            calendar.set(Calendar.MONTH, Calendar.JANUARY);
        } else if (calendar.get(Calendar.MONTH) == Calendar.OCTOBER) {
            calendar.set(Calendar.MONTH, Calendar.SEPTEMBER);
        }
        Student student = studentRepository.findById(grade.getStudent().getId()).orElseThrow();
        AcademyClass academyClass = academyClassRepository.getAcademyClassByStudent(student.getId()).orElseThrow();
        Subject subject = grade.getSubject() == null ? null : subjectRepository.findById(grade.getSubject().getId()).orElseThrow();


        Grade existing = gradeRepository.findGradeByAcademyClassIdAndSubjectIdAndGradeTypeAndExactMonth(academyClass.getId(),
                subject == null ? null : subject.getId(), student.getId(), grade.getGradeType(), exactDate);

//        addAbsenceGradeIfNecessary(grade, academyClass, student, calendar, existing);

        if (existing != null) {
            if (grade.getValue() == null) {
                gradeRepository.deleteById(existing.getId());
                return new Grade();
            }
            existing.setValue(grade.getValue());
            gradeRepository.save(existing);
            return existing;
        }

        if (grade.getValue() == null) {
            return new Grade();
        }
        grade.setExactMonth(checkForDiagnostic(calendar, grade.getGradeType(), semester));
        grade.setStudent(student);
        grade.setAcademyClass(academyClass);
        grade.setSubject(subject);
        return gradeRepository.save(grade);
    }

    private Date checkForDiagnostic(Calendar calendar, GradeType gradeType, String semester) {
        if (gradeType.equals(GradeType.DIAGNOSTICS_1) || gradeType.equals(GradeType.DIAGNOSTICS_2)) {
            calendar.set(Calendar.MONTH, Calendar.DECEMBER);
        } else if (gradeType.equals(GradeType.DIAGNOSTICS_3) || gradeType.equals(GradeType.DIAGNOSTICS_4)) {
            calendar.set(Calendar.MONTH, Calendar.JUNE);
        } else if (gradeType.equals(GradeType.SHEMOKMEDEBITOBA)) {
            if (semester.equalsIgnoreCase("firstSemester")) {
                calendar.set(Calendar.MONTH, Calendar.DECEMBER);
            } else if (semester.equalsIgnoreCase("secondSemester")) {
                calendar.set(Calendar.MONTH, Calendar.JUNE);
            }
        }
        return calendar.getTime();
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
                .filter(grade -> grade.getGradeType().toString().startsWith(gradeTypePrefix) || grade.getGradeType().toString().equalsIgnoreCase("GENERAL_ABSENCE_MONTHLY"))
                .collect(Collectors.toList());
        if (studentId != null) {
            Student student = studentRepository.findById(studentId).orElseThrow();
            return fillWithEmptyGradeListOfGradeType(List.of(student), gradeTypePrefix, academyClass, currSubject, existingGrades);
        }
        return fillWithEmptyGradeListOfGradeType(allStudentsInAcademyClass, gradeTypePrefix, academyClass, currSubject, existingGrades);
    }

    @Override
    public List<Grade> getStudentGradeByClassAndSubjectIdAndCreateTime(Long classId,
                                                                       Long subjectId,
                                                                       Long studentId,
                                                                       Date createTime,
                                                                       String gradeTypePrefix,
                                                                       Date closedPeriod) {
        List<Student> allStudentsInAcademyClass = studentRepository.findAllByAcademyClass(classId);
        Subject currSubject = subjectId == null ? null : subjectRepository.findById(subjectId).orElse(null);
        AcademyClass academyClass = academyClassRepository.findById(classId).orElse(null);

        List<Grade> existingGrades = gradeRepository.findGradeByAcademyClassIdAndSubjectIdAndCreateTime(classId, subjectId, studentId, createTime, closedPeriod)
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
    public Object getGradeByComponent(Long classId, Long studentId, String yearRange, Date createDate, String component, Date closedPeriod) throws SGSException {
        int startYear = 2023, endYear = 2023;
        if (yearRange != null) {
            String[] arr = yearRange.split("-");
            startYear = Integer.parseInt(arr[0]);
            endYear = Integer.parseInt(arr[1]);
        }
        Object gradeByStudent = new HashMap<>();
        switch (component) {
            case "firstSemester":
                gradeByStudent = fillMissingSubjects(classId, gradeRepository.findGradeBySemester(classId, startYear, true, closedPeriod), true, studentId);
                break;
            case "secondSemester":
                gradeByStudent = fillMissingSubjects(classId, gradeRepository.findGradeBySemester(classId, endYear, false, closedPeriod), false, studentId);
                break;
            case "anual":
                gradeByStudent = fillMissingSubjectsAnual(classId, getAnualGrades(classId, startYear, endYear, closedPeriod), studentId);
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
        return fillMissingSubjects(classId, result, studentId, createDate);
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
        Date latest = closedPeriodService.getLatestClosedPeriodBy(academyClass.getId());
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MONTH, month.intValue());
        if (year != null) {
            calendar.set(Calendar.YEAR, year.intValue());
        }
        gradeTypePrefix = academyClass.getIsTransit() ? "TRANSIT" : gradeTypePrefix;
        final String pref = gradeTypePrefix;
        List<Grade> existingGrades = gradeRepository.findGradeByAcademyClassIdAndSubjectIdAndExactMonthAndYear(academyClass.getId(),
                        subjectId, student.getId(), calendar.get(Calendar.MONTH), calendar.get(Calendar.YEAR), latest)
                .stream()
                .filter(grade -> grade.getGradeType().toString().startsWith(pref))
                .collect(Collectors.toList());

        return fillWithEmptyGradeListOfGradeType(List.of(student), gradeTypePrefix, academyClass, currSubject, existingGrades);
    }

    @Override
    public List<Grade> findAllMonthlyGradesForSubjectInYear(String studentUsername, Long subjectId, Long year) {
        Student student = studentRepository.findByUsername(studentUsername).orElseThrow();
        Subject currSubject = subjectId == null ? null : subjectRepository.findById(subjectId).orElseThrow();
        AcademyClass academyClass = academyClassRepository.getAcademyClassByStudent(student.getId()).orElseThrow();
        Date latest = closedPeriodService.getLatestClosedPeriodBy(academyClass.getId());
        Calendar calendar = Calendar.getInstance();
        if(year != null){
            calendar.set(Calendar.YEAR, year.intValue());
        }
        List<Grade> existingGrades = gradeRepository.findGradeByAcademyClassIdAndSubjectIdAndGradeTypeAndYear(academyClass.getId(),
                subjectId, student.getId(), academyClass.getIsTransit() ? GradeType.TRANSIT_SCHOOL_COMPLETE_MONTHLY : GradeType.GENERAL_COMPLETE_MONTHLY, calendar.get(Calendar.YEAR), latest);
        return fillWithEmptyGradeListOfGradeType(List.of(student), academyClass.getIsTransit() ? "TRANSIT_SCHOOL_COMPLETE_MONTHLY" : "GENERAL_COMPLETE_MONTHLY", academyClass, currSubject, existingGrades);
    }

    @Override
    public List<Grade> findAllMonthlyGradesForMonthAndYear(String studentUsername, Long month, Long year) {
        Student student = studentRepository.findByUsername(studentUsername).orElseThrow();
        AcademyClass academyClass = academyClassRepository.getAcademyClassByStudent(student.getId()).orElseThrow();
        Date latest = closedPeriodService.getLatestClosedPeriodBy(academyClass.getId());
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MONTH, month.intValue());
        if (year != null) {
            calendar.set(Calendar.YEAR, year.intValue());
        }
        List<Grade> existingGrades = gradeRepository.findGradeByAcademyClassIdAndSubjectIdAndGradeTypeAndExactMonthAndYear(academyClass.getId(),
                null, student.getId(), academyClass.getIsTransit() ? GradeType.TRANSIT_SCHOOL_COMPLETE_MONTHLY : GradeType.GENERAL_COMPLETE_MONTHLY, calendar.get(Calendar.MONTH), calendar.get(Calendar.YEAR), latest);
        return fillWithEmptyGradeListOfGradeType(student, academyClass.getIsTransit() ? "TRANSIT_SCHOOL_COMPLETE_MONTHLY" : "GENERAL_COMPLETE_MONTHLY", academyClass, academyClass.getSubjectList(), existingGrades, calendar);

    }

    @Override
    public List<Grade> findAllBehaviourGradesForMonthAndYear(String studentUsername, Long month, Long year) {
        Student student = studentRepository.findByUsername(studentUsername).orElseThrow();
        AcademyClass academyClass = academyClassRepository.getAcademyClassByStudent(student.getId()).orElseThrow();
        Date latest = closedPeriodService.getLatestClosedPeriodBy(academyClass.getId());

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MONTH, month.intValue());
        if (year != null) {
            calendar.set(Calendar.YEAR, year.intValue());
        }
        List<Grade> existingGrades = getStudentGradeByClassAndSubjectIdAndCreateTime(academyClass.getId(),
                null, student.getId(), calendar.getTime(), "BEHAVIOUR", latest);

        return existingGrades;

    }

    @Override
    public Object getGradeByComponent(String userName, String yearRange, Date date, String component) throws SGSException {
        Student student = studentRepository.findByUsername(userName).orElseThrow();
        AcademyClass academyClass = academyClassRepository.getAcademyClassByStudent(student.getId()).orElseThrow();
        Date latest = closedPeriodService.getLatestClosedPeriodBy(academyClass.getId());

        return getGradeByComponent(academyClass.getId(), student.getId(), yearRange, date, component, latest);
    }

    @Override
    public List<Grade> getAbsenceGrades(String username, String yearRange, Long month) throws SGSException {
        int startYear = 2023, endYear = 2023;
        if (yearRange != null) {
            String[] arr = yearRange.split("-");
            startYear = Integer.parseInt(arr[0]);
            endYear = Integer.parseInt(arr[1]);
        }

        Student student = studentRepository.findByUsername(username).orElseThrow();
        AcademyClass academyClass = academyClassRepository.getAcademyClassByStudent(student.getId()).orElseThrow();
        Date latest = closedPeriodService.getLatestClosedPeriodBy(academyClass.getId());

        return gradeRepository.findGradeByAcademyClassIdAndSubjectIdAndGradeTypeAndExactMonthAndYear(academyClass.getId(), student.getId(), GradeType.GENERAL_ABSENCE_MONTHLY, month, startYear, endYear, latest);
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

    private List<Grade> fillWithEmptyGradeListOfGradeType(Student student,
                                                          String gradeTypePrefix,
                                                          AcademyClass academyClass,
                                                          List<Subject> subjects,
                                                          List<Grade> existingGrades,
                                                          Calendar calendar) {
        List<GradeType> gradeTypes = Arrays.stream(GradeType.values())
                .filter(gradeType -> gradeType.toString().startsWith(gradeTypePrefix))
                .collect(Collectors.toList());
        List<Grade> result = new ArrayList<>(existingGrades);
        for (Subject subject : subjects) {
            for (GradeType gradeType : gradeTypes) {
                if (existingGrades.stream().filter(grade -> grade.getGradeType() == gradeType && subject.getId() == grade.getSubject().getId()).findAny().isEmpty()) {
                    Grade grade = buildGradeOfGradeType(gradeType, academyClass, student, subject);
                    result.add(grade);
                }
            }
        }

        Grade behaviourGrade = new Grade();
        behaviourGrade.setId(3L);
        Subject behaviourSubject = new Subject();
        behaviourSubject.setName("behaviour");
        behaviourSubject.setId(9999L);
        behaviourGrade.setSubject(behaviourSubject);
        behaviourGrade.setValue(gradeRepository.findBehaviourMonth(student.getId(), calendar.getTime()));
        result.add(behaviourGrade);

        Grade ratingGrade = new Grade();
        ratingGrade.setId(1L);
        Subject ratingSubject = new Subject();
        ratingSubject.setName("rating");
        ratingSubject.setId(7777L);
        ratingGrade.setSubject(ratingSubject);
        ratingGrade.setValue(adjustStudentRating(result));
        result.add(ratingGrade);

        Grade ebsenceGrade = new Grade();
        ebsenceGrade.setId(2L);
        Subject junkSubject = new Subject();
        junkSubject.setName("absence");
        junkSubject.setId(8888L);
        ebsenceGrade.setSubject(junkSubject);
        ebsenceGrade.setValue(gradeRepository.findTotalAbsenceHours(student.getId(), calendar.getTime()));
        result.add(ebsenceGrade);

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
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        if (!value1.equals(BigDecimal.ZERO)) {
            sum = sum.add(value1);
            count++;
        }
        if (!value2.equals(BigDecimal.ZERO)) {
            sum = sum.add(value2);
            count++;
        }
        BigDecimal finalAverage = sum.equals(BigDecimal.ZERO) ? BigDecimal.ZERO : sum.divide(BigDecimal.valueOf(count), RoundingMode.HALF_UP);

        if (!value3.equals(BigDecimal.ZERO)) {
            return finalAverage.add(value3).divide(BigDecimal.valueOf(2), RoundingMode.HALF_UP);
        }
        return finalAverage;
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
                                        if (subject.getName().equals("behaviour1")) {
                                            BigDecimal firstBehaviour = first.getOrDefault(student, new HashMap<>()).getOrDefault(subject, new HashMap<>()).getOrDefault(-7, BigDecimal.ZERO);
                                            temporaryMap.put(-7, firstBehaviour);
                                            return temporaryMap;
                                        } else if (subject.getName().equals("behaviour2")) {
                                            BigDecimal secondBehaviour = second.getOrDefault(student, new HashMap<>()).getOrDefault(subject, new HashMap<>()).getOrDefault(-7, BigDecimal.ZERO);
                                            temporaryMap.put(-7, secondBehaviour);
                                            return temporaryMap;
                                        }
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

    private Map<Student, Map<Subject, Map<Integer, BigDecimal>>> getAnualGrades(Long classId, int startYear, int endYear, Date closedPeriod) throws SGSException {
        Map<Student, Map<Subject, Map<Integer, BigDecimal>>> first = gradeRepository.findGradeBySemester(classId, startYear, true, closedPeriod);
        Map<Student, Map<Subject, Map<Integer, BigDecimal>>> second = gradeRepository.findGradeBySemester(classId, endYear, false, closedPeriod);
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
            if (existMap != null) {
                Subject behaviourSubject = existMap.keySet().stream().filter(sub -> sub.getName().equals("behaviour1")).findFirst().get();
                Subject behaviourSubject2 = existMap.keySet().stream().filter(sub -> sub.getName().equals("behaviour2")).findFirst().get();
                var finalMap = existMap.get(behaviourSubject);
                finalMap.put(-8, existMap.get(behaviourSubject2).get(-7));
                newMap.put(behaviourSubject, finalMap);
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
        List<Integer> mapKeys = firstSemester ? List.of(-7, -4, -3, -2, -1, 9, 11, 12) : List.of(-7, -6, -5, -2, -1, 1, 3, 4, 5, 6);

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
            if (existMap != null) {
                Subject behaviourSubject = existMap.keySet().stream().filter(sub -> sub.getName().equals(firstSemester ? "behaviour1" : "behaviour2")).findFirst().get();
                newMap.put(behaviourSubject, existMap.get(behaviourSubject));
            }
            if (studentId != null && student.getId() != studentId) {
                map.remove(student);
                continue;
            }
            map.put(student, newMap);
        }
        return map;
    }

    private Map<Student, Map<Subject, Map<Integer, BigDecimal>>> fillMissingSubjectsClient(Long classId, Map<Student, Map<Subject, Map<Integer, BigDecimal>>> map, boolean firstSemester, Long studentId) throws SGSException {
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

    private Map<Student, Map<Subject, BigDecimal>> fillMissingSubjects(Long classId, Map<Student, Map<Subject, BigDecimal>> map, Long studentId, Date createDate) throws SGSException {
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
            Subject behaviourSubject = new Subject();
            behaviourSubject.setName("behaviour");
            behaviourSubject.setId(9999L);
            newMap.put(behaviourSubject, gradeRepository.findBehaviourMonth(student.getId(), createDate));
            map.put(student, newMap);
            Subject ratingSubject = new Subject();
            ratingSubject.setName("rating");
            ratingSubject.setId(7777L);
            newMap.put(ratingSubject, adjustStudentRating(newMap));
            Subject junkSubject = new Subject();
            junkSubject.setName("absence");
            junkSubject.setId(8888L);
            newMap.put(junkSubject, gradeRepository.findTotalAbsenceHours(student.getId(), createDate));
        }
//        for (Subject subject : sums.keySet()) {
//            BigDecimal sum = sums.get(subject);
//            BigDecimal average = sum.divide(BigDecimal.valueOf(studentList.size()), RoundingMode.HALF_UP);
//            sums.put(subject, average);
//        }
//        Student average = Student.builder().id(-5).firstName("საშუალო").lastName("ქულა").build();
//        map.put(average, sums);
//        Student teacher = Student.builder().id(-6).firstName("მასწავლებელი").lastName("").build();
//        map.put(teacher, sums);
        return map;
    }

    private BigDecimal adjustStudentRating(Map<Subject, BigDecimal> newMap) {
        BigDecimal sum = BigDecimal.valueOf(0);
        long count = 0L;
        for (Subject s : newMap.keySet()) {
            BigDecimal value = newMap.get(s);
            if (value != null && value.compareTo(BigDecimal.ZERO) > 0) {
                sum = sum.add(value);
                count++;
            }
        }
        if (Objects.equals(sum, BigDecimal.ZERO) || count == 0) {
            return BigDecimal.ZERO;
        }
        return sum.divide(new BigDecimal(count), 0, RoundingMode.HALF_UP);
    }

    private BigDecimal adjustStudentRating(List<Grade> list) {
        BigDecimal sum = BigDecimal.valueOf(0);
        long count = 0L;
        for (Grade g : list) {
            BigDecimal value = g.getValue();
            if (value != null && value.compareTo(BigDecimal.ZERO) > 0) {
                sum = sum.add(value);
                count++;
            }
        }
        if (Objects.equals(sum, BigDecimal.ZERO) || count == 0) {
            return BigDecimal.ZERO;
        }
        return sum.divide(new BigDecimal(count), 0, RoundingMode.HALF_UP);
    }

    private void addAbsenceGradeIfNecessary(Grade grade, AcademyClass academyClass, Student student, Calendar calendar, Grade existing) {
        if (grade.getGradeType() == GradeType.GENERAL_ABSENCE_MONTHLY || grade.getGradeType() == GradeType.ABSENCE_DAILY) {
            AbsenceGrade absenceGrade = absenceService.findAbsenceGrade(academyClass.getId(), student.getId(), calendar.getTime());
            if (absenceGrade == null) {
                AbsenceGradeType absenceGradeType = absenceService.getGradeType(calendar.getTime());
                absenceService.addAbsenceGrade(student.getId(), academyClass.getId(), absenceGradeType, grade.getValue(), calendar.getTime());
            } else {
                BigDecimal total;
                if (existing != null) {
                    BigDecimal diff = existing.getValue().subtract(grade.getValue());
                    total = absenceGrade.getValue().add(diff);
                } else {
                    total = absenceGrade.getValue().add(grade.getValue());
                }
                absenceGrade.setValue(total);
                absenceRepository.save(absenceGrade);
            }
        }
    }
}
