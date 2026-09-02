package mthiebi.sgs.gradebook.service.content;

import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A post as the editor sends it.
 */
@Data
public class PostDraft {

    /**
     * Null when creating.
     */
    private String uuid;

    private Long classGroupId;
    private Long subjectId;
    private LocalDate eventDate;
    private String title;

    /**
     * Rich text. Sanitised on the server before it is stored, never trusted as sent.
     */
    private String bodyHtml;

    /**
     * Enrollment ids. Empty means the whole class, which is the default in the
     * picker and the common case.
     */
    private List<Long> targetEnrollmentIds = new ArrayList<>();

    private List<LinkDraft> links = new ArrayList<>();

    /**
     * Schedule and menu only: the weekday rows of the standing document.
     */
    private List<LineDraft> lines = new ArrayList<>();

    /**
     * News only.
     */
    private String categoryUuid;
    private String imageUuid;

    @Data
    public static class LinkDraft {
        private String url;
        private String label;
    }

    @Data
    public static class LineDraft {
        /**
         * 1 = Monday to 5 = Friday.
         */
        private int weekday;
        private int ordinal;
        /**
         * Free text - "8:00" or "8:00-8:45" or whatever the school types.
         */
        private String timeText;
        private String text;
    }
}
