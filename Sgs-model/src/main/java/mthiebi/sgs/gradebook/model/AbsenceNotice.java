package mthiebi.sgs.gradebook.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
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
import java.time.Instant;
import java.time.LocalDate;

/**
 * A parent who is owed a message about a day their child was absent.
 * <p>
 * Marking a cell autosaves in about a second, so sending immediately would mean
 * a mis-click tells a parent their child was absent with no way to unsend.
 * Marking queues a notice here instead; a scheduled job sends anything older
 * than the coalescing window, **re-reading the cell first** - so a correction
 * always wins whenever it lands, rather than depending on beating a timer.
 * <p>
 * One *pending* row per student per day, so several absences on one day become
 * one message rather than one per lesson.
 * <p>
 * Deliberately not a unique constraint on (student, day). A resolved notice must
 * not block a later one: a mark made and withdrawn in the morning is cancelled,
 * and if the child genuinely goes absent that afternoon a second notice has to
 * be possible. With the constraint in place that absence was silently never
 * reported.
 * <p>
 * Keyed on the date alone. It used to carry the day period the mark lived on,
 * so the job could re-read the cell; daily absence is no longer stored against
 * a period, and the enrollment and the date are the whole key to it.
 */
@Entity
@Table(name = "absence_notice", schema = "sgs",
        indexes = {
                @Index(name = "ix_absence_notice_pending", columnList = "sent_at,queued_at"),
                @Index(name = "ix_absence_notice_student", columnList = "enrollment_id,absence_date")
        })
@Getter
@Setter
public class AbsenceNotice {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "absence_notice_seq")
    @SequenceGenerator(name = "absence_notice_seq", sequenceName = "sgs.absence_notice_seq",
            allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrollment_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_absence_notice_enrollment"))
    private Enrollment enrollment;

    /**
     * The day the child was absent, not the day the mark was made.
     */
    @Column(name = "absence_date", nullable = false)
    private LocalDate absenceDate;

    @Column(name = "queued_at", nullable = false)
    private Instant queuedAt;

    /**
     * Null while the notice is still waiting. Set when the message goes out, or
     * when the job decides not to send one because the mark was withdrawn -
     * {@code cancelled} says which.
     */
    @Column(name = "sent_at")
    private Instant sentAt;

    /**
     * Resolved without sending, because the cell was cleared inside the window.
     * <p>
     * Kept rather than deleted so that "we decided not to tell them" is visible
     * afterwards, which is the whole point of the delay.
     */
    @Column(name = "is_cancelled", nullable = false)
    private boolean cancelled;
}
