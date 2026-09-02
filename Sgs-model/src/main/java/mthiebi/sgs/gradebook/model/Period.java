package mthiebi.sgs.gradebook.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDate;

/**
 * A node in the reporting calendar tree, e.g.
 * <p>
 * YEAR
 * |- T1 -- SEP_OCT, NOV
 * |- T2 -- DEC, JAN_FEB, MAR
 * +- T3 -- APR, MAY
 * <p>
 * This is what retires the old exactMonth arithmetic. Sep+Oct and Jan+Feb being
 * a single reporting period is now data, not a Feb->Jan / Oct->Sep normalisation
 * rule duplicated across insertStudentGrade and five query builders.
 */
@Entity
@Table(name = "period", schema = "sgs",
        uniqueConstraints = @UniqueConstraint(name = "uq_period_scheme_code",
                columnNames = {"scheme_id", "code"}),
        indexes = @Index(name = "ix_period_scheme_parent", columnList = "scheme_id,parent_id"))
@Getter
@Setter
public class Period {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "period_seq")
    @SequenceGenerator(name = "period_seq", sequenceName = "sgs.period_seq",
            allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scheme_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_period_scheme"))
    private PeriodScheme scheme;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", foreignKey = @ForeignKey(name = "fk_period_parent"))
    private Period parent;

    @Column(name = "code", nullable = false, length = 32)
    private String code;

    @Column(name = "label", nullable = false, length = 128)
    private String label;

    /**
     * Sibling ordering within a parent.
     */
    @Column(name = "ordinal", nullable = false)
    private int ordinal;

    /**
     * Distance from the root, maintained on write. The recompute engine orders
     * affected cells by depth descending so leaf periods resolve before the
     * rollups that consume them; keeping it stored avoids walking the tree
     * on every recompute.
     */
    @Column(name = "depth", nullable = false)
    private int depth;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 16)
    private PeriodKind kind;

    @Column(name = "starts_on")
    private LocalDate startsOn;

    @Column(name = "ends_on")
    private LocalDate endsOn;
}
