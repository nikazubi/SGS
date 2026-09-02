package mthiebi.sgs.gradebook.service.roster;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.SGSExceptionCode;
import mthiebi.sgs.gradebook.model.AcademicYear;
import mthiebi.sgs.gradebook.model.ClassGroup;
import mthiebi.sgs.gradebook.model.Period;
import mthiebi.sgs.gradebook.model.PeriodScheme;
import mthiebi.sgs.gradebook.model.School;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Academic years, and starting the next one.
 *
 * <h3>Rollover does very little, on purpose</h3>
 * <p>
 * It creates the year, copies the period tree, and optionally copies the class
 * list with the level incremented. <b>It enrols nobody.</b>
 * <p>
 * The school sits down every August and decides who goes where - classes merge,
 * split and get renamed, and the leaving year leaves - so a rollover that
 * placed children automatically would produce a list somebody then has to
 * unpick child by child. The legacy system automated none of this and the
 * school is used to that. Placing children is the students screen's job, and
 * automating it later would only mean writing the enrollments that screen
 * already writes.
 *
 * <h3>Why the period tree is copied rather than generated</h3>
 * <p>
 * Because the trimester boundaries are the school's, not arithmetic: 1 Sep to
 * 5 Dec, 8 Dec to 13 Mar, 16 Mar to 30 Jun. Nothing in a start and end date
 * implies them. Copying last year's shape and shifting it by a year keeps
 * whatever the school actually uses, and is wrong only in the way a calendar is
 * - by a few days, visibly, where it can be corrected.
 */
@Service
public class AcademicYearService {

    @PersistenceContext
    private EntityManager em;

    @Transactional(readOnly = true)
    public List<RosterView.YearRow> list() {
        return em.createQuery("select y from AcademicYear y order by y.startsOn desc",
                        AcademicYear.class)
                .getResultList().stream()
                .map(y -> new RosterView.YearRow(y.getId(), y.getCode(), y.getStartsOn(),
                        y.getEndsOn(), y.isCurrent(), classCount(y.getId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RosterView.SchoolRow> schools() {
        return em.createQuery("select s from School s order by s.ordinal", School.class)
                .getResultList().stream()
                .map(s -> new RosterView.SchoolRow(s.getId(), s.getCode(), s.getName(),
                        s.getOrdinal()))
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public AcademicYear startYear(RosterDraft.NewYear draft) throws SGSException {
        String code = SubjectService.trimmed(draft.getCode());
        if (code.isEmpty()) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, "წლის კოდი სავალდებულოა");
        }
        if (draft.getStartsOn() == null || draft.getEndsOn() == null) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, "წლის თარიღები სავალდებულოა");
        }
        if (!draft.getEndsOn().isAfter(draft.getStartsOn())) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST,
                    "წლის დასასრული დასაწყისზე გვიან უნდა იყოს");
        }
        if (!em.createQuery("select y.id from AcademicYear y where y.code = :c", Long.class)
                .setParameter("c", code).getResultList().isEmpty()) {
            throw new SGSException(SGSExceptionCode.CONFLICT, "ასეთი კოდის წელი უკვე არსებობს");
        }

        AcademicYear source = draft.getCopyClassesFromYearId() == null
                ? currentYear() : find(draft.getCopyClassesFromYearId());
        if (source == null) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST,
                    "საწყისი წელი ვერ მოიძებნა - პერიოდების სქემა კოპირდება არსებულიდან");
        }

        AcademicYear year = new AcademicYear();
        year.setCode(code);
        year.setStartsOn(draft.getStartsOn());
        year.setEndsOn(draft.getEndsOn());
        year.setCurrent(false);
        em.persist(year);
        em.flush();

        PeriodScheme scheme = copyScheme(source, year);

        if (draft.getCopyClassesFromYearId() != null) {
            copyClasses(source, year, scheme);
        }
        if (draft.isMakeCurrent()) {
            makeCurrent(year.getId());
        }
        return year;
    }

    /**
     * Exactly one year is current, so this clears the others in the same
     * statement rather than leaving a window where two are - the gradebook,
     * every class list and the parent portal all resolve "now" through it.
     */
    @Transactional(rollbackFor = Exception.class)
    public void makeCurrent(Long yearId) throws SGSException {
        AcademicYear year = find(yearId);
        if (year == null) {
            throw new SGSException(SGSExceptionCode.NOT_FOUND, "წელი ვერ მოიძებნა");
        }
        em.createQuery("update AcademicYear y set y.current = false where y.id <> :id")
                .setParameter("id", yearId).executeUpdate();
        year.setCurrent(true);
        em.flush();
    }

    // ---- copying -------------------------------------------------------------

    private PeriodScheme copyScheme(AcademicYear source, AcademicYear target)
            throws SGSException {

        List<PeriodScheme> schemes = em.createQuery(
                        "select s from PeriodScheme s where s.academicYear.id = :y", PeriodScheme.class)
                .setParameter("y", source.getId()).getResultList();
        if (schemes.isEmpty()) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST,
                    "საწყის წელს პერიოდების სქემა არ აქვს");
        }
        PeriodScheme sourceScheme = schemes.get(0);

        PeriodScheme scheme = new PeriodScheme();
        scheme.setName(sourceScheme.getName());
        scheme.setAcademicYear(target);
        em.persist(scheme);
        em.flush();

        int shift = target.getStartsOn().getYear() - source.getStartsOn().getYear();

        // Ordered by depth so a parent is always copied before its children and
        // the id mapping is complete by the time it is needed.
        List<Period> sourcePeriods = em.createQuery(
                "select p from Period p where p.scheme.id = :s order by p.depth, p.ordinal",
                Period.class).setParameter("s", sourceScheme.getId()).getResultList();

        Map<Long, Period> copied = new HashMap<>();
        for (Period source_ : sourcePeriods) {
            Period period = new Period();
            period.setScheme(scheme);
            period.setCode(source_.getCode());
            period.setLabel(source_.getLabel());
            period.setOrdinal(source_.getOrdinal());
            period.setDepth(source_.getDepth());
            period.setKind(source_.getKind());
            period.setStartsOn(shifted(source_.getStartsOn(), shift));
            period.setEndsOn(shifted(source_.getEndsOn(), shift));
            if (source_.getParent() != null) {
                period.setParent(copied.get(source_.getParent().getId()));
            }
            em.persist(period);
            copied.put(source_.getId(), period);
        }
        em.flush();
        return scheme;
    }

    /**
     * Whole years, so 1 September stays 1 September. A leap day clamps to the
     * 28th, which is what LocalDate does and the only sane answer.
     */
    private LocalDate shifted(LocalDate date, int years) {
        return date == null ? null : date.plusYears(years);
    }

    /**
     * Same names, one level up. 5ა becomes 6ა.
     * <p>
     * The name is left alone rather than renamed alongside the level, because
     * the letter is the class's identity and the digit in front of it is the
     * school's convention, not something to parse and rewrite. Renaming is a
     * two-second edit on a screen; guessing wrong is a class nobody recognises.
     */
    private void copyClasses(AcademicYear source, AcademicYear target, PeriodScheme scheme) {
        List<ClassGroup> classes = em.createQuery(
                "select c from ClassGroup c join fetch c.school where c.academicYear.id = :y",
                ClassGroup.class).setParameter("y", source.getId()).getResultList();

        for (ClassGroup source_ : classes) {
            ClassGroup copy = new ClassGroup();
            copy.setSchool(source_.getSchool());
            copy.setAcademicYear(target);
            copy.setPeriodScheme(scheme);
            copy.setLevel((short) (source_.getLevel() + 1));
            copy.setName(source_.getName());
            em.persist(copy);
        }
        em.flush();
    }

    // ---- helpers -------------------------------------------------------------

    private long classCount(Long yearId) {
        return em.createQuery("select count(c) from ClassGroup c where c.academicYear.id = :y",
                Long.class).setParameter("y", yearId).getSingleResult();
    }

    private AcademicYear currentYear() {
        List<AcademicYear> years = em.createQuery(
                        "select y from AcademicYear y where y.current = true", AcademicYear.class)
                .getResultList();
        return years.isEmpty() ? null : years.get(0);
    }

    private AcademicYear find(Long id) {
        return id == null ? null : em.find(AcademicYear.class, id);
    }
}
