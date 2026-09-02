package mthiebi.sgs.gradebook.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDate;

/**
 * Retained even though the school currently wipes data annually: the wipe is a
 * workaround rather than a requirement, and retrofitting a year dimension later
 * is far more expensive than carrying it now.
 */
@Entity
@Table(name = "academic_year", schema = "sgs",
        uniqueConstraints = @UniqueConstraint(name = "uq_year_code", columnNames = "code"))
@Getter
@Setter
public class AcademicYear {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "academic_year_seq")
    @SequenceGenerator(name = "academic_year_seq", sequenceName = "sgs.academic_year_seq",
            allocationSize = 50)
    private Long id;

    /**
     * e.g. "2025-2026"
     */
    @Column(name = "code", nullable = false, length = 16)
    private String code;

    @Column(name = "starts_on", nullable = false)
    private LocalDate startsOn;

    @Column(name = "ends_on", nullable = false)
    private LocalDate endsOn;

    @Column(name = "is_current", nullable = false)
    private boolean current;
}
