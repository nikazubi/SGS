package mthiebi.sgs.gradebook.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

/**
 * A subject taught to a class, and where column ordering finally becomes data.
 * The old system hardcoded the same Georgian subject list in three places
 * (SubjectOrderUtils, ExcelUtils, MonthlyGradePage/Helper.js) and sorted by
 * index into it.
 * <p>
 * templateVersion is an optional per-subject override; when null the class-wide
 * assignment applies.
 */
@Entity
@Table(name = "class_subject", schema = "sgs",
        uniqueConstraints = @UniqueConstraint(name = "uq_class_subject",
                columnNames = {"class_group_id", "subject_id"}),
        indexes = @Index(name = "ix_class_subject_class", columnList = "class_group_id,sort_index"))
@Getter
@Setter
public class ClassSubject {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "class_subject_seq")
    @SequenceGenerator(name = "class_subject_seq", sequenceName = "sgs.class_subject_seq",
            allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_group_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_cs_class"))
    private ClassGroup classGroup;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_cs_subject"))
    private Subject subject;

    @Column(name = "sort_index", nullable = false)
    private int sortIndex;

    /**
     * Who teaches this subject to this class, as a name.
     * <p>
     * The legacy system held it as free text on the subject row, which is why
     * ინგლისური ენა existed 16 times - one row per teacher. Folding those into
     * one subject would have destroyed the association, and it is a one-way
     * door: 658 class/subject pairs each have exactly one teacher, so the
     * mapping is recoverable now and never again.
     * <p>
     * A name rather than a reference because only 3 of the 98 teachers match a
     * row in SYSTEM_USER_TABLE - most have no login, and the accounts that do
     * exist are spelled in Latin while these are Georgian. {@link
     * TeachingAssignment} is the structured form; this is what is displayed
     * until the people in it have accounts to point at.
     */
    @Column(name = "teacher_name", length = 256)
    private String teacherName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_version_id",
            foreignKey = @ForeignKey(name = "fk_cs_template_version"))
    private TemplateVersion templateVersion;
}
