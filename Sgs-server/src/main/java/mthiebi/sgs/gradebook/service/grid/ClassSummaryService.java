package mthiebi.sgs.gradebook.service.grid;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.SGSExceptionCode;
import mthiebi.sgs.gradebook.model.*;
import mthiebi.sgs.gradebook.repository.*;
import mthiebi.sgs.gradebook.service.SpecialValueRegistry;
import mthiebi.sgs.gradebook.service.TemplateVersionResolver;
import mthiebi.sgs.gradebook.service.conversion.GradeConversionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The class summary: every subject's mark for one period, side by side.
 * <p>
 * What the old monthly page was - students down, a column per subject, then a
 * rating, the ethics mark and the hours missed - except per trimester, because
 * subject marks no longer exist per month. The school asked for it back.
 * <p>
 * Nothing here names a column or a journal. Which subject column to show is the
 * journal's own summary column at the period's level (the trimester grade on a
 * trimester, the annual grade on the year); the figures beside it are the
 * subject journal's class-wide columns at that level (the year's rating) and,
 * from every other class-wide journal, its column at that level - the same
 * one-column-per-level rule the absence register's own screen uses. Where the
 * subject journal has no class-wide column at the level, as on a trimester, the
 * rating is worked out here as the plain average of the row.
 */
@Service
public class ClassSummaryService {

    /**
     * Whole numbers throughout, rounded half up - what the old monthly page
     * showed, and what the school asked for here. Rounded once, on the server,
     * so the screen and the spreadsheet cannot disagree, and before the average
     * and the conversion so both work from the numbers on screen.
     */
    private static final int WHOLE = 0;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private ClassGroupRepository classGroupRepository;

    @Autowired
    private PeriodRepository periodRepository;

    @Autowired
    private GradeEntryRepository gradeEntryRepository;

    @Autowired
    private GradeComponentRepository gradeComponentRepository;

    @Autowired
    private GradingTemplateRepository gradingTemplateRepository;

    @Autowired
    private TemplateVersionResolver templateVersionResolver;

    @Autowired
    private SpecialValueRegistry specialValueRegistry;

    @Autowired
    private GradeConversionService gradeConversionService;

    /**
     * @param journalUuid the subject journal to read; null for the first
     *                    subject journal in the menu, which on production is
     *                    the only one
     */
    @Transactional(readOnly = true)
    public ClassSummary build(Long classGroupId, Long periodId, String journalUuid)
            throws SGSException {

        List<Enrollment> enrollments =
                new ArrayList<>(enrollmentRepository.findActiveByClassGroup(classGroupId));
        if (enrollments.isEmpty()) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, "კლასში მოსწავლეები ვერ მოიძებნა");
        }
        ClassGroup classGroup = enrollments.get(0).getClassGroup();
        enrollments.sort(Comparator
                .comparing((Enrollment e) -> nullSafe(e.getStudent().getLastName()))
                .thenComparing(e -> nullSafe(e.getStudent().getFirstName())));

        Period period = periodRepository.findById(periodId)
                .orElseThrow(() -> new SGSException(SGSExceptionCode.BAD_REQUEST,
                        "პერიოდი ვერ მოიძებნა"));
        if (!period.getScheme().getId().equals(classGroup.getPeriodScheme().getId())) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST,
                    "პერიოდი არ ეკუთვნის კლასის სასწავლო სქემას");
        }

        GradingTemplate journal = subjectJournal(journalUuid);
        TemplateVersion version = templateVersionResolver
                .resolve(classGroupId, null, periodId, journal.getId()).getVersion();
        List<GradeComponent> atLevel = atLevel(version, period);

        GradeComponent subjectColumn = subjectColumnOf(atLevel);
        if (subjectColumn == null) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST,
                    "ამ პერიოდისთვის ჟურნალს შემაჯამებელი სვეტი არ აქვს");
        }

        ConversionFormula formula = gradeConversionService.current();

        ClassSummary summary = new ClassSummary();
        summary.setClassGroupId(classGroup.getId());
        summary.setClassName(classGroup.getName());
        summary.setPeriodId(period.getId());
        summary.setPeriodLabel(period.getLabel());
        summary.setJournalUuid(journal.getUuid());
        summary.setJournalName(journal.getName());
        summary.setConvertible(formula != null);
        specialValueRegistry.behavioursFor(version.getId()).forEach((code, behaviour) ->
                summary.getSpecialValues().add(new GridSpecialValue(
                        code, specialValueRegistry.labelOf(code), behaviour)));

        int index = 1;
        for (Enrollment e : enrollments) {
            ClassSummary.Row row = new ClassSummary.Row();
            row.setEnrollmentId(e.getId());
            row.setStudentId(e.getStudent().getId());
            row.setFirstName(e.getStudent().getFirstName());
            row.setLastName(e.getStudent().getLastName());
            row.setIndex(index++);
            summary.getRows().add(row);
        }
        Map<Long, ClassSummary.Row> rows = summary.getRows().stream()
                .collect(Collectors.toMap(ClassSummary.Row::getEnrollmentId, r -> r));

        // ---- one column per subject ----------------------------------------
        List<ClassSubject> subjects = classGroupRepository.findClassSubjectsOf(classGroupId);
        List<String> subjectKeys = new ArrayList<>();
        for (ClassSubject cs : subjects) {
            String key = "S" + cs.getSubject().getId();
            subjectKeys.add(key);
            summary.getColumns().add(new ClassSummary.Column(key, cs.getSubject().getName(),
                    subjectColumn.getLabel(), ClassSummary.ColumnKind.SUBJECT,
                    cs.getSubject().getId(), cs.getTeacherName(), WHOLE, true));
        }
        for (GradeEntry entry : gradeEntryRepository.loadMatrix(classGroupId,
                Collections.singletonList(periodId), subjectColumn.getCode(), journal.getId())) {
            put(rows, entry, "S" + entry.getSubject().getId(), true, formula);
        }

        // ---- the rating ------------------------------------------------------
        List<GradeComponent> journalWide = atLevel.stream()
                .filter(c -> !c.isSubjectScoped())
                .collect(Collectors.toList());
        for (GradeComponent c : journalWide) {
            String key = "W" + c.getCode();
            summary.getColumns().add(new ClassSummary.Column(key, c.getLabel(), null,
                    ClassSummary.ColumnKind.SUBJECT_JOURNAL, null, null, WHOLE, true));
            for (GradeEntry entry : gradeEntryRepository.loadClassWide(
                    classGroupId, periodId, c.getCode(), journal.getId())) {
                put(rows, entry, key, true, formula);
            }
        }
        if (journalWide.isEmpty()) {
            summary.getColumns().add(new ClassSummary.Column("AVERAGE", "რეიტინგი",
                    "საგნების საშუალო", ClassSummary.ColumnKind.AVERAGE, null, null,
                    WHOLE, true));
            for (ClassSummary.Row row : summary.getRows()) {
                BigDecimal average = averageOf(row, subjectKeys);
                if (average != null) {
                    row.getCells().put("AVERAGE", new ClassSummary.Cell(average, null,
                            gradeConversionService.convert(average, formula)));
                }
            }
        }

        // ---- ethics, absence: every other class-wide journal ------------------
        for (GradingTemplate other : gradingTemplateRepository.findActive()) {
            if (other.isSubjectScoped() || other.getId().equals(journal.getId())
                    || !servesSchool(other, classGroup)) {
                continue;
            }
            GradeComponent column = classWideColumnOf(other, classGroupId, period);
            if (column == null) {
                continue;
            }
            String key = "J" + other.getUuid();
            summary.getColumns().add(new ClassSummary.Column(key, other.getName(),
                    column.getLabel(), ClassSummary.ColumnKind.CLASS_JOURNAL, null, null,
                    WHOLE, false));
            for (GradeEntry entry : gradeEntryRepository.loadClassWide(
                    classGroupId, periodId, column.getCode(), other.getId())) {
                put(rows, entry, key, false, formula);
            }
        }

        return summary;
    }

    /**
     * The journal named, or the first subject journal in the menu.
     */
    private GradingTemplate subjectJournal(String journalUuid) throws SGSException {
        if (journalUuid != null && !journalUuid.isEmpty()) {
            GradingTemplate journal = templateVersionResolver.journalByUuid(journalUuid);
            if (!journal.isSubjectScoped()) {
                throw new SGSException(SGSExceptionCode.BAD_REQUEST,
                        "ჟურნალი საგნების მიხედვით არ არის");
            }
            return journal;
        }
        return gradingTemplateRepository.findActive().stream()
                .filter(GradingTemplate::isSubjectScoped)
                .findFirst()
                .orElseThrow(() -> new SGSException(SGSExceptionCode.BAD_REQUEST,
                        "საგნების ჟურნალი ვერ მოიძებნა"));
    }

    /**
     * A column belongs on a period when it names that kind of period - the
     * grid's own rule, so this page never shows a column the grid would not.
     */
    private List<GradeComponent> atLevel(TemplateVersion version, Period period) {
        return gradeComponentRepository.findByTemplateVersion(version.getId()).stream()
                .filter(c -> c.getPeriodKind() == period.getKind())
                .sorted(Comparator.comparingInt(GradeComponent::getOrdinal))
                .collect(Collectors.toList());
    }

    /**
     * The column ticked as the summary; failing that, the last calculated one.
     * <p>
     * The tick first, because the school made the trimester grade a typed
     * column and "the last calculated column" then found nothing. The first
     * ticked one, because the year carries several (annual, final exam,
     * overall) and the annual grade comes first.
     */
    private GradeComponent subjectColumnOf(List<GradeComponent> atLevel) {
        Optional<GradeComponent> ticked = atLevel.stream()
                .filter(GradeComponent::isSubjectScoped)
                .filter(GradeComponent::isSummaryColumn)
                .findFirst();
        if (ticked.isPresent()) {
            return ticked.get();
        }
        GradeComponent lastDerived = null;
        for (GradeComponent c : atLevel) {
            if (c.isSubjectScoped() && c.getKind() == ComponentKind.DERIVED) {
                lastDerived = c;
            }
        }
        return lastDerived;
    }

    /**
     * Another journal's one column at this level, or null when it has none.
     * <p>
     * A journal with no usable version is left off rather than failing the
     * page: a draft nobody has activated says nothing about this class.
     */
    private GradeComponent classWideColumnOf(GradingTemplate journal, Long classGroupId,
                                             Period period) {
        TemplateVersion version;
        try {
            version = templateVersionResolver
                    .resolve(classGroupId, null, period.getId(), journal.getId()).getVersion();
        } catch (SGSException e) {
            return null;
        }
        return atLevel(version, period).stream()
                .filter(c -> !c.isSubjectScoped())
                .findFirst()
                .orElse(null);
    }

    private boolean servesSchool(GradingTemplate journal, ClassGroup classGroup) {
        return journal.getSchool() == null
                || journal.getSchool().getId().equals(classGroup.getSchool().getId());
    }

    private void put(Map<Long, ClassSummary.Row> rows, GradeEntry entry, String key,
                     boolean converts, ConversionFormula formula) {
        ClassSummary.Row row = rows.get(entry.getEnrollment().getId());
        if (row == null || (entry.getValue() == null && entry.getSpecialValue() == null)) {
            return;
        }
        BigDecimal value = entry.getValue() == null ? null
                : entry.getValue().setScale(WHOLE, RoundingMode.HALF_UP);
        // A special value is a code, never a number: ჩთ through "+3" is how it
        // would turn into a mark.
        BigDecimal converted = converts && entry.getSpecialValue() == null
                ? gradeConversionService.convert(value, formula) : null;
        row.getCells().put(key, new ClassSummary.Cell(value, entry.getSpecialValue(), converted));
    }

    /**
     * The plain average of the row's subject marks, as shown: blanks and ჩთ
     * left out, not counted as nought, and rounded half up to a whole number -
     * the old page's rating exactly.
     */
    private BigDecimal averageOf(ClassSummary.Row row, List<String> subjectKeys) {
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        for (String key : subjectKeys) {
            ClassSummary.Cell cell = row.getCells().get(key);
            if (cell != null && cell.getSpecialValue() == null && cell.getValue() != null) {
                sum = sum.add(cell.getValue());
                count++;
            }
        }
        return count == 0 ? null
                : sum.divide(BigDecimal.valueOf(count), WHOLE, RoundingMode.HALF_UP);
    }

    private String nullSafe(String s) {
        return s == null ? "" : s;
    }
}
