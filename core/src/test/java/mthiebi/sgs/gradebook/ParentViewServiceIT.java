package mthiebi.sgs.gradebook;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.db.QueryFactoryProvider;
import mthiebi.sgs.gradebook.model.GradeComponent;
import mthiebi.sgs.gradebook.model.GradeEntry;
import mthiebi.sgs.gradebook.model.ParentModule;
import mthiebi.sgs.gradebook.model.ParentModuleGrant;
import mthiebi.sgs.gradebook.service.*;
import mthiebi.sgs.gradebook.service.parent.*;
import mthiebi.sgs.gradebook.service.publish.GuardianNotifier;
import mthiebi.sgs.gradebook.service.publish.PublicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The parent portal.
 * <p>
 * The guarantee worth proving is negative: a parent must never see a working
 * value. Everything else - which rows, which columns - follows from the journal
 * and is checked here so that a journal the school invents next year appears
 * without anyone writing a page for it.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ParentViewService.class, ParentModuleService.class, PublicationService.class,
        GradeWriteService.class,
        GradeExplainService.class, TemplateGraphLoader.class, PeriodTreeLoader.class,
        TemplateVersionResolver.class, SpecialValueRegistry.class, QueryFactoryProvider.class,
        mthiebi.sgs.gradebook.service.conversion.GradeConversionService.class})
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.SQLServer2012Dialect",
        "spring.jpa.hibernate.ddl-auto=none"
})
class ParentViewServiceIT {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private ParentViewService parentViewService;

    @Autowired
    private PublicationService publicationService;

    @Autowired
    private GradeWriteService writeService;

    @Autowired
    private TemplateGraphLoader templateGraphLoader;

    @MockBean
    private GuardianNotifier guardianNotifier;

    private GradebookTestData data;
    private Long studentId;
    private ParentModuleGrant gradesGrant;

    @BeforeEach
    void setUp() {
        templateGraphLoader.evictAll();
        data = new GradebookTestData(em).build(UUID.randomUUID().toString().substring(0, 6));
        data.template.setParentVisible(true);
        studentId = data.enrollments.get(0).getStudent().getId();

        // The fixture builds a school of its own, which no seeded rule names.
        // It stands in for basic, so it grades - every level of it.
        gradesGrant = new ParentModuleGrant();
        gradesGrant.setSchool(data.classGroup.getSchool());
        gradesGrant.setModule(ParentModule.GRADES);
        em.persist(gradesGrant);
        em.flush();
    }

    private boolean offered(Long student) throws SGSException {
        return parentViewService.journals(student).stream()
                .anyMatch(j -> j.getUuid().equals(data.template.getUuid()));
    }

    private void enter(String code, String value) throws Exception {
        GradeWriteRequest request = new GradeWriteRequest();
        request.setJournalUuid(data.template.getUuid());
        request.setClassGroupId(data.classGroup.getId());
        request.setSubjectId(data.subject.getId());
        request.setPeriodId(data.trimester1.getId());
        GradeEntryUpdate u = new GradeEntryUpdate();
        u.setEnrollmentId(data.enrollments.get(0).getId());
        u.setComponentCode(code);
        u.setValue(new BigDecimal(value));
        request.setEntries(Collections.singletonList(u));
        writeService.apply(request, 1L);
        em.flush();
    }

    private void publish() throws SGSException {
        publicationService.publish(data.classGroup.getId(), data.trimester1.getId(), null, 1L);
        em.flush();
        em.clear();
    }

    private ParentView view() throws SGSException {
        return parentViewService.view(studentId, data.template.getUuid(),
                data.trimester1.getId(), null);
    }

    // ---- the guarantee ---------------------------------------------------

    @Test
    @DisplayName("an unpublished mark is invisible to a parent")
    void unpublishedMarksAreInvisible() throws Exception {
        enter("ONGOING_1", "7");
        em.clear();

        ParentView view = view();
        ParentRow row = view.getRows().stream()
                .filter(r -> data.subject.getId().equals(r.getSubjectId()))
                .findFirst().orElseThrow(AssertionError::new);

        // The mark exists and the teacher can see it. The parent cannot.
        assertEquals("", row.getValues().get("ONGOING_1"));
    }

    @Test
    @DisplayName("after publication the parent sees it")
    void publishedMarksAreVisible() throws Exception {
        enter("ONGOING_1", "7");
        publish();

        ParentRow row = view().getRows().stream()
                .filter(r -> data.subject.getId().equals(r.getSubjectId()))
                .findFirst().orElseThrow(AssertionError::new);

        assertEquals("7.00", row.getValues().get("ONGOING_1"));
    }

    @Test
    @DisplayName("a mark edited after publication still shows what parents were shown")
    void editsAfterPublicationDoNotLeak() throws Exception {
        enter("ONGOING_1", "7");
        publish();

        // The legacy portal filtered on createTime, which never moved when a row
        // was updated in place - so this edit reached parents immediately, with
        // no publication and no director.
        GradeEntry entry = em.createQuery(
                        "select g from GradeEntry g where g.enrollment.id = :e "
                                + "and g.component.code = 'ONGOING_1' and g.period.id = :p",
                        GradeEntry.class)
                .setParameter("e", data.enrollments.get(0).getId())
                .setParameter("p", data.trimester1.getId())
                .getSingleResult();
        entry.setValue(new BigDecimal("2"));
        em.flush();
        em.clear();

        ParentRow row = view().getRows().stream()
                .filter(r -> data.subject.getId().equals(r.getSubjectId()))
                .findFirst().orElseThrow(AssertionError::new);

        assertEquals("7.00", row.getValues().get("ONGOING_1"),
                "the parent must still see the published value");
    }

    @Test
    @DisplayName("two students may share a username; the pair with the password is what identifies one")
    void usernamesMayRepeatButTheLoginPairMayNot() throws Exception {
        // The school's rule. A username on its own is not an identity, which is
        // why the parent token carries the student id - resolving by username
        // would serve one family another's child.
        mthiebi.sgs.gradebook.model.Student first = data.enrollments.get(0).getStudent();
        mthiebi.sgs.gradebook.model.Student second = data.enrollments.get(1).getStudent();

        second.setUsername(first.getUsername());
        second.setPasswordHash(first.getPasswordHash() + "X");
        em.flush();

        enter("ONGOING_1", "7");
        publish();

        // Same username, different students, and each view is the right child.
        assertEquals("7.00", parentViewService.view(first.getId(), data.template.getUuid(),
                data.trimester1.getId(), null).getRows().get(0).getValues().get("ONGOING_1"));
        assertEquals("", parentViewService.view(second.getId(), data.template.getUuid(),
                data.trimester1.getId(), null).getRows().get(0).getValues().get("ONGOING_1"));
    }

    // ---- shape -----------------------------------------------------------

    @Test
    @DisplayName("a per-subject journal lists subjects as rows and offers its periods")
    void subjectJournalListsSubjects() throws Exception {
        enter("ONGOING_1", "7");
        publish();

        ParentView view = view();
        assertTrue(view.isSubjectScoped());
        assertFalse(view.getPeriods().isEmpty(), "a period picker is needed when rows are subjects");
        assertEquals(1, view.getRows().size());
        assertEquals(data.subject.getName(), view.getRows().get(0).getLabel());
    }

    @Test
    @DisplayName("narrowing to one subject gives the single row that renders as cards")
    void narrowingToOneSubjectGivesOneRow() throws Exception {
        enter("ONGOING_1", "7");
        publish();

        ParentView view = parentViewService.view(studentId, data.template.getUuid(),
                data.trimester1.getId(), data.subject.getId());

        // One row is what the console draws as cards - a one-row table is an
        // ugly way to show one thing, so the layout follows the data.
        assertEquals(1, view.getRows().size());
    }

    @Test
    @DisplayName("empty columns are listed, so a parent sees what is still to come")
    void blankColumnsAreShown() throws Exception {
        enter("ONGOING_1", "7");
        publish();

        ParentRow row = view().getRows().get(0);
        // Every column of the journal appears, whether or not it holds a value.
        assertTrue(row.getValues().containsKey("ONGOING_5"));
        assertEquals("", row.getValues().get("ONGOING_5"));
        assertTrue(row.getValues().size() >= 10);
    }

    // ---- visibility ------------------------------------------------------

    @Test
    @DisplayName("a journal the school has not released is refused")
    void hiddenJournalsAreRefused() {
        data.template.setParentVisible(false);
        em.flush();

        // Not merely absent from the list: a uuid is easy enough to guess at.
        assertThrows(SGSException.class, this::view);
    }

    @Test
    @DisplayName("a level range grants grades inside it and nowhere else")
    void gradesFollowTheLevelRange() throws Exception {
        // The school's case: grades for two years of a school, not all of it.
        gradesGrant.setLevelFrom((short) 5);
        gradesGrant.setLevelTo((short) 6);

        for (short level = 4; level <= 7; level++) {
            data.classGroup.setLevel(level);
            em.flush();
            assertEquals(level == 5 || level == 6, offered(studentId), "level " + level);
        }
    }

    @Test
    @DisplayName("a class saved without a level matches no ranged rule")
    void unsetLevelMatchesNoRange() throws Exception {
        // A class saved with საფეხური blank has level 0. Pinned so the gap is
        // known rather than discovered: a grade 5 class entered without its
        // level gets no grades from a 5-6 rule.
        gradesGrant.setLevelFrom((short) 5);
        gradesGrant.setLevelTo((short) 6);
        data.classGroup.setLevel((short) 0);
        em.flush();

        assertFalse(offered(studentId));
    }

    @Test
    @DisplayName("the gradebook's address is refused where the list withholds it")
    void withheldGradebookIsRefusedByUuid() throws Exception {
        // Hiding the box is not enough: a uuid is what a shared link carries.
        em.remove(gradesGrant);
        em.flush();

        assertFalse(offered(studentId));
        assertThrows(SGSException.class, this::view);
    }

    /**
     * Moves the fixture's class into the seeded primary school. Its level is 9,
     * which db/038's primary GRADES row (5-6) does not cover.
     */
    private void asPrimary() {
        mthiebi.sgs.gradebook.model.School primary = em.createQuery(
                "select s from School s where s.code = 'PRIMARY'",
                mthiebi.sgs.gradebook.model.School.class).getSingleResult();
        data.classGroup.setSchool(primary);
        em.flush();
        em.clear();
    }

    /**
     * Re-loaded: asPrimary() clears the context, and setting a field on a
     * detached entity flushes nothing.
     */
    private void gateJournalOn(ParentModule module) {
        em.find(mthiebi.sgs.gradebook.model.GradingTemplate.class, data.template.getId())
                .setParentModule(module);
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("primary keeps the absence register but loses the gradebook")
    void primarySeesTheRegisterOnly() throws Exception {
        // "Primary has no grades" is not "primary has no journals": absence is
        // delivered as one, and the first version of this rule silently took it
        // away from the school whose parents most need it.
        assertEquals(ParentModule.GRADES, data.template.getParentModule(),
                "a new journal is gated until somebody says otherwise");
        assertTrue(offered(studentId), "a gradebook is offered outside primary");

        asPrimary();
        assertFalse(offered(studentId), "and withheld inside it");

        // The absence register names no module.
        gateJournalOn(null);
        assertTrue(offered(studentId), "a journal naming no module reaches primary");
    }

    @Test
    @DisplayName("a journal's layout does not decide who sees it")
    void layoutDoesNotOpenAJournal() throws Exception {
        // Ethics: drawn periods-across exactly like the absence register, and
        // gated on GRADES all the same. Reading the layout as "a register, for
        // everyone" is what used to offer it to every primary child.
        em.find(mthiebi.sgs.gradebook.model.GradingTemplate.class, data.template.getId())
                .setGridMode(mthiebi.sgs.gradebook.model.GridMode.PERIODS);
        em.flush();

        asPrimary();

        assertFalse(offered(studentId));
        assertThrows(SGSException.class, this::view);
    }

    @Test
    @DisplayName("only released journals appear as boxes")
    void onlyReleasedJournalsAreListed() throws Exception {
        data.template.setParentVisible(false);
        em.flush();

        assertFalse(parentViewService.journals(data.enrollments.get(0).getStudent().getId()).stream()
                .map(ParentJournal::getUuid)
                .anyMatch(uuid -> uuid.equals(data.template.getUuid())));
    }

    @Test
    @DisplayName("a column marked staff-only is not shown to parents")
    void staffOnlyColumnsAreHidden() throws Exception {
        // parentVisible has been on the component since phase 1 and was read by
        // nothing at all, so a column marked staff-only appeared anyway.
        GradeComponent progress = data.components.get("PROGRESS");
        progress.setParentVisible(false);
        em.flush();
        templateGraphLoader.evictAll();

        enter("ONGOING_1", "7");
        publish();

        assertFalse(view().getColumns().stream()
                .anyMatch(c -> "PROGRESS".equals(c.getCode())));
    }

    @Test
    @DisplayName("the view carries the chart the journal names, or none")
    void chartKeyIsCarried() throws Exception {
        enter("ONGOING_1", "7");
        publish();
        assertEquals(null, view().getChartKey());

        // publish() clears the persistence context, so the fixture's journal is
        // detached by now - a setter on it would go nowhere.
        em.createQuery("update GradingTemplate t set t.chartKey = 'GRADE_TREND' "
                        + "where t.uuid = :u")
                .setParameter("u", data.template.getUuid()).executeUpdate();
        em.clear();
        // A journal with no chart still renders a complete page; the key only
        // tells the console which chart to draw over it.
        assertEquals("GRADE_TREND", view().getChartKey());
    }

    @Test
    @DisplayName("one parent cannot read another child")
    void aParentSeesOnlyTheirOwnChild() throws Exception {
        enter("ONGOING_1", "7");
        publish();

        Long other = data.enrollments.get(1).getStudent().getId();
        ParentView view = parentViewService.view(other, data.template.getUuid(),
                data.trimester1.getId(), null);

        // The identity comes from the token, never from a parameter - and the
        // other student has no marks.
        assertEquals("", view.getRows().get(0).getValues().get("ONGOING_1"));
    }
}
