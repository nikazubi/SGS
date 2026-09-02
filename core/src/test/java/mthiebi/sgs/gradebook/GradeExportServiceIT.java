package mthiebi.sgs.gradebook;

import mthiebi.sgs.db.QueryFactoryProvider;
import mthiebi.sgs.gradebook.model.ClassSubject;
import mthiebi.sgs.gradebook.service.GradeEntryUpdate;
import mthiebi.sgs.gradebook.service.GradeExplainService;
import mthiebi.sgs.gradebook.service.GradeWriteRequest;
import mthiebi.sgs.gradebook.service.GradeWriteService;
import mthiebi.sgs.gradebook.service.PeriodTreeLoader;
import mthiebi.sgs.gradebook.service.SpecialValueRegistry;
import mthiebi.sgs.gradebook.service.TemplateGraphLoader;
import mthiebi.sgs.gradebook.service.TemplateVersionResolver;
import mthiebi.sgs.gradebook.service.export.GradeExportService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import mthiebi.sgs.gradebook.model.ConversionFormula;
import mthiebi.sgs.gradebook.model.GradeEntry;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The exports, checked by reading the workbook back rather than by trusting it.
 * <p>
 * What matters is that the columns come from the template rather than from the
 * code, that a value prints as it is stored, and that an empty class does not
 * throw - the legacy exports did list.get(0) and blew up.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({GradeExportService.class, GradeWriteService.class, GradeExplainService.class,
        TemplateGraphLoader.class, PeriodTreeLoader.class, TemplateVersionResolver.class,
        SpecialValueRegistry.class, QueryFactoryProvider.class,
        mthiebi.sgs.gradebook.service.conversion.GradeConversionService.class})
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.SQLServer2012Dialect",
        "spring.jpa.hibernate.ddl-auto=none"
})
class GradeExportServiceIT {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private GradeExportService exportService;

    @Autowired
    private GradeWriteService writeService;

    @Autowired
    private TemplateGraphLoader templateGraphLoader;

    private GradebookTestData data;
    private Long enrollment;

    @BeforeEach
    void setUp() throws Exception {
        templateGraphLoader.evictAll();
        data = new GradebookTestData(em).build(UUID.randomUUID().toString().substring(0, 6));
        enrollment = data.enrollments.get(0).getId();

        // The fixture has no teacher on its class_subject rows; the export puts
        // one in the trailing პედაგოგი row, so give it one to find.
        ClassSubject cs = em.createQuery(
                        "select cs from ClassSubject cs where cs.classGroup.id = :c", ClassSubject.class)
                .setParameter("c", data.classGroup.getId()).getSingleResult();
        cs.setTeacherName("ცაავა ნინო");

        GradeWriteRequest request = new GradeWriteRequest();
        request.setJournalUuid(data.template.getUuid());
        request.setClassGroupId(data.classGroup.getId());
        request.setSubjectId(data.subject.getId());
        request.setPeriodId(data.trimester1.getId());
        List<GradeEntryUpdate> entries = new ArrayList<>();
        for (String[] pair : new String[][]{{"ONGOING_1", "6"}, {"ONGOING_2", "6"},
                {"ONGOING_3", "6"}, {"INITIAL_KNOWLEDGE", "5"},
                {"FINAL_TEST", "9"}}) {
            GradeEntryUpdate u = new GradeEntryUpdate();
            u.setEnrollmentId(enrollment);
            u.setComponentCode(pair[0]);
            u.setValue(new BigDecimal(pair[1]));
            entries.add(u);
        }
        request.setEntries(entries);
        writeService.apply(request, 1L);
        em.flush();
    }

    private String cell(Sheet sheet, int rowIndex, int col) {
        Row row = sheet.getRow(rowIndex);
        return row == null || row.getCell(col) == null
                ? null : row.getCell(col).getStringCellValue();
    }

    private int rowContaining(Sheet sheet, int col, String text) {
        for (int i = 0; i <= sheet.getLastRowNum(); i++) {
            if (text.equals(cell(sheet, i, col))) {
                return i;
            }
        }
        return -1;
    }

    @Test
    @DisplayName("the detail sheet takes its columns from the template")
    void detailColumnsComeFromTheTemplate() throws Exception {
        try (Workbook workbook = exportService.detail(
                data.classGroup.getId(), data.subject.getId(), data.trimester1.getId(), data.template.getUuid(), false)) {

            Sheet sheet = workbook.getSheetAt(0);
            assertTrue(cell(sheet, 0, 0).contains("სკოლა პანსიონ იბ მთიები"));

            // The fixture groups the seven ongoing marks, so there is a group
            // row above the labels - as on the screen it came from.
            assertEquals("მიმდინარე შეფასება", cell(sheet, 1, 1));
            assertEquals("მოსწავლის გვარი, სახელი", cell(sheet, 1, 0));

            Row labels = sheet.getRow(2);
            List<String> headers = new ArrayList<>();
            for (int i = 1; i < labels.getLastCellNum(); i++) {
                headers.add(labels.getCell(i).getStringCellValue());
            }
            assertTrue(headers.contains("ტრიმესტრის შეფასება"));
            assertTrue(headers.contains("ფინალური ტესტი"));
            assertEquals(12, headers.size());
        }
    }

    @Test
    @DisplayName("a value prints as it is stored, not truncated")
    void valuesArePrintedAsStored() throws Exception {
        try (Workbook workbook = exportService.detail(
                data.classGroup.getId(), data.subject.getId(), data.trimester1.getId(), data.template.getUuid(), false)) {

            Sheet sheet = workbook.getSheetAt(0);
            Row labels = sheet.getRow(2);
            int trimesterCol = -1;
            for (int i = 1; i < labels.getLastCellNum(); i++) {
                if ("ტრიმესტრის შეფასება".equals(labels.getCell(i).getStringCellValue())) {
                    trimesterCol = i;
                }
            }
            assertTrue(trimesterCol > 0);

            int studentRow = rowContaining(sheet, 0,
                    "1. " + data.enrollments.get(0).getStudent().getLastName()
                            + " " + data.enrollments.get(0).getStudent().getFirstName());
            // The fixture keeps one decimal on this component: 0.5*6 + 0.2*5 +
            // 0.3*9 = 6.7. The legacy export called longValue() and printed 6.
            assertEquals("6.7", cell(sheet, studentRow, trimesterCol));
        }
    }

    @Test
    @DisplayName("the matrix sheet lists subjects across and names the teacher")
    void matrixListsSubjectsAndTeacher() throws Exception {
        try (Workbook workbook = exportService.matrix(
                data.classGroup.getId(), data.trimester1.getId(), "TRIMESTER_GRADE", false, data.template.getUuid(), false)) {

            Sheet sheet = workbook.getSheetAt(0);
            assertEquals("მოსწავლის გვარი, სახელი", cell(sheet, 1, 0));
            assertEquals(data.subject.getName(), cell(sheet, 1, 1));

            int teacherRow = rowContaining(sheet, 0, "პედაგოგი");
            assertTrue(teacherRow > 0, "the teacher row should be present");
            assertEquals("ცაავა ნინო", cell(sheet, teacherRow, 1));
        }
    }

    @Test
    @DisplayName("the annual matrix gives each subject a column per trimester")
    void annualMatrixSplitsByChildPeriod() throws Exception {
        try (Workbook workbook = exportService.matrix(
                data.classGroup.getId(), data.year.getId(), "TRIMESTER_GRADE", true, data.template.getUuid(), false)) {

            Sheet sheet = workbook.getSheetAt(0);
            // The fixture has two trimesters under the year, so the subject
            // header spans two columns and the period names sit beneath it.
            assertEquals(data.subject.getName(), cell(sheet, 1, 1));
            assertEquals("I ტრიმესტრი", cell(sheet, 2, 1));
            assertEquals("II ტრიმესტრი", cell(sheet, 2, 2));
        }
    }

    @Test
    @DisplayName("an unknown column is refused rather than producing an empty sheet")
    void unknownComponentIsRefused() {
        org.junit.jupiter.api.Assertions.assertThrows(mthiebi.sgs.SGSException.class,
                () -> exportService.matrix(data.classGroup.getId(), data.trimester1.getId(),
                        "NO_SUCH_COLUMN", false, data.template.getUuid(), false));
    }

    @Test
    @DisplayName("the sheet is named after the class")
    void sheetIsNamedAfterTheClass() throws Exception {
        try (Workbook workbook = exportService.detail(
                data.classGroup.getId(), data.subject.getId(), data.trimester1.getId(), data.template.getUuid(), false)) {
            assertNotNull(workbook.getSheet(data.classGroup.getName()));
        }
    }

    @Test
    @DisplayName("subjects come out in the class's configured order")
    void subjectsFollowSortIndex() throws Exception {
        // Order is data now, not a hardcoded list of 39 Georgian names in
        // ExcelUtils that 20 of the school's 51 subjects never matched.
        try (Workbook workbook = exportService.matrix(
                data.classGroup.getId(), data.trimester1.getId(), "TRIMESTER_GRADE", false, data.template.getUuid(), false)) {
            assertEquals(data.subject.getName(), cell(workbook.getSheetAt(0), 1, 1));
        }
    }

    @Test
    @DisplayName("special values survive the export")
    void specialValuesSurvive() throws Exception {
        GradeWriteRequest request = new GradeWriteRequest();
        request.setJournalUuid(data.template.getUuid());
        request.setClassGroupId(data.classGroup.getId());
        request.setSubjectId(data.subject.getId());
        request.setPeriodId(data.trimester1.getId());
        GradeEntryUpdate u = new GradeEntryUpdate();
        u.setEnrollmentId(enrollment);
        u.setComponentCode("PROGRESS");
        u.setSpecialValue("CHT");
        request.setEntries(Arrays.asList(u));
        writeService.apply(request, 1L);
        em.flush();
        em.clear();

        try (Workbook workbook = exportService.detail(
                data.classGroup.getId(), data.subject.getId(), data.trimester1.getId(), data.template.getUuid(), false)) {

            Sheet sheet = workbook.getSheetAt(0);
            Row labels = sheet.getRow(2);
            int col = -1;
            for (int i = 1; i < labels.getLastCellNum(); i++) {
                if ("პროგრეს ტესტი".equals(labels.getCell(i).getStringCellValue())) {
                    col = i;
                }
            }
            int studentRow = rowContaining(sheet, 0,
                    "1. " + data.enrollments.get(0).getStudent().getLastName()
                            + " " + data.enrollments.get(0).getStudent().getFirstName());
            // The old exports encoded this as a -50 sentinel and turned it back
            // into text at print time.
            assertEquals("CHT", cell(sheet, studentRow, col));
        }
    }

    // ---- the printed scale -------------------------------------------------
    //
    // The school grades out of 7 and must report to the government out of 10.
    // One formula, applied only when the caller asks for it.

    @Test
    @DisplayName("the formula shifts the printed value without touching what is stored")
    void conversionShiftsOnlyThePrintedValue() throws Exception {
        formula("1", "3");

        assertEquals("12", printed("FINAL_TEST", "ფინალური ტესტი", true), "9 + 3 printed");

        BigDecimal stored = em.createQuery(
                        "select g.value from GradeEntry g where g.enrollment.id = :e "
                                + "and g.component.code = 'FINAL_TEST'", BigDecimal.class)
                .setParameter("e", data.enrollments.get(0).getId()).getSingleResult();
        assertEquals(0, stored.compareTo(new BigDecimal("9.00")), "storage is untouched");
    }

    @Test
    @DisplayName("without the flag the stored value is printed, formula or not")
    void notConvertedUnlessAsked() throws Exception {
        formula("1", "3");
        assertEquals("9.00", printed("FINAL_TEST", "ფინალური ტესტი", false));
    }

    @Test
    @DisplayName("a converted value is printed unrounded")
    void conversionIsNotRounded() throws Exception {
        // The school asked for the formula's output verbatim: 6.5 through "+3"
        // is 9.5, not 9 (which is what the legacy longValue() gave) and not 10.
        // The column declares 2 decimals; conversion ignores that deliberately.
        entryOf("FINAL_TEST").setValue(new BigDecimal("6.50"));
        formula("1", "3");

        assertEquals("9.5", printed("FINAL_TEST", "ფინალური ტესტი", true));
    }

    @Test
    @DisplayName("a special value is never put through the formula")
    void specialValuesAreNotConverted() throws Exception {
        // ჩთ means "not attested". Adding 3 to it is how it would become a mark.
        GradeEntry entry = entryOf("FINAL_TEST");
        entry.setValue(null);
        entry.setSpecialValue("CHT");
        formula("1", "3");

        assertEquals("CHT", printed("FINAL_TEST", "ფინალური ტესტი", true));
    }

    @Test
    @DisplayName("with no formula configured nothing converts")
    void noFormulaConfigured() throws Exception {
        // db/017 seeds the +3 the school runs on today, so this has to clear it
        // first. Rolled back with the rest of the test.
        em.createQuery("delete from ConversionFormula").executeUpdate();
        em.flush();
        em.clear();

        // Asking for conversion with nothing configured must print the stored
        // value, not a blank - "no formula" and "converted to nothing" are
        // different, and only one of them should ever reach a spreadsheet.
        assertEquals("9.00", printed("FINAL_TEST", "ფინალური ტესტი", true));
    }

    // ---- helpers ----------------------------------------------------------

    private void formula(String multiplier, String offset) {
        ConversionFormula f = new ConversionFormula();
        f.setName("ათბალიანი");
        f.setMultiplier(new BigDecimal(multiplier));
        f.setOffsetValue(new BigDecimal(offset));
        em.persist(f);
        em.flush();
        em.clear();
    }

    private GradeEntry entryOf(String code) {
        return em.createQuery(
                        "select g from GradeEntry g where g.enrollment.id = :e and g.component.code = :c",
                        GradeEntry.class)
                .setParameter("e", data.enrollments.get(0).getId())
                .setParameter("c", code)
                .getSingleResult();
    }

    /**
     * The cell under a named column, for the first student.
     */
    private String printed(String code, String columnLabel, boolean converted) throws Exception {
        try (Workbook workbook = exportService.detail(data.classGroup.getId(),
                data.subject.getId(), data.trimester1.getId(), data.template.getUuid(),
                converted)) {

            Sheet sheet = workbook.getSheetAt(0);
            Row labels = sheet.getRow(2);
            int col = -1;
            for (int i = 1; i < labels.getLastCellNum(); i++) {
                if (columnLabel.equals(labels.getCell(i).getStringCellValue())) {
                    col = i;
                }
            }
            int studentRow = rowContaining(sheet, 0,
                    "1. " + data.enrollments.get(0).getStudent().getLastName()
                            + " " + data.enrollments.get(0).getStudent().getFirstName());
            return cell(sheet, studentRow, col);
        }
    }
}
