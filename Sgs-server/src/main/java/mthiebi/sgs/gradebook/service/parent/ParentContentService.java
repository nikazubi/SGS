package mthiebi.sgs.gradebook.service.parent;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.SGSExceptionCode;
import mthiebi.sgs.gradebook.model.Enrollment;
import mthiebi.sgs.gradebook.model.HomeworkSeen;
import mthiebi.sgs.gradebook.model.Post;
import mthiebi.sgs.gradebook.repository.HomeworkSeenRepository;
import mthiebi.sgs.gradebook.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Homework and news, as a parent sees them.
 * <p>
 * Read-only apart from one thing: marking homework opened. Everything is scoped
 * to the child the token belongs to - the enrollment is resolved here from the
 * student id, never taken from the request.
 */
@Service
public class ParentContentService {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private HomeworkSeenRepository homeworkSeenRepository;

    @PersistenceContext
    private EntityManager em;

    // ---- homework -----------------------------------------------------------

    /**
     * A month of the calendar: which days hold homework, and how much of it is
     * still unopened.
     *
     * @param month ISO {@code yyyy-MM}. The console picks the month, so the
     *              range is derived here rather than trusting two dates that
     *              could be sent a year apart.
     */
    @Transactional(readOnly = true)
    public ParentContentView.HomeworkMonth homeworkMonth(Long studentId, String month)
            throws SGSException {

        Enrollment enrollment = enrollmentOf(studentId);
        YearMonth ym = parseMonth(month);

        List<Post> posts = postRepository.findHomeworkForChild(
                enrollment.getClassGroup().getId(), enrollment.getId(),
                ym.atDay(1), ym.atEndOfMonth());

        Set<Long> seen = seenIdsOf(enrollment, posts);

        // Insertion-ordered: the query is newest first, and the console draws a
        // calendar, so the order matters less than it being stable.
        Map<LocalDate, int[]> byDay = new LinkedHashMap<>();
        for (Post post : posts) {
            int[] counts = byDay.computeIfAbsent(post.getEventDate(), d -> new int[2]);
            counts[0]++;
            if (!seen.contains(post.getId())) {
                counts[1]++;
            }
        }

        ParentContentView.HomeworkMonth view =
                new ParentContentView.HomeworkMonth(ym.atDay(1).toString());
        byDay.forEach((date, counts) -> view.getDays().add(
                new ParentContentView.HomeworkDay(date.toString(), counts[0], counts[1])));
        return view;
    }

    /**
     * One day, grouped by subject.
     * <p>
     * Reading does <b>not</b> mark anything seen. The console says when a day
     * has been opened, in its own batched call - partly because it debounces,
     * and partly because a read endpoint that writes cannot be retried safely.
     */
    @Transactional(readOnly = true)
    public ParentContentView.HomeworkDayDetail homeworkDay(Long studentId, String date)
            throws SGSException {

        Enrollment enrollment = enrollmentOf(studentId);
        LocalDate day = parseDate(date);

        List<Post> posts = postRepository.findHomeworkForChild(
                enrollment.getClassGroup().getId(), enrollment.getId(), day, day);
        Set<Long> seen = seenIdsOf(enrollment, posts);

        ParentContentView.HomeworkDayDetail detail =
                new ParentContentView.HomeworkDayDetail(day.toString());

        Map<Long, ParentContentView.HomeworkSubject> bySubject = new LinkedHashMap<>();
        for (Post post : posts) {
            Long subjectId = post.getSubject() == null ? null : post.getSubject().getId();
            String subjectName = post.getSubject() == null
                    ? "" : post.getSubject().getName();

            ParentContentView.HomeworkSubject group = bySubject.computeIfAbsent(subjectId, id -> {
                ParentContentView.HomeworkSubject created =
                        new ParentContentView.HomeworkSubject(id, subjectName);
                detail.getSubjects().add(created);
                return created;
            });

            com.fasterxml.jackson.databind.JsonNode snapshot = published(post);
            ParentContentView.HomeworkItem item = new ParentContentView.HomeworkItem(
                    post.getUuid(), text(snapshot, "title"), text(snapshot, "bodyHtml"),
                    seen.contains(post.getId()));
            appendLinks(item.getLinks(), snapshot);
            group.getItems().add(item);
        }
        return detail;
    }

    /**
     * Records that a parent has opened these posts.
     * <p>
     * Idempotent by construction: anything already recorded is skipped, and the
     * unique constraint catches the rest. The console batches and debounces, so
     * the same uuid arriving twice is the normal case rather than an error.
     * <p>
     * Silently ignores a uuid this child cannot see. It is not worth failing a
     * batch of ten over one stale id, and answering "that post is not yours"
     * would tell a caller something about a post they are not allowed to know
     * exists.
     *
     * @return how many rows this call actually created.
     */
    @Transactional(rollbackFor = Exception.class)
    public int markSeen(Long studentId, List<String> postUuids) throws SGSException {
        if (postUuids == null || postUuids.isEmpty()) {
            return 0;
        }
        Enrollment enrollment = enrollmentOf(studentId);

        // Re-resolved through the same visibility query the calendar uses, so a
        // uuid cannot be used to mark - or to probe for - another child's work.
        List<Post> visible = em.createQuery(
                        "select p from Post p "
                                + "where p.uuid in :uuids "
                                + "  and p.kind = mthiebi.sgs.gradebook.model.PostKind.HOMEWORK "
                                + "  and p.archived = false "
                                + "  and p.status = mthiebi.sgs.gradebook.model.PostStatus.PUBLISHED "
                                + "  and p.classGroup.id = :classGroupId "
                                + "  and (p.targets is empty or :enrollmentId in "
                                + "        (select t.enrollment.id from PostTarget t where t.post.id = p.id))",
                        Post.class)
                .setParameter("uuids", postUuids)
                .setParameter("classGroupId", enrollment.getClassGroup().getId())
                .setParameter("enrollmentId", enrollment.getId())
                .getResultList();

        Set<Long> already = seenIdsOf(enrollment, visible);
        int created = 0;
        for (Post post : visible) {
            if (already.contains(post.getId())) {
                continue;
            }
            HomeworkSeen row = new HomeworkSeen();
            row.setEnrollment(enrollment);
            row.setPost(post);
            row.setSeenAt(Instant.now());
            homeworkSeenRepository.save(row);
            created++;
        }
        return created;
    }

    // ---- news ---------------------------------------------------------------

    /**
     * Published news, newest first.
     * <p>
     * Institution-wide: no class, no school, no enrollment. Every parent sees
     * every item, which the school confirmed - the category is a label to filter
     * on rather than a visibility rule.
     */
    @Transactional(readOnly = true)
    public ParentContentView.NewsPage news(Long categoryId, int page, int size) {
        int bounded = Math.min(Math.max(size, 1), 50);
        List<Post> posts = postRepository.findPublishedNews(
                categoryId, PageRequest.of(Math.max(page, 0), bounded));

        ParentContentView.NewsPage view = new ParentContentView.NewsPage(
                postRepository.countPublishedNews(categoryId));

        for (Post post : posts) {
            com.fasterxml.jackson.databind.JsonNode snapshot = published(post);
            String date = text(snapshot, "eventDate");
            ParentContentView.NewsItem item = new ParentContentView.NewsItem(
                    post.getUuid(),
                    text(snapshot, "title"),
                    text(snapshot, "bodyHtml"),
                    date.isEmpty() ? null : date,
                    // Category and image are not in the snapshot - they are
                    // references rather than content, and renaming a category
                    // should retitle the label everywhere rather than leave old
                    // items filed under a name that no longer exists.
                    post.getCategory() == null ? null : post.getCategory().getName(),
                    post.getImage() == null ? null : post.getImage().getUuid());
            appendLinks(item.getLinks(), snapshot);
            view.getItems().add(item);
        }
        return view;
    }

    // ---- schedule and menu --------------------------------------------------

    /**
     * The class's published schedule or menu.
     * <p>
     * One document per class for the whole year - no months, no trimesters, no
     * weekly versions, which the school was explicit about. The lines come from
     * the published snapshot rather than the row, so a coordinator halfway
     * through rearranging Tuesday is not showing parents a half-built timetable.
     *
     * @return null when the class has not made one. The console says so rather
     * than drawing an empty week, because "not written yet" and "written
     * and empty" are different answers to a parent.
     */
    @Transactional(readOnly = true)
    public ParentContentView.StandingDoc standingDoc(Long studentId,
                                                     mthiebi.sgs.gradebook.model.PostKind kind)
            throws SGSException {

        Enrollment enrollment = enrollmentOf(studentId);
        requireModule(enrollment, kind.name());

        List<Post> found = postRepository.findPublishedStanding(
                kind, enrollment.getClassGroup().getId());
        if (found.isEmpty()) {
            return null;
        }

        Post post = found.get(0);
        com.fasterxml.jackson.databind.JsonNode snapshot = published(post);

        ParentContentView.StandingDoc doc =
                new ParentContentView.StandingDoc(text(snapshot, "title"));

        // Five days always, in order, whether or not each holds anything - a
        // week with Wednesday missing reads as a mistake rather than as a day
        // with nothing on it.
        Map<Integer, ParentContentView.StandingDay> byWeekday = new LinkedHashMap<>();
        for (int weekday = 1; weekday <= 5; weekday++) {
            ParentContentView.StandingDay day = new ParentContentView.StandingDay(weekday);
            byWeekday.put(weekday, day);
            doc.getDays().add(day);
        }

        com.fasterxml.jackson.databind.JsonNode lines = snapshot.get("lines");
        if (lines != null && lines.isArray()) {
            List<com.fasterxml.jackson.databind.JsonNode> ordered = new ArrayList<>();
            lines.forEach(ordered::add);
            ordered.sort(java.util.Comparator.comparingInt(n -> n.get("ordinal") == null
                    ? 0 : n.get("ordinal").asInt()));

            for (com.fasterxml.jackson.databind.JsonNode line : ordered) {
                int weekday = line.get("weekday") == null ? 0 : line.get("weekday").asInt();
                ParentContentView.StandingDay day = byWeekday.get(weekday);
                if (day == null) {
                    // A weekend row should not exist; ignoring it is better than
                    // inventing a sixth column for one stray line.
                    continue;
                }
                day.getLines().add(new ParentContentView.StandingLine(
                        emptyToNull(text(line, "timeText")), text(line, "text")));
            }
        }
        return doc;
    }

    // ---- the child's description --------------------------------------------

    /**
     * What the school has written about this child, newest first.
     * <p>
     * Addressed by target rather than by class: a characterization names one
     * student, so an untargeted one reaches nobody rather than the whole class.
     */
    @Transactional(readOnly = true)
    public List<ParentContentView.Characterization> characterizations(Long studentId)
            throws SGSException {

        Enrollment enrollment = enrollmentOf(studentId);
        requireModule(enrollment, "CHARACTERIZATION");

        List<ParentContentView.Characterization> out = new ArrayList<>();

        for (Post post : postRepository.findCharacterizationsForChild(enrollment.getId())) {
            com.fasterxml.jackson.databind.JsonNode snapshot = published(post);
            String date = text(snapshot, "eventDate");
            ParentContentView.Characterization item = new ParentContentView.Characterization(
                    post.getUuid(),
                    text(snapshot, "title"),
                    text(snapshot, "bodyHtml"),
                    date.isEmpty() ? null : date,
                    post.getSubject() == null ? null : post.getSubject().getName());
            appendLinks(item.getLinks(), snapshot);
            out.add(item);
        }
        return out;
    }

    // ---- which modules this child's school shows ----------------------------

    /**
     * The boxes on the landing page, for this child's school.
     * <p>
     * Decided here rather than in the console, because the rule is about the
     * school and the school is in the data. Primary gets the whole content set;
     * basic and secondary get the two modules that are not primary-specific.
     * <p>
     * Absence is deliberately absent from this list. It is a journal, so it
     * arrives as one of the journal boxes for every school - see
     * ParentViewService.journals, which gives primary the register and withholds
     * the gradebook. Naming it here as well would draw the box twice.
     * <p>
     * Returned as names rather than as a boolean per module, so adding one is a
     * value in this list and a route in the console - not another flag threaded
     * through both.
     */
    @Transactional(readOnly = true)
    public List<String> modules(Long studentId) throws SGSException {
        return modulesFor(enrollmentOf(studentId));
    }

    /**
     * The rule itself, in one place.
     * <p>
     * Both the landing page and every endpoint guard read this, so a module
     * cannot be offered and refused - or refused and offered - by two lists
     * that drifted. Adding a name here opens its endpoint; removing it closes
     * it, and neither needs a second edit somewhere else.
     */
    private List<String> modulesFor(Enrollment enrollment) {
        String school = enrollment.getClassGroup().getSchool() == null
                ? null : enrollment.getClassGroup().getSchool().getCode();

        List<String> modules = new ArrayList<>();
        modules.add("HOMEWORK");
        modules.add("NEWS");
        if ("PRIMARY".equals(school)) {
            modules.add("SCHEDULE");
            modules.add("MENU");
            modules.add("CHARACTERIZATION");
        }
        return modules;
    }

    /**
     * Refuses a module this child's school does not have.
     * <p>
     * The console never offers the box, so reaching here means a guessed URL or
     * a stale tab. Refused rather than served: the school's answer is that these
     * are primary-school pages, and a route that quietly works for everyone is
     * one nobody remembers is supposed to be closed.
     */
    private void requireModule(Enrollment enrollment, String module) throws SGSException {
        if (!modulesFor(enrollment).contains(module)) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, "გვერდი ხელმისაწვდომი არ არის");
        }
    }

    private String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    // ---- helpers ------------------------------------------------------------

    /**
     * The published snapshot of a post, or an empty node.
     * <p>
     * A post carries two versions of itself: the columns are what the author is
     * editing, and {@code publishedPayload} is what was last released. Every
     * field a parent reads comes from the snapshot - title, body and links
     * alike. Serving the live columns would put half-finished edits in front of
     * parents and undo the point of having a publish button (decision 82), and
     * mixing the two would be worse still: a published title above an unpublished
     * body reads as though the school said something it has not said.
     * <p>
     * Visibility is the one thing still decided by the live row. Targeting
     * narrows *away* from the class, so reading it live can only ever hide a
     * post from someone, never reveal one - the safe direction when the two
     * disagree.
     * <p>
     * Parsed leniently on purpose. The snapshot's shape is whatever was written
     * at publish time, so a payload from before a field existed must render as
     * empty rather than throw on a parent's screen.
     */
    private com.fasterxml.jackson.databind.JsonNode published(Post post) {
        String payload = post.getPublishedPayload();
        if (payload == null || payload.isEmpty()) {
            return MAPPER.createObjectNode();
        }
        try {
            return MAPPER.readTree(payload);
        } catch (Exception e) {
            // Published with no readable snapshot should not happen. Showing
            // nothing is safer than falling back to the draft.
            return MAPPER.createObjectNode();
        }
    }

    private String text(com.fasterxml.jackson.databind.JsonNode node, String field) {
        com.fasterxml.jackson.databind.JsonNode value = node.get(field);
        return value == null || value.isNull() ? "" : value.asText();
    }

    private void appendLinks(List<ParentContentView.Link> into,
                             com.fasterxml.jackson.databind.JsonNode snapshot) {
        com.fasterxml.jackson.databind.JsonNode links = snapshot.get("links");
        if (links == null || !links.isArray()) {
            return;
        }
        for (com.fasterxml.jackson.databind.JsonNode link : links) {
            into.add(new ParentContentView.Link(text(link, "url"), text(link, "label")));
        }
    }

    /**
     * One mapper, not one per post: constructing it is not free.
     */
    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER =
            new com.fasterxml.jackson.databind.ObjectMapper();

    private Set<Long> seenIdsOf(Enrollment enrollment, List<Post> posts) {
        if (posts.isEmpty()) {
            return java.util.Collections.emptySet();
        }
        List<Long> ids = posts.stream().map(Post::getId).collect(Collectors.toList());
        return new HashSet<>(homeworkSeenRepository.seenPostIds(enrollment.getId(), ids));
    }

    /**
     * The child's current enrollment.
     * <p>
     * One active enrollment per student per year; taken from the student id in
     * the token rather than from anything the caller sent.
     */
    private Enrollment enrollmentOf(Long studentId) throws SGSException {
        List<Enrollment> found = em.createQuery(
                        "select e from Enrollment e where e.student.id = :s "
                                + "and e.academicYear.current = true order by e.id desc", Enrollment.class)
                .setParameter("s", studentId)
                .setMaxResults(1)
                .getResultList();
        if (found.isEmpty()) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, "ჩარიცხვა ვერ მოიძებნა");
        }
        return found.get(0);
    }

    private YearMonth parseMonth(String month) throws SGSException {
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, "თვის ფორმატი არასწორია");
        }
    }

    private LocalDate parseDate(String date) throws SGSException {
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, "თარიღის ფორმატი არასწორია");
        }
    }
}
