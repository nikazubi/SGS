package mthiebi.sgs.gradebook.service.content;

import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A post as the list and the editor read it.
 */
@Data
public class PostView {

    private String uuid;

    /**
     * Which module this belongs to.
     * <p>
     * Exposed so a controller can refuse to act on a post of another kind. Every
     * write endpoint is addressed by uuid and guarded by a per-module
     * permission, so without this check MANAGE_MENU could publish a schedule
     * simply by putting its uuid in the menu URL.
     */
    private String kind;

    private Long classGroupId;
    private Long subjectId;
    private String subjectName;
    private LocalDate eventDate;
    private String title;
    private String bodyHtml;

    private String status;
    private Instant publishedAt;

    /**
     * Published, then edited - parents are still being shown the older text.
     * <p>
     * The third state the console needs and the brief does not mention. Without
     * it a teacher edits, is satisfied, and the change never reaches anyone,
     * because the school's answer was that every edit needs a re-publish.
     */
    private boolean hasUnpublishedChanges;

    /**
     * Empty means the whole class.
     */
    private List<Long> targetEnrollmentIds = new ArrayList<>();

    /**
     * Shown in the list so a teacher can see who it went to without opening it.
     */
    private List<String> targetNames = new ArrayList<>();

    private List<PostDraft.LinkDraft> links = new ArrayList<>();

    /**
     * Schedule and menu only.
     */
    private List<PostDraft.LineDraft> lines = new ArrayList<>();

    /**
     * News only. The name as well as the uuid, so a grid need not resolve it.
     */
    private String categoryUuid;
    private String categoryName;

    /**
     * News only. The grid shows a placeholder where this is null.
     */
    private String imageUuid;
}
