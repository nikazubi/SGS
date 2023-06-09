package mthiebi.sgs.repository;

import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQueryFactory;
import mthiebi.sgs.models.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

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
        datePredicate = qGrade.createTime.month().eq(calendar.get(Calendar.MONTH) + 1).and(qGrade.createTime.year().eq(calendar.get(Calendar.YEAR)));
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
    public Map<Student, Map<Subject, BigDecimal>> findGradeBySemester(Long classId, int year, boolean firstSemester) {
        Predicate academyClassIdPredicate = classId == null ? qGrade.academyClass.id.isNotNull() : qGrade.academyClass.id.eq(classId);
        Predicate dateYearPredicate = qGrade.createTime.year().eq(year);
        Predicate dateMonthPredicate = firstSemester ? qGrade.createTime.month().in(9, 11, 12) : qGrade.createTime.month().in(1, 3, 4, 5, 6);
        Predicate gradeTypePredicate = qGrade.gradeType.eq(GradeType.GENERAL_COMPLETE_MONTHLY);
        List<Grade> gradeList =  qf.selectFrom(qGrade)
                                    .where(dateYearPredicate)
                                    .where(dateMonthPredicate)
                                    .where(academyClassIdPredicate)
                                    .where(gradeTypePredicate)
                                    .orderBy(qGrade.createTime.desc())
                                    .fetch();
        Map<Student, List<Grade>> gradeMap = gradeList.stream().collect(Collectors.groupingBy(Grade::getStudent));
        Map<Student, Map<Subject, BigDecimal>> result = new HashMap<>();
        for (Student student : gradeMap.keySet()) {
            List<Grade> old = gradeMap.get(student);
            Map<Subject, List<Grade>> newGradeList = old.stream().collect(Collectors.groupingBy(Grade::getSubject));
            Map<Subject, BigDecimal> averageGrades = newGradeList.entrySet()
                    .stream()
                    .collect(Collectors.toMap(
                        Map.Entry::getKey,
                            entry -> {
                                BigDecimal sum = entry.getValue()
                                        .stream()
                                        .map(grade -> new BigDecimal(grade.getValue()))
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                return sum.divide(BigDecimal.valueOf(entry.getValue().size()), RoundingMode.HALF_UP);
                            }
                    ));
            result.put(student, averageGrades);
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
}