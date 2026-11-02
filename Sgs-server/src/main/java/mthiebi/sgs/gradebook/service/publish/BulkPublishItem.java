package mthiebi.sgs.gradebook.service.publish;

import lombok.Data;

/**
 * One (class, journal) of a bulk release: what it would release, or what it
 * did.
 */
@Data
public class BulkPublishItem {

    private Long classGroupId;
    private String className;
    private String journalUuid;
    private String journalName;
    private Status status;
    private int count;
    private String error;
    public enum Status {
        /**
         * Preview: this many cells would be released.
         */
        READY,
        /**
         * Nothing new since the last release - and so nothing is logged.
         */
        UNCHANGED,
        /**
         * Released; count says how many cells.
         */
        PUBLISHED,
        /**
         * Not released; error says why.
         */
        FAILED
    }
}
