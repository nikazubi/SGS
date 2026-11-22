package mthiebi.sgs.gradebook;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.db.QueryFactoryProvider;
import mthiebi.sgs.gradebook.model.*;
import mthiebi.sgs.gradebook.service.*;
import mthiebi.sgs.gradebook.service.export.GradeExportService;
import mthiebi.sgs.gradebook.service.grid.ClassSummary;
import mthiebi.sgs.gradebook.service.grid.ClassSummaryService;
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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The class summary: every subject's mark for a period, a rating beside them,
 * and the class-wide journals' figures - the old monthly page, by trimester.
 * <p>
 * The database these run against carries its own journals, which show up as
 * extra class-wide columns with nothing in them. So columns are found by key,
 * never by position past the subjects.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ClassSummaryService.class, GradeExportService.class,
        mthiebi.sgs.gradebook.service.grid.SummaryBreakdown.class,
        GradeWriteService.class, GradeExplainService.class,
        TemplateGraphLoader.class, PeriodTreeLoader.class, TemplateVersionResolver.class,
        SpecialValueRegistry.class, QueryFactoryProvider.class,
        mthiebi.sgs.gradebook.service.conversion.GradeConversionService.class})
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.SQLServer2012Dialect",
        "spring.jpa.hibernate.ddl-auto=none"
})
class ClassSummaryServiceIT {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private ClassSummaryService summaryService;

    @Autowired
    private GradeExportService exportService;

    @Autowired
    private GradeWriteService writeService;

    @Autowired
    private TemplateGraphLoader templateGraphLoader;

    private GradebookTestData data;
    private Subject history;
    private GradingTemplate ethics;

    @BeforeEach
    void setUp() throws Exception {
        templateGraphLoader.evictAll();
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        data = new GradebookTestData(em).build(suffix);

        // A second subject, after the first in the class's own order.
        history = new Subject();
        history.setName("ისტორია " + suffix);
        history.setShortName("ისტ");
        em.persist(history);
        ClassSubject cs = new ClassSubject();
        cs.setClassGroup(data.classGroup);
        cs.setSubject(history);
        cs.setSortIndex(2);
        cs.setTeacherName("ბერიძე ნანა");
        em.persist(cs);

        ethics = classWideJournal("ეთიკა " + suffix);
        em.flush();

        // Only the final test, so the trimester grade is the final test itself:
        // the fixture's weights renormalize over what was sat.
        typeFor(data.subject, "FINAL_TEST", "9");
        typeFor(history, "FINAL_TEST", "6");
    }

    @Test
    @DisplayName("each subject shows its trimester grade, in the class's order, and the row's average beside them")
    void trimesterShowsEverySubjectAndTheAverage() throws Exception {
        ClassSummary summary = build(data.trimester1);

        List<String> keys = keys(summary);
        assertEquals("S" + data.subject.getId(), keys.get(0));
        assertEquals("S" + history.getId(), keys.get(1));
        assertEquals("AVERAGE", keys.get(2), "a trimester has no stored rating, so it is worked out");

        ClassSummary.Column math = column(summary, "S" + data.subject.getId());
        assertEquals(data.subject.getName(), math.getLabel());
        assertEquals("ტრიმესტრის შეფასება", math.getDetail(),
                "with nothing ticked, the last calculated column at the level");
        assertEquals("ბერიძე ნანა", column(summary, "S" + history.getId()).getTeacherName());

        ClassSummary.Row first = summary.getRows().get(0);
        assertNumber("9", first.getCells().get("S" + data.subject.getId()).getValue());
        assertNumber("6", first.getCells().get("S" + history.getId()).getValue());
        // (9 + 6) / 2 = 7.5, rounded half up as the old page did.
        assertEquals("8", first.getCells().get("AVERAGE").getValue().toPlainString());

        assertNull(summary.getRows().get(1).getCells().get("AVERAGE"),
                "a student with no marks has no average, not a nought");
    }

    @Test
    @DisplayName("ჩთ is shown as itself and left out of the average")
    void notAttestedIsLeftOutOfTheAverage() throws Exception {
        GradeEntry historyGrade = entry(history, "TRIMESTER_GRADE", data.trimester1);
        historyGrade.setValue(null);
        historyGrade.setSpecialValue(SpecialValueRegistry.NOT_ATTESTED);
        em.flush();

        ClassSummary.Row first = build(data.trimester1).getRows().get(0);
        assertEquals("CHT", first.getCells().get("S" + history.getId()).getSpecialValue());
        assertNumber("9", first.getCells().get("AVERAGE").getValue());
    }

    @Test
    @DisplayName("a column ticked as the summary wins over the calculated one")
    void theSummaryTickWins() throws Exception {
        // What the school did: the trimester grade became a typed column, and
        // the tick is what says it is the one that counts.
        data.components.get("FINAL_TEST").setSummaryColumn(true);
        em.flush();

        ClassSummary summary = build(data.trimester1);
        assertEquals("ფინალური ტესტი", column(summary, "S" + data.subject.getId()).getDetail());
        assertNumber("9", summary.getRows().get(0).getCells().get("S" + data.subject.getId()).getValue());
    }

    @Test
    @DisplayName("the year shows the annual grade and the stored rating, not a worked-out one")
    void yearShowsTheAnnualGradeAndTheRating() throws Exception {
        ClassSummary summary = build(data.year);

        assertEquals("წლიური შეფასება", column(summary, "S" + data.subject.getId()).getDetail());
        assertFalse(keys(summary).contains("AVERAGE"));
        ClassSummary.Column rating = column(summary, "WRATING");
        assertEquals(ClassSummary.ColumnKind.SUBJECT_JOURNAL, rating.getKind());

        ClassSummary.Row first = summary.getRows().get(0);
        assertNumber(whole(entry(data.subject, "ANNUAL", data.year).getValue()),
                first.getCells().get("S" + data.subject.getId()).getValue());
        assertNumber(whole(entry(null, "RATING", data.year).getValue()),
                first.getCells().get("WRATING").getValue());
    }

    @Test
    @DisplayName("a class-wide journal brings its own column for the period, under its own name")
    void classWideJournalsBringTheirColumn() throws Exception {
        typeClassWide("ETHICS_TRIM", data.trimester1, "8.5");

        ClassSummary summary = build(data.trimester1);
        ClassSummary.Column column = column(summary, "J" + ethics.getUuid());
        assertEquals(ethics.getName(), column.getLabel());
        assertEquals("ტრიმესტრი", column.getDetail());
        assertEquals(ClassSummary.ColumnKind.CLASS_JOURNAL, column.getKind());
        assertTrue(keys(summary).indexOf(column.getKey()) > keys(summary).indexOf("AVERAGE"),
                "after the subjects and the rating");

        // Stored as 8.5; shown as a whole number like everything else here.
        assertEquals("9", summary.getRows().get(0).getCells().get(column.getKey()).getValue().toPlainString());

        // On the year it is the journal's year column instead.
        assertEquals("წლიური", column(build(data.year), "J" + ethics.getUuid()).getDetail());
    }

    @Test
    @DisplayName("the 10-point scale converts marks and the rating, never ethics or hours")
    void conversionReachesOnlyAcademicMarks() throws Exception {
        em.createQuery("delete from ConversionFormula").executeUpdate();
        ConversionFormula formula = new ConversionFormula();
        formula.setName("ათბალიანი");
        formula.setMultiplier(BigDecimal.ONE);
        formula.setOffsetValue(new BigDecimal("3"));
        em.persist(formula);
        typeClassWide("ETHICS_TRIM", data.trimester1, "8.5");
        em.flush();

        ClassSummary summary = build(data.trimester1);
        assertTrue(summary.isConvertible());
        ClassSummary.Row first = summary.getRows().get(0);
        assertNumber("12", first.getCells().get("S" + data.subject.getId()).getConvertedValue());
        assertNumber("11", first.getCells().get("AVERAGE").getConvertedValue(),
                "the rounded rating (8) converted, not 7.5 + 3");
        assertNull(first.getCells().get("J" + ethics.getUuid()).getConvertedValue());
    }

    @Test
    @DisplayName("the spreadsheet has the screen's columns, and prints ჩთ as ჩთ")
    void theSpreadsheetMatchesTheScreen() throws Exception {
        GradeEntry historyGrade = entry(history, "TRIMESTER_GRADE", data.trimester1);
        historyGrade.setValue(null);
        historyGrade.setSpecialValue(SpecialValueRegistry.NOT_ATTESTED);
        em.flush();

        ClassSummary summary = build(data.trimester1);
        try (Workbook workbook = exportService.classSummary(summary, false)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(1);
            assertEquals("მოსწავლის გვარი, სახელი", header.getCell(0).getStringCellValue());
            for (int i = 0; i < summary.getColumns().size(); i++) {
                assertEquals(summary.getColumns().get(i).getLabel(),
                        header.getCell(i + 1).getStringCellValue());
            }
            Row first = sheet.getRow(2);
            assertTrue(first.getCell(0).getStringCellValue().startsWith("1. "));
            assertEquals("9", first.getCell(1).getStringCellValue(), "whole numbers, as on screen");
            assertEquals("ჩთ", first.getCell(2).getStringCellValue());
            assertEquals("9", first.getCell(3).getStringCellValue());

            Row teachers = sheet.getRow(2 + summary.getRows().size());
            assertEquals("პედაგოგი", teachers.getCell(0).getStringCellValue());
            assertEquals("ბერიძე ნანა", teachers.getCell(2).getStringCellValue());
        }
    }

    @Test
    @DisplayName("a class-wide journal cannot stand in for the subject journal")
    void aClassWideJournalIsRefusedAsTheSubjectJournal() {
        assertThrows(SGSException.class, () -> summaryService.build(
                data.classGroup.getId(), data.trimester1.getId(), ethics.getUuid()));
    }

    // ---- helpers ----------------------------------------------------------

    private ClassSummary build(Period period) throws SGSException {
        em.flush();
        em.clear();
        return summaryService.build(data.classGroup.getId(), period.getId(), data.template.getUuid());
    }

    private List<String> keys(ClassSummary summary) {
        return summary.getColumns().stream().map(ClassSummary.Column::getKey)
                .collect(Collectors.toList());
    }

    private ClassSummary.Column column(ClassSummary summary, String key) {
        return summary.getColumns().stream().filter(c -> c.getKey().equals(key)).findFirst()
                .orElseThrow(() -> new AssertionError("no column " + key + " in " + keys(summary)));
    }

    private void assertNumber(String expected, BigDecimal actual) {
        assertNumber(expected, actual, "");
    }

    private void assertNumber(String expected, BigDecimal actual, String why) {
        assertNotNull(actual, "expected " + expected + " " + why);
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                "expected " + expected + " but was " + actual + " " + why);
    }

    private String whole(BigDecimal value) {
        return value.setScale(0, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private void typeFor(Subject subject, String code, String value) throws Exception {
        GradeWriteRequest request = new GradeWriteRequest();
        request.setJournalUuid(data.template.getUuid());
        request.setClassGroupId(data.classGroup.getId());
        request.setSubjectId(subject.getId());
        request.setPeriodId(data.trimester1.getId());
        GradeEntryUpdate u = new GradeEntryUpdate();
        u.setEnrollmentId(data.enrollments.get(0).getId());
        u.setComponentCode(code);
        u.setValue(new BigDecimal(value));
        request.setEntries(new ArrayList<>(Collections.singletonList(u)));
        writeService.apply(request, 1L);
        em.flush();
    }

    private void typeClassWide(String code, Period period, String value) throws Exception {
        GradeWriteRequest request = new GradeWriteRequest();
        request.setJournalUuid(ethics.getUuid());
        request.setClassGroupId(data.classGroup.getId());
        request.setPeriodId(period.getId());
        GradeEntryUpdate u = new GradeEntryUpdate();
        u.setEnrollmentId(data.enrollments.get(0).getId());
        u.setComponentCode(code);
        u.setValue(new BigDecimal(value));
        request.setEntries(new ArrayList<>(Collections.singletonList(u)));
        writeService.apply(request, 1L);
        em.flush();
    }

    private GradeEntry entry(Subject subject, String code, Period period) {
        return em.createQuery(
                        "select g from GradeEntry g where g.enrollment.id = :e and g.component.code = :c "
                                + "and g.period.id = :p and "
                                + (subject == null ? "g.subject is null" : "g.subject.id = :s"),
                        GradeEntry.class)
                .setParameter("e", data.enrollments.get(0).getId())
                .setParameter("c", code)
                .setParameter("p", period.getId())
                .setParameter(subject == null ? "e" : "s",
                        subject == null ? data.enrollments.get(0).getId() : subject.getId())
                .getSingleResult();
    }

    /**
     * An ethics-shaped journal: one typed mark per trimester and one per year,
     * neither tied to a subject.
     */
    private GradingTemplate classWideJournal(String name) {
        GradingTemplate template = new GradingTemplate();
        template.setName(name);
        template.setFrequency(JournalFrequency.TRIMESTER);
        template.setSubjectScoped(false);
        template.setGridMode(GridMode.PERIODS);
        template.setLocksOnPublish(false);
        em.persist(template);

        TemplateVersion version = new TemplateVersion();
        version.setTemplate(template);
        version.setVersionNo(1);
        version.setStatus(TemplateVersionStatus.ACTIVE);
        version.setPeriodScheme(data.scheme);
        em.persist(version);

        classWideColumn(version, "ETHICS_TRIM", "ტრიმესტრი", 0, PeriodKind.ROLLUP);
        classWideColumn(version, "ETHICS_YEAR", "წლიური", 1, PeriodKind.YEAR);
        return template;
    }

    private void classWideColumn(TemplateVersion version, String code, String label, int ordinal,
                                 PeriodKind periodKind) {
        GradeComponent c = new GradeComponent();
        c.setTemplateVersion(version);
        c.setCode(code);
        c.setLabel(label);
        c.setOrdinal(ordinal);
        c.setKind(ComponentKind.INPUT);
        c.setPeriodKind(periodKind);
        c.setSubjectScoped(false);
        c.setDecimals(2);
        c.setScaleMin(BigDecimal.ZERO);
        c.setScaleMax(new BigDecimal("10"));
        em.persist(c);
    }
}
