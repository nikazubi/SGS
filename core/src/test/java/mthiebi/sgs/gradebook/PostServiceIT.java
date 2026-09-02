package mthiebi.sgs.gradebook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mthiebi.sgs.db.QueryFactoryProvider;
import mthiebi.sgs.gradebook.model.Post;
import mthiebi.sgs.gradebook.model.PostKind;
import mthiebi.sgs.gradebook.service.content.HtmlSanitizer;
import mthiebi.sgs.gradebook.service.content.PostDraft;
import mthiebi.sgs.gradebook.service.content.PostService;
import mthiebi.sgs.gradebook.service.content.PostView;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Staff-authored content, against a real database.
 * <p>
 * The centre of this is the school's answer that **any edit needs a
 * re-publish**: what a parent sees is the snapshot taken when someone published,
 * not the row a teacher is editing. Everything else follows from that.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PostService.class, HtmlSanitizer.class, QueryFactoryProvider.class,
        mthiebi.sgs.gradebook.service.content.CategoryService.class})
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.SQLServer2012Dialect",
        "spring.jpa.hibernate.ddl-auto=none"
})
class PostServiceIT {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private PostService postService;

    @Autowired
    private mthiebi.sgs.gradebook.service.content.CategoryService categoryService;

    private GradebookTestData data;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        data = new GradebookTestData(em).build(UUID.randomUUID().toString().substring(0, 8));
        em.flush();
    }

    // ---- the state machine -------------------------------------------------

    @Test
    @DisplayName("a new post is a draft and has been published to nobody")
    void newPostIsADraft() throws Exception {
        PostView saved = postService.save(PostKind.HOMEWORK, draft("კითხვა 4"), 1L);

        assertEquals("DRAFT", saved.getStatus());
        assertNull(saved.getPublishedAt());
        assertFalse(saved.isHasUnpublishedChanges());
        assertNull(entity(saved.getUuid()).getPublishedPayload(),
                "a draft has never been shown to anyone");
    }

    @Test
    @DisplayName("publishing snapshots what was published")
    void publishTakesASnapshot() throws Exception {
        PostView saved = postService.save(PostKind.HOMEWORK, draft("კითხვა 4"), 1L);
        PostView published = postService.publish(saved.getUuid(), 1L);

        assertEquals("PUBLISHED", published.getStatus());
        assertNotNull(published.getPublishedAt());
        assertFalse(published.isHasUnpublishedChanges());

        JsonNode payload = mapper.readTree(entity(saved.getUuid()).getPublishedPayload());
        assertEquals("კითხვა 4", payload.get("title").asText());
    }

    @Test
    @DisplayName("editing a published post does NOT change what parents see")
    void editDoesNotLeakToParents() throws Exception {
        // The school's answer, and the whole reason for the snapshot. Live
        // editing was the obvious alternative and they chose against it.
        PostView saved = postService.save(PostKind.HOMEWORK, draft("კითხვა 4"), 1L);
        postService.publish(saved.getUuid(), 1L);

        PostDraft edit = draft("კითხვა 5");
        edit.setUuid(saved.getUuid());
        PostView edited = postService.save(PostKind.HOMEWORK, edit, 1L);

        assertEquals("კითხვა 5", edited.getTitle(), "the working copy moved");
        assertTrue(edited.isHasUnpublishedChanges(), "and the teacher is told a re-publish is owed");
        assertEquals("PUBLISHED", edited.getStatus(), "it is still a published item");

        JsonNode payload = mapper.readTree(entity(saved.getUuid()).getPublishedPayload());
        assertEquals("კითხვა 4", payload.get("title").asText(),
                "but parents are still being shown what was published");
    }

    @Test
    @DisplayName("re-publishing clears the flag and rewrites the snapshot")
    void republishCatchesUp() throws Exception {
        PostView saved = postService.save(PostKind.HOMEWORK, draft("კითხვა 4"), 1L);
        postService.publish(saved.getUuid(), 1L);

        PostDraft edit = draft("კითხვა 5");
        edit.setUuid(saved.getUuid());
        postService.save(PostKind.HOMEWORK, edit, 1L);

        PostView republished = postService.publish(saved.getUuid(), 1L);
        assertFalse(republished.isHasUnpublishedChanges());

        JsonNode payload = mapper.readTree(entity(saved.getUuid()).getPublishedPayload());
        assertEquals("კითხვა 5", payload.get("title").asText());
    }

    // ---- targeting ---------------------------------------------------------

    @Test
    @DisplayName("no targets means the whole class")
    void emptyTargetsMeansEveryone() throws Exception {
        PostView saved = postService.save(PostKind.HOMEWORK, draft("all"), 1L);
        assertTrue(saved.getTargetEnrollmentIds().isEmpty());
    }

    @Test
    @DisplayName("named students are stored, and their names are snapshotted")
    void targetsAreSnapshottedByName() throws Exception {
        PostDraft d = draft("some");
        d.setTargetEnrollmentIds(Arrays.asList(
                data.enrollments.get(0).getId(), data.enrollments.get(1).getId()));
        PostView saved = postService.save(PostKind.HOMEWORK, d, 1L);
        assertEquals(2, saved.getTargetEnrollmentIds().size());

        postService.publish(saved.getUuid(), 1L);
        JsonNode targets = mapper.readTree(entity(saved.getUuid()).getPublishedPayload())
                .get("targets");
        assertEquals(2, targets.size());
        // Names as well as ids, so phase 11 need not join back to an enrollment
        // that may since have moved class or been archived by the annual wipe.
        assertFalse(targets.get(0).get("name").asText().isEmpty());
    }

    @Test
    @DisplayName("re-saving replaces the target list rather than appending to it")
    void targetsAreReplaced() throws Exception {
        PostDraft d = draft("some");
        d.setTargetEnrollmentIds(Arrays.asList(
                data.enrollments.get(0).getId(), data.enrollments.get(1).getId()));
        PostView saved = postService.save(PostKind.HOMEWORK, d, 1L);

        PostDraft edit = draft("some");
        edit.setUuid(saved.getUuid());
        edit.setTargetEnrollmentIds(Collections.singletonList(data.enrollments.get(2).getId()));
        PostView edited = postService.save(PostKind.HOMEWORK, edit, 1L);

        assertEquals(1, edited.getTargetEnrollmentIds().size());
        assertEquals(data.enrollments.get(2).getId(), edited.getTargetEnrollmentIds().get(0));
    }

    // ---- what must not be stored -------------------------------------------

    @Test
    @DisplayName("script does not survive being saved")
    void scriptIsStrippedOnWrite() throws Exception {
        PostDraft d = draft("x");
        d.setBodyHtml("<p>ok</p><script>alert(1)</script>");
        PostView saved = postService.save(PostKind.HOMEWORK, d, 1L);

        // Checked in the database, not on the returned object: the point is that
        // nothing dangerous is ever *stored*, so a later reader is safe too.
        assertFalse(entity(saved.getUuid()).getBodyHtml().toLowerCase().contains("<script"));
    }

    @Test
    @DisplayName("a javascript link is dropped rather than stored")
    void javascriptLinkDropped() throws Exception {
        PostDraft d = draft("x");
        PostDraft.LinkDraft bad = new PostDraft.LinkDraft();
        bad.setUrl("javascript:alert(1)");
        PostDraft.LinkDraft good = new PostDraft.LinkDraft();
        good.setUrl("https://example.edu.ge/a");
        d.setLinks(Arrays.asList(bad, good));

        PostView saved = postService.save(PostKind.HOMEWORK, d, 1L);
        assertEquals(1, saved.getLinks().size(), "the bad one is dropped, the good one kept");
        assertEquals("https://example.edu.ge/a", saved.getLinks().get(0).getUrl());
    }

    // ---- listing -----------------------------------------------------------

    @Test
    @DisplayName("soft delete hides a post without removing the row")
    void archiveHidesButKeeps() throws Exception {
        PostView saved = postService.save(PostKind.HOMEWORK, draft("gone"), 1L);
        postService.archive(saved.getUuid(), true, 1L);

        List<PostView> visible = postService.list(PostKind.HOMEWORK, data.classGroup.getId(),
                data.subject.getId(), null, null, 10);
        assertTrue(visible.stream().noneMatch(p -> p.getUuid().equals(saved.getUuid())));

        assertNotNull(entity(saved.getUuid()), "the row is still there");
    }

    @Test
    @DisplayName("an archived post cannot be published")
    void archivedCannotBePublished() throws Exception {
        PostView saved = postService.save(PostKind.HOMEWORK, draft("gone"), 1L);
        postService.archive(saved.getUuid(), true, 1L);

        assertThrows(mthiebi.sgs.SGSException.class,
                () -> postService.publish(saved.getUuid(), 1L));
    }

    @Test
    @DisplayName("the list is newest first and honours the limit")
    void listIsNewestFirstAndLimited() throws Exception {
        for (int i = 1; i <= 5; i++) {
            PostDraft d = draft("hw " + i);
            d.setEventDate(LocalDate.of(2026, 3, i));
            postService.save(PostKind.HOMEWORK, d, 1L);
        }
        List<PostView> top = postService.list(PostKind.HOMEWORK, data.classGroup.getId(),
                data.subject.getId(), null, null, 3);

        assertEquals(3, top.size());
        assertEquals("hw 5", top.get(0).getTitle(), "newest first");
        assertEquals(5, postService.count(PostKind.HOMEWORK, data.classGroup.getId(),
                data.subject.getId(), null, null), "and the count knows there are more");
    }

    @Test
    @DisplayName("Georgian round-trips through the sanitiser and the database")
    void georgianRoundTrips() throws Exception {
        PostDraft d = draft("დავალება");
        d.setBodyHtml("<p>წაიკითხეთ <strong>მეოთხე თავი</strong></p>");
        PostView saved = postService.save(PostKind.HOMEWORK, d, 1L);
        em.flush();
        em.clear();

        PostView reread = postService.get(saved.getUuid());
        assertEquals("დავალება", reread.getTitle());
        assertTrue(reread.getBodyHtml().contains("მეოთხე თავი"), reread.getBodyHtml());
    }

    // ---- phase 9: the other four modules -----------------------------------

    @Test
    @DisplayName("news saves with no class at all")
    void newsHasNoClass() throws Exception {
        // The one module that is school-wide. Phase 8 made class_group_id
        // nullable for exactly this and then required it in the service anyway.
        PostDraft d = new PostDraft();
        d.setTitle("სკოლის სიახლე");
        d.setBodyHtml("<p>ტექსტი</p>");
        d.setEventDate(LocalDate.of(2026, 3, 10));

        PostView saved = postService.save(PostKind.NEWS, d, 1L);
        assertNull(saved.getClassGroupId());
        assertEquals("NEWS", saved.getKind());
    }

    @Test
    @DisplayName("every other kind still requires a class")
    void otherKindsStillRequireAClass() {
        PostDraft d = new PostDraft();
        d.setTitle("x");
        assertThrows(mthiebi.sgs.SGSException.class,
                () -> postService.save(PostKind.HOMEWORK, d, 1L));
        assertThrows(mthiebi.sgs.SGSException.class,
                () -> postService.save(PostKind.SCHEDULE, d, 1L));
    }

    @Test
    @DisplayName("a class has one standing schedule, edited rather than added to")
    void standingDocumentIsSingular() throws Exception {
        PostDraft d = draft(null);
        d.setSubjectId(null);
        d.setLines(Arrays.asList(line(1, 0, "8:00", "ტანვარჯიში"),
                line(1, 1, "8:30", "საუზმე")));
        PostView first = postService.save(PostKind.SCHEDULE, d, 1L);

        PostView standing = postService.standing(PostKind.SCHEDULE, data.classGroup.getId());
        assertEquals(first.getUuid(), standing.getUuid());
        assertEquals(2, standing.getLines().size());

        // Editing it keeps the same document rather than creating a second.
        PostDraft edit = draft(null);
        edit.setUuid(first.getUuid());
        edit.setSubjectId(null);
        edit.setLines(Collections.singletonList(line(1, 0, "9:00", "გაკვეთილი")));
        postService.save(PostKind.SCHEDULE, edit, 1L);

        PostView after = postService.standing(PostKind.SCHEDULE, data.classGroup.getId());
        assertEquals(first.getUuid(), after.getUuid());
        assertEquals(1, after.getLines().size(), "lines are replaced, not appended");
        assertEquals("9:00", after.getLines().get(0).getTimeText());
    }

    @Test
    @DisplayName("lines keep their weekday and order")
    void linesKeepOrder() throws Exception {
        PostDraft d = draft(null);
        d.setSubjectId(null);
        d.setLines(Arrays.asList(line(2, 0, "10:00", "მათემატიკა"),
                line(1, 1, "9:00", "მეორე"),
                line(1, 0, "8:00", "პირველი")));
        PostView saved = postService.save(PostKind.SCHEDULE, d, 1L);
        em.flush();
        em.clear();

        PostView reread = postService.get(saved.getUuid());
        assertEquals(3, reread.getLines().size());
        // Ordered by weekday then ordinal, so Monday's two come before Tuesday's.
        assertEquals("პირველი", reread.getLines().get(0).getText());
        assertEquals("მეორე", reread.getLines().get(1).getText());
        assertEquals("მათემატიკა", reread.getLines().get(2).getText());
    }

    @Test
    @DisplayName("an empty line is dropped rather than stored")
    void emptyLinesDropped() throws Exception {
        PostDraft d = draft(null);
        d.setSubjectId(null);
        d.setLines(Arrays.asList(line(1, 0, "8:00", "რეალური"),
                line(1, 1, null, null)));
        PostView saved = postService.save(PostKind.SCHEDULE, d, 1L);
        assertEquals(1, saved.getLines().size(), "the row nobody filled in is not kept");
    }

    @Test
    @DisplayName("a repeated category name is reused, not duplicated")
    void categoriesAreReused() throws Exception {
        // The reason categories are a table: a stray space or a capital should
        // not create a second one.
        var first = categoryService.findOrCreate("საბავშვო ბაღი", 1L);
        var again = categoryService.findOrCreate("  საბავშვო ბაღი  ", 1L);
        assertEquals(first.getId(), again.getId());
    }

    @Test
    @DisplayName("the schedule is snapshotted with its lines")
    void scheduleSnapshotCarriesLines() throws Exception {
        PostDraft d = draft(null);
        d.setSubjectId(null);
        d.setLines(Collections.singletonList(line(1, 0, "8:00", "ტანვარჯიში")));
        PostView saved = postService.save(PostKind.SCHEDULE, d, 1L);
        postService.publish(saved.getUuid(), 1L);

        JsonNode lines = mapper.readTree(entity(saved.getUuid()).getPublishedPayload())
                .get("lines");
        assertEquals(1, lines.size());
        assertEquals("ტანვარჯიში", lines.get(0).get("text").asText());
    }

    // ---- helpers -----------------------------------------------------------

    private PostDraft draft(String title) {
        PostDraft d = new PostDraft();
        d.setClassGroupId(data.classGroup.getId());
        d.setSubjectId(data.subject.getId());
        d.setEventDate(LocalDate.of(2026, 3, 10));
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

    private Post entity(String uuid) {
        em.flush();
        return em.createQuery("select p from Post p where p.uuid = :u", Post.class)
                .setParameter("u", uuid).getSingleResult();
    }
}
