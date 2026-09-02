package mthiebi.sgs.gradebook.service.export;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.SGSExceptionCode;
import mthiebi.sgs.gradebook.model.ClassGroup;
import mthiebi.sgs.gradebook.model.ClassSubject;
import mthiebi.sgs.gradebook.model.Enrollment;
import mthiebi.sgs.gradebook.model.GradeComponent;
import mthiebi.sgs.gradebook.model.GradeEntry;
import mthiebi.sgs.gradebook.model.Period;
import mthiebi.sgs.gradebook.model.Student;
import mthiebi.sgs.gradebook.model.TemplateVersion;
import mthiebi.sgs.gradebook.repository.ClassGroupRepository;
import mthiebi.sgs.gradebook.repository.EnrollmentRepository;
import mthiebi.sgs.gradebook.repository.GradeComponentRepository;
import mthiebi.sgs.gradebook.repository.GradeEntryRepository;
import mthiebi.sgs.gradebook.repository.PeriodRepository;
import mthiebi.sgs.gradebook.service.TemplateVersionResolver;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Excel exports, built from the template rather than from hardcoded columns.
 * <p>
 * Two shapes cover all four legacy Excel exports:
 * <p>
 * matrix  - students down, subjects across, one component per cell
 * detail  - students down, the template's components across, one subject
 * <p>
 * They differ only in which component and which periods, which is what makes
 * them configuration rather than five near-identical methods. The legacy
 * versions ran to 768 lines and were roughly 85% the same code.
 * <p>
 * Values are the working ones, not the published snapshot: an export is a staff
 * document produced from the journal on screen, and a spreadsheet that
 * disagrees with the screen is worse than one that is ahead of what parents see.
 */
@Service
public class GradeExportService {

    private static final String SCHOOL = "სკოლა პანსიონ იბ მთიები";
    private static final int STUDENT_COLUMN_WIDTH = 260 * 26;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private ClassGroupRepository classGroupRepository;

    @Autowired
    private GradeEntryRepository gradeEntryRepository;

    @Autowired
    private GradeComponentRepository gradeComponentRepository;

    @Autowired
    private PeriodRepository periodRepository;

    @Autowired
    private TemplateVersionResolver templateVersionResolver;

    @Autowired
    private mthiebi.sgs.gradebook.service.PeriodTreeLoader periodTreeLoader;

    @Autowired
    private mthiebi.sgs.gradebook.service.conversion.GradeConversionService gradeConversionService;

    // ---- matrix ---------------------------------------------------------

    /**
     * Students down, subjects across, one component per cell.
     * <p>
     * With one period this is the monthly export; with a period's children it
     * is the annual one, where each subject spans a column per trimester.
     */
    @Transactional(readOnly = true)
    public Workbook matrix(Long classGroupId, Long periodId, String componentCode,
                           boolean splitByChildPeriod, String journalUuid,
                           boolean converted) throws SGSException {

        // Resolved once. Null when the box was not ticked, which is what makes
        // render() print the stored value - the legacy isDecimalSystem flag,
        // except the mapping is now configuration rather than a literal 3.
        mthiebi.sgs.gradebook.model.ConversionFormula formula =
                converted ? gradeConversionService.current() : null;

        Context ctx = context(classGroupId, periodId, journalUuid);
        List<Period> columns = splitByChildPeriod
                ? childPeriodsOf(ctx.period, ctx.classGroup,
                ctx.version.getTemplate().getFrequency().getDepth())
                : Collections.singletonList(ctx.period);
        if (columns.isEmpty()) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST,
                    "პერიოდს არ აქვს ქვეპერიოდები");
        }

        GradeComponent component = componentOf(ctx.version, componentCode);

        List<ClassSubject> subjects = classGroupRepository.findClassSubjectsOf(classGroupId);
        if (subjects.isEmpty()) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, "კლასს არ აქვს საგნები");
        }

        List<Long> periodIds = columns.stream().map(Period::getId).collect(Collectors.toList());
        Map<String, GradeEntry> values = new HashMap<>();
        for (GradeEntry entry : gradeEntryRepository.loadMatrix(
                classGroupId, periodIds, componentCode,
                ctx.version.getTemplate().getId())) {
            values.put(key(entry.getEnrollment().getId(), entry.getSubject().getId(),
                    entry.getPeriod().getId()), entry);
        }

        Workbook workbook = new XSSFWorkbook();
        SheetStyles styles = new SheetStyles(workbook);
        Sheet sheet = workbook.createSheet(safeSheetName(ctx.classGroup.getName()));

        int width = 1 + subjects.size() * columns.size();
        titleRow(sheet, styles, width,
                SCHOOL + " - კლასი " + ctx.classGroup.getName()
                        + " - " + ctx.period.getLabel() + " - " + component.getLabel());

        int headerRows = columns.size() > 1 ? 2 : 1;
        Row subjectRow = sheet.createRow(1);
        cell(subjectRow, 0, "მოსწავლის გვარი, სახელი", styles.header);
        if (headerRows == 2) {
            sheet.addMergedRegion(new CellRangeAddress(1, 2, 0, 0));
        }

        int col = 1;
        for (ClassSubject subject : subjects) {
            cell(subjectRow, col, subject.getSubject().getName(), styles.header);
            if (columns.size() > 1) {
                sheet.addMergedRegion(
                        new CellRangeAddress(1, 1, col, col + columns.size() - 1));
                for (int i = 1; i < columns.size(); i++) {
                    cell(subjectRow, col + i, "", styles.header);
                }
            }
            col += columns.size();
        }

        if (headerRows == 2) {
            Row periodRow = sheet.createRow(2);
            cell(periodRow, 0, "", styles.header);
            int pc = 1;
            for (int i = 0; i < subjects.size(); i++) {
                for (Period period : columns) {
                    cell(periodRow, pc++, period.getLabel(), styles.header);
                }
            }
        }

        int rowIndex = headerRows + 1;
        int index = 1;
        for (Enrollment enrollment : ctx.enrollments) {
            Row row = sheet.createRow(rowIndex++);
            cell(row, 0, studentLabel(index++, enrollment.getStudent()), styles.studentName);
            int c = 1;
            for (ClassSubject subject : subjects) {
                for (Period period : columns) {
                    GradeEntry entry = values.get(key(enrollment.getId(),
                            subject.getSubject().getId(), period.getId()));
                    cell(row, c++, render(entry, component, formula), styles.cell);
                }
            }
        }

        teacherRow(sheet, styles, rowIndex, subjects, columns.size());
        sizeColumns(sheet, width);
        return workbook;
    }

    // ---- detail ---------------------------------------------------------

    /**
     * Students down, the template's components across, for one subject.
     * <p>
     * The grade entry screen as a spreadsheet, which is what the semester,
     * dashboard and Word exports each were in their own way.
     */
    @Transactional(readOnly = true)
    public Workbook detail(Long classGroupId, Long subjectId, Long periodId,
                           String journalUuid, boolean converted) throws SGSException {

        mthiebi.sgs.gradebook.model.ConversionFormula formula =
                converted ? gradeConversionService.current() : null;

        Context ctx = context(classGroupId, periodId, journalUuid);
        // Matched the way the grid matches - a column belongs on the kind of
        // period it names - or a journal would export a different set of columns
        // than the screen it came from shows.
        List<GradeComponent> components =
                gradeComponentRepository.findByTemplateVersion(ctx.version.getId()).stream()
                        .filter(c -> c.getPeriodKind() == ctx.period.getKind())
                        .filter(GradeComponent::isSubjectScoped)
                        .collect(Collectors.toList());
        if (components.isEmpty()) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST,
                    "ამ პერიოდისთვის შაბლონში სვეტები არ არის განსაზღვრული");
        }

        String subjectName = classGroupRepository.findClassSubjectsOf(classGroupId).stream()
                .filter(cs -> cs.getSubject().getId().equals(subjectId))
                .map(cs -> cs.getSubject().getName())
                .findFirst()
                .orElseThrow(() -> new SGSException(SGSExceptionCode.BAD_REQUEST,
                        "კლასი არ სწავლობს ამ საგანს"));

        Map<String, GradeEntry> values = new HashMap<>();
        for (GradeEntry entry : gradeEntryRepository.loadGrid(
                ctx.enrollmentIds(), Collections.singletonList(periodId), subjectId)) {
            values.put(entry.getEnrollment().getId() + ":" + entry.getComponent().getCode(), entry);
        }

        Workbook workbook = new XSSFWorkbook();
        SheetStyles styles = new SheetStyles(workbook);
        Sheet sheet = workbook.createSheet(safeSheetName(ctx.classGroup.getName()));

        titleRow(sheet, styles, components.size() + 1,
                SCHOOL + " - კლასი " + ctx.classGroup.getName()
                        + " - " + subjectName + " - " + ctx.period.getLabel());

        // The template's own grouping, e.g. the seven ongoing marks under one
        // heading, so the sheet reads like the screen it came from.
        Map<String, List<GradeComponent>> groups = new LinkedHashMap<>();
        boolean grouped = components.stream()
                .anyMatch(c -> c.getGroupLabel() != null && !c.getGroupLabel().isEmpty());
        for (GradeComponent c : components) {
            groups.computeIfAbsent(c.getGroupLabel() == null ? "" : c.getGroupLabel(),
                    k -> new ArrayList<>()).add(c);
        }

        int headerTop = 1;
        if (grouped) {
            Row groupRow = sheet.createRow(headerTop);
            cell(groupRow, 0, "მოსწავლის გვარი, სახელი", styles.header);
            sheet.addMergedRegion(new CellRangeAddress(1, 2, 0, 0));
            int c = 1;
            for (Map.Entry<String, List<GradeComponent>> group : groups.entrySet()) {
                int span = group.getValue().size();
                cell(groupRow, c, group.getKey(), styles.header);
                if (span > 1) {
                    sheet.addMergedRegion(new CellRangeAddress(1, 1, c, c + span - 1));
                    for (int i = 1; i < span; i++) {
                        cell(groupRow, c + i, "", styles.header);
                    }
                }
                c += span;
            }
        }

        int labelRowIndex = grouped ? 2 : 1;
        Row labelRow = sheet.createRow(labelRowIndex);
        if (!grouped) {
            cell(labelRow, 0, "მოსწავლის გვარი, სახელი", styles.header);
        } else {
            cell(labelRow, 0, "", styles.header);
        }
        int c = 1;
        List<GradeComponent> ordered = groups.values().stream()
                .flatMap(List::stream).collect(Collectors.toList());
        for (GradeComponent component : ordered) {
            cell(labelRow, c++, component.getLabel(), styles.header);
        }

        int rowIndex = labelRowIndex + 1;
        int index = 1;
        for (Enrollment enrollment : ctx.enrollments) {
            Row row = sheet.createRow(rowIndex++);
            cell(row, 0, studentLabel(index++, enrollment.getStudent()), styles.studentName);
            int cc = 1;
            for (GradeComponent component : ordered) {
                GradeEntry entry = values.get(
                        enrollment.getId() + ":" + component.getCode());
                cell(row, cc++, render(entry, component, formula), styles.cell);
            }
        }

        sizeColumns(sheet, ordered.size() + 1);
        return workbook;
    }

    // ---- shared ---------------------------------------------------------

    /**
     * A value as it should be printed.
     * <p>
     * Rounding already happened once, when the engine calculated it, so this
     * only formats. The legacy exports called longValue() here, which truncated
     * 6.7 to 6 - defensible for whole-number grades, wrong as a general rule,
     * and in the wrong place either way.
     * <p>
     * A converted value is printed exactly as the formula produced it, with no
     * rounding at all: the school asked for the formula's output verbatim, so
     * 6.5 through "+3" prints as 9.5. Only an unconverted value is formatted to
     * the column's declared decimals.
     */
    private String render(GradeEntry entry, GradeComponent component,
                          mthiebi.sgs.gradebook.model.ConversionFormula formula) {
        if (entry == null) {
            return "";
        }
        // Special values are codes. ჩთ means "not attested"; putting it through
        // the formula is how it would silently become a number.
        if (entry.getSpecialValue() != null) {
            return entry.getSpecialValue();
        }
        if (entry.getValue() == null) {
            return "";
        }
        java.math.BigDecimal converted = gradeConversionService.convert(entry.getValue(), formula);
        if (converted != null) {
            return gradeConversionService.text(converted);
        }
        return entry.getValue()
                .setScale(component.getDecimals(), RoundingMode.HALF_UP).toPlainString();
    }

    private void teacherRow(Sheet sheet, SheetStyles styles, int rowIndex,
                            List<ClassSubject> subjects, int span) {
        Row row = sheet.createRow(rowIndex);
        cell(row, 0, "პედაგოგი", styles.teacher);
        int col = 1;
        for (ClassSubject subject : subjects) {
            // Held per class rather than per subject: the same subject is taught
            // to different classes by different people.
            cell(row, col, subject.getTeacherName() == null ? "" : subject.getTeacherName(),
                    styles.teacher);
            if (span > 1) {
                sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, col, col + span - 1));
                for (int i = 1; i < span; i++) {
                    cell(row, col + i, "", styles.teacher);
                }
            }
            col += span;
        }
    }

    private void titleRow(Sheet sheet, SheetStyles styles, int width, String text) {
        Row row = sheet.createRow(0);
        cell(row, 0, text, styles.title);
        if (width > 1) {
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, width - 1));
        }
    }

    /**
     * Fixed widths rather than autoSizeColumn, which measures every row and is
     * the slowest thing in the legacy exports.
     */
    private void sizeColumns(Sheet sheet, int width) {
        sheet.setColumnWidth(0, STUDENT_COLUMN_WIDTH);
        for (int i = 1; i < width; i++) {
            sheet.setColumnWidth(i, 256 * 12);
        }
    }

    private void cell(Row row, int index, String value, org.apache.poi.ss.usermodel.CellStyle style) {
        Cell cell = row.createCell(index);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private String studentLabel(int index, Student student) {
        return index + ". " + student.getLastName() + " " + student.getFirstName();
    }

    private String key(Long enrollmentId, Long subjectId, Long periodId) {
        return enrollmentId + ":" + subjectId + ":" + periodId;
    }

    /**
     * Excel rejects : \ / ? * [ ] in a sheet name and caps it at 31 characters.
     */
    private String safeSheetName(String name) {
        String cleaned = name.replaceAll("[\\\\/:*?\\[\\]]", "-");
        return cleaned.length() > 31 ? cleaned.substring(0, 31) : cleaned;
    }

    /**
     * The periods a "split by child period" export puts across the top.
     * <p>
     * Through {@link mthiebi.sgs.gradebook.engine.PeriodReach}, at the journal's
     * own level - not a parent_id match. This was the one call site the reach
     * consolidation missed, and matching on parent_id is only right when the
     * gap is exactly one level: a monthly journal exported at the year produced
     * a column per *trimester*, which holds no cells, so the sheet came out with
     * empty columns and reported success.
     */
    private List<Period> childPeriodsOf(Period parent, ClassGroup classGroup, int journalDepth) {
        java.util.Set<Long> wanted = new java.util.LinkedHashSet<>(
                mthiebi.sgs.gradebook.engine.PeriodReach
                        .of(periodTreeLoader.treeOf(classGroup.getPeriodScheme().getId()))
                        .atDepth(parent.getId(), journalDepth));
        if (wanted.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return periodRepository.findByScheme(classGroup.getPeriodScheme().getId()).stream()
                .filter(p -> wanted.contains(p.getId()))
                .sorted(java.util.Comparator.comparingInt(Period::getOrdinal))
                .collect(Collectors.toList());
    }

    private GradeComponent componentOf(TemplateVersion version, String code) throws SGSException {
        return gradeComponentRepository.findByTemplateVersion(version.getId()).stream()
                .filter(c -> c.getCode().equals(code))
                .findFirst()
                .orElseThrow(() -> new SGSException(SGSExceptionCode.BAD_REQUEST,
                        "უცნობი სვეტი: " + code));
    }

    private Context context(Long classGroupId, Long periodId, String journalUuid)
            throws SGSException {
        List<Enrollment> enrollments = enrollmentRepository.findActiveByClassGroup(classGroupId);
        if (enrollments.isEmpty()) {
            // The legacy exports did list.get(0) here and threw
            // IndexOutOfBoundsException on an empty class.
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, "კლასში მოსწავლეები ვერ მოიძებნა");
        }
        ClassGroup classGroup = enrollments.get(0).getClassGroup();

        Period period = periodRepository.findById(periodId)
                .orElseThrow(() -> new SGSException(SGSExceptionCode.BAD_REQUEST,
                        "პერიოდი ვერ მოიძებნა"));

        TemplateVersion version = templateVersionResolver
                .resolve(classGroupId, null, periodId,
                        templateVersionResolver.journalByUuid(journalUuid).getId())
                .getVersion();

        enrollments.sort((a, b) -> {
            int byLast = nullSafe(a.getStudent().getLastName())
                    .compareTo(nullSafe(b.getStudent().getLastName()));
            return byLast != 0 ? byLast
                    : nullSafe(a.getStudent().getFirstName())
                    .compareTo(nullSafe(b.getStudent().getFirstName()));
        });

        return new Context(classGroup, period, version, enrollments);
    }

    private String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private static final class Context {
        final ClassGroup classGroup;
        final Period period;
        final TemplateVersion version;
        final List<Enrollment> enrollments;

        Context(ClassGroup classGroup, Period period, TemplateVersion version,
                List<Enrollment> enrollments) {
            this.classGroup = classGroup;
            this.period = period;
            this.version = version;
            this.enrollments = enrollments;
        }

        List<Long> enrollmentIds() {
            return enrollments.stream().map(Enrollment::getId).collect(Collectors.toList());
        }
    }
}
