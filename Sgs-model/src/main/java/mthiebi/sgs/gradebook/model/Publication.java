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

/**
 * One release of grades to parents.
 * <p>
 * The audit trail, not the mechanism: what parents can actually see is decided
 * per cell by grade_entry.published_at. This records who released which scope
 * and when, which is the list the close-period screen shows.
 * <p>
 * Deriving that list by scanning grade_entry for distinct timestamps would be
 * both slower and lossy - a republish overwrites the previous stamp.
 */
@Entity
@Table(name = "publication", schema = "sgs",
        indexes = {
                @Index(name = "ix_publication_class_period",
                        columnList = "class_group_id,period_id,published_at"),
                @Index(name = "ix_publication_at", columnList = "published_at")
        })
@Getter
@Setter
public class Publication {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "publication_seq")
    @SequenceGenerator(name = "publication_seq", sequenceName = "sgs.publication_seq",
            allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_group_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_publication_class"))
    private ClassGroup classGroup;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "period_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_publication_period"))
    private Period period;

    /**
     * Null means every subject the class takes - the usual case.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id",
            foreignKey = @ForeignKey(name = "fk_publication_subject"))
    private Subject subject;

    @Column(name = "published_at", nullable = false)
    private Instant publishedAt;

    @Column(name = "published_by")
    private Long publishedBy;

    /**
     * How many cells this release actually moved, for the audit list.
     */
    @Column(name = "cell_count", nullable = false)
    private int cellCount;

    /**
     * Set when an approved change request released a cell and everything
     * published downstream of it, rather than a person releasing a period.
     */
    @Column(name = "from_change_request", nullable = false)
    private boolean fromChangeRequest;
}
