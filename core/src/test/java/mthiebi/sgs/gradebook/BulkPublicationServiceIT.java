package mthiebi.sgs.gradebook;

import mthiebi.sgs.db.QueryFactoryProvider;
import mthiebi.sgs.gradebook.model.ClassGroup;
import mthiebi.sgs.gradebook.model.Publication;
import mthiebi.sgs.gradebook.service.*;
import mthiebi.sgs.gradebook.service.grid.GradebookLookupService;
import mthiebi.sgs.gradebook.service.publish.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Releasing several classes and journals in one go, from the history page.
 * <p>
 * Worth proving: the preview counts what the release then releases; a pair that
 * fails does not stop the others; a class outside the caller's scope is not
 * touched; and a family is emailed once, not once per journal.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({BulkPublicationService.class, PublicationService.class, GradebookLookupService.class,
        GradeWriteService.class, GradeExplainService.class, TemplateGraphLoader.class,
        PeriodTreeLoader.class, TemplateVersionResolver.class, SpecialValueRegistry.class,
        QueryFactoryProvider.class,
        mthiebi.sgs.gradebook.service.conversion.GradeConversionService.class})
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.SQLServer2012Dialect",
        "spring.jpa.hibernate.ddl-auto=none"
})
class BulkPublicationServiceIT {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private BulkPublicationService bulk;

    @Autowired
    private PublicationService publicationService;

    @Autowired
    private GradeWriteService writeService;

    @Autowired
    private TemplateGraphLoader templateGraphLoader;

    @MockBean
    private GuardianNotifier guardianNotifier;

    private GradebookTestData first;
    private GradebookTestData second;

    @BeforeEach
    void setUp() {
        templateGraphLoader.evictAll();
        first = new GradebookTestData(em).build(UUID.randomUUID().toString().substring(0, 6));
        // A second class on a scheme of its own: the chosen period has to be
        // found in it by code.
        second = new GradebookTestData(em).build(UUID.randomUUID().toString().substring(0, 6));
        em.flush();
    }

    private void enterTrimester(GradebookTestData data, String mark) throws Exception {
        GradeWriteRequest request = new GradeWriteRequest();
        request.setJournalUuid(data.template.getUuid());
        request.setClassGroupId(data.classGroup.getId());
        request.setSubjectId(data.subject.getId());
        request.setPeriodId(data.trimester1.getId());
        List<GradeEntryUpdate> entries = new ArrayList<>();
        for (String code : Arrays.asList("ONGOING_1", "INITIAL_KNOWLEDGE", "FINAL_TEST")) {
            GradeEntryUpdate u = new GradeEntryUpdate();
            u.setEnrollmentId(data.enrollments.get(0).getId());
            u.setComponentCode(code);
            u.setValue(new BigDecimal(mark));
            entries.add(u);
        }
        request.setEntries(entries);
        writeService.apply(request, 1L);
        em.flush();
    }

    private BulkPublishRequest request(GradebookTestData... classes) {
        BulkPublishRequest request = new BulkPublishRequest();
        // The first class's trimester I; the second class has its own.
        request.setPeriodId(first.trimester1.getId());
        for (GradebookTestData data : classes) {
            request.getTargets().add(target(data.classGroup.getId(), data.template.getUuid()));
        }
        return request;
    }

    private BulkPublishRequest.Target target(Long classGroupId, String journalUuid) {
        BulkPublishRequest.Target target = new BulkPublishRequest.Target();
        target.setClassGroupId(classGroupId);
        target.setJournalUuid(journalUuid);
        return target;
    }

    private BulkPublishItem itemOf(List<BulkPublishItem> items, Long classGroupId) {
        return items.stream().filter(i -> i.getClassGroupId().equals(classGroupId))
                .findFirst().orElseThrow(AssertionError::new);
    }

    private long publishedCellsOf(GradebookTestData data) {
        return em.createQuery("select count(g) from GradeEntry g "
                        + "where g.enrollment.classGroup.id = :c and g.publishedAt is not null",
                Long.class).setParameter("c", data.classGroup.getId()).getSingleResult();
    }

    @Test
    @DisplayName("the preview counts exactly what the release then releases")
    void previewMatchesTheRelease() throws Exception {
        enterTrimester(first, "8");

        BulkPublishItem preview = itemOf(bulk.preview(request(first), Collections.emptySet()),
                first.classGroup.getId());
        assertEquals(BulkPublishItem.Status.READY, preview.getStatus());
        assertTrue(preview.getCount() > 0);
        assertEquals(first.classGroup.getName(), preview.getClassName());
        assertEquals(first.template.getName(), preview.getJournalName());
        assertEquals(0, publishedCellsOf(first), "a preview writes nothing");

        BulkPublishItem released = itemOf(bulk.publish(request(first), 1L, Collections.emptySet()),
                first.classGroup.getId());
        assertEquals(BulkPublishItem.Status.PUBLISHED, released.getStatus());
        assertEquals(preview.getCount(), released.getCount());
        assertEquals(preview.getCount(), publishedCellsOf(first));

        // Nothing new now: unchanged, and no row of zero in the log.
        int logged = publicationService.log(first.classGroup.getId(), Collections.emptySet()).size();
        BulkPublishItem again = itemOf(bulk.publish(request(first), 1L, Collections.emptySet()),
                first.classGroup.getId());
        assertEquals(BulkPublishItem.Status.UNCHANGED, again.getStatus());
        assertEquals(logged,
                publicationService.log(first.classGroup.getId(), Collections.emptySet()).size());
    }

    @Test
    @DisplayName("a pair that fails does not stop the ones after it")
    void eachPairStandsAlone() throws Exception {
        enterTrimester(first, "8");

        // A class nobody is enrolled in yet - the commonest real failure.
        ClassGroup empty = new ClassGroup();
        empty.setSchool(first.classGroup.getSchool());
        empty.setAcademicYear(first.classGroup.getAcademicYear());
        empty.setPeriodScheme(first.classGroup.getPeriodScheme());
        empty.setLevel((short) 9);
        empty.setName("9Z-" + UUID.randomUUID().toString().substring(0, 4));
        em.persist(empty);
        em.flush();

        BulkPublishRequest request = new BulkPublishRequest();
        request.setPeriodId(first.trimester1.getId());
        request.getTargets().add(target(empty.getId(), first.template.getUuid()));
        request.getTargets().add(target(first.classGroup.getId(), first.template.getUuid()));

        List<BulkPublishItem> items = bulk.publish(request, 1L, Collections.emptySet());

        BulkPublishItem failed = itemOf(items, empty.getId());
        assertEquals(BulkPublishItem.Status.FAILED, failed.getStatus());
        assertEquals("კლასში მოსწავლეები ვერ მოიძებნა", failed.getError());

        assertEquals(BulkPublishItem.Status.PUBLISHED, itemOf(items, first.classGroup.getId()).getStatus());
        assertTrue(publishedCellsOf(first) > 0, "released despite the failure before it");
    }

    @Test
    @DisplayName("a class outside the caller's scope is refused and left alone")
    void scopeIsEnforced() throws Exception {
        enterTrimester(first, "8");
        enterTrimester(second, "7");

        List<BulkPublishItem> items = bulk.publish(request(first, second), 1L,
                Collections.singleton(first.classGroup.getId()));

        assertEquals(BulkPublishItem.Status.PUBLISHED, itemOf(items, first.classGroup.getId()).getStatus());
        BulkPublishItem refused = itemOf(items, second.classGroup.getId());
        assertEquals(BulkPublishItem.Status.FAILED, refused.getStatus());
        assertEquals("ამ კლასზე წვდომა არ გაქვთ", refused.getError());
        assertEquals(0, publishedCellsOf(second));

        // And the options offer only the caller's classes.
        BulkPublishOptions options = bulk.options(Collections.singleton(first.classGroup.getId()));
        assertEquals(1, options.getClasses().size());
        assertEquals(first.classGroup.getId(), options.getClasses().get(0).getId());
    }

    @Test
    @DisplayName("each family is emailed once for the whole run, and the log names the journal")
    void oneEmailPerFamily() throws Exception {
        // Siblings in the two classes share an address.
        first.enrollments.get(0).getStudent().setGuardianEmail("shared.family@example.invalid");
        second.enrollments.get(0).getStudent().setGuardianEmail("shared.family@example.invalid");
        first.enrollments.get(1).getStudent().setGuardianEmail("first.only@example.invalid");
        enterTrimester(first, "8");
        enterTrimester(second, "7");

        List<BulkPublishItem> items = bulk.publish(request(first, second), 1L, Collections.emptySet());
        assertTrue(items.stream().allMatch(i -> i.getStatus() == BulkPublishItem.Status.PUBLISHED));
        // The second class found its own trimester I by code.
        assertTrue(publishedCellsOf(second) > 0);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> recipients = ArgumentCaptor.forClass(List.class);
        verify(guardianNotifier, times(1)).notifyPublished(recipients.capture(), any(), eq("I ტრიმესტრი"));
        Set<String> sent = new HashSet<>(recipients.getValue());
        assertEquals(recipients.getValue().size(), sent.size(), "no address twice");
        assertTrue(sent.containsAll(Arrays.asList(
                "shared.family@example.invalid", "first.only@example.invalid")));
        verify(guardianNotifier, never()).notifyChangeApproved(anyString(), any());

        PublicationLogEntry logged = publicationService.log(first.classGroup.getId(),
                Collections.emptySet()).get(0);
        assertEquals(first.template.getName(), logged.getJournalName());
        Publication row = em.find(Publication.class, logged.getId());
        assertEquals(first.template.getId(), row.getTemplate().getId());
    }

    @Test
    @DisplayName("nothing released, nobody emailed")
    void noReleaseNoEmail() throws Exception {
        bulk.publish(request(first, second), 1L, Collections.emptySet());
        verify(guardianNotifier, never()).notifyPublished(any(), any(), any());
    }
}
