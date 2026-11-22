package mthiebi.sgs.gradebook;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.db.QueryFactoryProvider;
import mthiebi.sgs.gradebook.engine.ValidationIssue;
import mthiebi.sgs.gradebook.model.*;
import mthiebi.sgs.gradebook.service.*;
import mthiebi.sgs.gradebook.service.journal.*;
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
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

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
        mthiebi.sgs.gradebook.service.grid.SummaryBreakdown.class,
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

    private static ComponentDraft byCode(List<ComponentDraft> columns, String code) {
        return columns.stream().filter(c -> c.getCode().equals(code)).findFirst()
                .orElseThrow(() -> new AssertionError("no column " + code));
    }

    /**
     * Deep enough that changing one column's rule leaves the structure the
     * service hands back - and compares against - untouched.
     */
    private static List<ComponentDraft> copies(List<ComponentDraft> columns) {
        List<ComponentDraft> out = new ArrayList<>();
        for (ComponentDraft c : columns) {
            ComponentDraft d = new ComponentDraft();
            d.setCode(c.getCode());
            d.setLabel(c.getLabel());
            d.setOrdinal(c.getOrdinal());
            d.setGroupLabel(c.getGroupLabel());
            d.setKind(c.getKind());
            d.setPeriodKind(c.getPeriodKind());
            d.setSubjectScoped(c.isSubjectScoped());
            d.setScaleMin(c.getScaleMin());
            d.setScaleMax(c.getScaleMax());
            d.setDecimals(c.getDecimals());
            d.setAllowSpecialValues(c.isAllowSpecialValues());
            d.setAllowOverride(c.isAllowOverride());
            d.setParentVisible(c.isParentVisible());
            d.setSummaryColumn(c.getSummaryColumn());
            if (c.getRule() != null) {
                RuleDraft r = new RuleDraft();
                r.setType(c.getRule().getType());
                r.setNullPolicy(c.getRule().getNullPolicy());
                r.setRenormalizeWeights(c.getRule().isRenormalizeWeights());
                r.setRoundingMode(c.getRule().getRoundingMode());
                r.setDecimals(c.getRule().getDecimals());
                for (TermDraft t : c.getRule().getTerms()) {
                    TermDraft u = new TermDraft();
                    u.setWeight(t.getWeight());
                    u.setSourceKind(t.getSourceKind());
                    u.setReduce(t.getReduce());
                    u.setPeriodRef(t.getPeriodRef());
                    u.setPeriodId(t.getPeriodId());
                    u.setLabel(t.getLabel());
                    for (SourceDraft s : t.getSources()) {
                        SourceDraft x = new SourceDraft();
                        x.setJournalUuid(s.getJournalUuid());
                        x.setComponentCode(s.getComponentCode());
                        u.getSources().add(x);
                    }
                    r.getTerms().add(u);
                }
                d.setRule(r);
            }
            out.add(d);
        }
        return out;
    }

    /**
     * A mark typed and then emptied: the version is referenced, so it is not
     * edited in place, but the journal holds no mark this year, so it is not
     * locked either - the state of a journal between school years.
     */
    private void leaveAnEmptiedCell() throws Exception {
        Long enrollment = data.enrollments.get(0).getId();
        put(data.subject.getId(), enrollment, "ONGOING_1", "7");
        put(data.subject.getId(), enrollment, "ONGOING_1", null);
    }

    // ---- the lock once marks exist (November 2026) ------------------------

    @Test
    @DisplayName("before any marks this year, editing a version that holds cells forks a draft")
    void editingAVersionWithDataForks() throws Exception {
        leaveAnEmptiedCell();

        VersionStructure before = journalService.currentStructure(data.template.getUuid());
        assertFalse(before.isEditableInPlace(), "a version with cells is not edited in place");
        assertFalse(before.isHoldsMarks(), "an emptied cell is not a mark");

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
    @DisplayName("activating leaves existing cells on the version they were entered under")
    void activationDoesNotDisturbExistingPeriods() throws Exception {
        leaveAnEmptiedCell();

        Long originalVersion = data.version.getId();

        VersionStructure before = journalService.currentStructure(data.template.getUuid());
        List<ComponentDraft> columns = new ArrayList<>(before.getComponents());
        columns.add(input("EXTRA", "დამატებითი", 99));
        SaveResult forked = journalService.save(data.template.getUuid(),
                before.getVersionId(), columns);
        journalService.activate(data.template.getUuid(), forked.getVersion().getVersionId());
        em.flush();
        em.clear();

        // The cell stays pinned. Activation reaches future periods only;
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
    @DisplayName("with marks this year, a rename and the summary tick save in place, with no new version")
    void withMarksVisualChangesSaveInPlace() throws Exception {
        Long enrollment = data.enrollments.get(0).getId();
        put(data.subject.getId(), enrollment, "ONGOING_1", "7");
        long versionsBefore = versionCount();

        VersionStructure before = journalService.currentStructure(data.template.getUuid());
        assertTrue(before.isHoldsMarks());
        assertEquals(Long.valueOf(1), before.getMarksByCode().get("ONGOING_1"),
                "the editor is told how many marks a column holds, to warn before a rename");

        List<ComponentDraft> columns = new ArrayList<>(before.getComponents());
        ComponentDraft ongoing1 = byCode(columns, "ONGOING_1");
        ongoing1.setLabel("საწყისი ტესტი");
        ongoing1.setGroupLabel("ახალი ჯგუფი");
        byCode(columns, "PROGRESS").setSummaryColumn(true);

        SaveResult saved = journalService.save(data.template.getUuid(), before.getVersionId(), columns);
        em.flush();
        em.clear();

        assertFalse(saved.isForked(), "a visual change is not a new version");
        assertFalse(saved.isActivatable(), "nothing to activate: it is already live");
        assertEquals(versionsBefore, versionCount());

        GradeComponent renamed = em.find(GradeComponent.class, componentId(data.version.getId(), "ONGOING_1"));
        assertEquals("საწყისი ტესტი", renamed.getLabel());
        assertEquals("ახალი ჯგუფი", renamed.getGroupLabel());
        assertTrue(em.find(GradeComponent.class, componentId(data.version.getId(), "PROGRESS")).isSummaryColumn());

        // The mark is untouched: same version, same column, same value.
        GradeEntry mark = markOf(enrollment, "ONGOING_1");
        assertEquals(data.version.getId(), mark.getTemplateVersion().getId());
        assertEquals(0, new BigDecimal("7").compareTo(mark.getValue()));
    }

    @Test
    @DisplayName("with marks this year, adding, removing or changing a column is refused")
    void withMarksStructuralChangesAreRefused() throws Exception {
        put(data.subject.getId(), data.enrollments.get(0).getId(), "ONGOING_1", "7");
        VersionStructure before = journalService.currentStructure(data.template.getUuid());
        long versionsBefore = versionCount();

        List<ComponentDraft> added = new ArrayList<>(before.getComponents());
        added.add(input("EXTRA", "დამატებითი", 99));
        assertRefused(before, added, "დაემატა სვეტი");

        List<ComponentDraft> removed = new ArrayList<>(before.getComponents());
        removed.removeIf(c -> c.getCode().equals("FINAL_TEST"));
        assertRefused(before, removed, "წაიშალა სვეტი");

        List<ComponentDraft> retyped = copies(before.getComponents());
        byCode(retyped, "TRIMESTER_GRADE").setKind(ComponentKind.INPUT);
        byCode(retyped, "TRIMESTER_GRADE").setRule(null);
        assertRefused(before, retyped, "ტიპი");

        List<ComponentDraft> rescaled = copies(before.getComponents());
        byCode(rescaled, "PROGRESS").setScaleMax(new BigDecimal("7"));
        assertRefused(before, rescaled, "შკალა");

        List<ComponentDraft> reweighted = copies(before.getComponents());
        byCode(reweighted, "TRIMESTER_GRADE").getRule().getTerms().get(0).setWeight(new BigDecimal("0.9"));
        assertRefused(before, reweighted, "ფორმულა");

        assertEquals(versionsBefore, versionCount(), "a refused save leaves no draft behind");
    }

    @Test
    @DisplayName("with marks this year, a draft made before them cannot be activated")
    void withMarksNoOtherVersionIsActivated() throws Exception {
        leaveAnEmptiedCell();
        VersionStructure before = journalService.currentStructure(data.template.getUuid());
        List<ComponentDraft> columns = new ArrayList<>(before.getComponents());
        columns.add(input("EXTRA", "დამატებითი", 99));
        SaveResult draft = journalService.save(data.template.getUuid(), before.getVersionId(), columns);
        em.flush();
        assertTrue(draft.isForked());

        // Marks arrive on the live version before anyone activates the draft.
        put(data.subject.getId(), data.enrollments.get(1).getId(), "PROGRESS", "8");

        SGSException refused = assertThrows(SGSException.class, () ->
                journalService.activate(data.template.getUuid(), draft.getVersion().getVersionId()));
        assertTrue(refused.getMessage().contains("შეყვანილია ნიშნები"), refused.getMessage());
    }

    @Test
    @DisplayName("the summary tick round-trips through the editor and survives a fork")
    void theSummaryTickSurvivesAFork() throws Exception {
        em.find(GradeComponent.class, data.components.get("TRIMESTER_GRADE").getId()).setSummaryColumn(true);
        em.flush();
        leaveAnEmptiedCell();

        VersionStructure before = journalService.currentStructure(data.template.getUuid());
        assertEquals(Boolean.TRUE, byCode(before.getComponents(), "TRIMESTER_GRADE").getSummaryColumn());

        // An older console sends no summary tick at all.
        List<ComponentDraft> columns = copies(before.getComponents());
        columns.forEach(c -> c.setSummaryColumn(null));
        columns.add(input("EXTRA", "დამატებითი", 99));
        SaveResult forked = journalService.save(data.template.getUuid(), before.getVersionId(), columns);
        em.flush();
        em.clear();

        assertTrue(forked.isForked());
        GradeComponent copied = em.find(GradeComponent.class,
                componentId(forked.getVersion().getVersionId(), "TRIMESTER_GRADE"));
        assertTrue(copied.isSummaryColumn(),
                "a fork used to start empty and drop every setting the editor did not send");
    }

    @Test
    @DisplayName("a subject starting on a new version keeps every mark on it while another subject stays pinned")
    void aFreshSubjectIsNotFiledUnderAnotherSubjectsVersion() throws Exception {
        // The 7/5 case. Mathematics has a cell on version 1; the journal is then
        // edited and the new version activated; chemistry, with no marks yet,
        // starts on the new version. Its first mark landed there and its second
        // was filed under version 1 - because mathematics held that code there -
        // and the grid refused to open the period.
        Subject chemistry = new Subject();
        chemistry.setName("ქიმია " + UUID.randomUUID().toString().substring(0, 6));
        chemistry.setShortName("ქიმ");
        em.persist(chemistry);
        ClassSubject taught = new ClassSubject();
        taught.setClassGroup(data.classGroup);
        taught.setSubject(chemistry);
        taught.setSortIndex(2);
        em.persist(taught);
        em.flush();

        leaveAnEmptiedCell();
        VersionStructure before = journalService.currentStructure(data.template.getUuid());
        List<ComponentDraft> columns = copies(before.getComponents());
        byCode(columns, "PROGRESS").setLabel("პროგრესი");
        SaveResult draft = journalService.save(data.template.getUuid(), before.getVersionId(), columns);
        journalService.activate(data.template.getUuid(), draft.getVersion().getVersionId());
        em.flush();
        Long v2 = draft.getVersion().getVersionId();

        Long student = data.enrollments.get(0).getId();
        put(data.subject.getId(), student, "PROGRESS", "6");          // mathematics, pinned to v1
        put(chemistry.getId(), student, "ONGOING_1", "9");             // chemistry, first mark: v2
        put(chemistry.getId(), student, "PROGRESS", "9");              // the one that went to v1

        List<Long> versions = em.createQuery(
                        "select distinct g.templateVersion.id from GradeEntry g "
                                + "where g.enrollment.id = :e and g.subject.id = :s and g.period.id = :p",
                        Long.class)
                .setParameter("e", student).setParameter("s", chemistry.getId())
                .setParameter("p", data.trimester1.getId())
                .getResultList();
        assertEquals(Collections.singletonList(v2), versions);

        // And the grid opens.
        assertDoesNotThrow(() -> gridService.load(data.classGroup.getId(), chemistry.getId(),
                data.trimester1.getId(), data.template.getUuid()));
        assertDoesNotThrow(() -> gridService.load(data.classGroup.getId(), data.subject.getId(),
                data.trimester1.getId(), data.template.getUuid()));
    }

    private void assertRefused(VersionStructure before, List<ComponentDraft> columns, String naming) {
        SGSException refused = assertThrows(SGSException.class, () ->
                journalService.save(data.template.getUuid(), before.getVersionId(), columns));
        assertTrue(refused.getMessage().contains(naming), refused.getMessage());
    }

    private void put(Long subjectId, Long enrollment, String code, String value) throws Exception {
        GradeWriteRequest request = new GradeWriteRequest();
        request.setJournalUuid(data.template.getUuid());
        request.setClassGroupId(data.classGroup.getId());
        request.setSubjectId(subjectId);
        request.setPeriodId(data.trimester1.getId());
        GradeEntryUpdate u = new GradeEntryUpdate();
        u.setEnrollmentId(enrollment);
        u.setComponentCode(code);
        u.setValue(value == null ? null : new BigDecimal(value));
        request.setEntries(Collections.singletonList(u));
        GradeWriteResult result = writeService.apply(request, 1L);
        assertTrue(result.getConflicts().isEmpty(), result.getConflicts().toString());
        em.flush();
    }

    private GradeEntry markOf(Long enrollment, String code) {
        return em.createQuery("select g from GradeEntry g where g.enrollment.id = :e "
                        + "and g.component.code = :c and g.period.id = :p", GradeEntry.class)
                .setParameter("e", enrollment).setParameter("c", code)
                .setParameter("p", data.trimester1.getId())
                .getSingleResult();
    }

    private long versionCount() {
        return em.createQuery("select count(v) from TemplateVersion v where v.template.id = :t", Long.class)
                .setParameter("t", data.template.getId()).getSingleResult();
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
