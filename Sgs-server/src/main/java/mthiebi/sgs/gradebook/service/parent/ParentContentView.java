package mthiebi.sgs.gradebook.service.parent;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * What the parent side of homework and news is served.
 * <p>
 * Deliberately not the staff {@code PostView}. That one carries the draft body,
 * the unpublished-changes flag and the target list - the working state of a
 * document a teacher is still editing. None of it is a parent's business, and a
 * DTO shared between the two is one field away from leaking it.
 */
public final class ParentContentView {

    private ParentContentView() {
    }

    /**
     * A month of the homework calendar.
     * <p>
     * The days are the ones that hold something, not every day of the month -
     * the console draws the grid from the month itself and only needs to know
     * which cells to mark.
     */
    @Data
    public static class HomeworkMonth {
        /**
         * ISO, first of the month being shown.
         */
        private final String month;
        private final List<HomeworkDay> days = new ArrayList<>();
    }

    @Data
    public static class HomeworkDay {
        private final String date;
        private final int total;
        /**
         * How many of that day's assignments this child has not opened.
         * <p>
         * A count rather than a flag, so the console can show "2" as easily as a
         * dot, and so a day that is partly read is distinguishable from one that
         * is wholly unread.
         */
        private final int unseen;
    }

    /**
     * One day, expanded: the subjects that hold something, each with its posts.
     */
    @Data
    public static class HomeworkDayDetail {
        private final String date;
        private final List<HomeworkSubject> subjects = new ArrayList<>();
    }

    @Data
    public static class HomeworkSubject {
        private final Long subjectId;
        private final String subjectName;
        private final List<HomeworkItem> items = new ArrayList<>();
    }

    @Data
    public static class HomeworkItem {
        private final String uuid;
        private final String title;
        private final String bodyHtml;
        private final boolean seen;
        private final List<Link> links = new ArrayList<>();
    }

    @Data
    public static class Link {
        private final String url;
        private final String label;
    }

    /**
     * The class's schedule or menu: five days, each a list of rows.
     * <p>
     * One shape for both, because the difference is a column being null - the
     * schedule types a time against each row and the menu does not.
     */
    @Data
    public static class StandingDoc {
        private final String title;
        private final List<StandingDay> days = new ArrayList<>();
    }

    @Data
    public static class StandingDay {
        /**
         * 1 = Monday to 5 = Friday, as ISO numbers them.
         */
        private final int weekday;
        private final List<StandingLine> lines = new ArrayList<>();
    }

    @Data
    public static class StandingLine {
        /**
         * Free text - "8:00", "8:00-8:45", or nothing at all for a menu.
         */
        private final String timeText;
        private final String text;
    }

    /**
     * One characterization written about this child.
     */
    @Data
    public static class Characterization {
        private final String uuid;
        private final String title;
        private final String bodyHtml;
        private final String date;
        private final String subjectName;
        private final List<Link> links = new ArrayList<>();
    }

    /**
     * The news list.
     */
    @Data
    public static class NewsPage {
        private final List<NewsItem> items = new ArrayList<>();
        private final long total;
    }

    /**
     * One news item.
     * <p>
     * The body travels with the list rather than being fetched on open. A news
     * item is a few paragraphs, the page shows ten of them, and the alternative
     * is a request every time somebody clicks "more" - to render text the
     * browser could already have had.
     */
    @Data
    public static class NewsItem {
        private final String uuid;
        private final String title;
        private final String bodyHtml;
        /**
         * ISO date the item is *about*, which is what the school dates it by.
         */
        private final String date;
        private final String categoryName;
        /**
         * Null when the item has no picture; the console leaves the space out.
         */
        private final String imageUuid;
        private final List<Link> links = new ArrayList<>();
    }
}
