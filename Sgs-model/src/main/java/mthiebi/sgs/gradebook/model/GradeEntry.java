package mthiebi.sgs.gradebook.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.math.BigDecimal;

/**
 * One mark. Addressed by (enrollment, subject, period, component) - "a cell".
 * <p>
 * A single row per grade is what makes the grid configurable: a column is a
 * component row, so adding one is an INSERT rather than DDL. It is also what
 * gives every cell its own version, override flag and source, none of which
 * have anywhere to live in a wide table or a JSON document.
 * <p>
 * This is not stringly-typed EAV: component_id is a foreign key into a
 * validated catalogue scoped to a template version, and values are typed.
 * <p>
 * Nothing outside the recompute engine may write this table, or materialised
 * derived values drift out of true.
 */
@Entity
@Table(name = "grade_entry", schema = "sgs",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_grade_cell",
                columnNames = {"enrollment_id", "subject_id", "period_id", "component_id"}),
        indexes = {
                // The working-set index is NOT declared here. It needs INCLUDE
                // columns so a grid read is covered by the index alone, and JPA has
                // no way to express that - see db/002_indexes.sql.
                @Index(name = "ix_grade_component_period", columnList = "component_id,period_id"),
                @Index(name = "ix_grade_template_version", columnList = "template_version_id")
        })
@Getter
@Setter
public class GradeEntry extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "grade_entry_seq")
    @SequenceGenerator(name = "grade_entry_seq", sequenceName = "sgs.grade_entry_seq",
            allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrollment_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_grade_enrollment"))
    private Enrollment enrollment;

    /**
     * Null for ethics, absence and student-wide aggregates.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", foreignKey = @ForeignKey(name = "fk_grade_subject"))
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "period_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_grade_period"))
    private Period period;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "component_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_grade_component"))
    private GradeComponent component;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_version_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_grade_version"))
    private TemplateVersion templateVersion;

    @Column(name = "value", precision = 6, scale = 2)
    private BigDecimal value;

    /**
     * Replaces the -50 sentinel that meant "ჩთ" and was averaged as a number
     * on some screens while being rendered as text on others.
     */
    @Column(name = "special_value", length = 16)
    private String specialValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 16)
    private GradeSource source = GradeSource.MANUAL;

    /**
     * A derived cell typed over by a person: not recomputed, but still read by
     * everything downstream.
     */
    @Column(name = "is_override", nullable = false)
    private boolean override;

    /**
     * Per-cell optimistic concurrency. Cells are independent, so two people
     * only conflict when they edit the same one.
     */
    @Version
    @Column(name = "row_version", nullable = false)
    private int rowVersion;

    // ---- publication ---------------------------------------------------
    //
    // The value parents see, held separately from the one teachers work on.
    // The legacy system published by timestamp - parent queries filtered
    // `grade.createTime < <close event>` - but grades are updated in place, so
    // createTime never moved and an edit made after publication reached parents
    // immediately, bypassing the director entirely. A cut-off compared against
    // a mutable row cannot work; the row changes underneath it.
    //
    // Publishing copies value -> publishedValue and stamps publishedAt.
    // "Locked" is then `publishedAt != null`, and "changed since publication"
    // is `value != publishedValue` - a state that is now representable rather
    // than invisible.

    @Column(name = "published_value", precision = 6, scale = 2)
    private BigDecimal publishedValue;

    @Column(name = "published_special_value", length = 16)
    private String publishedSpecialValue;

    @Column(name = "published_at")
    private java.time.Instant publishedAt;

    /**
     * Whether a person may edit this cell directly. Recomputation is never
     * blocked: it writes {@code value} only, so a published derived cell may
     * legitimately diverge from what parents were shown, and that divergence is
     * exactly what the change-request flow exists to resolve.
     */
    public boolean isPublished() {
        return publishedAt != null;
    }
}
