package mthiebi.sgs.gradebook.service.publish;

import lombok.Data;

/**
 * Scope is (class, period), with subject optional.
 * <p>
 * Legacy had neither: the period was implicit in a timestamp and the subject
 * was never a unit at all. The subject filter is for releasing the rest of a
 * class when one teacher is late.
 */
@Data
public class PublishRequest {
    private Long classGroupId;
    private Long periodId;
    private Long subjectId;

    /**
     * Which journal is being released. Null means every journal at that period,
     * which is what this did before absence arrived.
     * <p>
     * It matters now because publication reaches the levels beneath the chosen
     * period: without it, publishing the absence register's year released every
     * unpublished cell in the class - trimester grades included - and emailed
     * the guardians about all of it. The school was also explicit that the two
     * absence journals are independent of each other.
     */
    private String journalUuid;
}
