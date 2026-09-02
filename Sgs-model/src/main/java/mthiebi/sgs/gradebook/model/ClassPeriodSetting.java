package mthiebi.sgs.gradebook.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.math.BigDecimal;

/**
 * Per class, per period settings that are not per student - the client brief's
 * "total academic hours for the month" and "permitted number of missed hours",
 * the latter driving the green-to-red threshold on the absence chart.
 * <p>
 * Kept as key/value rather than columns so the absence module can add settings
 * without a schema change, in the same spirit as the template model.
 */
@Entity
@Table(name = "class_period_setting", schema = "sgs",
        uniqueConstraints = @UniqueConstraint(name = "uq_cps_class_period_key",
                columnNames = {"class_group_id", "period_id", "setting_key"}))
@Getter
@Setter
public class ClassPeriodSetting extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "class_period_setting_seq")
    @SequenceGenerator(name = "class_period_setting_seq", sequenceName = "sgs.class_period_setting_seq",
            allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_group_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_cps_class"))
    private ClassGroup classGroup;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "period_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_cps_period"))
    private Period period;

    /**
     * TOTAL_ACADEMIC_HOURS, PERMITTED_MISSED_HOURS
     */
    @Column(name = "setting_key", nullable = false, length = 64)
    private String settingKey;

    @Column(name = "setting_value", precision = 12, scale = 2)
    private BigDecimal settingValue;
}
