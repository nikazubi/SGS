package mthiebi.sgs.gradebook.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * What a news item is about - "daycare", and whatever else the school adds.
 * <p>
 * A table behind an autocomplete rather than a free-text tag. The school said
 * either was fine; a table is what stops "საბავშვო ბაღი" and "საბავშვო  ბაღი"
 * becoming two categories through a stray space, while still feeling like free
 * text to whoever is typing.
 */
@Entity
@Table(name = "post_category", schema = "sgs",
        uniqueConstraints = @UniqueConstraint(name = "uq_post_category_name",
                columnNames = "name"))
@Getter
@Setter
public class PostCategory extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "post_category_seq")
    @SequenceGenerator(name = "post_category_seq", sequenceName = "sgs.post_category_seq",
            allocationSize = 50)
    private Long id;

    @Column(name = "uuid", nullable = false, length = 36)
    private String uuid;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    /**
     * Kept rather than deleted: news already filed under it stays readable.
     */
    @Column(name = "is_archived", nullable = false)
    private boolean archived;
}
