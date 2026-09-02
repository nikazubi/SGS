package mthiebi.sgs.gradebook.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDate;

/**
 * Where a child sat, and when.
 * <p>
 * A stretch of an enrollment spent in one class. The open one - {@code toDate}
 * null - is where they are now; the closed ones are where they were.
 *
 * <h3>Why this is not simply a second enrollment</h3>
 * <p>
 * Because enrollment is the spine. Five tables key on it - grade_entry,
 * daily_absence, homework_seen, post_target, absence_notice - so a child with
 * two enrollments in one year has their marks split down the middle of it. The
 * annual assessment is an average of the trimesters computed per enrollment, so
 * a transferred child would get two half years and no annual mark at all; the
 * absence yearly total has the same shape; and the parent portal's lookup of
 * "this student's enrollment" would start choosing between two rows.
 * <p>
 * So the year stays whole and the placement history sits beside it.
 * {@link Enrollment#getClassGroup()} remains the current class, which is what
 * every existing query already reads, and this table answers the questions that
 * query cannot: where were they in October, and when did they move.
 *
 * <h3>The duplication, and what keeps it honest</h3>
 * <p>
 * The current class is therefore recorded twice - here, on the open row, and on
 * the enrollment. They can only disagree if something writes one without the
 * other, so exactly one service method moves a child and nothing else assigns
 * {@code class_group_id}. Named here because a reader will notice the
 * duplication and should know it was chosen rather than overlooked.
 */
@Entity
@Table(name = "enrollment_placement", schema = "sgs")
@Getter
@Setter
public class EnrollmentPlacement extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "enrollment_placement_seq")
    @SequenceGenerator(name = "enrollment_placement_seq",
            sequenceName = "sgs.enrollment_placement_seq",
            allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrollment_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_placement_enrollment"))
    private Enrollment enrollment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_group_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_placement_class"))
    private ClassGroup classGroup;

    /**
     * The day they joined this class.
     * <p>
     * Not null: a placement with no start cannot be ordered against the others,
     * and "which class were they in on the 12th" is the only question this table
     * exists to answer.
     */
    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    /**
     * The day they left it, or null while they are still there.
     * <p>
     * Null is the open placement, and there may only be one per enrollment -
     * enforced by a filtered unique index rather than in code, because two
     * people moving the same child at the same moment is exactly the case a
     * check-then-write loses. JPA cannot express a filtered index, so it lives
     * in db/031 next to the table.
     */
    @Column(name = "to_date")
    private LocalDate toDate;
}
