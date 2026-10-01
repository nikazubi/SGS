package mthiebi.sgs.gradebook.service.export;

import java.util.ArrayList;
import java.util.List;

/**
 * What a homework PDF contains, independent of who asked for it.
 * <p>
 * The parent portal and the staff console arrange homework differently - one
 * by day for one child, the other by subject for a class over a range - but the
 * page they want printed is the same shape: a heading, then subjects, then
 * assignments. Rendering against this rather than against either console's own
 * view is what lets one renderer serve both; the alternative was a second copy
 * of the layout that would drift from the first.
 */
public final class HomeworkPdfDoc {

    /**
     * Under the title: a date, or a range.
     */
    private final String dateLine;

    /**
     * Who or what this is about - a child and their class, or a class and the
     * filter it was taken from. Null leaves the line out.
     */
    private final String contextLine;

    private final List<Section> sections = new ArrayList<>();

    public HomeworkPdfDoc(String dateLine, String contextLine) {
        this.dateLine = dateLine;
        this.contextLine = contextLine;
    }

    public String getDateLine() {
        return dateLine;
    }

    public String getContextLine() {
        return contextLine;
    }

    public List<Section> getSections() {
        return sections;
    }

    public boolean isEmpty() {
        return sections.stream().allMatch(s -> s.getItems().isEmpty());
    }

    /**
     * One subject's heading and the assignments under it.
     */
    public static final class Section {

        private final String subjectName;
        private final List<Item> items = new ArrayList<>();

        public Section(String subjectName) {
            this.subjectName = subjectName;
        }

        public String getSubjectName() {
            return subjectName;
        }

        public List<Item> getItems() {
            return items;
        }
    }

    public static final class Item {

        private final String title;
        private final String bodyHtml;

        /**
         * A small line between the title and the body: the staff console puts
         * the date and the publication state here, since its list spans days
         * and includes work parents have not been sent. The parent side leaves
         * it null - every assignment in that document is from the one day named
         * at the top, and all of it is published by definition.
         */
        private final String note;

        private final List<Link> links = new ArrayList<>();

        public Item(String title, String bodyHtml, String note) {
            this.title = title;
            this.bodyHtml = bodyHtml;
            this.note = note;
        }

        public String getTitle() {
            return title;
        }

        public String getBodyHtml() {
            return bodyHtml;
        }

        public String getNote() {
            return note;
        }

        public List<Link> getLinks() {
            return links;
        }
    }

    public static final class Link {

        private final String url;
        private final String label;

        public Link(String url, String label) {
            this.url = url;
            this.label = label;
        }

        public String getUrl() {
            return url;
        }

        public String getLabel() {
            return label;
        }
    }
}
