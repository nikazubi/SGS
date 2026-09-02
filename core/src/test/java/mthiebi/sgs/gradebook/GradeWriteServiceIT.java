package mthiebi.sgs.gradebook;

import mthiebi.sgs.db.QueryFactoryProvider;
import mthiebi.sgs.gradebook.model.GradeEntry;
import mthiebi.sgs.gradebook.repository.GradeEntryRepository;
import mthiebi.sgs.gradebook.service.CellResult;
import mthiebi.sgs.gradebook.service.GradeEntryUpdate;
import mthiebi.sgs.gradebook.service.GradeExplainService;
import mthiebi.sgs.gradebook.service.GradeExplanation;
import mthiebi.sgs.gradebook.service.GradeWriteRequest;
import mthiebi.sgs.gradebook.service.GradeWriteResult;
import mthiebi.sgs.gradebook.service.GradeWriteService;
import mthiebi.sgs.gradebook.service.PeriodTreeLoader;
import mthiebi.sgs.gradebook.service.SpecialValueRegistry;
import mthiebi.sgs.gradebook.service.TemplateGraphLoader;
import mthiebi.sgs.gradebook.service.TemplateVersionResolver;
import mthiebi.sgs.gradebook.service.grid.GradeGridService;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Proves the write path end to end against a real SQL Server: entity mappings,
 * the working-set query, recompute, and persistence.
 * <p>
 * Runs inside the test transaction and rolls back, so it leaves the database as
 * it found it. Requires the dev database to be reachable - it is an integration
 * test, and skipping the database would defeat the point of it.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({GradeWriteService.class, GradeExplainService.class, GradeGridService.class,
        TemplateGraphLoader.class, PeriodTreeLoader.class, TemplateVersionResolver.class,
        SpecialValueRegistry.class, QueryFactoryProvider.class,
        mthiebi.sgs.gradebook.service.conversion.GradeConversionService.class})
@TestPropertySource(properties = {
        // The pinned SQLServerDialect is the SQL Server 2000 one and has no
        // sequence support, so Hibernate looks for a *table* called
        // sgs.school_seq and fails. Sequences are what keep JDBC insert
        // batching available, which IDENTITY would disable.
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.SQLServer2012Dialect",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.properties.hibernate.jdbc.batch_size=50",
        "spring.jpa.properties.hibernate.order_inserts=true",
        "spring.jpa.properties.hibernate.order_updates=true"
})
class GradeWriteServiceIT {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private GradeWriteService writeService;

    @Autowired
    private GradeEntryRepository gradeEntryRepository;

    @Autowired
    private TemplateGraphLoader templateGraphLoader;

    @Autowired
    private GradeExplainService explainService;

    private GradebookTestData data;

    @BeforeEach
    void setUp() {
        templateGraphLoader.evictAll();
        data = new GradebookTestData(em).build(UUID.randomUUID().toString().substring(0, 6));
    }

    private GradeWriteRequest request(Long periodId, List<GradeEntryUpdate> entries) {
        GradeWriteRequest request = new GradeWriteRequest();
        request.setJournalUuid(data.template.getUuid());
        request.setClassGroupId(data.classGroup.getId());
        request.setSubjectId(data.subject.getId());
        request.setPeriodId(periodId);
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

    private Optional<CellResult> cell(List<CellResult> cells, Long enrollmentId, String code) {
        return cells.stream()
                .filter(c -> c.getEnrollmentId().equals(enrollmentId)
                        && c.getComponentCode().equals(code))
                .findFirst();
    }

    @Test
    @DisplayName("a row of marks saves and brings its calculated values back with it")
    void savesARowAndReturnsDerivedValues() throws Exception {
        Long enrollment = data.enrollments.get(0).getId();

        GradeWriteResult result = writeService.apply(request(data.trimester1.getId(), Arrays.asList(
                update(enrollment, "ONGOING_1", "7"),
                update(enrollment, "ONGOING_2", "8"),
                update(enrollment, "ONGOING_3", "6"),
                update(enrollment, "INITIAL_KNOWLEDGE", "5"),
                update(enrollment, "FINAL_TEST", "9"))), null);

        assertTrue(result.getConflicts().isEmpty());
        assertEquals(5, result.getApplied().size());

        // ongoing average = (7+8+6)/3 = 7
        assertEquals(0, cell(result.getDerived(), enrollment, "ONGOING_AVG")
                .orElseThrow(AssertionError::new).getValue().compareTo(new BigDecimal("7.00")));

        // 0.50*7 + 0.20*5 + 0.30*9 = 3.5 + 1.0 + 2.7 = 7.2
        assertEquals(0, cell(result.getDerived(), enrollment, "TRIMESTER_GRADE")
                .orElseThrow(AssertionError::new).getValue().compareTo(new BigDecimal("7.2")));

        // rolls up into the year, and into the student-wide rating
        assertEquals(0, cell(result.getDerived(), enrollment, "ANNUAL")
                .orElseThrow(AssertionError::new).getValue().compareTo(new BigDecimal("7.2")));
        assertEquals(0, cell(result.getDerived(), enrollment, "RATING")
                .orElseThrow(AssertionError::new).getValue().compareTo(new BigDecimal("7")));
    }

    @Test
    @DisplayName("the calculated values are actually persisted, not just returned")
    void derivedValuesArePersisted() throws Exception {
        Long enrollment = data.enrollments.get(0).getId();

        writeService.apply(request(data.trimester1.getId(), Arrays.asList(
                update(enrollment, "ONGOING_1", "6"),
                update(enrollment, "FINAL_TEST", "6"))), null);
        em.flush();
        em.clear();

        List<GradeEntry> stored = gradeEntryRepository.loadGrid(
                java.util.Collections.singletonList(enrollment),
                Arrays.asList(data.trimester1.getId(), data.year.getId()),
                data.subject.getId());

        Optional<GradeEntry> trimester = stored.stream()
                .filter(g -> g.getComponent().getCode().equals("TRIMESTER_GRADE"))
                .findFirst();
        assertTrue(trimester.isPresent(), "the trimester grade should be a row, not a computed-on-read value");
        assertEquals(0, trimester.get().getValue().compareTo(new BigDecimal("6.0")));
    }

    @Test
    @DisplayName("editing one mark rewrites only what actually changed")
    void secondEditWritesOnlyWhatMoved() throws Exception {
        Long enrollment = data.enrollments.get(0).getId();

        writeService.apply(request(data.trimester1.getId(), Arrays.asList(
                update(enrollment, "ONGOING_1", "6"),
                update(enrollment, "ONGOING_2", "6"))), null);

        // Rewriting the same value must not churn rows.
        GradeWriteResult unchanged = writeService.apply(request(data.trimester1.getId(),
                java.util.Collections.singletonList(update(enrollment, "ONGOING_1", "6"))), null);
        assertTrue(unchanged.getDerived().isEmpty(),
                "nothing downstream moved, so nothing downstream should be written");
    }

    @Test
    @DisplayName("a stale edit is reported as a conflict while the rest of the batch still lands")
    void conflictsAreReportedPerCell() throws Exception {
        Long first = data.enrollments.get(0).getId();
        Long second = data.enrollments.get(1).getId();

        writeService.apply(request(data.trimester1.getId(),
                java.util.Collections.singletonList(update(first, "ONGOING_1", "6"))), null);
        em.flush();

        GradeEntryUpdate stale = update(first, "ONGOING_1", "9");
        stale.setExpectedVersion(-1);

        GradeWriteResult result = writeService.apply(request(data.trimester1.getId(),
                Arrays.asList(stale, update(second, "ONGOING_1", "7"))), null);

        assertEquals(1, result.getConflicts().size());
        assertEquals("ONGOING_1", result.getConflicts().get(0).getComponentCode());
        // The other teacher's cell still saved.
        assertTrue(cell(result.getApplied(), second, "ONGOING_1").isPresent());
    }

    @Test
    @DisplayName("a whole class of marks saves in one batch")
    void savesAWholeClass() throws Exception {
        List<GradeEntryUpdate> entries = new ArrayList<>();
        for (int i = 0; i < data.enrollments.size(); i++) {
            Long enrollment = data.enrollments.get(i).getId();
            entries.add(update(enrollment, "ONGOING_1", String.valueOf(5 + (i % 5))));
            entries.add(update(enrollment, "FINAL_TEST", String.valueOf(4 + (i % 6))));
        }

        GradeWriteResult result = writeService.apply(request(data.trimester1.getId(), entries), null);

        assertEquals(GradebookTestData.STUDENT_COUNT * 2, result.getApplied().size());
        // Four derived cells per student: ongoing average, trimester, annual, rating.
        assertEquals(GradebookTestData.STUDENT_COUNT * 4, result.getDerived().size());
        assertTrue(result.getConflicts().isEmpty());
    }

    @Test
    @DisplayName("a calculated cell can explain which marks it used and which it skipped")
    void explainsHowACellWasCalculated() throws Exception {
        Long enrollment = data.enrollments.get(0).getId();

        GradeEntryUpdate notAttested = update(enrollment, "ONGOING_2", null);
        notAttested.setSpecialValue("CHT");

        writeService.apply(request(data.trimester1.getId(), Arrays.asList(
                update(enrollment, "ONGOING_1", "6"),
                notAttested)), null);
        em.flush();

        GradeExplanation explanation = explainService.explain(
                enrollment, data.subject.getId(), data.trimester1.getId(), "ONGOING_AVG",
                data.template.getUuid());

        assertEquals(0, explanation.getValue().compareTo(new BigDecimal("6.00")));

        List<String> statuses = explanation.getTrace().getTerms().get(0).getSources().stream()
                .map(src -> src.getComponentCode() + "=" + src.getStatus())
                .collect(java.util.stream.Collectors.toList());

        assertTrue(statuses.contains("ONGOING_1=USED"), statuses.toString());
        assertTrue(statuses.contains("ONGOING_2=SPECIAL_EXCLUDED"), statuses.toString());
        assertTrue(statuses.contains("ONGOING_3=EMPTY"), statuses.toString());
        // Seven ongoing columns feed the average, whatever their state.
        assertEquals(GradebookTestData.ONGOING_COUNT, statuses.size());
    }

    @Test
    @DisplayName("a period stays on the template version its marks were entered under")
    void aPeriodKeepsItsTemplateVersionWhenANewOneIsActivated() throws Exception {
        Long enrollment = data.enrollments.get(0).getId();
        Long originalVersionId = data.version.getId();

        writeService.apply(request(data.trimester1.getId(),
                java.util.Collections.singletonList(update(enrollment, "ONGOING_1", "6"))), null);
        em.flush();

        // A second version is activated and assigned to the class mid-year.
        mthiebi.sgs.gradebook.model.TemplateVersion v2 =
                new mthiebi.sgs.gradebook.model.TemplateVersion();
        v2.setTemplate(data.version.getTemplate());
        v2.setVersionNo(2);
        v2.setStatus(mthiebi.sgs.gradebook.model.TemplateVersionStatus.ACTIVE);
        v2.setPeriodScheme(data.version.getPeriodScheme());
        em.persist(v2);

        mthiebi.sgs.gradebook.model.TemplateAssignment assignment = em.createQuery(
                        "select a from TemplateAssignment a where a.classGroup.id = :id",
                        mthiebi.sgs.gradebook.model.TemplateAssignment.class)
                .setParameter("id", data.classGroup.getId()).getSingleResult();
        assignment.setTemplateVersion(v2);
        em.flush();

        // Correcting an earlier mark must not drag the period onto the new rules.
        writeService.apply(request(data.trimester1.getId(),
                java.util.Collections.singletonList(update(enrollment, "ONGOING_1", "9"))), null);
        em.flush();
        em.clear();

        List<GradeEntry> stored = gradeEntryRepository.loadGrid(
                java.util.Collections.singletonList(enrollment),
                java.util.Collections.singletonList(data.trimester1.getId()),
                data.subject.getId());

        assertFalse(stored.isEmpty());
        stored.forEach(entry -> assertEquals(originalVersionId, entry.getTemplateVersion().getId(),
                "editing an existing period must not migrate it to a newer template version"));
    }
}
