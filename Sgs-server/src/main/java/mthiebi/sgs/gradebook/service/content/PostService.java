package mthiebi.sgs.gradebook.service.content;

import com.fasterxml.jackson.databind.ObjectMapper;
import mthiebi.sgs.SGSException;
import mthiebi.sgs.SGSExceptionCode;
import mthiebi.sgs.gradebook.model.Enrollment;
import mthiebi.sgs.gradebook.model.Post;
import mthiebi.sgs.gradebook.model.PostKind;
import mthiebi.sgs.gradebook.model.PostLine;
import mthiebi.sgs.gradebook.model.PostLink;
import mthiebi.sgs.gradebook.model.PostStatus;
import mthiebi.sgs.gradebook.model.PostTarget;
import mthiebi.sgs.gradebook.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Staff-authored content: creating it, editing it, and releasing it.
 * <p>
 * Shared by all five of the brief's modules; phase 8 uses it for homework only.
 * Nothing here is specific to homework except the caller's choice of
 * {@link PostKind}.
 * <p>
 * **Publication is frozen.** The school's answer was that any edit needs a
 * re-publish, so what parents see is the snapshot taken at publish time, not the
 * row. That is the same split grade_entry makes with published_value, and the
 * same reasoning as decision 16.
 */
@Service
public class PostService {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private HtmlSanitizer htmlSanitizer;

    @Autowired
    private mthiebi.sgs.gradebook.repository.PostCategoryRepository postCategoryRepository;

    @Autowired
    private mthiebi.sgs.gradebook.repository.PostImageRepository postImageRepository;

    @PersistenceContext
    private EntityManager em;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ---- reading ----------------------------------------------------------

    /**
     * One subject's posts, newest first.
     * <p>
     * {@code limit} is the accordion's "top few"; a null limit is the "see more"
     * dialog asking for everything.
     */
    @Transactional(readOnly = true)
    public List<PostView> list(PostKind kind, Long classGroupId, Long subjectId,
                               LocalDate from, LocalDate to, Integer limit) {
        List<Post> posts = postRepository.findForClass(kind, classGroupId, subjectId, from, to,
                limit == null ? PageRequest.of(0, Integer.MAX_VALUE) : PageRequest.of(0, limit));
        return posts.stream().map(this::toView).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long count(PostKind kind, Long classGroupId, Long subjectId,
                      LocalDate from, LocalDate to) {
        return postRepository.countForClass(kind, classGroupId, subjectId, from, to);
    }

    /**
     * The class's standing schedule or menu, or null when it has none yet.
     * <p>
     * One document per class, so the screen either edits what is there or
     * creates the first one - there is no list.
     */
    @Transactional(readOnly = true)
    public PostView standing(PostKind kind, Long classGroupId) {
        List<Post> found = postRepository.findStanding(kind, classGroupId);
        return found.isEmpty() ? null : toView(found.get(0));
    }

    /**
     * News, which belongs to no class.
     */
    @Transactional(readOnly = true)
    public List<PostView> news(Long categoryId, LocalDate from, LocalDate to, Integer limit) {
        return postRepository.findNews(PostKind.NEWS, categoryId, from, to,
                        limit == null ? PageRequest.of(0, Integer.MAX_VALUE)
                                : PageRequest.of(0, limit))
                .stream().map(this::toView).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PostView get(String uuid) throws SGSException {
        return toView(require(uuid));
    }

    // ---- writing ----------------------------------------------------------

    /**
     * Create or update, without publishing.
     * <p>
     * rollbackFor is explicit because SGSException is checked and Spring rolls
     * back on unchecked exceptions only - the defect a review found in three
     * other services, where a guard refused an operation and committed its
     * partial work anyway.
     */
    @Transactional(rollbackFor = Exception.class)
    public PostView save(PostKind kind, PostDraft draft, Long actorId) throws SGSException {
        Post post;
        if (draft.getUuid() == null || draft.getUuid().isEmpty()) {
            post = new Post();
            post.setUuid(UUID.randomUUID().toString());
            post.setKind(kind);
            post.setStatus(PostStatus.DRAFT);
        } else {
            post = require(draft.getUuid());
        }

        apply(post, draft);
        post.setUpdatedBy(actorId);
        if (post.getCreatedBy() == null) {
            post.setCreatedBy(actorId);
        }

        // Editing something parents have already been shown does not change what
        // they see - it flags that a re-publish is owed. Without this the
        // teacher has no way to tell that their correction is still sitting here.
        if (post.getStatus() == PostStatus.PUBLISHED) {
            post.setHasUnpublishedChanges(true);
        }

        return toView(postRepository.save(post));
    }

    /**
     * Release it, and snapshot what was released.
     * <p>
     * No approval: the school was explicit that this is not the grade publish
     * flow and needs nobody's sign-off.
     */
    @Transactional(rollbackFor = Exception.class)
    public PostView publish(String uuid, Long actorId) throws SGSException {
        Post post = require(uuid);
        if (post.isArchived()) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST,
                    "წაშლილი ჩანაწერის გამოქვეყნება შეუძლებელია");
        }
        post.setStatus(PostStatus.PUBLISHED);
        post.setPublishedAt(Instant.now());
        post.setPublishedPayload(snapshot(post));
        post.setHasUnpublishedChanges(false);
        post.setUpdatedBy(actorId);
        return toView(postRepository.save(post));
    }

    /**
     * Soft delete.
     * <p>
     * The brief's wireframe says "deactivate", and something a parent has
     * already read should leave a trace rather than vanish from the record.
     */
    @Transactional(rollbackFor = Exception.class)
    public void archive(String uuid, boolean archived, Long actorId) throws SGSException {
        Post post = require(uuid);
        post.setArchived(archived);
        post.setUpdatedBy(actorId);
        postRepository.save(post);
    }

    // ---- internals --------------------------------------------------------

    private void apply(Post post, PostDraft draft) throws SGSException {
        // Every kind belongs to a class except news, which is school-wide. The
        // column was made nullable for exactly that in phase 8 and the service
        // then required it anyway - the table was right and this contradicted it.
        if (post.getKind() != PostKind.NEWS && draft.getClassGroupId() == null) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, "კლასი აუცილებელია");
        }
        post.setClassGroup(draft.getClassGroupId() == null ? null
                : em.getReference(mthiebi.sgs.gradebook.model.ClassGroup.class,
                draft.getClassGroupId()));
        post.setSubject(draft.getSubjectId() == null ? null
                : em.getReference(mthiebi.sgs.gradebook.model.Subject.class, draft.getSubjectId()));
        post.setEventDate(draft.getEventDate());
        post.setTitle(trimToNull(draft.getTitle()));

        // Sanitised here, on the way in. Never on the way out: a stored payload
        // would still be waiting for whatever renders it next.
        post.setBodyHtml(htmlSanitizer.clean(draft.getBodyHtml()));

        applyTargets(post, draft);
        applyLinks(post, draft);
        applyLines(post, draft);
        applyNewsFields(post, draft);
    }

    /**
     * The weekday rows of a schedule or a menu.
     * <p>
     * Replaced wholesale rather than merged: the editor sends the document as it
     * should now read, and reconciling row by row would only invent ways for the
     * order to drift from what someone is looking at.
     */
    private void applyLines(Post post, PostDraft draft) {
        post.getLines().clear();
        for (PostDraft.LineDraft line : draft.getLines()) {
            String text = trimToNull(line.getText());
            String time = trimToNull(line.getTimeText());
            if (text == null && time == null) {
                // An empty row the author added and never filled in.
                continue;
            }
            PostLine stored = new PostLine();
            stored.setPost(post);
            stored.setWeekday(line.getWeekday());
            stored.setOrdinal(line.getOrdinal());
            stored.setTimeText(time);
            stored.setText(text);
            post.getLines().add(stored);
        }
    }

    private void applyNewsFields(Post post, PostDraft draft) throws SGSException {
        if (post.getKind() != PostKind.NEWS) {
            return;
        }
        post.setCategory(draft.getCategoryUuid() == null || draft.getCategoryUuid().isEmpty()
                ? null
                : postCategoryRepository.findByUuid(draft.getCategoryUuid())
                .orElseThrow(() -> new SGSException(SGSExceptionCode.BAD_REQUEST,
                        "კატეგორია ვერ მოიძებნა")));

        post.setImage(draft.getImageUuid() == null || draft.getImageUuid().isEmpty()
                ? null
                : postImageRepository.findByUuid(draft.getImageUuid())
                .orElseThrow(() -> new SGSException(SGSExceptionCode.BAD_REQUEST,
                        "სურათი ვერ მოიძებნა")));
    }

    private void applyTargets(Post post, PostDraft draft) {
        post.getTargets().clear();
        for (Long enrollmentId : draft.getTargetEnrollmentIds()) {
            if (enrollmentId == null) {
                continue;
            }
            PostTarget target = new PostTarget();
            target.setPost(post);
            target.setEnrollment(em.getReference(Enrollment.class, enrollmentId));
            post.getTargets().add(target);
        }
    }

    private void applyLinks(Post post, PostDraft draft) {
        post.getLinks().clear();
        int ordinal = 0;
        for (PostDraft.LinkDraft link : draft.getLinks()) {
            String url = htmlSanitizer.cleanUrl(link.getUrl());
            if (url == null) {
                // Silently dropped rather than refused: the field is optional and
                // a half-typed url should not cost someone their homework text.
                continue;
            }
            PostLink stored = new PostLink();
            stored.setPost(post);
            stored.setOrdinal(ordinal++);
            stored.setUrl(url);
            stored.setLabel(trimToNull(link.getLabel()));
            post.getLinks().add(stored);
        }
    }

    /**
     * What parents will be shown, frozen at this moment.
     * <p>
     * Targets are snapshotted as names as well as ids: phase 11 renders this and
     * should not have to join back to enrollments that may since have moved
     * class, or been archived by the annual wipe.
     */
    private String snapshot(Post post) throws SGSException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", post.getTitle());
        payload.put("bodyHtml", post.getBodyHtml());
        payload.put("eventDate", post.getEventDate() == null ? null
                : post.getEventDate().toString());
        payload.put("subjectId", post.getSubject() == null ? null : post.getSubject().getId());

        List<Map<String, Object>> targets = new ArrayList<>();
        for (PostTarget target : post.getTargets()) {
            Map<String, Object> one = new LinkedHashMap<>();
            one.put("enrollmentId", target.getEnrollment().getId());
            one.put("name", studentName(target.getEnrollment()));
            targets.add(one);
        }
        payload.put("targets", targets);

        List<Map<String, Object>> links = new ArrayList<>();
        for (PostLink link : post.getLinks()) {
            Map<String, Object> one = new LinkedHashMap<>();
            one.put("url", link.getUrl());
            one.put("label", link.getLabel());
            links.add(one);
        }
        payload.put("links", links);

        List<Map<String, Object>> lines = new ArrayList<>();
        for (PostLine line : post.getLines()) {
            Map<String, Object> one = new LinkedHashMap<>();
            one.put("weekday", line.getWeekday());
            one.put("ordinal", line.getOrdinal());
            one.put("timeText", line.getTimeText());
            one.put("text", line.getText());
            lines.add(one);
        }
        payload.put("lines", lines);

        payload.put("category", post.getCategory() == null ? null : post.getCategory().getName());
        payload.put("imageUuid", post.getImage() == null ? null : post.getImage().getUuid());

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST,
                    "გამოქვეყნება ვერ მოხერხდა: " + e.getMessage());
        }
    }

    private PostView toView(Post post) {
        PostView view = new PostView();
        view.setUuid(post.getUuid());
        view.setKind(post.getKind().name());
        view.setClassGroupId(post.getClassGroup() == null ? null : post.getClassGroup().getId());
        view.setSubjectId(post.getSubject() == null ? null : post.getSubject().getId());
        view.setSubjectName(post.getSubject() == null ? null : post.getSubject().getName());
        view.setEventDate(post.getEventDate());
        view.setTitle(post.getTitle());
        view.setBodyHtml(post.getBodyHtml());
        view.setStatus(post.getStatus().name());
        view.setPublishedAt(post.getPublishedAt());
        view.setHasUnpublishedChanges(post.isHasUnpublishedChanges());

        view.setCategoryUuid(post.getCategory() == null ? null : post.getCategory().getUuid());
        view.setCategoryName(post.getCategory() == null ? null : post.getCategory().getName());
        view.setImageUuid(post.getImage() == null ? null : post.getImage().getUuid());

        for (PostLine line : post.getLines()) {
            PostDraft.LineDraft one = new PostDraft.LineDraft();
            one.setWeekday(line.getWeekday());
            one.setOrdinal(line.getOrdinal());
            one.setTimeText(line.getTimeText());
            one.setText(line.getText());
            view.getLines().add(one);
        }

        for (PostTarget target : post.getTargets()) {
            view.getTargetEnrollmentIds().add(target.getEnrollment().getId());
            view.getTargetNames().add(studentName(target.getEnrollment()));
        }
        for (PostLink link : post.getLinks()) {
            PostDraft.LinkDraft one = new PostDraft.LinkDraft();
            one.setUrl(link.getUrl());
            one.setLabel(link.getLabel());
            view.getLinks().add(one);
        }
        return view;
    }

    private String studentName(Enrollment enrollment) {
        return enrollment.getStudent().getLastName() + " " + enrollment.getStudent().getFirstName();
    }

    private Post require(String uuid) throws SGSException {
        return postRepository.findByUuid(uuid)
                .orElseThrow(() -> new SGSException(SGSExceptionCode.BAD_REQUEST,
                        "ჩანაწერი ვერ მოიძებნა: " + uuid));
    }

    private String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String trimmed = s.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
