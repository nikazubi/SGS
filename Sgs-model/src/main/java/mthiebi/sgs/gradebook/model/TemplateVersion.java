package mthiebi.sgs.gradebook.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.Instant;

/**
 * An immutable-once-published snapshot of a template's shape.
 * <p>
 * Every grade_entry stores the version it was entered against, so a mid-year
 * edit to a grid cannot retroactively re-render marks that have already gone
 * out to parents. Changes create a new version effective from a stated period
 * forward; earlier periods keep computing against the version they were
 * entered under.
 */
@Entity
@Table(name = "template_version", schema = "sgs",
        uniqueConstraints = @UniqueConstraint(name = "uq_template_version_no",
                columnNames = {"template_id", "version_no"}),
        indexes = @Index(name = "ix_tv_template_status", columnList = "template_id,status"))
@Getter
@Setter
public class TemplateVersion extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "template_version_seq")
    @SequenceGenerator(name = "template_version_seq", sequenceName = "sgs.template_version_seq",
            allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_tv_template"))
    private GradingTemplate template;

    @Column(name = "version_no", nullable = false)
    private int versionNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private TemplateVersionStatus status = TemplateVersionStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "period_scheme_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_tv_scheme"))
    private PeriodScheme periodScheme;

    /**
     * Null means "from the start of the year".
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "effective_from_period_id",
            foreignKey = @ForeignKey(name = "fk_tv_effective_period"))
    private Period effectiveFromPeriod;

    @Column(name = "activated_at")
    private Instant activatedAt;
}
