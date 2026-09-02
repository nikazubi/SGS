package mthiebi.sgs.gradebook.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.math.BigDecimal;

/**
 * One weighted contribution to a rule. A term's sources may be a single
 * component, a group of components reduced to one number, or the same component
 * across every subject.
 * <p>
 * The group form is what makes "average the classwork columns, then take 25% of
 * that" expressible as configuration.
 */
@Entity
@Table(name = "derivation_term", schema = "sgs",
        indexes = @Index(name = "ix_term_rule", columnList = "rule_id,ordinal"))
@Getter
@Setter
public class DerivationTerm {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "derivation_term_seq")
    @SequenceGenerator(name = "derivation_term_seq", sequenceName = "sgs.derivation_term_seq",
            allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rule_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_term_rule"))
    private DerivationRule rule;

    @Column(name = "ordinal", nullable = false)
    private int ordinal;

    /**
     * 0.50 for "50%". Ignored unless the rule type is WEIGHTED_SUM.
     */
    @Column(name = "weight", nullable = false, precision = 6, scale = 4)
    private BigDecimal weight = BigDecimal.ONE;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_kind", nullable = false, length = 16)
    private SourceKind sourceKind = SourceKind.COMPONENT;

    @Enumerated(EnumType.STRING)
    @Column(name = "reduce", nullable = false, length = 16)
    private ReduceType reduce = ReduceType.AVERAGE;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_ref", nullable = false, length = 16)
    private PeriodRef periodRef = PeriodRef.SAME;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "period_id", foreignKey = @ForeignKey(name = "fk_term_period"))
    private Period period;

    /**
     * Label shown in the explain popover, e.g. "საშუალო(მიმდინარე 1-7)".
     */
    @Column(name = "label", length = 256)
    private String label;
}
