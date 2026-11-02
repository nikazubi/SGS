package mthiebi.sgs.gradebook.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

/**
 * One rule about what the parent portal shows: classes matching this row get
 * this module.
 * <p>
 * A child's modules are every row their class matches, so rows only ever add.
 * A blank criterion matches everything - a row with no school is for every
 * school, and a row with no levels is for every level of its school. That is
 * the whole language, and it is meant to be edited with an INSERT or a DELETE
 * rather than a deployment: "grades 5 and 6 of primary see grades" is one row
 * (PRIMARY, 5, 6, GRADES).
 * <p>
 * Keyed on school and level rather than on the class, because classes belong
 * to one academic year and rollover makes new ones every September - a rule
 * naming a class would stop applying the year after it was written. A level
 * range keeps applying.
 * <p>
 * The level is {@link ClassGroup#getLevel()}, the grade a class is in (1-12).
 * A class saved without one has level 0, and a ranged row never matches it.
 */
@Entity
@Table(name = "parent_module_grant", schema = "sgs")
@Getter
@Setter
public class ParentModuleGrant extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "parent_module_grant_seq")
    @SequenceGenerator(name = "parent_module_grant_seq",
            sequenceName = "sgs.parent_module_grant_seq", allocationSize = 50)
    private Long id;

    /**
     * Null for every school.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id",
            foreignKey = @ForeignKey(name = "fk_parent_module_grant_school"))
    private School school;

    /**
     * Lowest level the row applies to, inclusive. Null for no lower bound.
     */
    @Column(name = "level_from")
    private Short levelFrom;

    /**
     * Highest level the row applies to, inclusive. Null for no upper bound.
     */
    @Column(name = "level_to")
    private Short levelTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "module", nullable = false, length = 32)
    private ParentModule module;
}
