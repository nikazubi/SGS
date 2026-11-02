package mthiebi.sgs.gradebook.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * One piece of staff-authored content: an assignment, a menu, a news item.
 * <p>
 * The client brief asks for five modules beyond grades, and they differ in about
 * four fields while agreeing on everything structural. One table with a
 * {@link PostKind} rather than five that drift apart.
 * <p>
 * Phase 8 carries only the columns homework needs. The schedule's weekday lines,
 * and news's picture and category, are nullable additions phase 9 makes when it
 * has something to put in them - a table built for a guess is worse than a
 * migration.
 */
@Entity
@Table(name = "post", schema = "sgs",
        uniqueConstraints = @UniqueConstraint(name = "uq_post_uuid", columnNames = "uuid"),
        indexes = {
                // The list screen: this class's homework for a subject, newest first.
                @Index(name = "ix_post_class_kind",
                        columnList = "kind,class_group_id,subject_id,event_date"),
                // Phase 11's parent calendar: what was published, by date.
                @Index(name = "ix_post_published", columnList = "kind,status,event_date")
        })
@Getter
@Setter
public class Post extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "post_seq")
    @SequenceGenerator(name = "post_seq", sequenceName = "sgs.post_seq", allocationSize = 50)
    private Long id;

    /**
     * Addressed by uuid rather than id, as journals are: ids differ per environment.
     */
    @Column(name = "uuid", nullable = false, length = 36)
    private String uuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 24)
    private PostKind kind;

    /**
     * Null only for NEWS, which is addressed through {@link #audience} instead.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_group_id", foreignKey = @ForeignKey(name = "fk_post_class"))
    private ClassGroup classGroup;

    /**
     * Homework and characterizations hang off a subject; the others do not.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", foreignKey = @ForeignKey(name = "fk_post_subject"))
    private Subject subject;

    /**
     * The date the content is *for*, not when it was written.
     * <p>
     * A real column rather than something inside the snapshot, because phase
     * 11's parent calendar asks "what was published for this date" and must not
     * have to parse JSON to answer.
     */
    @Column(name = "event_date")
    private LocalDate eventDate;

    @Column(name = "title", length = 512)
    private String title;

    /**
     * Rich text, sanitised on write.
     * <p>
     * What a WYSIWYG editor produces, stripped to a fixed allowlist before it is
     * stored - never on read. Sanitising on read alone would leave the original
     * payload in the database for whoever renders it somewhere else later.
     */
    @Column(name = "body_html", length = 8000)
    private String bodyHtml;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private PostStatus status = PostStatus.DRAFT;

    @Column(name = "published_at")
    private Instant publishedAt;

    /**
     * What parents were shown, as JSON, written at publish time.
     * <p>
     * The school's answer was that **any edit needs a re-publish**, so the
     * working copy above and what parents see are two different things - the
     * same split {@code grade_entry} makes with published_value, and the same
     * reasoning as decision 16.
     * <p>
     * A snapshot rather than mirrored published_* columns because the content
     * spans child tables, and mirroring those is how this becomes a versioning
     * system. Nothing reads it until phase 11; it is written now because the
     * alternative is retrofitting it into a term's worth of accumulated data.
     */
    // Explicitly long. Left to JPA's default this was nvarchar(255), and a
    // snapshot carrying a rich-text body overflows that on the first real
    // publish. Over 4000 with nationalized data becomes nvarchar(MAX).
    @Column(name = "published_payload", length = 20000)
    private String publishedPayload;

    /**
     * Published, then edited: parents are still seeing the older text.
     * <p>
     * The brief describes two states, saved and sent. Frozen publication needs a
     * third, or a teacher edits, walks away satisfied, and the change never
     * reaches anyone. Set by any edit to a published item, cleared on publish.
     */
    @Column(name = "has_unpublished_changes", nullable = false)
    private boolean hasUnpublishedChanges;

    /**
     * Soft delete. The brief's wireframe says "deactivate", and something a
     * parent has already read should leave a trace rather than vanish.
     */
    @Column(name = "is_archived", nullable = false)
    private boolean archived;

    /**
     * Who it is for. Empty means the whole class, which is the common case and
     * the default in the picker.
     */
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<PostTarget> targets = new ArrayList<>();

    /**
     * Links stand in for file attachments: the school's server is short of
     * space, so they asked for links rather than uploads.
     */
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    @OrderBy("ordinal")
    private List<PostLink> links = new ArrayList<>();

    /**
     * The weekday rows of a schedule or a menu.
     * <p>
     * Only those two kinds use them. There is one SCHEDULE and one MENU post per
     * class, standing for the year, so these lines are the document's content
     * rather than a list of separate items.
     */
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    @OrderBy("weekday, ordinal")
    private List<PostLine> lines = new ArrayList<>();

    /**
     * News only. What the item is about.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", foreignKey = @ForeignKey(name = "fk_post_category"))
    private PostCategory category;

    /**
     * News only. The classes it is for; empty means every class.
     * <p>
     * News began school-wide, which the school confirmed at the time. They later
     * asked for items aimed at one class or a few, so this is a set rather than
     * the single {@link #classGroup} homework uses. Empty stays "everyone", which
     * is what every item written before it already means.
     * <p>
     * Classes rather than children, unlike {@link #targets}: a child who joins
     * the class later should see its news, and a list of enrollments frozen at
     * save time would leave them out. Classes are per academic year, so an item
     * aimed at a class stops reaching those children when the year rolls over -
     * which the school was told and accepted.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "post_audience", schema = "sgs",
            joinColumns = @JoinColumn(name = "post_id",
                    foreignKey = @ForeignKey(name = "fk_post_audience_post")),
            inverseJoinColumns = @JoinColumn(name = "class_group_id",
                    foreignKey = @ForeignKey(name = "fk_post_audience_class")),
            indexes = @Index(name = "ix_post_audience_class", columnList = "class_group_id"))
    private Set<ClassGroup> audience = new HashSet<>();

    /**
     * News only. Nullable - the school's grid shows a placeholder where an item
     * has no picture, so an item without one is expected rather than incomplete.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id", foreignKey = @ForeignKey(name = "fk_post_image"))
    private PostImage image;
}
