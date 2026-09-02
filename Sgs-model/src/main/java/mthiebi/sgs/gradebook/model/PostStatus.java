package mthiebi.sgs.gradebook.model;

/**
 * Whether parents have been shown this.
 * <p>
 * Two values rather than three, even though the console shows three states: a
 * published item that has since been edited is still PUBLISHED - parents are
 * still being shown the published snapshot - and the pending edit is carried by
 * {@code hasUnpublishedChanges}. Making "edited" a status would mean a published
 * item stopped counting as published the moment someone fixed a typo.
 */
public enum PostStatus {

    /**
     * Never published. Nothing outside the staff console has seen it.
     */
    DRAFT,

    /**
     * Published at least once. What parents see is the snapshot, not this row.
     */
    PUBLISHED
}
