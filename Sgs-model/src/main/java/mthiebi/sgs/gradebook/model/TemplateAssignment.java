package mthiebi.sgs.gradebook.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

/**
 * Which version of a journal a class uses, optionally narrowed to one subject.
 * <p>
 * Keyed by journal rather than by scope: a class can keep one journal on an
 * older version while another moves on, which is what makes migrating a period
 * a per-journal decision. This is also where AcademyClass.isTransit went - a
 * transit class is just a class pointed at a different version.
 */
@Entity
@Table(name = "template_assignment", schema = "sgs",
        uniqueConstraints = @UniqueConstraint(name = "uq_assignment_class_subject_journal",
                columnNames = {"class_group_id", "subject_id",
                        "template_id"}),
        indexes = @Index(name = "ix_assignment_class", columnList = "class_group_id"))
@Getter
@Setter
public class TemplateAssignment extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "template_assignment_seq")
    @SequenceGenerator(name = "template_assignment_seq", sequenceName = "sgs.template_assignment_seq",
            allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_group_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_assignment_class"))
    private ClassGroup classGroup;

    /**
     * Null means every subject in the class.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", foreignKey = @ForeignKey(name = "fk_assignment_subject"))
    private Subject subject;

    /**
     * The journal. Denormalised from templateVersion.template so the unique
     * constraint can hold: one assignment per class, subject and journal,
     * whichever version it currently points at.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_assignment_template"))
    private GradingTemplate template;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_version_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_assignment_version"))
    private TemplateVersion templateVersion;
}
