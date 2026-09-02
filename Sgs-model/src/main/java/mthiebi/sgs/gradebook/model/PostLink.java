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
 * A link attached to a post.
 * <p>
 * The school asked for links rather than file uploads - their server is short of
 * space - so this is what an attachment is here.
 * <p>
 * The URL is checked on write: only http and https are stored, because a
 * javascript: href is a script that a parent's browser would run.
 */
@Entity
@Table(name = "post_link", schema = "sgs",
        indexes = @Index(name = "ix_post_link_post", columnList = "post_id,ordinal"))
@Getter
@Setter
public class PostLink {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "post_link_seq")
    @SequenceGenerator(name = "post_link_seq", sequenceName = "sgs.post_link_seq",
            allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_post_link_post"))
    private Post post;

    @Column(name = "ordinal", nullable = false)
    private int ordinal;

    @Column(name = "url", nullable = false, length = 2048)
    private String url;

    /**
     * What the link is called. Falls back to the URL when the author leaves it blank.
     */
    @Column(name = "label", length = 256)
    private String label;
}
