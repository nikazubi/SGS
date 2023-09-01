package mthiebi.sgs.repository;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.DatePath;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.jpa.impl.JPAQueryFactory;
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
    public Map<Student, Map<Subject, Map<Integer, BigDecimal>>> findGradeBySemester(Long classId, int year, boolean firstSemester) {
        Predicate academyClassIdPredicate = classId == null ? qGrade.academyClass.id.isNotNull() : qGrade.academyClass.id.eq(classId);
        Predicate dateYearPredicate = qGrade.exactMonth.year().eq(year);
        Predicate dateMonthPredicate = firstSemester ? qGrade.exactMonth.month().in(9, 11, 12) : qGrade.exactMonth.month().in(1, 3, 4, 5, 6);
        Predicate gradeTypePredicate = qGrade.gradeType.eq(GradeType.GENERAL_COMPLETE_MONTHLY).or(qGrade.gradeType.eq(GradeType.GENERAL_SCHOOL_WORK_MONTH));
        List<Grade> gradeList =  qf.selectFrom(qGrade)
                                    .where(dateYearPredicate)
                                    .where(dateMonthPredicate)
                                    .where(academyClassIdPredicate)
                                    .where(gradeTypePredicate)
                                    .orderBy(qGrade.createTime.desc())
                                    .fetch();

        Map<Student, List<Grade>> gradeMap = gradeList.stream().collect(Collectors.groupingBy(Grade::getStudent));
        Map<Student, Map<Subject, Map<Integer, BigDecimal>>> result = new HashMap<>();
        Map<Subject, Map<Integer, BigDecimal>> bySubject = new HashMap<>();
        for (Student student : gradeMap.keySet()) {
            List<Grade> old = gradeMap.get(student);
            Map<Subject, List<Grade>> newGradeList = old.stream().collect(Collectors.groupingBy(Grade::getSubject));

            for (Subject subject : newGradeList.keySet()) {
                Map<Integer, BigDecimal> gradeByMonth = new HashMap<>();
                List<Grade> curr = newGradeList.get(subject);
                Long sum = 0L;
                int count = 0;
                Long sumOfSchoolWork = 0L;
                int countOfSchoolWork = 0;
                for (Grade grade: curr) {
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
                BigDecimal average = BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(count), RoundingMode.HALF_UP);
                BigDecimal averageOfSchoolWork = countOfSchoolWork == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(sumOfSchoolWork).divide(BigDecimal.valueOf(countOfSchoolWork), RoundingMode.HALF_UP);
                gradeByMonth.put(-1, average);
                gradeByMonth.put(-2, averageOfSchoolWork);
                bySubject.put(subject, gradeByMonth);
            }
            result.put(student, bySubject);
        }

        return result;
    }

    @Override
    public Map<Student, List<Grade>> findGradeByMonth(Long classId, Date createDate) {
        Predicate academyClassIdPredicate = classId == null ? qGrade.academyClass.id.isNotNull() : qGrade.academyClass.id.eq(classId);
        Predicate datePredicate;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(createDate);
        datePredicate = qGrade.createTime.month().eq(calendar.get(Calendar.MONTH) + 1).and(qGrade.createTime.year().eq(calendar.get(Calendar.YEAR)));

        Predicate gradeTypePredicate = qGrade.gradeType.eq(GradeType.GENERAL_COMPLETE_MONTHLY);
        List<Grade> gradeList =  qf.selectFrom(qGrade)
                .where(datePredicate)
                .where(academyClassIdPredicate)
                .where(gradeTypePredicate)
                .orderBy(qGrade.createTime.desc())
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
}