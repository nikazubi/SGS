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
import javax.persistence.UniqueConstraint;

/**
 * One student a post is aimed at.
 * <p>
 * Keyed on the enrollment rather than the student, because a post belongs to a
 * class in an academic year and that is exactly what an enrollment is. Keyed on
 * the student instead, homework set for a child would follow them into next
 * year's class.
 * <p>
 * No rows at all means the whole class. That is the common case, so it is the
 * one that costs nothing to store.
 */
@Entity
@Table(name = "post_target", schema = "sgs",
        uniqueConstraints = @UniqueConstraint(name = "uq_post_target",
                columnNames = {"post_id", "enrollment_id"}),
        indexes = @Index(name = "ix_post_target_enrollment", columnList = "enrollment_id"))
@Getter
@Setter
public class PostTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "post_target_seq")
    @SequenceGenerator(name = "post_target_seq", sequenceName = "sgs.post_target_seq",
            allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_post_target_post"))
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrollment_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_post_target_enrollment"))
    private Enrollment enrollment;
}
