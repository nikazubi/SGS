package mthiebi.sgs.gradebook.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

/**
 * Note there is no class reference here. Class membership lives on Enrollment,
 * so a student can be represented in more than one academic year - which the
 * old schema could not do, since it hung academy_class_id directly off STUDENTS.
 * <p>
 * passwordHash carries a Spring Security DelegatingPasswordEncoder prefix, so
 * migrated rows arrive as {MD5}... and are silently re-hashed to bcrypt on the
 * owner's next successful login.
 */
@Entity
@Table(name = "student", schema = "sgs",
        // Not username alone: two children may share one, provided their
        // passwords differ. What has to be unique is the login pair and the
        // personal number - both are filtered indexes in db/015, because SQL
        // Server permits only a single NULL under a plain UNIQUE constraint and
        // a personal number known to be wrong is cleared rather than kept.
        uniqueConstraints = {},
        indexes = @Index(name = "ix_student_last_name", columnList = "last_name"))
@Getter
@Setter
public class Student extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "student_seq")
    @SequenceGenerator(name = "student_seq", sequenceName = "sgs.student_seq",
            allocationSize = 50)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 128)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 128)
    private String lastName;

    @Column(name = "personal_number", length = 32)
    private String personalNumber;

    @Column(name = "username", nullable = false, length = 64)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 256)
    private String passwordHash;

    /**
     * Guardian address; one login per child, so this is a contact, not an account.
     */
    @Column(name = "guardian_email", length = 256)
    private String guardianEmail;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
