package mthiebi.sgs.gradebook.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDate;

/**
 * A student's membership of a class for one academic year, and the anchor every
 * grade hangs off. Anchoring grades here rather than on the student means moving
 * a student between classes cannot silently re-parent their history.
 */
@Entity
@Table(name = "enrollment", schema = "sgs",
        uniqueConstraints = @UniqueConstraint(name = "uq_enrollment_student_year",
                columnNames = {"student_id", "academic_year_id"}),
        indexes = {
                @Index(name = "ix_enrollment_class", columnList = "class_group_id"),
                @Index(name = "ix_enrollment_student", columnList = "student_id")
        })
@Getter
@Setter
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "enrollment_seq")
    @SequenceGenerator(name = "enrollment_seq", sequenceName = "sgs.enrollment_seq",
            allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_enrollment_student"))
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_group_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_enrollment_class"))
    private ClassGroup classGroup;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_year_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_enrollment_year"))
    private AcademicYear academicYear;

    @Column(name = "joined_on")
    private LocalDate joinedOn;

    @Column(name = "left_on")
    private LocalDate leftOn;
}
