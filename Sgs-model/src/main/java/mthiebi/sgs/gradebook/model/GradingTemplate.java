package mthiebi.sgs.gradebook.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

/**
 * A journal: a named grid the school creates, names, and sees in the menu.
 * <p>
 * The journal itself is just identity and the few things that never vary by
 * version - what it is called, how often it is filled in, and whether it is
 * held per subject. Everything that can change lives on TemplateVersion, so
 * editing a grid never mutates the shape existing marks were entered against.
 * <p>
 * This replaces TemplateScope, which was the three legacy journals written into
 * an enum. The school has run many journals and changes them often, so a
 * journal is a row.
 */
@Entity
@Table(name = "grading_template", schema = "sgs",
        uniqueConstraints = @UniqueConstraint(name = "uq_template_uuid", columnNames = "uuid"))
@Getter
@Setter
public class GradingTemplate extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "grading_template_seq")
    @SequenceGenerator(name = "grading_template_seq", sequenceName = "sgs.grading_template_seq",
            allocationSize = 50)
    private Long id;

    /**
     * The stable external key.
     * <p>
     * The name is the menu label, so renaming a journal is routine - if the
     * name were the identity, every formula referencing it and every assignment
     * would break. The bigint id stays for storage, because sequences are what
     * keep JDBC insert batching available.
     */
    @Column(name = "uuid", nullable = false, length = 36, updatable = false)
    private String uuid = java.util.UUID.randomUUID().toString();

    /**
     * Shown in the menu and on the tab. Free to change.
     */
    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "description", length = 512)
    private String description;

    /**
     * How often it is filled in. ONCE_A_YEAR behaves like a plain table: one
     * grid, and no period dropdown anywhere.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false, length = 16)
    private JournalFrequency frequency = JournalFrequency.ONCE_A_YEAR;

    /**
     * One grid per subject, or one grid for the whole class.
     * <p>
     * Academic grades are per subject; ethics and absence are per student and
     * have no subject at all. Individual columns may still differ - a rating
     * averaged across every subject lives in a per-subject journal - so this is
     * the default the wizard applies, not a constraint.
     */
    @Column(name = "subject_scoped", nullable = false)
    private boolean subjectScoped = true;

    /**
     * Menu order.
     */
    @Column(name = "sort_index", nullable = false)
    private int sortIndex;

    /**
     * Removed from the menu without being deleted. Grades point at a journal,
     * so deleting one would take its history with it.
     */
    @Column(name = "is_archived", nullable = false)
    private boolean archived;

    /**
     * Whether parents see this journal at all.
     * <p>
     * Off by default: a journal the school creates is a staff working document
     * until someone decides otherwise, and an internal tracking grid appearing
     * on the parent portal the moment it is created is the wrong failure. What
     * parents then see inside it is narrowed further per column by
     * {@code GradeComponent.parentVisible}.
     */
    @Column(name = "is_parent_visible", nullable = false)
    private boolean parentVisible;

    /**
     * Which chart the parent view draws, if any.
     * <p>
     * A chart has to know what is an axis and what is a series, so the chart
     * itself is code - but which journal gets which is data. Null means no
     * chart, and the page renders completely without one.
     * <p>
     * Keyed by a stable name rather than by this journal's uuid: uuids are
     * generated per environment, so a uuid-keyed registry would need different
     * code in development and production.
     */
    @Column(name = "chart_key", length = 32)
    private String chartKey;

    /**
     * Which way round this journal's grid is drawn. See {@link GridMode}.
     * <p>
     * Defaults to COMPONENTS, which is every journal but the two absence ones.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "grid_mode", nullable = false, length = 16)
    private GridMode gridMode = GridMode.COMPONENTS;

    /**
     * Whether a published cell of this journal becomes read-only.
     * <p>
     * True for grades, and the reason publication exists at all: once parents
     * have been shown a mark, changing it goes past the director.
     * <p>
     * False for the absence register, which was a deliberate decision rather
     * than an omission. Missed hours accumulate through a month and the
     * coordinator republishes as they do - that is the normal workflow, not an
     * exception to it - so a lock would put an approval between them and every
     * top-up. Publication still means what it means: parents see the published
     * figure and nothing newer until it is published again.
     * <p>
     * Publishing and freezing were one idea until this flag separated them. That
     * conflation is what drove the blank-cell problem: a frozen register had to
     * assert its blanks, which meant writing a row per student per day so the
     * lock had something to fire on. Without the freeze there is nothing to
     * assert - a day missing from the published set was, at publish time, not
     * marked absent, which is exactly what "blank means present" says.
     */
    @Column(name = "locks_on_publish", nullable = false)
    private boolean locksOnPublish = true;

    /**
     * Optional narrowing; null means the journal is offered everywhere.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", foreignKey = @ForeignKey(name = "fk_template_school"))
    private School school;
}
