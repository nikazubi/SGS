package mthiebi.sgs.gradebook.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

/**
 * Primary / basic / secondary. The client brief scopes access school -> class,
 * where today the system only knows about classes.
 */
@Entity
@Table(name = "school", schema = "sgs",
        uniqueConstraints = @UniqueConstraint(name = "uq_school_code", columnNames = "code"))
@Getter
@Setter
public class School {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "school_seq")
    @SequenceGenerator(name = "school_seq", sequenceName = "sgs.school_seq",
            allocationSize = 50)
    private Long id;

    @Column(name = "code", nullable = false, length = 32)
    private String code;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "ordinal", nullable = false)
    private int ordinal;
}
