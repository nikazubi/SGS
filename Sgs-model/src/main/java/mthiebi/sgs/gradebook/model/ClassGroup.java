package mthiebi.sgs.gradebook.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

/**
 * Replaces AcademyClass. Two differences worth noting:
 * <p>
 * - it belongs to an academic year, so the same "9A" in two years is two rows;
 * - there is no isTransit flag. A transit class is simply a class whose
 * template assignment points at a different template.
 */
@Entity
@Table(name = "class_group", schema = "sgs",
        uniqueConstraints = @UniqueConstraint(name = "uq_class_year_school_name",
                columnNames = {"academic_year_id", "school_id", "name"}),
        indexes = @Index(name = "ix_class_year", columnList = "academic_year_id"))
@Getter
@Setter
public class ClassGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "class_group_seq")
    @SequenceGenerator(name = "class_group_seq", sequenceName = "sgs.class_group_seq",
            allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "school_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_class_school"))
    private School school;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_year_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_class_year"))
    private AcademicYear academicYear;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "period_scheme_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_class_scheme"))
    private PeriodScheme periodScheme;

    /**
     * 1..12
     */
    @Column(name = "level", nullable = false)
    private short level;

    @Column(name = "name", nullable = false, length = 64)
    private String name;
}
