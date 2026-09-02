package mthiebi.sgs.gradebook;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.db.QueryFactoryProvider;
import mthiebi.sgs.gradebook.engine.ValidationIssue;
import mthiebi.sgs.gradebook.model.ComponentKind;
import mthiebi.sgs.gradebook.model.GradeEntry;
import mthiebi.sgs.gradebook.model.JournalFrequency;
import mthiebi.sgs.gradebook.model.PeriodKind;
import mthiebi.sgs.gradebook.model.RuleType;
import mthiebi.sgs.gradebook.model.SourceKind;
import mthiebi.sgs.gradebook.model.TemplateVersionStatus;
import mthiebi.sgs.gradebook.service.GradeEntryUpdate;
import mthiebi.sgs.gradebook.service.GradeExplainService;
import mthiebi.sgs.gradebook.service.GradeWriteRequest;
import mthiebi.sgs.gradebook.service.GradeWriteResult;
import mthiebi.sgs.gradebook.service.GradeWriteService;
import mthiebi.sgs.gradebook.service.PeriodTreeLoader;
import mthiebi.sgs.gradebook.service.SpecialValueRegistry;
import mthiebi.sgs.gradebook.service.TemplateGraphLoader;
import mthiebi.sgs.gradebook.service.TemplateVersionResolver;
import mthiebi.sgs.gradebook.service.journal.ColumnRef;
import mthiebi.sgs.gradebook.service.journal.ComponentDraft;
import mthiebi.sgs.gradebook.service.journal.JournalDraft;
import mthiebi.sgs.gradebook.service.journal.JournalService;
import mthiebi.sgs.gradebook.service.journal.JournalView;
import mthiebi.sgs.gradebook.service.journal.RuleDraft;
import mthiebi.sgs.gradebook.service.journal.SaveResult;
import mthiebi.sgs.gradebook.service.journal.SourceDraft;
import mthiebi.sgs.gradebook.service.journal.TermDraft;
import mthiebi.sgs.gradebook.service.journal.VersionStructure;
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
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Journals as data: created from the UI, named by the user, with columns and
 * formulas that may reach into other journals.
 * <p>
 * The cases worth proving are the ones the old design made impossible - a
 * second journal existing at all, a formula crossing between two, and a cycle
 * spanning them being caught before it can be activated.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JournalService.class, GradeWriteService.class, GradeExplainService.class,
        mthiebi.sgs.gradebook.service.grid.GradeGridService.class,
        TemplateGraphLoader.class, PeriodTreeLoader.class, TemplateVersionResolver.class,
        SpecialValueRegistry.class, QueryFactoryProvider.class,
        mthiebi.sgs.gradebook.service.conversion.GradeConversionService.class})
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.SQLServer2012Dialect",
        "spring.jpa.hibernate.ddl-auto=none"
})
class JournalServiceIT {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private JournalService journalService;

    @Autowired
    private GradeWriteService writeService;

    @Autowired
    private TemplateGraphLoader templateGraphLoader;

    @Autowired
    private mthiebi.sgs.gradebook.service.grid.GradeGridService gridService;

    private GradebookTestData data;

    @BeforeEach
    void setUp() {
        templateGraphLoader.evictAll();
        data = new GradebookTestData(em).build(UUID.randomUUID().toString().substring(0, 6));
    }

    // ---- helpers --------------------------------------------------------

    private JournalView newJournal(String name, JournalFrequency frequency,
                                   boolean subjectScoped) throws SGSException {
        JournalDraft draft = new JournalDraft();
        draft.setName(name);
        draft.setFrequency(frequency);
        draft.setSubjectScoped(subjectScoped);
        JournalView view = journalService.create(draft, data.scheme.getId());
        em.flush();
        return view;
    }

    private ComponentDraft input(String code, String label, int ordinal) {
        ComponentDraft c = new ComponentDraft();
        c.setCode(code);
        c.setLabel(label);
        c.setOrdinal(ordinal);
        c.setKind(ComponentKind.INPUT);
        c.setPeriodKind(PeriodKind.ROLLUP);
        c.setSubjectScoped(false);
        c.setScaleMin(BigDecimal.ZERO);
        c.setScaleMax(new BigDecimal("10"));
        c.setDecimals(0);
        return c;
    }

    private ComponentDraft derived(String code, String label, int ordinal,
                                   List<SourceDraft> sources) {
        ComponentDraft c = input(code, label, ordinal);
        c.setKind(ComponentKind.DERIVED);

        TermDraft term = new TermDraft();
        term.setWeight(BigDecimal.ONE);
        term.setSourceKind(SourceKind.GROUP);
        term.setSources(new ArrayList<>(sources));

        RuleDraft rule = new RuleDraft();
        rule.setType(RuleType.AVERAGE);
        rule.setDecimals(0);
        rule.setTerms(Collections.singletonList(term));
        c.setRule(rule);
        return c;
    }

    private SourceDraft source(String journalUuid, String code) {
        SourceDraft s = new SourceDraft();
        s.setJournalUuid(journalUuid);
        s.setComponentCode(code);
        return s;
    }

    // ---- the wizard ------------------------------------------------------

    @Test
    @DisplayName("a journal is created with a stable id independent of its name")
    void journalHasStableIdentity() throws Exception {
        JournalView created = newJournal("ეთიკური ნორმა", JournalFrequency.MONTH, false);

        assertNotNull(created.getUuid());
        assertEquals(JournalFrequency.MONTH, created.getFrequency());
        assertFalse(created.isSubjectScoped());

        // The name is the menu label, so renaming has to be free. If the name
        // were the identity every formula pointing at it would break.
        JournalDraft rename = new JournalDraft();
        rename.setName("ქცევის ჟურნალი");
        JournalView renamed = journalService.update(created.getUuid(), rename);

        assertEquals(created.getUuid(), renamed.getUuid());
        assertEquals("ქცევის ჟურნალი", renamed.getName());
    }

    @Test
    @DisplayName("a journal starts as a draft and appears in the menu")
    void newJournalAppearsInTheMenu() throws Exception {
        int before = journalService.list(false).size();
        JournalView created = newJournal("გაცდენები", JournalFrequency.MONTH, false);
        em.flush();

        List<String> names = journalService.list(false).stream()
                .map(JournalView::getName).collect(Collectors.toList());
        assertEquals(before + 1, names.size());
        assertTrue(names.contains("გაცდენები"));

        assertEquals(TemplateVersionStatus.DRAFT,
                journalService.currentStructure(created.getUuid()).getStatus());
    }

    @Test
    @DisplayName("archiving takes it out of the menu without deleting it")
    void archivingHidesWithoutDeleting() throws Exception {
        JournalView created = newJournal("დროებითი", JournalFrequency.ONCE_A_YEAR, false);
        em.flush();

        journalService.archive(created.getUuid(), true);
        em.flush();

        assertFalse(journalService.list(false).stream()
                .anyMatch(j -> j.getUuid().equals(created.getUuid())));
        // Still there, because grades point at it.
        assertNotNull(journalService.get(created.getUuid()));
    }

    // ---- the editor ------------------------------------------------------

    @Test
    @DisplayName("columns are saved and read back")
    void columnsRoundTrip() throws Exception {
        JournalView journal = newJournal("ქცევა", JournalFrequency.MONTH, false);
        VersionStructure structure = journalService.currentStructure(journal.getUuid());

        SaveResult saved = journalService.save(journal.getUuid(), structure.getVersionId(),
                Arrays.asList(input("W1", "კვირა 1", 0), input("W2", "კვირა 2", 1)));
        em.flush();
        em.clear();

        VersionStructure reloaded = journalService.currentStructure(journal.getUuid());
        assertEquals(2, reloaded.getComponents().size());
        assertEquals("კვირა 1", reloaded.getComponents().get(0).getLabel());
        assertTrue(saved.isActivatable());
    }

    @Test
    @DisplayName("a calculated column keeps its formula through a save")
    void formulasRoundTrip() throws Exception {
        JournalView journal = newJournal("ქცევა", JournalFrequency.MONTH, false);
        VersionStructure structure = journalService.currentStructure(journal.getUuid());

        journalService.save(journal.getUuid(), structure.getVersionId(), Arrays.asList(
                input("W1", "კვირა 1", 0),
                input("W2", "კვირა 2", 1),
                derived("AVG", "საშუალო", 2,
                        Arrays.asList(source(null, "W1"), source(null, "W2")))));
        em.flush();
        em.clear();

        ComponentDraft avg = journalService.currentStructure(journal.getUuid())
                .getComponents().stream()
                .filter(c -> c.getCode().equals("AVG"))
                .findFirst().orElseThrow(AssertionError::new);

        assertEquals(ComponentKind.DERIVED, avg.getKind());
        assertEquals(RuleType.AVERAGE, avg.getRule().getType());
        assertEquals(2, avg.getRule().getTerms().get(0).getSources().size());
    }

    @Test
    @DisplayName("a column keeps its identity when renamed, so references survive")
    void renamingAColumnKeepsItsIdentity() throws Exception {
        JournalView journal = newJournal("ქცევა", JournalFrequency.MONTH, false);
        VersionStructure structure = journalService.currentStructure(journal.getUuid());

        journalService.save(journal.getUuid(), structure.getVersionId(),
                Collections.singletonList(input("W1", "კვირა 1", 0)));
        em.flush();

        Long idBefore = componentId(structure.getVersionId(), "W1");

        ComponentDraft renamed = input("W1", "პირველი კვირა", 0);
        journalService.save(journal.getUuid(), structure.getVersionId(),
                Collections.singletonList(renamed));
        em.flush();

        // Diffed by code, not replaced wholesale - a delete-and-recreate would
        // orphan every cross-journal formula pointing at this column.
        assertEquals(idBefore, componentId(structure.getVersionId(), "W1"));
    }

    // ---- cross-journal ---------------------------------------------------

    @Test
    @DisplayName("the picker offers every journal and every column")
    void pickerListsEveryJournalsColumns() throws Exception {
        JournalView ethics = newJournal("ქცევა", JournalFrequency.TRIMESTER, false);
        VersionStructure s = journalService.currentStructure(ethics.getUuid());
        journalService.save(ethics.getUuid(), s.getVersionId(),
                Collections.singletonList(input("BEHAVIOUR", "ქცევა", 0)));
        em.flush();

        List<ColumnRef> columns = journalService.pickableColumns(ethics.getUuid());

        assertTrue(columns.stream().anyMatch(c -> "BEHAVIOUR".equals(c.getComponentCode())));
        // The fixture's academic journal is there too, and it is on the same
        // frequency - so a reference to it needs no period question.
        ColumnRef academic = columns.stream()
                .filter(c -> "TRIMESTER_GRADE".equals(c.getComponentCode()))
                .findFirst().orElseThrow(AssertionError::new);
        assertTrue(academic.isSameFrequencyAsCaller());
    }

    @Test
    @DisplayName("a formula may read a column in another journal")
    void formulaReadsAnotherJournal() throws Exception {
        JournalView ethics = newJournal("ქცევა", JournalFrequency.TRIMESTER, false);
        VersionStructure s = journalService.currentStructure(ethics.getUuid());

        // ETHICS.MIRROR = the academic journal's trimester grade.
        SaveResult saved = journalService.save(ethics.getUuid(), s.getVersionId(), Arrays.asList(
                input("BEHAVIOUR", "ქცევა", 0),
                derived("MIRROR", "აკადემიური", 1,
                        Collections.singletonList(
                                source(data.template.getUuid(), "TRIMESTER_GRADE")))));
        em.flush();
        em.clear();

        // It validates: a cross-journal source is a real reference, not a
        // dangling one, because the graph spans both journals.
        assertTrue(saved.getIssues().stream()
                        .noneMatch(i -> i.getSeverity() == ValidationIssue.Severity.ERROR),
                saved.getIssues().toString());

        ComponentDraft mirror = journalService.currentStructure(ethics.getUuid())
                .getComponents().stream()
                .filter(c -> c.getCode().equals("MIRROR"))
                .findFirst().orElseThrow(AssertionError::new);

        SourceDraft ref = mirror.getRule().getTerms().get(0).getSources().get(0);
        assertEquals(data.template.getUuid(), ref.getJournalUuid());
        assertEquals("TRIMESTER_GRADE", ref.getComponentCode());
    }

    @Test
    @DisplayName("a cycle spanning two journals is caught before activation")
    void cycleAcrossJournalsIsCaught() throws Exception {
        JournalView a = newJournal("A", JournalFrequency.TRIMESTER, false);
        JournalView b = newJournal("B", JournalFrequency.TRIMESTER, false);

        VersionStructure sa = journalService.currentStructure(a.getUuid());
        VersionStructure sb = journalService.currentStructure(b.getUuid());

        journalService.save(a.getUuid(), sa.getVersionId(),
                Collections.singletonList(input("X", "X", 0)));
        em.flush();

        // B.Y reads A.X
        journalService.save(b.getUuid(), sb.getVersionId(),
                Collections.singletonList(
                        derived("Y", "Y", 0, Collections.singletonList(source(a.getUuid(), "X")))));
        em.flush();

        // ...and now A.X is made to read B.Y, closing the loop across journals.
        SaveResult result = journalService.save(a.getUuid(), sa.getVersionId(),
                Collections.singletonList(
                        derived("X", "X", 0, Collections.singletonList(source(b.getUuid(), "Y")))));
        em.flush();

        assertFalse(result.isActivatable(), "a cycle must block activation");
        assertThrows(SGSException.class,
                () -> journalService.activate(a.getUuid(), sa.getVersionId()));
    }

    // ---- versions --------------------------------------------------------

    @Test
    @DisplayName("editing a version that holds marks forks a draft instead")
    void editingAVersionWithDataForks() throws Exception {
        // The fixture's journal already has marks entered against version 1.
        GradeWriteRequest write = new GradeWriteRequest();
        write.setJournalUuid(data.template.getUuid());
        write.setClassGroupId(data.classGroup.getId());
        write.setSubjectId(data.subject.getId());
        write.setPeriodId(data.trimester1.getId());
        GradeEntryUpdate u = new GradeEntryUpdate();
        u.setEnrollmentId(data.enrollments.get(0).getId());
        u.setComponentCode("ONGOING_1");
        u.setValue(new BigDecimal("7"));
        write.setEntries(Collections.singletonList(u));
        writeService.apply(write, 1L);
        em.flush();

        VersionStructure before = journalService.currentStructure(data.template.getUuid());
        assertFalse(before.isEditableInPlace(), "a version with marks is not edited in place");

        List<ComponentDraft> columns = new ArrayList<>(before.getComponents());
        columns.add(input("EXTRA", "დამატებითი", 99));

        SaveResult saved = journalService.save(data.template.getUuid(),
                before.getVersionId(), columns);
        em.flush();

        // Editing in place would silently re-render marks already entered.
        assertTrue(saved.isForked());
        assertEquals(TemplateVersionStatus.DRAFT, saved.getVersion().getStatus());
        assertTrue(saved.getVersion().getVersionNo() > before.getVersionNo());
    }

    @Test
    @DisplayName("activating leaves existing marks on the version they were entered under")
    void activationDoesNotDisturbExistingPeriods() throws Exception {
        GradeWriteRequest write = new GradeWriteRequest();
        write.setJournalUuid(data.template.getUuid());
        write.setClassGroupId(data.classGroup.getId());
        write.setSubjectId(data.subject.getId());
        write.setPeriodId(data.trimester1.getId());
        GradeEntryUpdate u = new GradeEntryUpdate();
        u.setEnrollmentId(data.enrollments.get(0).getId());
        u.setComponentCode("ONGOING_1");
        u.setValue(new BigDecimal("7"));
        write.setEntries(Collections.singletonList(u));
        writeService.apply(write, 1L);
        em.flush();

        Long originalVersion = data.version.getId();

        VersionStructure before = journalService.currentStructure(data.template.getUuid());
        List<ComponentDraft> columns = new ArrayList<>(before.getComponents());
        columns.add(input("EXTRA", "დამატებითი", 99));
        SaveResult forked = journalService.save(data.template.getUuid(),
                before.getVersionId(), columns);
        journalService.activate(data.template.getUuid(), forked.getVersion().getVersionId());
        em.flush();
        em.clear();

        // The mark stays pinned. Activation reaches future periods only;
        // moving this one is a separate act with a recalculation attached.
        GradeEntry entry = em.createQuery(
                        "select g from GradeEntry g where g.enrollment.id = :e "
                                + "and g.component.code = 'ONGOING_1' and g.period.id = :p",
                        GradeEntry.class)
                .setParameter("e", data.enrollments.get(0).getId())
                .setParameter("p", data.trimester1.getId())
                .getSingleResult();

        assertEquals(originalVersion, entry.getTemplateVersion().getId());
    }

    @Test
    @DisplayName("a journal's columns appear only at the level it is filled in on")
    void columnsAppearOnlyAtTheJournalsOwnLevel() throws Exception {
        // Months and weeks are both PeriodKind.REPORTING, so matching on kind
        // would have shown a monthly journal's columns on all forty weeks too.
        // The journal's frequency maps to a depth instead.
        List<mthiebi.sgs.gradebook.model.Period> periods = em.createQuery(
                        "select p from Period p where p.scheme.id = :s",
                        mthiebi.sgs.gradebook.model.Period.class)
                .setParameter("s", data.scheme.getId()).getResultList();

        // The fixture's scheme has a year and two trimesters, so a trimester
        // journal sees depth 1 and a monthly one would see nothing here.
        assertTrue(periods.stream().anyMatch(p -> p.getDepth() == 1));
        assertEquals(JournalFrequency.TRIMESTER.getDepth(), 1);
        assertEquals(JournalFrequency.MONTH.getDepth(), 2);
        assertEquals(JournalFrequency.DAY.getDepth(), 3);
        assertEquals(JournalFrequency.ONCE_A_YEAR.getDepth(), 0);
    }

    // ---- the path a user actually takes ---------------------------------

    @Test
    @DisplayName("a journal made in the wizard can actually be opened")
    void wizardJournalOpensAGrid() throws Exception {
        // Nothing used to create a TemplateAssignment outside the test fixture,
        // so every wizard-made journal failed on grid open, forever. The old
        // tests missed it by hand-building the assignment themselves.
        JournalView journal = newJournal("გაცდენები", JournalFrequency.TRIMESTER, false);
        VersionStructure structure = journalService.currentStructure(journal.getUuid());
        journalService.save(journal.getUuid(), structure.getVersionId(),
                Collections.singletonList(input("DAYS", "დღეები", 0)));
        journalService.activate(journal.getUuid(), structure.getVersionId());
        em.flush();
        em.clear();

        mthiebi.sgs.gradebook.service.grid.GradeGrid grid = gridService.load(
                data.classGroup.getId(), null, data.trimester1.getId(), journal.getUuid());

        assertEquals(1, grid.getColumns().size());
        assertEquals("DAYS", grid.getColumns().get(0).getCode());
        assertFalse(grid.getStudents().isEmpty());
    }

    @Test
    @DisplayName("activating a new version moves empty periods onto it")
    void activationRepointsEmptyPeriods() throws Exception {
        JournalView journal = newJournal("ტესტი", JournalFrequency.TRIMESTER, false);
        VersionStructure v1 = journalService.currentStructure(journal.getUuid());
        journalService.save(journal.getUuid(), v1.getVersionId(),
                Collections.singletonList(input("A", "A", 0)));
        journalService.activate(journal.getUuid(), v1.getVersionId());
        em.flush();

        // An assignment names a specific version, so leaving it alone would
        // keep empty periods resolving the superseded one.
        VersionStructure v2 = journalService.currentStructure(journal.getUuid());
        journalService.save(journal.getUuid(), v2.getVersionId(),
                Arrays.asList(input("A", "A", 0), input("B", "B", 1)));
        journalService.activate(journal.getUuid(), v2.getVersionId());
        em.flush();
        em.clear();

        assertEquals(2, gridService.load(data.classGroup.getId(), null,
                data.trimester1.getId(), journal.getUuid()).getColumns().size());
    }

    @Test
    @DisplayName("saving a mark twice does not invent a conflict")
    void correctingTheSameCellTwiceWorks() throws Exception {
        // @Version increments at flush, not at save(), so the first response
        // used to carry the pre-increment number - and the second correction
        // was then judged against a version the database had moved past.
        Long enrollment = data.enrollments.get(0).getId();

        GradeWriteResult first = write(enrollment, "ONGOING_1", "5", null);
        Integer v1 = first.getApplied().get(0).getRowVersion();

        GradeWriteResult second = write(enrollment, "ONGOING_1", "6", v1);
        assertTrue(second.getConflicts().isEmpty(), second.getConflicts().toString());
        Integer v2 = second.getApplied().get(0).getRowVersion();

        GradeWriteResult third = write(enrollment, "ONGOING_1", "7", v2);
        assertTrue(third.getConflicts().isEmpty(),
                "a third correction must not conflict: " + third.getConflicts());
    }

    @Test
    @DisplayName("a value outside the column's scale is refused, not stored")
    void valuesOutsideTheScaleAreRefused() throws Exception {
        Long enrollment = data.enrollments.get(0).getId();
        GradeWriteResult result = write(enrollment, "ONGOING_1", "999", null);

        assertTrue(result.getApplied().isEmpty());
        assertEquals(1, result.getConflicts().size());
        assertEquals(mthiebi.sgs.gradebook.service.CellRejectionReason.OUT_OF_RANGE,
                result.getConflicts().get(0).getReason());
    }

    @Test
    @DisplayName("an undeclared special code is refused")
    void unknownSpecialCodesAreRefused() throws Exception {
        GradeWriteRequest request = new GradeWriteRequest();
        request.setJournalUuid(data.template.getUuid());
        request.setClassGroupId(data.classGroup.getId());
        request.setSubjectId(data.subject.getId());
        request.setPeriodId(data.trimester1.getId());
        GradeEntryUpdate u = new GradeEntryUpdate();
        u.setEnrollmentId(data.enrollments.get(0).getId());
        u.setComponentCode("ONGOING_1");
        // What the console used to send for a typed ჩთ: the uppercase of the
        // Georgian, not the code the template declares.
        u.setSpecialValue("ჩთ".toUpperCase());
        request.setEntries(Collections.singletonList(u));

        GradeWriteResult result = writeService.apply(request, 1L);
        assertEquals(1, result.getConflicts().size());
        assertEquals(mthiebi.sgs.gradebook.service.CellRejectionReason.INVALID_VALUE,
                result.getConflicts().get(0).getReason());
    }

    private GradeWriteResult write(Long enrollment, String code, String value,
                                   Integer expectedVersion) throws Exception {
        GradeWriteRequest request = new GradeWriteRequest();
        request.setJournalUuid(data.template.getUuid());
        request.setClassGroupId(data.classGroup.getId());
        request.setSubjectId(data.subject.getId());
        request.setPeriodId(data.trimester1.getId());
        GradeEntryUpdate u = new GradeEntryUpdate();
        u.setEnrollmentId(enrollment);
        u.setComponentCode(code);
        u.setValue(new BigDecimal(value));
        u.setExpectedVersion(expectedVersion);
        request.setEntries(Collections.singletonList(u));
        GradeWriteResult result = writeService.apply(request, 1L);
        em.flush();
        return result;
    }

    private Long componentId(Long versionId, String code) {
        return em.createQuery(
                        "select c.id from GradeComponent c "
                                + "where c.templateVersion.id = :v and c.code = :c", Long.class)
                .setParameter("v", versionId).setParameter("c", code).getSingleResult();
    }
}
