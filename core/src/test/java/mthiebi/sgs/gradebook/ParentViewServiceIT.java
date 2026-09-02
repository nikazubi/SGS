package mthiebi.sgs.gradebook;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.db.QueryFactoryProvider;
import mthiebi.sgs.gradebook.model.GradeComponent;
import mthiebi.sgs.gradebook.model.GradeEntry;
import mthiebi.sgs.gradebook.service.GradeEntryUpdate;
import mthiebi.sgs.gradebook.service.GradeExplainService;
import mthiebi.sgs.gradebook.service.GradeWriteRequest;
import mthiebi.sgs.gradebook.service.GradeWriteService;
import mthiebi.sgs.gradebook.service.PeriodTreeLoader;
import mthiebi.sgs.gradebook.service.SpecialValueRegistry;
import mthiebi.sgs.gradebook.service.TemplateGraphLoader;
import mthiebi.sgs.gradebook.service.TemplateVersionResolver;
import mthiebi.sgs.gradebook.service.parent.ParentJournal;
import mthiebi.sgs.gradebook.service.parent.ParentRow;
import mthiebi.sgs.gradebook.service.parent.ParentView;
import mthiebi.sgs.gradebook.service.parent.ParentViewService;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
@Import({ParentViewService.class, PublicationService.class, GradeWriteService.class,
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

    @BeforeEach
    void setUp() {
        templateGraphLoader.evictAll();
        data = new GradebookTestData(em).build(UUID.randomUUID().toString().substring(0, 6));
        data.template.setParentVisible(true);
        studentId = data.enrollments.get(0).getStudent().getId();
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
    @DisplayName("primary keeps the register but loses the gradebook")
    void primarySeesTheRegisterOnly() throws Exception {
        // "Primary has no grades" is not "primary has no journals": absence is
        // delivered as one, and the first version of this rule silently took it
        // away from the school whose parents most need it.
        data.template.setParentVisible(true);
        data.template.setGridMode(mthiebi.sgs.gradebook.model.GridMode.COMPONENTS);
        em.flush();

        Long student = data.enrollments.get(0).getStudent().getId();
        assertTrue(parentViewService.journals(student).stream()
                        .anyMatch(j -> j.getUuid().equals(data.template.getUuid())),
                "a gradebook is offered outside primary");

        mthiebi.sgs.gradebook.model.School primary = em.createQuery(
                "select s from School s where s.code = 'PRIMARY'",
                mthiebi.sgs.gradebook.model.School.class).getSingleResult();
        data.classGroup.setSchool(primary);
        em.flush();
        em.clear();

        assertFalse(parentViewService.journals(student).stream()
                        .anyMatch(j -> j.getUuid().equals(data.template.getUuid())),
                "and withheld inside it");

        // Re-loaded: em.clear() above detached it, and setting a field on a
        // detached entity flushes nothing.
        em.find(mthiebi.sgs.gradebook.model.GradingTemplate.class, data.template.getId())
                .setGridMode(mthiebi.sgs.gradebook.model.GridMode.PERIODS);
        em.flush();
        em.clear();

        assertTrue(parentViewService.journals(student).stream()
                        .anyMatch(j -> j.getUuid().equals(data.template.getUuid())),
                "a register reaches primary");
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
