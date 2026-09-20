package mthiebi.sgs.gradebook.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

/**
 * One button on the parent's landing page.
 * <p>
 * The school's five parent screens are three journals: per-subject trimester,
 * the summary across subjects and the year view are all the trimester journal
 * at different settings. The rewrite draws them with one page - JournalPage
 * turns a single row into cards on its own - so the screens were never the
 * problem. What was lost was the five labelled doors in front of them.
 * <p>
 * A view is that door and nothing more: which journal, which tier of period,
 * one subject or all of them, which chart, and the words the school puts on it.
 * <p>
 * A journal with no rows here keeps the old behaviour - one button carrying the
 * journal's own name and chart - so a journal invented later needs no migration
 * to appear.
 * <p>
 * Called a menu item rather than a view because {@code ParentView} already
 * means a journal's marks as a parent sees them, and has since the parent side
 * was built. This is a row deciding which of those the landing page offers;
 * the two meet only there.
 */
@Entity
@Table(name = "parent_menu_item", schema = "sgs")
@Getter
@Setter
public class ParentMenuItem extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "parent_menu_item_seq")
    @SequenceGenerator(name = "parent_menu_item_seq", sequenceName = "sgs.parent_menu_item_seq",
            allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_parent_menu_template"))
    private GradingTemplate template;

    /**
     * Order on the landing page. Not the journal's sort index: three of these share a journal.
     */
    @Column(name = "ordinal", nullable = false)
    private int ordinal;

    /**
     * The words on the button.
     * <p>
     * Its own column rather than the journal's name, because three views share
     * one journal and the school's wording for them differs. It also means a
     * rename in the journal editor cannot silently retitle a parent's screen.
     */
    @Column(name = "label", nullable = false, length = 200)
    private String label;

    /**
     * Which tier of period the view opens on, or null for the journal's own.
     * <p>
     * The same enum db/033 settled, where the kind names the tier a column lives
     * on rather than being a year-or-not binary. That is what makes "the annual
     * view" a value here instead of a special case in the console.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "period_kind", length = 20)
    private PeriodKind periodKind;

    @Enumerated(EnumType.STRING)
    @Column(name = "subject_mode", nullable = false, length = 10)
    private SubjectMode subjectMode;

    /**
     * The chart this view draws, independent of the journal's.
     * <p>
     * The three trimester views want three different answers - a trend for one
     * subject, bars across subjects, and nothing at all on the annual table - so
     * it cannot be inherited. Null means no chart, which renders a complete page.
     */
    @Column(name = "chart_key", length = 40)
    private String chartKey;

    /**
     * Which column the chart plots, by component code. Null lets the chart
     * choose, which is the last numeric column it finds.
     * <p>
     * Needed because no ordering gets both summaries right: the trimester one
     * wants the trimester assessment, which is last, and the annual one wants
     * the final academic grade, which is not. It is an editorial choice about
     * which mark is the headline, so it is stated rather than inferred.
     */
    @Column(name = "chart_column", length = 64)
    private String chartColumn;
}
