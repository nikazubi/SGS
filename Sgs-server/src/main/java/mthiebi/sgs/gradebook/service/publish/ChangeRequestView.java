package mthiebi.sgs.gradebook.service.publish;

import lombok.AllArgsConstructor;
import lombok.Data;
import mthiebi.sgs.gradebook.model.ChangeRequestStatus;
import mthiebi.sgs.gradebook.model.GradeChangeRequest;
import mthiebi.sgs.gradebook.model.GradeEntry;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One row of the director's queue.
 * <p>
 * Flattened on purpose: the legacy screen showed the class, student and subject
 * per row and resolved each of them lazily, so drawing the queue cost a handful
 * of queries per row.
 */
@Data
@AllArgsConstructor
public class ChangeRequestView {

    private Long id;
    private Long gradeEntryId;

    private String className;
    private String studentName;
    private String subjectName;
    private String periodLabel;
    private String componentCode;
    private String componentLabel;

    private BigDecimal previousValue;
    private String previousSpecialValue;
    private BigDecimal requestedValue;
    private String requestedSpecialValue;

    /**
     * What the cell says now. If it differs from previousValue, something moved
     * underneath the request after it was raised, and the director should see
     * that rather than be silently overruled by it.
     */
    private BigDecimal currentPublishedValue;

    private ChangeRequestStatus status;
    private String reason;
    private String decisionComment;
    private Long requestedBy;
    private Instant requestedAt;
    private Long decidedBy;
    private Instant decidedAt;

    public static ChangeRequestView of(GradeChangeRequest r) {
        GradeEntry e = r.getGradeEntry();
        return new ChangeRequestView(
                r.getId(),
                e.getId(),
                e.getEnrollment().getClassGroup().getName(),
                e.getEnrollment().getStudent().getLastName() + " "
                        + e.getEnrollment().getStudent().getFirstName(),
                e.getSubject() == null ? null : e.getSubject().getName(),
                e.getPeriod().getLabel(),
                e.getComponent().getCode(),
                e.getComponent().getLabel(),
                r.getPreviousValue(),
                r.getPreviousSpecialValue(),
                r.getRequestedValue(),
                r.getRequestedSpecialValue(),
                e.getPublishedValue(),
                r.getStatus(),
                r.getReason(),
                r.getDecisionComment(),
                r.getRequestedBy(),
                r.getRequestedAt(),
                r.getDecidedBy(),
                r.getDecidedAt());
    }
}
