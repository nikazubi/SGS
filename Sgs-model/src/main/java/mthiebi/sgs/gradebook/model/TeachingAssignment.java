package mthiebi.sgs.gradebook.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

/**
 * Who teaches a subject to a class. Needed by the exports and by the
 * "Subject ... Teacher" header block in the 2026 client brief, both of which
 * want the per-class teacher rather than one global string per subject.
 */
@Entity
@Table(name = "teaching_assignment", schema = "sgs",
        uniqueConstraints = @UniqueConstraint(name = "uq_teaching_assignment",
                columnNames = {"class_subject_id", "system_user_id"}),
        indexes = @Index(name = "ix_ta_user", columnList = "system_user_id"))
@Getter
@Setter
public class TeachingAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "teaching_assignment_seq")
    @SequenceGenerator(name = "teaching_assignment_seq", sequenceName = "sgs.teaching_assignment_seq",
            allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_subject_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_ta_class_subject"))
    private ClassSubject classSubject;

    /**
     * References the existing SYSTEM_USER_TABLE id; kept as a plain column
     * while the legacy auth tables are still in place.
     */
    @Column(name = "system_user_id", nullable = false)
    private Long systemUserId;

    @Column(name = "is_primary", nullable = false)
    private boolean primaryTeacher = true;
}
