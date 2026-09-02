package mthiebi.sgs.gradebook.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * A request to change a grade parents have already been shown.
 * <p>
 * Once a cell is published it is read-only: correcting it needs the director's
 * signature, which is the flow the school runs on.
 * <p>
 * The legacy version stored a Grade reference resolved from the wrong id
 * (`gradeRepository.findById(changeRequest.getId())`, with a TODO admitting
 * it), and duplicated the previous value with nothing tying the two together.
 * Here the cell is a foreign key: it always exists, because a cell has to be
 * published before it can be disputed.
 */
@Entity
@Table(name = "grade_change_request", schema = "sgs",
        indexes = {
                // The director's queue: pending first, newest first.
                @Index(name = "ix_change_request_status", columnList = "status,requested_at"),
                @Index(name = "ix_change_request_entry", columnList = "grade_entry_id")
        })
@Getter
@Setter
public class GradeChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "grade_change_request_seq")
    @SequenceGenerator(name = "grade_change_request_seq",
            sequenceName = "sgs.grade_change_request_seq", allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "grade_entry_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_change_request_entry"))
    private GradeEntry gradeEntry;

    /**
     * What parents were shown when the request was raised.
     * <p>
     * Stored rather than read back at decision time: it is what the requester
     * saw, and if it no longer matches when the director decides, something
     * moved underneath the request and the director should be told rather than
     * silently overruled.
     */
    @Column(name = "previous_value", precision = 6, scale = 2)
    private BigDecimal previousValue;

    @Column(name = "previous_special_value", length = 16)
    private String previousSpecialValue;

    @Column(name = "requested_value", precision = 6, scale = 2)
    private BigDecimal requestedValue;

    @Column(name = "requested_special_value", length = 16)
    private String requestedSpecialValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ChangeRequestStatus status = ChangeRequestStatus.PENDING;

    /**
     * The teacher's explanation. Required: an unexplained request cannot be judged.
     */
    @Column(name = "reason", nullable = false, length = 1024)
    private String reason;

    /**
     * The director's, and what the guardian is told when it is approved.
     */
    @Column(name = "decision_comment", length = 1024)
    private String decisionComment;

    @Column(name = "requested_by")
    private Long requestedBy;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "decided_by")
    private Long decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    /**
     * Two directors opening the queue at once both pass the PENDING check
     * otherwise - publishing twice, emailing twice, or one rejection
     * overwriting the other's applied approval.
     */
    @javax.persistence.Version
    @Column(name = "row_version", nullable = false)
    private int rowVersion;
}
