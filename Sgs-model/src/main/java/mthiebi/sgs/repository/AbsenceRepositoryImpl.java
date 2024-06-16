package mthiebi.sgs.repository;

import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import mthiebi.sgs.models.AbsenceGrade;
import mthiebi.sgs.models.AbsenceGradeType;
import mthiebi.sgs.models.QAbsenceGrade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static mthiebi.sgs.repository.QueryUtils.True;

@Repository
public class AbsenceRepositoryImpl implements AbsenceRepositoryCustom {

    private static final QAbsenceGrade qAbsenceGrade = QAbsenceGrade.absenceGrade;

    @Autowired
    private JPAQueryFactory qf;

    @Autowired
    private AcademyClassRepository academyClassRepository;

    @Override
    public List<AbsenceGrade> findAbsenceGrade(Long academyClassId, Long studentId, int year, int endYear) {
        return qf.select(qAbsenceGrade)
                .from(qAbsenceGrade)
                .where(QueryUtils.longEq(qAbsenceGrade.academyClass.id, academyClassId)
                        .and(QueryUtils.longEq(qAbsenceGrade.student.id, studentId))
                        .and(qAbsenceGrade.subject.id.isNull())
                        .and(qAbsenceGrade.exactMonth.year().eq(year).or(qAbsenceGrade.exactMonth.year().eq(endYear)))
                )
                .fetch();
    }

    @Override
    public AbsenceGrade findAbsenceGradeByMonthAndYear(Long academyClassId, Long studentId, int year, int month) {
        return qf.select(qAbsenceGrade)
                .from(qAbsenceGrade)
                .where(QueryUtils.longEq(qAbsenceGrade.academyClass.id, academyClassId)
                        .and(QueryUtils.longEq(qAbsenceGrade.student.id, studentId))
                        .and(qAbsenceGrade.subject.id.isNull())
                        .and(qAbsenceGrade.exactMonth.year().eq(year).and(qAbsenceGrade.exactMonth.month().eq(month)))
                )
                .fetchOne();
    }

    @Override
    public AbsenceGrade findAbsenceGrade(Long academyClassId, Long studentId, int year, AbsenceGradeType gradeType) {
        return qf.select(qAbsenceGrade)
                .from(qAbsenceGrade)
                .where(QueryUtils.longEq(qAbsenceGrade.academyClass.id, academyClassId)
                        .and(QueryUtils.longEq(qAbsenceGrade.student.id, studentId))
                        .and(qAbsenceGrade.subject.id.isNull())
                        .and(qAbsenceGrade.exactMonth.year().eq(year))
                        .and(qAbsenceGrade.gradeType.eq(gradeType))
                )
                .fetchFirst();
    }

    @Override
    public List<AbsenceGrade> findAbsenceGrade(Long studentId, int startYear, int endYear, Long month) {
        BooleanExpression monthB = month == null ? True() : qAbsenceGrade.exactMonth.month().eq(month.intValue());
        return qf.select(qAbsenceGrade)
                .from(qAbsenceGrade)
                .where(QueryUtils.longEq(qAbsenceGrade.student.id, studentId)
                        .and(qAbsenceGrade.subject.id.isNull())
                        .and(qAbsenceGrade.exactMonth.year().eq(startYear).or(qAbsenceGrade.exactMonth.year().eq(endYear)))
                        .and(monthB)
                )
                .fetch();
    }

    @Override
    public AbsenceGrade findAbsenceGrade(Long studentId, int month) {
        return qf.select(qAbsenceGrade)
                .from(qAbsenceGrade)
                .where(QueryUtils.longEq(qAbsenceGrade.student.id, studentId)
                        .and(qAbsenceGrade.exactMonth.month().eq(month)))
                .fetchOne();
    }

    @Override
    public BigDecimal findAbsenceGradeBySemester(Long studentId, Long classId, boolean isFirstSemester) {
        AbsenceGradeType[] gradeTypes = isFirstSemester ? new AbsenceGradeType[]{
                AbsenceGradeType.SEPTEMBER_OCTOBER,
                AbsenceGradeType.NOVEMBER,
                AbsenceGradeType.DECEMBER
        } : new AbsenceGradeType[]{
                AbsenceGradeType.JANUARY_FEBRUARY,
                AbsenceGradeType.MARCH,
                AbsenceGradeType.APRIL,
                AbsenceGradeType.MAY
        };

        NumberPath<BigDecimal> valuePath = qAbsenceGrade.value;

        return Optional.ofNullable(qf.select(valuePath.sum())
                .from(qAbsenceGrade)
                .where(QueryUtils.longEq(qAbsenceGrade.academyClass.id, classId)
                        .and(QueryUtils.longEq(qAbsenceGrade.student.id, studentId))
                        .and(qAbsenceGrade.subject.id.isNull())
                        .and(qAbsenceGrade.gradeType.in(gradeTypes))
                ).fetchFirst()).orElse(BigDecimal.ZERO);
    }
}
