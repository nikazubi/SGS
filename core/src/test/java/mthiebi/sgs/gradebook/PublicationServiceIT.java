package mthiebi.sgs.gradebook;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.db.QueryFactoryProvider;
import mthiebi.sgs.gradebook.model.ChangeRequestStatus;
import mthiebi.sgs.gradebook.model.GradeEntry;
import mthiebi.sgs.gradebook.repository.GradeChangeRequestRepository;
import mthiebi.sgs.gradebook.repository.GradeEntryRepository;
import mthiebi.sgs.gradebook.repository.PublicationRepository;
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
import mthiebi.sgs.gradebook.service.grid.GradeGridService;
import mthiebi.sgs.gradebook.service.publish.ChangeRequestService;
import mthiebi.sgs.gradebook.service.publish.ChangeRequestView;
import mthiebi.sgs.gradebook.service.publish.GuardianNotifier;
import mthiebi.sgs.gradebook.service.publish.PublicationResult;
import mthiebi.sgs.gradebook.service.publish.PublicationService;
import mthiebi.sgs.gradebook.service.publish.RaiseChangeRequest;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Publishing, and changing a grade after it has been published.
 * <p>
 * The flow the school runs on: teachers work privately, the journal is released
 * in a batch, and after that a correction needs the director. What is worth
 * proving is that the release is per cell, that the lock actually holds, and
 * that an approval does not leave parents looking at a corrected mark beside an
 * average computed from the old one.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PublicationService.class, ChangeRequestService.class, GradeWriteService.class,
        GradeGridService.class, GradeExplainService.class, TemplateGraphLoader.class,
        PeriodTreeLoader.class, TemplateVersionResolver.class, SpecialValueRegistry.class,
        QueryFactoryProvider.class,
        mthiebi.sgs.gradebook.service.conversion.GradeConversionService.class,
        // ChangeRequestService queues an absence notice on approval - the one
        // route that reaches a parent once a day is published.
        mthiebi.sgs.gradebook.service.absence.AbsenceNotifier.class,
        mthiebi.sgs.gradebook.service.absence.AbsenceNoticeSender.class})
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.SQLServer2012Dialect",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.properties.hibernate.jdbc.batch_size=50"
})
class PublicationServiceIT {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private PublicationService publicationService;

    @Autowired
    private ChangeRequestService changeRequestService;

    @Autowired
    private GradeWriteService writeService;

    @Autowired
    private GradeEntryRepository gradeEntryRepository;

    @Autowired
    private GradeChangeRequestRepository changeRequestRepository;

    @Autowired
    private PublicationRepository publicationRepository;

    @Autowired
    private TemplateGraphLoader templateGraphLoader;

    @Autowired
    private mthiebi.sgs.gradebook.service.grid.GradeGridService gridService;

    /**
     * Email is a notification, not part of the flow being tested.
     */
    @MockBean
    private GuardianNotifier guardianNotifier;

    /**
     * AbsenceNoticeSender needs one, and no test here should send mail.
     */
    @MockBean
    private mthiebi.sgs.SMTP.EmailService emailService;

    private GradebookTestData data;
    private Long enrollment;

    @BeforeEach
    void setUp() {
        templateGraphLoader.evictAll();
        data = new GradebookTestData(em).build(UUID.randomUUID().toString().substring(0, 6));
        enrollment = data.enrollments.get(0).getId();
    }

    // ---- helpers --------------------------------------------------------

    private GradeWriteRequest request(List<GradeEntryUpdate> entries) {
        GradeWriteRequest r = new GradeWriteRequest();
        r.setJournalUuid(data.template.getUuid());
        r.setClassGroupId(data.classGroup.getId());
        r.setSubjectId(data.subject.getId());
        r.setPeriodId(data.trimester1.getId());
        r.setEntries(entries);
        return r;
    }

    private GradeEntryUpdate update(String code, String value) {
        GradeEntryUpdate u = new GradeEntryUpdate();
        u.setEnrollmentId(enrollment);
        u.setComponentCode(code);
        u.setValue(value == null ? null : new BigDecimal(value));
        return u;
    }

    private GradeEntry entry(String code) {
        return em.createQuery(
                        "select g from GradeEntry g where g.enrollment.id = :e "
                                + "and g.component.code = :c and g.period.id = :p", GradeEntry.class)
                .setParameter("e", enrollment)
                .setParameter("c", code)
                .setParameter("p", data.trimester1.getId())
                .getSingleResult();
    }

    private PublicationResult publish() throws SGSException {
        PublicationResult result = publicationService.publish(
                data.classGroup.getId(), data.trimester1.getId(), null, 1L);
        em.flush();
        em.clear();
        return result;
    }

    private void enterMarks() throws Exception {
        writeService.apply(request(Arrays.asList(
                update("ONGOING_1", "6"),
                update("ONGOING_2", "6"),
                update("ONGOING_3", "6"),
                update("INITIAL_KNOWLEDGE", "5"),
                update("FINAL_TEST", "9"))), 1L);
        em.flush();
    }

    // ---- publish --------------------------------------------------------

    @Test
    @DisplayName("publishing copies the working values into what parents see")
    void publishCopiesValues() throws Exception {
        enterMarks();
        PublicationResult result = publish();

        assertTrue(result.getReleased() > 0);
        assertNotNull(result.getPublicationId());

        GradeEntry ongoing = entry("ONGOING_1");
        assertTrue(ongoing.isPublished());
        assertEquals(0, ongoing.getPublishedValue().compareTo(new BigDecimal("6.00")));

        // Calculated cells are released too - they are what parents actually read.
        GradeEntry trimester = entry("TRIMESTER_GRADE");
        assertTrue(trimester.isPublished());
        assertEquals(0, trimester.getPublishedValue().compareTo(trimester.getValue()));
    }

    @Test
    @DisplayName("a blank cell is not published, so it stays editable")
    void blankCellsAreNotPublished() throws Exception {
        enterMarks();
        publish();

        // ONGOING_4 was never entered. Publishing it would stamp published_at on
        // a cell that never held a value and lock out the teacher who still has
        // to fill it.
        List<GradeEntry> blank = em.createQuery(
                        "select g from GradeEntry g where g.enrollment.id = :e "
                                + "and g.component.code = 'ONGOING_4'", GradeEntry.class)
                .setParameter("e", enrollment).getResultList();
        assertTrue(blank.isEmpty(), "a blank cell should not even have a row");

        GradeWriteResult result = writeService.apply(request(
                Collections.singletonList(update("ONGOING_4", "8"))), 1L);
        assertTrue(result.getConflicts().isEmpty(), "filling a blank must not need a director");
        assertEquals(1, result.getApplied().size());
    }

    @Test
    @DisplayName("republishing releases only what has actually moved")
    void republishingReleasesOnlyChanges() throws Exception {
        enterMarks();
        publish();

        PublicationResult second = publish();
        assertEquals(0, second.getReleased(), "nothing changed, so nothing should be released");
        assertTrue(second.getInScope() > 0);
    }

    @Test
    @DisplayName("the release is logged for the audit list")
    void publishIsLogged() throws Exception {
        enterMarks();
        publish();

        assertEquals(1, publicationRepository.findLog(data.classGroup.getId()).size());
    }

    // ---- change requests ------------------------------------------------

    @Test
    @DisplayName("a request cannot be raised for a cell nobody has published")
    void cannotRequestAnUnpublishedCell() throws Exception {
        enterMarks();
        em.clear();

        RaiseChangeRequest raise = new RaiseChangeRequest();
        raise.setGradeEntryId(entry("ONGOING_1").getId());
        raise.setRequestedValue(new BigDecimal("9"));
        raise.setReason("შეცდომა");

        // It is simply editable - a request would queue work the teacher can do.
        assertThrows(SGSException.class, () -> changeRequestService.raise(raise, 1L));
    }

    @Test
    @DisplayName("a request needs a reason")
    void requestNeedsAReason() throws Exception {
        enterMarks();
        publish();

        RaiseChangeRequest raise = new RaiseChangeRequest();
        raise.setGradeEntryId(entry("ONGOING_1").getId());
        raise.setRequestedValue(new BigDecimal("9"));
        raise.setReason("   ");

        assertThrows(SGSException.class, () -> changeRequestService.raise(raise, 1L));
    }

    @Test
    @DisplayName("rejecting changes nothing")
    void rejectionChangesNothing() throws Exception {
        enterMarks();
        publish();

        Long entryId = entry("ONGOING_1").getId();
        ChangeRequestView raised = raise(entryId, "9", "შეცდომით შევიყვანე");

        changeRequestService.decide(raised.getId(), false, "არ დასტურდება", 2L);
        em.flush();
        em.clear();

        GradeEntry after = entry("ONGOING_1");
        assertEquals(0, after.getValue().compareTo(new BigDecimal("6.00")));
        assertEquals(0, after.getPublishedValue().compareTo(new BigDecimal("6.00")));
        assertEquals(ChangeRequestStatus.REJECTED,
                changeRequestRepository.findById(raised.getId()).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("approving writes the value and releases everything it moved")
    void approvalRepublishesTheDependencyClosure() throws Exception {
        enterMarks();
        publish();

        // ongoing average = 6, trimester = 0.5*6 + 0.2*5 + 0.3*9 = 6.7
        GradeEntry trimesterBefore = entry("TRIMESTER_GRADE");
        assertEquals(0, trimesterBefore.getPublishedValue().compareTo(new BigDecimal("6.7")));

        Long entryId = entry("ONGOING_1").getId();
        ChangeRequestView raised = raise(entryId, "9", "ნაშრომი ხელახლა შემოწმდა");

        changeRequestService.decide(raised.getId(), true, "დასტურდება", 2L);
        em.flush();
        em.clear();

        // ongoing average = (9+6+6)/3 = 7, trimester = 0.5*7 + 0.2*5 + 0.3*9 = 7.2
        GradeEntry ongoing = entry("ONGOING_1");
        assertEquals(0, ongoing.getValue().compareTo(new BigDecimal("9.00")));
        assertEquals(0, ongoing.getPublishedValue().compareTo(new BigDecimal("9.00")));

        // The point of the whole exercise: parents must not be shown 9, 6, 6
        // beside an average of 6 that matches none of them.
        GradeEntry avg = entry("ONGOING_AVG");
        assertEquals(0, avg.getPublishedValue().compareTo(new BigDecimal("7.00")));

        GradeEntry trimester = entry("TRIMESTER_GRADE");
        assertEquals(0, trimester.getValue().compareTo(new BigDecimal("7.2")));
        assertEquals(0, trimester.getPublishedValue().compareTo(new BigDecimal("7.2")));
    }

    @Test
    @DisplayName("an approval does not release cells that were never published")
    void approvalDoesNotReleaseUnpublishedCells() throws Exception {
        // Publish a bare grid, then add a mark that stays unpublished.
        writeService.apply(request(Collections.singletonList(update("ONGOING_1", "6"))), 1L);
        em.flush();
        publish();

        writeService.apply(request(Collections.singletonList(update("PROGRESS", "4"))), 1L);
        em.flush();
        em.clear();

        Long entryId = entry("ONGOING_1").getId();
        ChangeRequestView raised = raise(entryId, "8", "გადამოწმდა");
        changeRequestService.decide(raised.getId(), true, "ok", 2L);
        em.flush();
        em.clear();

        // PROGRESS feeds nothing here, and was never published. Approving a
        // change elsewhere must not push it out ahead of its period.
        GradeEntry progress = entry("PROGRESS");
        assertFalse(progress.isPublished());
        assertNull(progress.getPublishedValue());
    }

    @Test
    @DisplayName("a decided request cannot be decided again")
    void cannotDecideTwice() throws Exception {
        enterMarks();
        publish();

        ChangeRequestView raised = raise(entry("ONGOING_1").getId(), "9", "მიზეზი");
        changeRequestService.decide(raised.getId(), false, "არა", 2L);
        em.flush();

        assertThrows(SGSException.class,
                () -> changeRequestService.decide(raised.getId(), true, "გადავიფიქრე", 2L));
    }

    @Test
    @DisplayName("the queue carries what the director needs to judge, without a query per row")
    void queueIsFlattened() throws Exception {
        enterMarks();
        publish();
        raise(entry("ONGOING_1").getId(), "9", "ნაშრომი ხელახლა შემოწმდა");
        em.flush();
        em.clear();

        // An empty allowed-set is how an unrestricted user - a director - is
        // expressed. A restricted one is narrowed to their own classes; see
        // narrowsTheQueueToTheCallersClasses below.
        List<ChangeRequestView> queue = changeRequestService.queue(
                ChangeRequestStatus.PENDING, data.classGroup.getId(), Collections.emptySet());

        assertEquals(1, queue.size());
        ChangeRequestView view = queue.get(0);
        assertNotNull(view.getStudentName());
        assertNotNull(view.getClassName());
        assertNotNull(view.getSubjectName());
        assertNotNull(view.getPeriodLabel());
        assertEquals("ONGOING_1", view.getComponentCode());
        assertEquals(0, view.getPreviousValue().compareTo(new BigDecimal("6.00")));
        assertEquals(0, view.getRequestedValue().compareTo(new BigDecimal("9")));
        assertEquals("ნაშრომი ხელახლა შემოწმდა", view.getReason());
    }


    @Test
    @DisplayName("the queue is narrowed to the caller's own classes")
    void narrowsTheQueueToTheCallersClasses() throws Exception {
        enterMarks();
        publish();
        raise(entry("ONGOING_1").getId(), "9", "ნაშრომი ხელახლა შემოწმდა");
        em.flush();
        em.clear();

        // The console asks for every class, so before this the endpoint handed
        // a class-scoped user every class's requests: student names, both
        // values, and the teacher's stated reason.
        List<ChangeRequestView> mine = changeRequestService.queue(
                ChangeRequestStatus.PENDING, null,
                java.util.Collections.singleton(data.classGroup.getId()));
        assertEquals(1, mine.size(), "own class is visible");

        List<ChangeRequestView> other = changeRequestService.queue(
                ChangeRequestStatus.PENDING, null,
                java.util.Collections.singleton(data.classGroup.getId() + 999_999L));
        assertTrue(other.isEmpty(), "another class's requests are not");
    }

    @Test
    @DisplayName("the grid marks a locked cell that already has a request outstanding")
    void gridShowsPendingRequests() throws Exception {
        enterMarks();
        publish();
        Long entryId = entry("ONGOING_1").getId();
        raise(entryId, "9", "გადამოწმდა");
        em.flush();
        em.clear();

        // Without this the teacher only finds out by being refused - the unique
        // index is what prevents a second request, this is so they are told
        // before they type one.
        mthiebi.sgs.gradebook.service.grid.GridCell cell =
                gridService.load(data.classGroup.getId(), data.subject.getId(),
                                data.trimester1.getId(), data.template.getUuid())
                        .getCells().stream()
                        .filter(c -> c.getEnrollmentId().equals(enrollment)
                                && "ONGOING_1".equals(c.getComponentCode()))
                        .findFirst().orElseThrow(AssertionError::new);

        assertEquals(entryId, cell.getId(), "the cell must carry its row id to be disputable");
        assertTrue(cell.isPublished());
        assertTrue(cell.isChangeRequestPending());
    }

    @Test
    @DisplayName("only one request can be open on a cell at a time")
    void onlyOneOpenRequestPerCell() throws Exception {
        enterMarks();
        publish();
        Long entryId = entry("ONGOING_1").getId();
        raise(entryId, "9", "პირველი");
        em.flush();

        // Enforced by a filtered unique index rather than a check-then-insert:
        // two teachers submitting at once is exactly when reading first would
        // let both through.
        assertThrows(SGSException.class, () -> raise(entryId, "8", "მეორე"));
    }

    private ChangeRequestView raise(Long entryId, String value, String reason)
            throws SGSException {
        RaiseChangeRequest raise = new RaiseChangeRequest();
        raise.setGradeEntryId(entryId);
        raise.setRequestedValue(new BigDecimal(value));
        raise.setReason(reason);
        ChangeRequestView view = changeRequestService.raise(raise, 1L);
        em.flush();
        return view;
    }
}
