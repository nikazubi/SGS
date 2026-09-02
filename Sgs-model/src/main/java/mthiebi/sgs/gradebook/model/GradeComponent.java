package mthiebi.sgs.gradebook.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.math.BigDecimal;

/**
 * One column of a gradebook. This is what retires the 95-value GradeType enum
 * and all the gradeType.toString().startsWith(prefix) matching that went with it:
 * adding a column is now an INSERT, not an enum value plus a redeploy.
 * <p>
 * Named GradeComponent rather than Component purely to avoid reading as
 * Spring's @Component at a glance; the table is still "component".
 */
@Entity
@Table(name = "component", schema = "sgs",
        uniqueConstraints = @UniqueConstraint(name = "uq_component_version_code",
                columnNames = {"template_version_id", "code"}),
        indexes = @Index(name = "ix_component_version", columnList = "template_version_id,ordinal"))
@Getter
@Setter
public class GradeComponent {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "component_seq")
    @SequenceGenerator(name = "component_seq", sequenceName = "sgs.component_seq",
            allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_version_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_component_version"))
    private TemplateVersion templateVersion;

    /**
     * Stable within a version, e.g. ONGOING_1, TRIMESTER_GRADE.
     */
    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Column(name = "label", nullable = false, length = 256)
    private String label;

    @Column(name = "ordinal", nullable = false)
    private int ordinal;

    /**
     * Merged column-group header, e.g. "მიმდინარე შეფასება".
     */
    @Column(name = "group_label", length = 256)
    private String groupLabel;

    /**
     * Which tier of the period tree this column is entered or computed at.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "period_kind", nullable = false, length = 16)
    private PeriodKind periodKind = PeriodKind.ROLLUP;

    /**
     * False for ethics, absence and student-wide aggregates such as rating.
     */
    @Column(name = "subject_scoped", nullable = false)
    private boolean subjectScoped = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 16)
    private ComponentKind kind = ComponentKind.INPUT;

    @Column(name = "scale_min", precision = 6, scale = 2)
    private BigDecimal scaleMin;

    @Column(name = "scale_max", precision = 6, scale = 2)
    private BigDecimal scaleMax;

    @Column(name = "decimals", nullable = false)
    private int decimals;

    @Column(name = "allow_special_values", nullable = false)
    private boolean allowSpecialValues;

    /**
     * Lets staff-only working columns exist without leaking to the parent
     * portal - something the old system had no way to express.
     */
    @Column(name = "parent_visible", nullable = false)
    private boolean parentVisible = true;

    /**
     * Whether this column belongs in the cross-period summary.
     * <p>
     * The brief's "trimester and final assessment" table is one row per student
     * reading: Trimester I, Trimester II, Trimester III, Annual, Final exam,
     * Overall, Academic project. The first three are one component shown at
     * three periods; the rest are the year's own columns. So the summary is a
     * transposed grid over several levels - which the register already draws -
     * and the only thing it cannot work out for itself is *which* columns
     * belong in it.
     * <p>
     * Nothing else in the model implies the answer. Every column of the seeded
     * journal is parent-visible; ONGOING_AVG and TRIMESTER_GRADE are both
     * derived and both ungrouped. A report card is an editorial selection, so
     * it is stated rather than inferred.
     * <p>
     * Off by default: a journal that never says otherwise has no summary, which
     * is the right answer for the absence register and for anything created in
     * the wizard before somebody thinks about it.
     */
    @Column(name = "summary_column", nullable = false)
    private boolean summaryColumn = false;

    /**
     * Whether a person may type over a computed value.
     * <p>
     * Defaults to true: a formula is a convenience, not a cage, and a teacher
     * must normally be able to correct what it produced. Setting it false is the
     * deliberate exception, for columns a school wants locked to the calculation.
     * <p>
     * An overridden cell is flagged and audited, is not recomputed while the
     * override stands, and still feeds everything downstream. Releasing the
     * override hands the cell back to the engine.
     */
    @Column(name = "allow_override", nullable = false)
    private boolean allowOverride = true;
}
