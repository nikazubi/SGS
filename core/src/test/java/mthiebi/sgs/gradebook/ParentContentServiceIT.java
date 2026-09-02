package mthiebi.sgs.gradebook;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.db.QueryFactoryProvider;
import mthiebi.sgs.gradebook.model.PostKind;
import mthiebi.sgs.gradebook.service.content.HtmlSanitizer;
import mthiebi.sgs.gradebook.service.content.PostDraft;
import mthiebi.sgs.gradebook.service.content.PostService;
import mthiebi.sgs.gradebook.service.content.PostView;
import mthiebi.sgs.gradebook.service.parent.ParentContentService;
import mthiebi.sgs.gradebook.service.parent.ParentContentView;
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
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Homework and news, as a parent sees them.
 * <p>
 * Two things are load-bearing here and neither is visible from the staff side:
 * a parent must never be shown a draft, and must never be shown another child's
 * work. Both are easy to get right once and lose later, so both are pinned.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ParentContentService.class, PostService.class, HtmlSanitizer.class,
        QueryFactoryProvider.class,
        mthiebi.sgs.gradebook.service.content.CategoryService.class})
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.SQLServer2012Dialect",
        "spring.jpa.hibernate.ddl-auto=none"
})
class ParentContentServiceIT {

    private static final LocalDate DAY = LocalDate.of(2026, 3, 10);
    private static final String MONTH = "2026-03";

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private PostService postService;

    @Autowired
    private ParentContentService parentContentService;

    private GradebookTestData data;

    @BeforeEach
    void setUp() {
        data = new GradebookTestData(em).build(UUID.randomUUID().toString().substring(0, 8));
        em.flush();
    }

    // ---- what a parent may see ---------------------------------------------

    @Test
    @DisplayName("a draft is invisible to the parent")
    void draftsAreNotServed() throws Exception {
        postService.save(PostKind.HOMEWORK, draft("არ გაგზავნილა"), 1L);
        em.flush();
        em.clear();

        // Saved is not sent. The staff console shows drafts in a different
        // colour precisely because they have not left the building.
        assertTrue(month().getDays().isEmpty());
    }

    @Test
    @DisplayName("a published assignment appears on its own day")
    void publishedHomeworkAppears() throws Exception {
        publish(draft("კითხვა 4"));

        List<ParentContentView.HomeworkDay> days = month().getDays();
        assertEquals(1, days.size());
        assertEquals(DAY.toString(), days.get(0).getDate());
        assertEquals(1, days.get(0).getTotal());
        assertEquals(1, days.get(0).getUnseen(), "nobody has opened it yet");
    }

    @Test
    @DisplayName("the parent is served the published snapshot, not the working row")
    void servesTheSnapshot() throws Exception {
        PostView sent = publish(draft("პირველი"));

        PostDraft edited = draft("შესწორებული");
        edited.setUuid(sent.getUuid());
        edited.setBodyHtml("<p>ახალი ტექსტი</p>");
        postService.save(PostKind.HOMEWORK, edited, 1L);
        em.flush();
        em.clear();

        // Edited but not re-published. Decision 82: any edit needs a re-publish,
        // so what the parent reads is still what was last sent.
        ParentContentView.HomeworkItem item = onlyItem();
        assertEquals("პირველი", item.getTitle());
        assertFalse(item.getBodyHtml().contains("ახალი"),
                "the working body has not been published");
    }

    @Test
    @DisplayName("homework aimed at one child is hidden from another")
    void targetingIsRespected() throws Exception {
        PostDraft targeted = draft("მხოლოდ ერთისთვის");
        targeted.setTargetEnrollmentIds(
                Collections.singletonList(data.enrollments.get(0).getId()));
        publish(targeted);

        // The child it is for.
        assertEquals(1, parentContentService.homeworkMonth(
                studentOf(0), MONTH).getDays().size());

        // And one it is not. A post with targets is for exactly those children;
        // without this clause adding the first target would silently leave it
        // visible to the whole class.
        assertTrue(parentContentService.homeworkMonth(
                studentOf(1), MONTH).getDays().isEmpty());
    }

    @Test
    @DisplayName("an untargeted assignment is for the whole class")
    void untargetedIsClassWide() throws Exception {
        publish(draft("ყველასთვის"));

        assertEquals(1, parentContentService.homeworkMonth(
                studentOf(0), MONTH).getDays().size());
        assertEquals(1, parentContentService.homeworkMonth(
                studentOf(1), MONTH).getDays().size());
    }

    // ---- read state ---------------------------------------------------------

    @Test
    @DisplayName("opening an assignment clears its unread count")
    void markingSeenClearsTheCount() throws Exception {
        PostView sent = publish(draft("კითხვა 4"));

        assertEquals(1, parentContentService.markSeen(
                studentOf(0), Collections.singletonList(sent.getUuid())));
        em.flush();
        em.clear();

        ParentContentView.HomeworkDay day = month().getDays().get(0);
        assertEquals(1, day.getTotal());
        assertEquals(0, day.getUnseen());
    }

    @Test
    @DisplayName("marking the same assignment twice is not an error")
    void markingIsIdempotent() throws Exception {
        PostView sent = publish(draft("კითხვა 4"));
        List<String> batch = Collections.singletonList(sent.getUuid());

        assertEquals(1, parentContentService.markSeen(studentOf(0), batch));
        em.flush();
        assertEquals(0, parentContentService.markSeen(studentOf(0), batch),
                "already recorded, so nothing is created");
        em.flush();
        em.clear();

        // What makes the console's debounced batch safe: a re-send after a
        // dropped response costs nothing and duplicates nothing.
        assertEquals(0, month().getDays().get(0).getUnseen());
    }

    @Test
    @DisplayName("one parent opening it does not mark it read for another child")
    void seenIsPerChild() throws Exception {
        PostView sent = publish(draft("ყველასთვის"));
        parentContentService.markSeen(studentOf(0), Collections.singletonList(sent.getUuid()));
        em.flush();
        em.clear();

        assertEquals(0, parentContentService.homeworkMonth(studentOf(0), MONTH)
                .getDays().get(0).getUnseen());
        assertEquals(1, parentContentService.homeworkMonth(studentOf(1), MONTH)
                .getDays().get(0).getUnseen(), "the other child has still not read it");
    }

    @Test
    @DisplayName("a second assignment makes an already-opened day unread again")
    void aLaterAssignmentReopensTheDay() throws Exception {
        PostView first = publish(draft("პირველი"));
        parentContentService.markSeen(studentOf(0), Collections.singletonList(first.getUuid()));
        em.flush();

        // The reason seen is keyed per post rather than per date. Keyed by date,
        // this assignment would arrive on a day already marked read and never
        // announce itself.
        publish(draft("მეორე"));
        em.clear();

        ParentContentView.HomeworkDay day = month().getDays().get(0);
        assertEquals(2, day.getTotal());
        assertEquals(1, day.getUnseen());
    }

    @Test
    @DisplayName("a uuid this child cannot see is ignored rather than recorded")
    void cannotMarkAnotherChildsWork() throws Exception {
        PostDraft targeted = draft("მხოლოდ ერთისთვის");
        targeted.setTargetEnrollmentIds(
                Collections.singletonList(data.enrollments.get(0).getId()));
        PostView sent = publish(targeted);

        // Silently, and deliberately: refusing would confirm that a post with
        // that uuid exists, to somebody who is not allowed to know it does.
        assertEquals(0, parentContentService.markSeen(
                studentOf(1), Collections.singletonList(sent.getUuid())));
    }

    // ---- the day ------------------------------------------------------------

    @Test
    @DisplayName("a day is grouped by subject")
    void dayIsGroupedBySubject() throws Exception {
        publish(draft("პირველი"));
        publish(draft("მეორე"));
        em.clear();

        ParentContentView.HomeworkDayDetail detail =
                parentContentService.homeworkDay(studentOf(0), DAY.toString());

        assertEquals(1, detail.getSubjects().size(), "both are the same subject");
        assertEquals(2, detail.getSubjects().get(0).getItems().size());
    }

    @Test
    @DisplayName("reading a day does not mark it read")
    void readingDoesNotWrite() throws Exception {
        publish(draft("კითხვა 4"));
        em.clear();

        parentContentService.homeworkDay(studentOf(0), DAY.toString());
        em.flush();
        em.clear();

        // The console says when a day has been opened, in its own batched call.
        // A read endpoint that writes cannot be retried safely.
        assertEquals(1, month().getDays().get(0).getUnseen());
    }

    @Test
    @DisplayName("a malformed month is refused rather than guessed at")
    void badMonthIsRefused() {
        org.junit.jupiter.api.Assertions.assertThrows(SGSException.class,
                () -> parentContentService.homeworkMonth(studentOf(0), "not-a-month"));
    }

    // ---- news ---------------------------------------------------------------

    @Test
    @DisplayName("only published news reaches a parent, newest first")
    void newsIsPublishedAndOrdered() throws Exception {
        publishNews("ძველი", LocalDate.of(2026, 1, 5));
        publishNews("ახალი", LocalDate.of(2026, 3, 1));
        postService.save(PostKind.NEWS, news("დაუმთავრებელი", LocalDate.of(2026, 4, 1)), 1L);
        em.flush();
        em.clear();

        ParentContentView.NewsPage page = parentContentService.news(null, 0, 10);

        assertEquals(2, page.getTotal(), "the draft is not counted");
        assertEquals(Arrays.asList("ახალი", "ძველი"),
                Arrays.asList(page.getItems().get(0).getTitle(),
                        page.getItems().get(1).getTitle()));
    }

    // ---- schedule, menu, description ----------------------------------------

    /**
     * Moves the fixture's class into the seeded primary school.
     * <p>
     * These three modules exist only there, so a test about them has to be run
     * from a class that has them. Attached to the real PRIMARY row rather than
     * renaming the fixture's own school, because school.code is unique and
     * db/006 already seeds one.
     */
    private void asPrimary() {
        mthiebi.sgs.gradebook.model.School primary = em.createQuery(
                "select s from School s where s.code = 'PRIMARY'",
                mthiebi.sgs.gradebook.model.School.class).getSingleResult();
        data.classGroup.setSchool(primary);
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("an unwritten schedule is null, not an empty week")
    void missingStandingDocIsNull() throws Exception {
        asPrimary();
        // "Not written yet" and "written and empty" are different answers to a
        // parent, and the console says so differently.
        assertNull(parentContentService.standingDoc(studentOf(0), PostKind.SCHEDULE));
    }

    @Test
    @DisplayName("a published schedule comes back as five days in order")
    void scheduleIsFiveDays() throws Exception {
        asPrimary();
        PostDraft doc = standing("დღის რეჟიმი");
        doc.setLines(Arrays.asList(
                line(1, 0, "8:00", "მათემატიკა"),
                line(1, 1, "9:00", "ქართული"),
                line(3, 0, "8:00", "ისტორია")));
        PostView saved = postService.save(PostKind.SCHEDULE, doc, 1L);
        postService.publish(saved.getUuid(), 1L);
        em.flush();
        em.clear();

        ParentContentView.StandingDoc view =
                parentContentService.standingDoc(studentOf(0), PostKind.SCHEDULE);

        // Always five, whether or not each holds anything: a week with
        // Wednesday missing reads as a fault rather than as a quiet day.
        assertEquals(5, view.getDays().size());
        assertEquals(2, view.getDays().get(0).getLines().size());
        assertEquals(0, view.getDays().get(1).getLines().size());
        assertEquals("მათემატიკა", view.getDays().get(0).getLines().get(0).getText());
        assertEquals("8:00", view.getDays().get(0).getLines().get(0).getTimeText());
    }

    @Test
    @DisplayName("an unpublished schedule is invisible")
    void draftScheduleIsNotServed() throws Exception {
        asPrimary();
        PostDraft doc = standing("დღის რეჟიმი");
        doc.setLines(Collections.singletonList(line(1, 0, "8:00", "მათემატიკა")));
        postService.save(PostKind.SCHEDULE, doc, 1L);
        em.flush();
        em.clear();

        assertNull(parentContentService.standingDoc(studentOf(0), PostKind.SCHEDULE));
    }

    @Test
    @DisplayName("a characterization reaches the child it names and nobody else")
    void characterizationIsTargeted() throws Exception {
        asPrimary();
        PostDraft about = standing("დახასიათება");
        about.setSubjectId(data.subject.getId());
        about.setEventDate(DAY);
        about.setTargetEnrollmentIds(
                Collections.singletonList(data.enrollments.get(0).getId()));
        PostView saved = postService.save(PostKind.CHARACTERIZATION, about, 1L);
        postService.publish(saved.getUuid(), 1L);
        em.flush();
        em.clear();

        assertEquals(1, parentContentService.characterizations(studentOf(0)).size());
        assertTrue(parentContentService.characterizations(studentOf(1)).isEmpty(),
                "it names one child, so it is about one child");
    }

    @Test
    @DisplayName("an untargeted characterization reaches nobody")
    void untargetedCharacterizationIsForNobody() throws Exception {
        // Unlike homework, where no target means the whole class. A
        // characterization with no student named is unfinished, not universal.
        asPrimary();
        PostDraft about = standing("დახასიათება");
        about.setSubjectId(data.subject.getId());
        about.setEventDate(DAY);
        PostView saved = postService.save(PostKind.CHARACTERIZATION, about, 1L);
        postService.publish(saved.getUuid(), 1L);
        em.flush();
        em.clear();

        assertTrue(parentContentService.characterizations(studentOf(0)).isEmpty());
        assertTrue(parentContentService.characterizations(studentOf(1)).isEmpty());
    }

    @Test
    @DisplayName("a school without the module is refused the endpoint, not just the box")
    void primaryOnlyModulesAreClosedElsewhere() throws Exception {
        // Basic and secondary do not have these pages at all. Hiding the box
        // while leaving the route open would be a door nobody remembers is
        // supposed to be shut.
        org.junit.jupiter.api.Assertions.assertThrows(SGSException.class,
                () -> parentContentService.standingDoc(studentOf(0), PostKind.SCHEDULE));
        org.junit.jupiter.api.Assertions.assertThrows(SGSException.class,
                () -> parentContentService.standingDoc(studentOf(0), PostKind.MENU));
        org.junit.jupiter.api.Assertions.assertThrows(SGSException.class,
                () -> parentContentService.characterizations(studentOf(0)));
    }

    @Test
    @DisplayName("the module list follows the child's school")
    void modulesFollowTheSchool() throws Exception {
        // GradebookTestData builds a school of its own; whatever its code is, it
        // is not PRIMARY, so this is the basic/secondary answer.
        List<String> modules = parentContentService.modules(studentOf(0));
        assertEquals(Arrays.asList("HOMEWORK", "NEWS"), modules);

        asPrimary();

        assertEquals(Arrays.asList("HOMEWORK", "NEWS", "SCHEDULE", "MENU", "CHARACTERIZATION"),
                parentContentService.modules(studentOf(0)),
                "primary adds the three modules the brief gives it");
    }

    // ---- helpers ------------------------------------------------------------

    private ParentContentView.HomeworkMonth month() throws Exception {
        return parentContentService.homeworkMonth(studentOf(0), MONTH);
    }

    private ParentContentView.HomeworkItem onlyItem() throws Exception {
        return parentContentService.homeworkDay(studentOf(0), DAY.toString())
                .getSubjects().get(0).getItems().get(0);
    }

    private Long studentOf(int index) {
        return data.enrollments.get(index).getStudent().getId();
    }

    private PostView publish(PostDraft draft) throws Exception {
        PostView saved = postService.save(PostKind.HOMEWORK, draft, 1L);
        PostView sent = postService.publish(saved.getUuid(), 1L);
        em.flush();
        em.clear();
        return sent;
    }

    private void publishNews(String title, LocalDate date) throws Exception {
        PostView saved = postService.save(PostKind.NEWS, news(title, date), 1L);
        postService.publish(saved.getUuid(), 1L);
        em.flush();
    }

    private PostDraft standing(String title) {
        PostDraft d = new PostDraft();
        d.setClassGroupId(data.classGroup.getId());
        d.setTitle(title);
        d.setBodyHtml("<p>body</p>");
        return d;
    }

    private PostDraft.LineDraft line(int weekday, int ordinal, String time, String text) {
        PostDraft.LineDraft l = new PostDraft.LineDraft();
        l.setWeekday(weekday);
        l.setOrdinal(ordinal);
        l.setTimeText(time);
        l.setText(text);
        return l;
    }

    private PostDraft draft(String title) {
        PostDraft d = new PostDraft();
        d.setClassGroupId(data.classGroup.getId());
        d.setSubjectId(data.subject.getId());
        d.setEventDate(DAY);
        d.setTitle(title);
        d.setBodyHtml("<p>body</p>");
        return d;
    }

    private PostDraft news(String title, LocalDate date) {
        PostDraft d = new PostDraft();
        d.setEventDate(date);
        d.setTitle(title);
        d.setBodyHtml("<p>body</p>");
        return d;
    }
}
