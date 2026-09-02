package mthiebi.sgs.gradebook.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * One row of a weekday's schedule or menu.
 * <p>
 * The school enters these once for the year and adjusts them occasionally, so
 * there is exactly one SCHEDULE post and one MENU post per class and these lines
 * hang off it. No week key, no period: they were explicit that there are no
 * months, no trimesters and no versions - one page, five days.
 * <p>
 * The schedule uses both columns (a time or a range, typed by hand, and free
 * text). The menu uses only the text. One entity rather than two because the
 * difference is a column being null.
 */
@Entity
@Table(name = "post_line", schema = "sgs",
        indexes = @Index(name = "ix_post_line_post", columnList = "post_id,weekday,ordinal"))
@Getter
@Setter
public class PostLine {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "post_line_seq")
    @SequenceGenerator(name = "post_line_seq", sequenceName = "sgs.post_line_seq",
            allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_post_line_post"))
    private Post post;

    /**
     * 1 = Monday to 5 = Friday, as ISO numbers them. The school works Mon-Fri.
     */
    @Column(name = "weekday", nullable = false)
    private int weekday;

    @Column(name = "ordinal", nullable = false)
    private int ordinal;

    /**
     * Free text, not a time type. The school types "8:00" or "8:00-8:45" or
     * whatever they like - the brief asks for a hand-typed time or range, and
     * parsing it would only create a way to reject what someone meant.
     */
    @Column(name = "time_text", length = 64)
    private String timeText;

    @Column(name = "text", length = 1024)
    private String text;
}
