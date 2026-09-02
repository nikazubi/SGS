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
import javax.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;

/**
 * One child, one day, absent.
 * <p>
 * A row means absent. No row means present. That is the entire model, and it is
 * the reason this table exists rather than a column on grade_entry.
 * <p>
 * Daily absence was a journal until phase 10 was revised. It fit badly. A
 * journal cell carries a value, and absence has no value - only a tick or a
 * cross - so "absent" was stored as the number 1 and "present" as a blank. That
 * left one question with no good answer: what does a blank cell mean? A mark not
 * yet made, or a child who was there? Every serious bug in the register came out
 * of some part of the system answering it differently from another, and the
 * attempted fix - writing a row per student per day at publish time so the blank
 * became explicit - introduced a duplicate insert that broke publishing outright.
 * <p>
 * Here the question cannot be asked. Existence is the value, so there is no
 * third state to interpret, and {@code uq_daily_absence} makes a child absent
 * twice on one day impossible in the database rather than guarded in a service.
 * <p>
 * What is deliberately absent from this table:
 *
 * <ul>
 *   <li><b>No value, scale or special value.</b> Nothing to validate, nothing to
 *       round, nothing to reject as out of range.</li>
 *   <li><b>No row version.</b> Marking is insert-or-delete and therefore
 *       idempotent: two coordinators marking the same child converge instead of
 *       raising an optimistic-lock conflict over a boolean.</li>
 *   <li><b>No publication columns.</b> The daily register is staff-only. What
 *       parents get is the email, sent the same day, and the monthly hours
 *       figure - which is still a journal, because it holds a real number and is
 *       genuinely published.</li>
 *   <li><b>No period.</b> A date, not a period id. This is what took the daily
 *       register out of the period tree: "days absent in March" is a date range,
 *       not a three-level descent past trimesters that hold nothing.</li>
 * </ul>
 * <p>
 * {@code markedAt} and {@code markedBy} are the only trace of who did what.
 * Dropping the director's approval means nothing else records a change, and one
 * timestamp and one user id are cheap enough that going without would be a
 * choice rather than a saving.
 */
@Entity
@Table(name = "daily_absence", schema = "sgs",
        uniqueConstraints = @UniqueConstraint(name = "uq_daily_absence",
                columnNames = {"enrollment_id", "absence_date"}),
        indexes = @Index(name = "ix_daily_absence_date", columnList = "absence_date"))
@Getter
@Setter
public class DailyAbsence {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "daily_absence_seq")
    @SequenceGenerator(name = "daily_absence_seq", sequenceName = "sgs.daily_absence_seq",
            allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrollment_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_daily_absence_enrollment"))
    private Enrollment enrollment;

    /**
     * The day missed. School days only; nothing writes a weekend.
     */
    @Column(name = "absence_date", nullable = false)
    private LocalDate absenceDate;

    @Column(name = "marked_at", nullable = false)
    private Instant markedAt;

    /**
     * Null only for rows migrated out of grade_entry, which recorded no author.
     */
    @Column(name = "marked_by")
    private Long markedBy;
}
