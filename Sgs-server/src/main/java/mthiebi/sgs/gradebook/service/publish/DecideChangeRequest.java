package mthiebi.sgs.gradebook.service.publish;

import lombok.Data;

@Data
public class DecideChangeRequest {
    private Long changeRequestId;
    private boolean approve;
    /**
     * The director's note. On approval it is what the guardian is emailed.
     */
    private String comment;
}
