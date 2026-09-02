package mthiebi.sgs.gradebook.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.math.RoundingMode;

/**
 * How a DERIVED component computes itself. Structured rather than a formula
 * language, so it can be validated when saved (cycles, dangling refs, weight
 * totals), rendered back in Georgian in the UI, and executed in bulk - none of
 * which is true of free text that has to be evaluated.
 */
@Entity
@Table(name = "derivation_rule", schema = "sgs",
        uniqueConstraints = @UniqueConstraint(name = "uq_rule_component_chain",
                columnNames = {"component_id", "chain_order"}))
@Getter
@Setter
public class DerivationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "derivation_rule_seq")
    @SequenceGenerator(name = "derivation_rule_seq", sequenceName = "sgs.derivation_rule_seq",
            allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "component_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_rule_component"))
    private GradeComponent component;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 24)
    private RuleType type = RuleType.WEIGHTED_SUM;

    @Enumerated(EnumType.STRING)
    @Column(name = "null_policy", nullable = false, length = 16)
    private NullPolicy nullPolicy = NullPolicy.IGNORE;

    /**
     * When a term contributes nothing under IGNORE, rescale the surviving
     * weights back to their original total. Without this a missing 30% final
     * test quietly caps a student at 70% of scale.
     */
    @Column(name = "renormalize_weights", nullable = false)
    private boolean renormalizeWeights = true;

    /**
     * Applied once to the final value, never to intermediates.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "rounding_mode", nullable = false, length = 16)
    private RoundingMode roundingMode = RoundingMode.HALF_UP;

    @Column(name = "decimals", nullable = false)
    private int decimals;

    /**
     * Position in the component's fallback chain: 0 is the rule tried first,
     * 1 the next, and so on. Each is attempted in turn until one produces a
     * value, which is how the legacy "avg(S1,S2), else avg(one, resit), else
     * the resit alone" rule is expressed with no conditional logic in code.
     * <p>
     * An ordered chain rather than a self-referencing fallback link, because a
     * rule belongs to exactly one component: a linked fallback would have no
     * valid component of its own to hang from.
     */
    @Column(name = "chain_order", nullable = false)
    private int chainOrder;
}
