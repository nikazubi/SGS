package mthiebi.sgs.repository;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.jpa.impl.JPAQueryFactory;
import mthiebi.sgs.ExceptionKeys;
import mthiebi.sgs.SGSException;
import mthiebi.sgs.SGSExceptionCode;
import mthiebi.sgs.models.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

import static com.querydsl.core.types.dsl.Expressions.dateTemplate;

@Repository
public class GradeRepositoryCustomImpl implements mthiebi.sgs.repository.GradeRepositoryCustom {

    private static final QGrade qGrade = QGrade.grade;
    @Autowired
    private JPAQueryFactory qf;

    @Autowired
    private AcademyClassRepository academyClassRepository;

    @Override
    public List<Grade> findGradeByAcademyClassIdAndSubjectIdAndCreateTime(Long academyClassId,
                                                                          Long subjectId,
                                                                          Long studentId,
                                                                          Date createTime) {
        Predicate academyClassIdPredicate = academyClassId == null ? qGrade.academyClass.id.isNotNull() : qGrade.academyClass.id.eq(academyClassId);
        Predicate subjectIdPredicate = subjectId != null ? qGrade.subject.id.eq(subjectId) : qGrade.subject.id.isNull();
        Predicate studentIdPredicate = studentId != null ? qGrade.student.id.eq(studentId) : qGrade.student.id.isNotNull();
        Predicate datePredicate;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(createTime);
        int month = calendar.get(Calendar.MONTH) == Calendar.FEBRUARY ? 0 : calendar.get(Calendar.MONTH) == Calendar.OCTOBER ? 8 : calendar.get(Calendar.MONTH);
        datePredicate = qGrade.exactMonth.month().eq(month + 1).and(qGrade.exactMonth.year().eq(calendar.get(Calendar.YEAR)));
        //        StringExpression prefixPredicate = qGrade.gradeType.stringValue();
//        BooleanExpression booleanExpression = prefixPredicate.startsWith("GENERAL_");

        return qf.selectFrom(qGrade)
                .where(academyClassIdPredicate)
                .where(subjectIdPredicate)
                .where(studentIdPredicate)
                .where(datePredicate)
//                .where(booleanExpression)
                .orderBy(qGrade.createTime.desc())
                .fetch();
    }

    @Override
    public List<Grade> findGradeByAcademyClassIdAndSubjectIdAndCreateTime(Long academyClassId,
                                                                          Long subjectId,
                                                                          Long studentId,
                                                                          Date createTime,
                                                                          Date closedPeriod) {
        Predicate academyClassIdPredicate = academyClassId == null ? qGrade.academyClass.id.isNotNull() : qGrade.academyClass.id.eq(academyClassId);
        Predicate subjectIdPredicate = subjectId != null ? qGrade.subject.id.eq(subjectId) : qGrade.subject.id.isNull();
        Predicate studentIdPredicate = studentId != null ? qGrade.student.id.eq(studentId) : qGrade.student.id.isNotNull();
        Predicate datePredicate;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(createTime);
        int month = calendar.get(Calendar.MONTH) == Calendar.FEBRUARY ? 0 : calendar.get(Calendar.MONTH) == Calendar.OCTOBER ? 8 : calendar.get(Calendar.MONTH);
        datePredicate = qGrade.exactMonth.month().eq(month + 1).and(qGrade.exactMonth.year().eq(calendar.get(Calendar.YEAR)));
        //        StringExpression prefixPredicate = qGrade.gradeType.stringValue();
//        BooleanExpression booleanExpression = prefixPredicate.startsWith("GENERAL_");

        return qf.selectFrom(qGrade)
                .where(academyClassIdPredicate)
                .where(subjectIdPredicate)
                .where(studentIdPredicate)
                .where(datePredicate)
                .where(QueryUtils.dateTimeLess(qGrade.exactMonth, closedPeriod))
//                .where(booleanExpression)
                .orderBy(qGrade.createTime.desc())
                .fetch();
    }

    @Override
    public Map<Student, Map<Subject, Map<Integer, BigDecimal>>> findGradeBySemester(Long classId, int year, boolean firstSemester) {
        Predicate academyClassIdPredicate = classId == null ? qGrade.academyClass.id.isNotNull() : qGrade.academyClass.id.eq(classId);
        Predicate dateYearPredicate = qGrade.exactMonth.year().eq(year);
        Predicate dateMonthPredicate = firstSemester ? qGrade.exactMonth.month().in(9, 11, 12) : qGrade.exactMonth.month().in(1, 3, 4, 5, 6);
        Predicate gradeTypePredicate = qGrade.gradeType.eq(GradeType.GENERAL_COMPLETE_MONTHLY).or(qGrade.gradeType.eq(GradeType.GENERAL_SCHOOL_WORK_MONTH));
        List<Grade> gradeList = qf.selectFrom(qGrade)
                .where(dateYearPredicate)
                .where(dateMonthPredicate)
                                    .where(academyClassIdPredicate)
                                    .where(gradeTypePredicate)
                                    .orderBy(qGrade.createTime.desc())
                                    .fetch();

        Map<Student, List<Grade>> gradeMap = gradeList.stream().collect(Collectors.groupingBy(Grade::getStudent));
        Map<Student, Map<Subject, Map<Integer, BigDecimal>>> result = new HashMap<>();
        for (Student student : gradeMap.keySet()) {
            Map<Subject, Map<Integer, BigDecimal>> bySubject = new HashMap<>();
            List<Grade> old = gradeMap.get(student);
            Map<Subject, List<Grade>> newGradeList = old.stream().collect(Collectors.groupingBy(Grade::getSubject));

            for (Subject subject : newGradeList.keySet()) {
                Map<Integer, BigDecimal> gradeByMonth = new HashMap<>();
                List<Grade> curr = newGradeList.get(subject);
                Long sum = 0L;
                int count = 0;
                Long sumOfSchoolWork = 0L;
                int countOfSchoolWork = 0;
                for (Grade grade : curr) {
                    if (grade.getGradeType() != GradeType.GENERAL_SCHOOL_WORK_MONTH) {
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime(grade.getExactMonth());
                        Integer month = calendar.get(Calendar.MONTH);
                        gradeByMonth.put(month + 1, grade.getValue());
                        sum += grade.getValue().longValue();
                        count++;
                    } else {
                        sumOfSchoolWork += grade.getValue().longValue();
                        countOfSchoolWork++;
                    }
                }
                BigDecimal average = BigDecimal.ZERO.equals(BigDecimal.valueOf(sum)) ? BigDecimal.ZERO : BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(count), RoundingMode.HALF_UP);
                BigDecimal averageOfSchoolWork = countOfSchoolWork == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(sumOfSchoolWork).divide(BigDecimal.valueOf(countOfSchoolWork), RoundingMode.HALF_UP);
                gradeByMonth.put(-1, average);
                gradeByMonth.put(-2, averageOfSchoolWork);
                //TODO OOOO!!!!
                List<Grade> diagnostics = qf.selectFrom(qGrade)
                        .where(dateYearPredicate)
                        .where(dateMonthPredicate)
                        .where(academyClassIdPredicate)
                        .where(qGrade.subject.eq(subject))
                        .where(qGrade.student.id.eq(student.getId()))
                        .where(qGrade.gradeType.eq(GradeType.DIAGNOSTICS_1).or(qGrade.gradeType.eq(GradeType.DIAGNOSTICS_2)))
                        .orderBy(qGrade.createTime.desc())
                        .fetch();
                List<Grade> first = diagnostics.stream().filter(v -> v.getGradeType().equals(GradeType.DIAGNOSTICS_1)).collect(Collectors.toList());
                List<Grade> second = diagnostics.stream().filter(v -> v.getGradeType().equals(GradeType.DIAGNOSTICS_2)).collect(Collectors.toList());
                gradeByMonth.put(-3, first.isEmpty() ? BigDecimal.ZERO : first.get(0).getValue());
                gradeByMonth.put(-4, second.isEmpty() ? BigDecimal.ZERO : second.get(0).getValue());
                bySubject.put(subject, gradeByMonth);
            }
            result.put(student, bySubject);
        }

        return result;
    }

    @Override
    public Map<Student, Map<Subject, Map<Integer, BigDecimal>>> findGradeBySemester(Long classId, int year, boolean firstSemester, Date closedPeriod) {
        Predicate academyClassIdPredicate = classId == null ? qGrade.academyClass.id.isNotNull() : qGrade.academyClass.id.eq(classId);
        Predicate dateYearPredicate = qGrade.exactMonth.year().eq(year);
        Predicate dateMonthPredicate = firstSemester ? qGrade.exactMonth.month().in(9, 11, 12) : qGrade.exactMonth.month().in(1, 3, 4, 5, 6);
        Predicate gradeTypePredicate = qGrade.gradeType.eq(GradeType.GENERAL_COMPLETE_MONTHLY).or(qGrade.gradeType.eq(GradeType.GENERAL_SCHOOL_WORK_MONTH));
        List<Grade> gradeList = qf.selectFrom(qGrade)
                .where(dateYearPredicate)
                .where(dateMonthPredicate)
                .where(academyClassIdPredicate)
                .where(gradeTypePredicate)
                .where(QueryUtils.dateTimeLess(qGrade.exactMonth, closedPeriod))
                .orderBy(qGrade.createTime.desc())
                .fetch();

        Map<Student, List<Grade>> gradeMap = gradeList.stream().collect(Collectors.groupingBy(Grade::getStudent));
        Map<Student, Map<Subject, Map<Integer, BigDecimal>>> result = new HashMap<>();
        for (Student student : gradeMap.keySet()) {
            Map<Subject, Map<Integer, BigDecimal>> bySubject = new HashMap<>();
            List<Grade> old = gradeMap.get(student);
            Map<Subject, List<Grade>> newGradeList = old.stream().collect(Collectors.groupingBy(Grade::getSubject));

            for (Subject subject : newGradeList.keySet()) {
                Map<Integer, BigDecimal> gradeByMonth = new HashMap<>();
                List<Grade> curr = newGradeList.get(subject);
                Long sum = 0L;
                int count = 0;
                Long sumOfSchoolWork = 0L;
                int countOfSchoolWork = 0;
                for (Grade grade : curr) {
                    if (grade.getGradeType() != GradeType.GENERAL_SCHOOL_WORK_MONTH) {
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime(grade.getExactMonth());
                        Integer month = calendar.get(Calendar.MONTH);
                        gradeByMonth.put(month + 1, grade.getValue());
                        sum += grade.getValue().longValue();
                        count++;
                    } else {
                        sumOfSchoolWork += grade.getValue().longValue();
                        countOfSchoolWork++;
                    }
                }
                BigDecimal average = BigDecimal.ZERO.equals(BigDecimal.valueOf(sum)) ? BigDecimal.ZERO : BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(count), RoundingMode.HALF_UP);
                BigDecimal averageOfSchoolWork = countOfSchoolWork == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(sumOfSchoolWork).divide(BigDecimal.valueOf(countOfSchoolWork), RoundingMode.HALF_UP);
                gradeByMonth.put(-1, average);
                gradeByMonth.put(-2, averageOfSchoolWork);
                //TODO OOOO!!!!
                List<Grade> diagnostics = qf.selectFrom(qGrade)
                        .where(dateYearPredicate)
                        .where(dateMonthPredicate)
                        .where(academyClassIdPredicate)
                        .where(qGrade.subject.eq(subject))
                        .where(qGrade.student.id.eq(student.getId()))
                        .where(qGrade.gradeType.eq(GradeType.DIAGNOSTICS_1).or(qGrade.gradeType.eq(GradeType.DIAGNOSTICS_2)))
                        .orderBy(qGrade.createTime.desc())
                        .fetch();
                List<Grade> first = diagnostics.stream().filter(v -> v.getGradeType().equals(GradeType.DIAGNOSTICS_1)).collect(Collectors.toList());
                List<Grade> second = diagnostics.stream().filter(v -> v.getGradeType().equals(GradeType.DIAGNOSTICS_2)).collect(Collectors.toList());
                gradeByMonth.put(-3, first.isEmpty() ? BigDecimal.ZERO : first.get(0).getValue());
                gradeByMonth.put(-4, second.isEmpty() ? BigDecimal.ZERO : second.get(0).getValue());
                bySubject.put(subject, gradeByMonth);
            }
            result.put(student, bySubject);
        }

        return result;
    }

    @Override
    public Map<Student, List<Grade>> findGradeByMonth(Long classId, Date createDate) throws SGSException {
        AcademyClass academyClass = academyClassRepository.findById(classId).orElseThrow(() -> new SGSException(SGSExceptionCode.BAD_REQUEST, ExceptionKeys.ACADEMY_CLASS_NOT_FOUND));
        Predicate academyClassIdPredicate = qGrade.academyClass.id.eq(classId);
        Predicate datePredicate;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(createDate);
        int month = calendar.get(Calendar.MONTH) == Calendar.FEBRUARY ? 0 : calendar.get(Calendar.MONTH) == Calendar.OCTOBER ? 8 : calendar.get(Calendar.MONTH);
        datePredicate = qGrade.exactMonth.month().eq(month + 1).and(qGrade.exactMonth.year().eq(calendar.get(Calendar.YEAR)));

        Predicate gradeTypePredicate = qGrade.gradeType.eq(academyClass.getIsTransit() ? GradeType.TRANSIT_SCHOOL_COMPLETE_MONTHLY : GradeType.GENERAL_COMPLETE_MONTHLY);
        List<Grade> gradeList =  qf.selectFrom(qGrade)
                .where(datePredicate)
                .where(academyClassIdPredicate)
                .where(gradeTypePredicate)
                .orderBy(qGrade.student.lastName.desc())
                .fetch();
        return gradeList.stream().collect(Collectors.groupingBy(Grade::getStudent));
    }

    @Override
    public Integer getMinYear() {
        DateTimePath<Date> createDatePath = qGrade.exactMonth;

        return qf.select(dateTemplate(Integer.class, "YEAR({0})", createDatePath).min())
                .from(qGrade)
                .fetchOne();
    }

    @Override
    public Integer getMaxYear() {
        DateTimePath<Date> createDatePath = qGrade.exactMonth;

        return qf.select(dateTemplate(Integer.class, "YEAR({0})", createDatePath).max())
                .from(qGrade)
                .fetchOne();
    }

    @Override
    public List<Grade> findGradeByAcademyClassIdAndSubjectIdAndGradeTypeAndYear(Long academyClassId, Long subjectId, Long studentId, GradeType gradeType, int maxYear) {
        return qf.select(qGrade)
                .from(qGrade)
                .where(qGrade.academyClass.id.eq(academyClassId)
                        .and(qGrade.student.id.eq(studentId))
                        .and(qGrade.gradeType.eq(gradeType))
                        .and(qGrade.subject.id.eq(subjectId)
                                .and(qGrade.exactMonth.year().eq(maxYear))))
                .orderBy(qGrade.exactMonth.desc())
                .fetch();
    }

    @Override
    public List<Grade> findGradeByAcademyClassIdAndSubjectIdAndGradeTypeAndYear(Long academyClassId, Long subjectId, Long studentId, GradeType gradeType, int maxYear, Date latest) {
        return qf.select(qGrade)
                .from(qGrade)
                .where(qGrade.academyClass.id.eq(academyClassId)
                        .and(qGrade.student.id.eq(studentId))
                        .and(qGrade.gradeType.eq(gradeType))
                        .and(qGrade.subject.id.eq(subjectId)
                                .and(qGrade.lastUpdateTime.before(latest))
                                .and(qGrade.exactMonth.year().eq(maxYear))))
                .orderBy(qGrade.exactMonth.desc())
                .fetch();
    }

    @Override
    public Grade findGradeByAcademyClassIdAndSubjectIdAndGradeTypeAndExactMonth(Long academyClassId, Long subjectId, Long studentId, GradeType gradeType, Date exactMonth) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(exactMonth);
        int month = calendar.get(Calendar.MONTH) == Calendar.FEBRUARY ? 0 : calendar.get(Calendar.MONTH) == Calendar.OCTOBER ? 8 : calendar.get(Calendar.MONTH);
        Predicate datePredicate = qGrade.exactMonth.month().eq(month + 1).and(qGrade.exactMonth.year().eq(calendar.get(Calendar.YEAR))); //erased + 1
        return qf.select(qGrade)
                .from(qGrade)
                .where(QueryUtils.longEq(qGrade.academyClass.id, academyClassId)
                        .and(QueryUtils.longEq(qGrade.student.id, studentId))
                        .and(qGrade.gradeType.eq(gradeType))
                        .and(QueryUtils.longEq(qGrade.subject.id, subjectId))
                        .and(datePredicate))
                .orderBy(qGrade.exactMonth.desc())
                .fetchFirst();
    }

    @Override
    public List<Grade> findGradeByAcademyClassIdAndSubjectIdAndExactMonthAndYear(Long academyClassId, Long subjectId, Long studentId, int month, int year, Date latest) {
        return qf.select(qGrade)
                .from(qGrade)
                .where(QueryUtils.longEq(qGrade.academyClass.id, academyClassId)
                                .and(QueryUtils.longEq(qGrade.student.id, studentId))
                                .and(QueryUtils.longEq(qGrade.subject.id, subjectId)
                                        .and(qGrade.exactMonth.month().eq(month + 1)))
                                .and(qGrade.exactMonth.year().eq(year))
                        .and(qGrade.lastUpdateTime.before(latest))
                )
                .orderBy(qGrade.exactMonth.desc())
                .fetch();
    }

    @Override
    public List<Grade> findGradeByAcademyClassIdAndSubjectIdAndGradeTypeAndExactMonthAndYear(Long academyClassId, Long subjectId, Long studentId, GradeType gradeType, int month, int year, Date latest) {
        return qf.select(qGrade)
                .from(qGrade)
                .where(QueryUtils.longEq(qGrade.academyClass.id, academyClassId)
                        .and(QueryUtils.longEq(qGrade.student.id, studentId))
                        .and(QueryUtils.longEq(qGrade.subject.id, subjectId)
                                .and(QueryUtils.enumEq(qGrade.gradeType, gradeType))
                                .and(qGrade.exactMonth.month().eq(month + 1)))
                        .and(qGrade.lastUpdateTime.before(latest))
                        .and(qGrade.exactMonth.year().eq(year)))
                .orderBy(qGrade.exactMonth.desc())
                .fetch();
    }

    @Override
    public List<Grade> findGradeByAcademyClassIdAndSubjectIdAndGradeTypeAndExactMonthAndYear(Long academyClassId, Long studentId, GradeType gradeType, Long month, int startYear, int endYear, Date latest) {
        int monthInt = month.intValue();
        return qf.select(qGrade)
                .from(qGrade)
                .where(QueryUtils.longEq(qGrade.academyClass.id, academyClassId)
                        .and(QueryUtils.longEq(qGrade.student.id, studentId))
                        .and(QueryUtils.enumEq(qGrade.gradeType, gradeType)
                                .and(qGrade.exactMonth.month().eq(monthInt + 1)))
                        .and(qGrade.lastUpdateTime.before(latest))
                        .and(qGrade.exactMonth.year().between(startYear, endYear)))
                .orderBy(qGrade.exactMonth.desc())
                .fetch();
    }

    @Override
    public BigDecimal findTotalAbsenceHours(long studentId, Date createDate) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(createDate);
        if (calendar.get(Calendar.MONTH) == Calendar.FEBRUARY) {
            calendar.set(Calendar.MONTH, Calendar.JANUARY);
        } else if (calendar.get(Calendar.MONTH) == Calendar.OCTOBER) {
            calendar.set(Calendar.MONTH, Calendar.SEPTEMBER);
        }
        return qf.select(qGrade.value.sum())
                .from(qGrade)
                .where(qGrade.gradeType.eq(GradeType.GENERAL_ABSENCE_MONTHLY)
                        .and(qGrade.student.id.eq(studentId))
                        .and(qGrade.exactMonth.month().eq(calendar.get(Calendar.MONTH) + 1)))// todo: დაამატე წელი
                .fetchOne();
    }

    @Override
    public BigDecimal findBehaviourMonth(long id, Date createDate) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(createDate);
        if (calendar.get(Calendar.MONTH) == Calendar.FEBRUARY) {
            calendar.set(Calendar.MONTH, Calendar.JANUARY);
        } else if (calendar.get(Calendar.MONTH) == Calendar.OCTOBER) {
            calendar.set(Calendar.MONTH, Calendar.SEPTEMBER);
        }
        return qf.select(qGrade.value)
                .from(qGrade)
                .where(qGrade.gradeType.eq(GradeType.BEHAVIOUR_MONTHLY)
                        .and(qGrade.student.id.eq(id))
                        .and(qGrade.exactMonth.month().eq(calendar.get(Calendar.MONTH) + 1)))
                .fetchOne();
    }
}