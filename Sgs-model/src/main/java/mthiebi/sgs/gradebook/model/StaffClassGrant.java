package mthiebi.sgs.gradebook.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

/**
 * One class a member of staff is limited to.
 * <p>
 * Replaces {@code dbo.system_user_table_academy_class_list} as the source of
 * a user's class scope. That table points at {@code dbo.academy_class}, which
 * the roster screens no longer write, so a class created through the console
 * could never be granted to anybody - and the guard had to translate every
 * legacy grant into a class group by name, which a rename or a year rollover
 * silently broke.
 * <p>
 * Holding no rows still means unrestricted, as it always has: the grant is a
 * narrowing, not a requirement.
 * <p>
 * {@code system_user_id} is a plain column rather than an association because
 * staff accounts are still the legacy {@code dbo} entity, which is outside the
 * gradebook model. db/036 adds the foreign key by hand.
 */
@Entity
@Table(name = "staff_class_grant", schema = "sgs",
        uniqueConstraints = @UniqueConstraint(name = "uq_staff_class_grant",
                columnNames = {"system_user_id", "class_group_id"}),
        indexes = @Index(name = "ix_staff_class_grant_class", columnList = "class_group_id"))
@Getter
@Setter
public class StaffClassGrant {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "staff_class_grant_seq")
    @SequenceGenerator(name = "staff_class_grant_seq", sequenceName = "sgs.staff_class_grant_seq",
            allocationSize = 50)
    private Long id;

    @Column(name = "system_user_id", nullable = false)
    private Long systemUserId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_group_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_staff_grant_class"))
    private ClassGroup classGroup;
}
