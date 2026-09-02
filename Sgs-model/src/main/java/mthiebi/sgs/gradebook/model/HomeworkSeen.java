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
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.time.Instant;

/**
 * One child's parent has opened one homework post.
 * <p>
 * A row means seen. No row means unseen. Nothing else is stored, and there is
 * no third state.
 *
 * <b>Seen, not unseen</b> - the inversion matters. Recording what a parent has
 * <i>not</i> read would mean every new homework post appending a date to every
 * child in the class: one write becoming thirty, correct only for as long as
 * none of them ever fails, and growing without bound across a school year.
 * Recording what they <i>have</i> read means creating homework writes nothing
 * here at all, and "unseen" is a query - the posts this child can see, minus
 * the ones with a row.
 *
 * <b>Per post, not per date.</b> A parent opens Tuesday; the teacher then adds
 * a second assignment for Tuesday. Keyed by date, the day would stay marked as
 * read and the new assignment would never announce itself. Keyed by post, the
 * day lights up again on its own.
 *
 * <b>Why a row rather than a column of comma-separated dates.</b> A list in a
 * column is a read-modify-write: two devices open two different days and the
 * second save silently discards the first. It also makes the debounce the
 * console wants actively dangerous, because the longer a batch is held the more
 * likely it is to overwrite what another device has written in the meantime.
 * Here a batch is a set of inserts - {@code uq_homework_seen} makes a re-send, a
 * double tap and a retry all land on the same state.
 */
@Entity
@Table(name = "homework_seen", schema = "sgs",
        uniqueConstraints = @UniqueConstraint(name = "uq_homework_seen",
                columnNames = {"enrollment_id", "post_id"}))
@Getter
@Setter
public class HomeworkSeen {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "homework_seen_seq")
    @SequenceGenerator(name = "homework_seen_seq", sequenceName = "sgs.homework_seen_seq",
            allocationSize = 50)
    private Long id;

    /**
     * The enrollment, not the student: a child who changes class keeps a clean
     * slate for work that was never theirs to read.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrollment_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_homework_seen_enrollment"))
    private Enrollment enrollment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_homework_seen_post"))
    private Post post;

    /**
     * When it was opened. Not read by anything yet; cheap, and the obvious question later.
     */
    @Column(name = "seen_at", nullable = false)
    private Instant seenAt;
}
