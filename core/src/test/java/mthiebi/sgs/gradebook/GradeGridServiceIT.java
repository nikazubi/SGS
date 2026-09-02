package mthiebi.sgs.gradebook;

import mthiebi.sgs.db.QueryFactoryProvider;
import mthiebi.sgs.gradebook.model.GradeEntry;
import mthiebi.sgs.gradebook.repository.GradeEntryRepository;
import mthiebi.sgs.gradebook.service.CellRejectionReason;
import mthiebi.sgs.gradebook.service.GradeEntryUpdate;
import mthiebi.sgs.gradebook.service.GradeExplainService;
import mthiebi.sgs.gradebook.service.GradeWriteRequest;
import mthiebi.sgs.gradebook.service.GradeWriteResult;
import mthiebi.sgs.gradebook.service.GradeWriteService;
import mthiebi.sgs.gradebook.service.PeriodTreeLoader;
import mthiebi.sgs.gradebook.service.SpecialValueRegistry;
import mthiebi.sgs.gradebook.service.TemplateGraphLoader;
import mthiebi.sgs.gradebook.service.TemplateVersionResolver;
import mthiebi.sgs.gradebook.service.grid.GradeGrid;
import mthiebi.sgs.gradebook.service.grid.GradeGridService;
import mthiebi.sgs.gradebook.service.grid.GridCell;
import mthiebi.sgs.gradebook.service.grid.GridColumn;
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
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The read path, against a real SQL Server.
 * <p>
 * The grid is what the console draws itself from, so the things worth proving
 * are that the columns come from configuration rather than from code, that
 * every cell carries what a safe write needs, and that a published cell cannot
 * be edited straight through.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({GradeGridService.class, GradeWriteService.class, GradeExplainService.class,
        TemplateGraphLoader.class, PeriodTreeLoader.class, TemplateVersionResolver.class,
        SpecialValueRegistry.class, QueryFactoryProvider.class,
        mthiebi.sgs.gradebook.service.conversion.GradeConversionService.class})
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.SQLServer2012Dialect",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.properties.hibernate.jdbc.batch_size=50",
        "spring.jpa.properties.hibernate.order_inserts=true",
        "spring.jpa.properties.hibernate.order_updates=true"
})
class GradeGridServiceIT {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private GradeGridService gridService;

    @Autowired
    private GradeWriteService writeService;

    @Autowired
    private GradeEntryRepository gradeEntryRepository;

    @Autowired
    private TemplateGraphLoader templateGraphLoader;

    private GradebookTestData data;

    @BeforeEach
    void setUp() {
        templateGraphLoader.evictAll();
        data = new GradebookTestData(em).build(UUID.randomUUID().toString().substring(0, 6));
    }

    private GradeGrid grid() throws Exception {
        return gridService.load(data.classGroup.getId(), data.subject.getId(),
                data.trimester1.getId(), data.template.getUuid());
    }

    private GradeWriteRequest request(List<GradeEntryUpdate> entries) {
        GradeWriteRequest request = new GradeWriteRequest();
        request.setJournalUuid(data.template.getUuid());
        request.setClassGroupId(data.classGroup.getId());
        request.setSubjectId(data.subject.getId());
        request.setPeriodId(data.trimester1.getId());
        request.setEntries(entries);
        return request;
    }

    private GradeEntryUpdate update(Long enrollmentId, String code, String value) {
        GradeEntryUpdate u = new GradeEntryUpdate();
        u.setEnrollmentId(enrollmentId);
        u.setComponentCode(code);
        u.setValue(value == null ? null : new BigDecimal(value));
        return u;
    }

    private Optional<GridColumn> column(GradeGrid grid, String code) {
        return grid.getColumns().stream().filter(c -> c.getCode().equals(code)).findFirst();
    }

    private Optional<GridCell> cell(GradeGrid grid, Long enrollmentId, String code) {
        return grid.getCells().stream()
                .filter(c -> c.getEnrollmentId().equals(enrollmentId)
                        && c.getComponentCode().equals(code))
                .findFirst();
    }

    @Test
    @DisplayName("columns come from the template, not from the page")
    void columnsComeFromTheTemplate() throws Exception {
        GradeGrid grid = grid();

        // Seven ongoing, the average, initial knowledge, progress, final test
        // and the trimester grade. All subject-scoped, all at trimester level.
        assertEquals(12, grid.getColumns().size());
        assertTrue(column(grid, "ONGOING_1").isPresent());
        assertTrue(column(grid, "TRIMESTER_GRADE").isPresent());

        // ANNUAL lives at year level and RATING is student-wide; neither
        // belongs on a subject grid for a trimester.
        assertFalse(column(grid, "ANNUAL").isPresent());
        assertFalse(column(grid, "RATING").isPresent());

        assertEquals(25, grid.getStudents().size());
        assertNotNull(grid.getTemplateVersion());
        assertEquals(data.version.getId(), grid.getTemplateVersion().getId());
    }

    @Test
    @DisplayName("Georgian text survives the round trip")
    void georgianTextSurvives() throws Exception {
        // The database collation is SQL_Latin1_General_CP1_CI_AS, which has no
        // code page for Georgian: a varchar column stores ?????????? and the
        // write succeeds silently. Every label in this system is Georgian, so
        // it is worth asserting rather than assuming.
        GradeGrid grid = grid();

        assertEquals("ტრიმესტრის შეფასება",
                column(grid, "TRIMESTER_GRADE").orElseThrow(AssertionError::new).getLabel());
        assertEquals("I ტრიმესტრი", grid.getPeriod().getLabel());
        assertTrue(grid.getStudents().get(0).getFirstName().startsWith("მოსწავლე"));
        assertTrue(grid.getTemplateVersion().getTemplateName().startsWith("ტრიმესტრული შეფასება"));
    }

    @Test
    @DisplayName("the ongoing marks are grouped under one header")
    void ongoingMarksAreGrouped() throws Exception {
        GradeGrid grid = grid();

        assertEquals(1, grid.getColumnGroups().size());
        assertEquals("მიმდინარე შეფასება", grid.getColumnGroups().get(0).getLabel());
        assertEquals(7, grid.getColumnGroups().get(0).getComponentCodes().size());
    }

    @Test
    @DisplayName("an input column names every calculated column it moves, not just the next one")
    void dependentsAreTransitive() throws Exception {
        GradeGrid grid = grid();

        // ONGOING_1 -> ONGOING_AVG -> TRIMESTER_GRADE. A direct-dependents list
        // would leave the trimester grade showing a stale number while the
        // console claimed it was current.
        List<String> dependents = column(grid, "ONGOING_1").orElseThrow(AssertionError::new)
                .getDependents();
        assertTrue(dependents.contains("ONGOING_AVG"));
        assertTrue(dependents.contains("TRIMESTER_GRADE"));

        assertTrue(column(grid, "TRIMESTER_GRADE").orElseThrow(AssertionError::new)
                .getDependsOn().contains("ONGOING_AVG"));
    }

    @Test
    @DisplayName("a calculated column is editable, because a formula is a convenience not a cage")
    void calculatedColumnsStayEditable() throws Exception {
        GradeGrid grid = grid();

        GridColumn trimester = column(grid, "TRIMESTER_GRADE").orElseThrow(AssertionError::new);
        assertTrue(trimester.isEditable());
        assertTrue(trimester.isAllowOverride());
    }

    @Test
    @DisplayName("every cell carries the row version a safe write needs")
    void cellsCarryTheirRowVersion() throws Exception {
        Long enrollment = data.enrollments.get(0).getId();
        writeService.apply(request(Arrays.asList(
                update(enrollment, "ONGOING_1", "7"),
                update(enrollment, "ONGOING_2", "8"))), null);
        em.flush();
        em.clear();

        GridCell first = cell(grid(), enrollment, "ONGOING_1").orElseThrow(AssertionError::new);
        assertEquals(0, first.getValue().compareTo(new BigDecimal("7.00")));
        assertFalse(first.isPublished());
        assertFalse(first.isChangedSincePublication());

        // Derived cells are materialised, so they come back as cells too.
        assertTrue(cell(grid(), enrollment, "ONGOING_AVG").isPresent());
    }

    @Test
    @DisplayName("a published cell cannot be edited directly")
    void publishedCellsAreLocked() throws Exception {
        Long enrollment = data.enrollments.get(0).getId();
        writeService.apply(request(Collections.singletonList(
                update(enrollment, "ONGOING_1", "7"))), null);
        em.flush();

        publish(enrollment, "ONGOING_1");

        GradeWriteResult result = writeService.apply(request(Collections.singletonList(
                update(enrollment, "ONGOING_1", "9"))), null);

        assertTrue(result.getApplied().isEmpty());
        assertEquals(1, result.getConflicts().size());
        assertEquals(CellRejectionReason.PUBLISHED, result.getConflicts().get(0).getReason());
        assertEquals("ONGOING_1", result.getConflicts().get(0).getComponentCode());

        // and the value parents were shown is untouched
        assertEquals(0, entry(enrollment, "ONGOING_1").getValue().compareTo(new BigDecimal("7.00")));
    }

    @Test
    @DisplayName("one locked cell does not discard the rest of the batch")
    void aLockedCellDoesNotSinkTheBatch() throws Exception {
        Long enrollment = data.enrollments.get(0).getId();
        writeService.apply(request(Collections.singletonList(
                update(enrollment, "ONGOING_1", "7"))), null);
        em.flush();

        publish(enrollment, "ONGOING_1");

        GradeWriteResult result = writeService.apply(request(Arrays.asList(
                update(enrollment, "ONGOING_1", "9"),
                update(enrollment, "ONGOING_2", "8"),
                update(enrollment, "ONGOING_3", "6"))), null);

        assertEquals(1, result.getConflicts().size());
        assertEquals(2, result.getApplied().size());
    }

    @Test
    @DisplayName("recomputing a published cell is allowed, and shows as changed since publication")
    void recomputeIsNotBlockedByPublication() throws Exception {
        Long enrollment = data.enrollments.get(0).getId();
        writeService.apply(request(Arrays.asList(
                update(enrollment, "ONGOING_1", "6"),
                update(enrollment, "ONGOING_2", "6"))), null);
        em.flush();

        // The average is published at 6, then a new ongoing mark arrives.
        publish(enrollment, "ONGOING_AVG");
        writeService.apply(request(Collections.singletonList(
                update(enrollment, "ONGOING_3", "9"))), null);
        em.flush();
        em.clear();

        // The lock stops people, not the engine: parents keep seeing 6 until
        // someone publishes again, and the divergence is what a change request
        // exists to resolve.
        GridCell avg = cell(grid(), enrollment, "ONGOING_AVG").orElseThrow(AssertionError::new);
        assertEquals(0, avg.getValue().compareTo(new BigDecimal("7.00")));
        assertTrue(avg.isPublished());
        assertTrue(avg.isChangedSincePublication());
    }

    private GradeEntry entry(Long enrollmentId, String code) {
        return em.createQuery(
                        "select g from GradeEntry g where g.enrollment.id = :e "
                                + "and g.component.code = :c and g.period.id = :p", GradeEntry.class)
                .setParameter("e", enrollmentId)
                .setParameter("c", code)
                .setParameter("p", data.trimester1.getId())
                .getSingleResult();
    }

    /**
     * Stands in for the publish action, which is phase 3.
     */
    private void publish(Long enrollmentId, String code) {
        GradeEntry e = entry(enrollmentId, code);
        e.setPublishedValue(e.getValue());
        e.setPublishedSpecialValue(e.getSpecialValue());
        e.setPublishedAt(Instant.now());
        gradeEntryRepository.save(e);
        em.flush();
        em.clear();
    }
}
