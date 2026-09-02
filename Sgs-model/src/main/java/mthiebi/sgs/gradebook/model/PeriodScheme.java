package mthiebi.sgs.gradebook.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

/**
 * A reporting calendar. Separate schemes exist so primary school can run a
 * different shape from basic/secondary without any code branching.
 */
@Entity
@Table(name = "period_scheme", schema = "sgs")
@Getter
@Setter
public class PeriodScheme {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "period_scheme_seq")
    @SequenceGenerator(name = "period_scheme_seq", sequenceName = "sgs.period_scheme_seq",
            allocationSize = 50)
    private Long id;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_year_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_scheme_year"))
    private AcademicYear academicYear;
}
