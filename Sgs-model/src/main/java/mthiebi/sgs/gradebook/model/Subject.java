package mthiebi.sgs.gradebook.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

/**
 * The teacher is deliberately not here. The old Subject.teacher free-text column
 * meant one subject could only ever have one teacher across every class, which
 * is plainly untrue; see TeachingAssignment.
 */
@Entity
@Table(name = "subject", schema = "sgs",
        uniqueConstraints = @UniqueConstraint(name = "uq_subject_name", columnNames = "name"))
@Getter
@Setter
public class Subject extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "subject_seq")
    @SequenceGenerator(name = "subject_seq", sequenceName = "sgs.subject_seq",
            allocationSize = 50)
    private Long id;

    @Column(name = "name", nullable = false, length = 256)
    private String name;

    @Column(name = "short_name", length = 64)
    private String shortName;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
