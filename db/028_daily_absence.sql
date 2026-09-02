-- Daily absence leaves grade_entry.
--
-- Why, in one line: a journal cell carries a value, and daily absence has none.
-- "Absent" was stored as the number 1 and "present" as a blank, which left the
-- question "what does a blank cell mean?" with no answer the whole system
-- agreed on - a mark not yet made, or a child who was there. Every serious bug
-- in the register was some part of the code answering it differently from
-- another, and the attempted fix (writing a row per student per day at publish
-- time so the blank became explicit) introduced a duplicate insert that broke
-- publishing outright.
--
-- In sgs.daily_absence a row means absent and no row means present. The third
-- state does not exist, so nothing has to interpret it, and uq_daily_absence
-- makes a child absent twice on one day impossible in the database rather than
-- guarded in a service.
--
-- Three consequences, all of them deletions:
--
--   * The day periods go. Daily absence is keyed on a date, so the ~217 dated
--     depth-3 rows db/021 generated are unused again - exactly like the numbered
--     weeks they replaced. The register's columns are now the weekdays between a
--     month's own two dates, computed in Java, where the weekend filter cannot
--     depend on a session's DATEFIRST setting.
--   * The DAYS_ABSENT rollup goes. It was a DERIVED column summing a DESCENDANTS
--     term three levels down, recomputed and stored on every mark. It is a
--     COUNT(*) over a date range.
--   * absence_notice loses period_id. Enrollment and date are the whole key.
--
-- The monthly register is untouched: it holds a real typed number, has a yearly
-- total and is genuinely published, so it stays a journal.
--
-- ---------------------------------------------------------------------------
-- ORDERING. This script is deliberately shaped around two T-SQL facts that a
-- previous draft got wrong, both of which turn a clean refusal into a half-
-- migrated database:
--
--   1. RAISERROR ... RETURN exits only its OWN batch. Batches after the next GO
--      still run. So every guard and everything destructive live in ONE batch
--      (the last), where RETURN really does stop the rest.
--   2. A foreign key must be gone before its target rows are. absence_notice
--      .period_id references a day period on every notice ever queued, so it is
--      dropped inside the same transaction, before the periods go - not in a
--      batch of its own, where a later guard refusing would still have left the
--      column dropped.
--
-- The destructive work is also one transaction: the marks move, the journal is
-- retired and the periods go, or none of it happens.
-- ---------------------------------------------------------------------------

SET
XACT_ABORT ON;
SET
NOCOUNT ON;
SET
QUOTED_IDENTIFIER ON;
SET
ANSI_NULLS ON;

-- ---- the table -----------------------------------------------------------

IF
NOT EXISTS (SELECT 1 FROM sys.sequences q
               JOIN sys.schemas c ON c.schema_id = q.schema_id
               WHERE q.name = 'daily_absence_seq' AND c.name = 'sgs')
CREATE SEQUENCE sgs.daily_absence_seq AS bigint START WITH 1 INCREMENT BY 50;
GO

IF OBJECT_ID('sgs.daily_absence') IS NULL
CREATE TABLE sgs.daily_absence
(
    id            bigint    NOT NULL
        CONSTRAINT pk_daily_absence PRIMARY KEY,
    enrollment_id bigint    NOT NULL,
    absence_date  date      NOT NULL,
    marked_at     datetime2 NOT NULL,
    -- Null where the source row recorded no author. The director's approval is
    -- not required for absence, so this and marked_at are the only trace of who
    -- changed what.
    marked_by     bigint NULL,
    CONSTRAINT uq_daily_absence UNIQUE (enrollment_id, absence_date),
    CONSTRAINT fk_daily_absence_enrollment FOREIGN KEY (enrollment_id)
        REFERENCES sgs.enrollment
);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes
               WHERE name = 'ix_daily_absence_date'
                 AND object_id = OBJECT_ID('sgs.daily_absence'))
CREATE INDEX ix_daily_absence_date ON sgs.daily_absence (absence_date);
GO

-- ---- guards, then the whole extraction, in one batch and one transaction --

DECLARE
@scheme bigint = (SELECT TOP 1 s.id FROM sgs.period_scheme s
                          JOIN sgs.academic_year y ON y.id = s.academic_year_id
                          WHERE y.is_current = 1);
-- By frequency, not by name. grading_template has no unique constraint on name
-- and the console permits renames, so a renamed register would have left this
-- NULL - and with no day marks to trip a guard, the day periods would have been
-- deleted while the journal survived with its period level gone. Silent.
DECLARE
@daily bigint =
    (SELECT TOP 1 id FROM sgs.grading_template WHERE frequency = N'DAY' ORDER BY id);

IF
@scheme IS NULL
BEGIN
    RAISERROR
('No period scheme - run 006_migrate_from_dbo.sql first.', 16, 1);
    RETURN;
END

-- Nothing but the daily register may own a mark on a day period. Anything else
-- would be silently deleted with the periods below, so refuse instead: a human
-- has to decide what it was.
IF
EXISTS (SELECT 1 FROM sgs.grade_entry g
           JOIN sgs.component c ON c.id = g.component_id
           JOIN sgs.template_version v ON v.id = c.template_version_id
           JOIN sgs.period p ON p.id = g.period_id
           WHERE p.depth = 3 AND p.scheme_id = @scheme
             AND (@daily IS NULL OR v.template_id <> @daily))
BEGIN
    RAISERROR
('A journal other than the daily register has marks on day periods; refusing.', 16, 1);
    RETURN;
END

-- The copy below is bounded to the current scheme's day periods; the teardown
-- deletes every entry of the journal. Anything of this journal's outside that
-- bound would therefore be dropped without being migrated, so refuse instead.
IF
@daily IS NOT NULL AND EXISTS (
        SELECT 1
        FROM sgs.grade_entry g
        JOIN sgs.component c ON c.id = g.component_id
        JOIN sgs.template_version v ON v.id = c.template_version_id
        JOIN sgs.period p ON p.id = g.period_id
        WHERE v.template_id = @daily
          AND c.code = N'ABSENT'
          AND g.value IS NOT NULL AND g.value > 0
          AND (p.scheme_id <> @scheme OR p.depth <> 3))
BEGIN
    RAISERROR
('Daily marks exist outside the current scheme''s day periods; refusing.', 16, 1);
    RETURN;
END

IF
EXISTS (SELECT 1 FROM sgs.derivation_term t
           JOIN sgs.period p ON p.id = t.period_id
           WHERE p.depth = 3 AND p.scheme_id = @scheme)
BEGIN
    RAISERROR
('A SPECIFIC term names a day period; refusing to delete them.', 16, 1);
    RETURN;
END

-- A derivation_source has its own FK to component, independent of the term that
-- owns it, so another journal may read the daily register's columns. Deleting
-- those components would then fail on fk_source_component - or, worse, silently
-- break the other journal's rule. Refuse and let a person unpick it.
IF
@daily IS NOT NULL AND EXISTS (
        SELECT 1
        FROM sgs.derivation_source s
        JOIN sgs.component sc ON sc.id = s.component_id
        JOIN sgs.template_version sv ON sv.id = sc.template_version_id
        JOIN sgs.derivation_term t ON t.id = s.term_id
        JOIN sgs.derivation_rule r ON r.id = t.rule_id
        JOIN sgs.component oc ON oc.id = r.component_id
        JOIN sgs.template_version ov ON ov.id = oc.template_version_id
        WHERE sv.template_id = @daily AND ov.template_id <> @daily)
BEGIN
    RAISERROR
('Another journal reads the daily register''s columns; refusing.', 16, 1);
    RETURN;
END

BEGIN
TRANSACTION;

-- ---- the notice stops pointing at a period -------------------------------
--
-- Before the periods are deleted, because every absence_notice row references
-- the day period its mark lived on and the column is NOT NULL. Inside the
-- transaction, because DDL is transactional in SQL Server and a guard above
-- must be able to leave the database untouched - an earlier draft dropped this
-- column two batches early, so a refusal still cost the notice its period link.
--
-- EXEC because SQL Server compiles a whole batch before running it: deferred
-- name resolution covers a missing table, not a missing column, so a bare
-- reference to a dropped column fails to compile even inside an IF that never
-- runs.

IF
EXISTS (SELECT 1 FROM sys.foreign_keys
           WHERE name = 'fk_absence_notice_period'
             AND parent_object_id = OBJECT_ID('sgs.absence_notice'))
    EXEC('ALTER TABLE sgs.absence_notice DROP CONSTRAINT fk_absence_notice_period');

IF
EXISTS (SELECT 1 FROM sys.columns
           WHERE object_id = OBJECT_ID('sgs.absence_notice') AND name = 'period_id')
    EXEC('ALTER TABLE sgs.absence_notice DROP COLUMN period_id');

IF
@daily IS NOT NULL
BEGIN
    -- ---- move what is there ----------------------------------------------
    --
    -- Deduplicated in a derived table rather than by NOT EXISTS. In SQL Server
    -- a NOT EXISTS subquery reads the state before the statement, so it cannot
    -- see rows the same INSERT is adding - it guards a re-run and nothing else.
    -- Duplicates within one run are entirely possible: uq_grade_cell includes
    -- component_id, so a v1 and a v2 ABSENT cell for the same child and day
    -- coexist legally, and partial version migration is normal.
    --
    -- depth = 3 matters too. Filtering only on starts_on IS NOT NULL would
    -- catch the year, the trimester and the month, all of which begin on the
    -- same date as the first day of the year.
INSERT INTO sgs.daily_absence (id, enrollment_id, absence_date, marked_at, marked_by)
SELECT NEXT VALUE FOR sgs.daily_absence_seq, d.enrollment_id, d.absence_date, d.marked_at, d.marked_by
FROM (
    SELECT g.enrollment_id AS enrollment_id, p.starts_on AS absence_date, MAX (g.updated_at) AS marked_at,
    -- Whoever last touched it. Not perfect provenance, but it is
    -- what the old row recorded and better than discarding it.
    MAX (g.updated_by) AS marked_by
    FROM sgs.grade_entry g
    JOIN sgs.component c ON c.id = g.component_id
    JOIN sgs.template_version v ON v.id = c.template_version_id
    JOIN sgs.period p ON p.id = g.period_id
    WHERE v.template_id = @daily
    AND c.code = N'ABSENT'
    AND g.value IS NOT NULL
    AND g.value > 0
    AND p.depth = 3
    AND p.scheme_id = @scheme
    AND p.starts_on IS NOT NULL
    GROUP BY g.enrollment_id, p.starts_on
    ) d
WHERE NOT EXISTS (SELECT 1 FROM sgs.daily_absence x
    WHERE x.enrollment_id = d.enrollment_id
  AND x.absence_date = d.absence_date);

-- ---- retire the journal ----------------------------------------------
--
-- Children before parents throughout: change requests reference entries,
-- entries reference components, terms reference rules.

DELETE
r
    FROM sgs.grade_change_request r
    JOIN sgs.grade_entry g ON g.id = r.grade_entry_id
    JOIN sgs.component c ON c.id = g.component_id
    JOIN sgs.template_version v ON v.id = c.template_version_id
    WHERE v.template_id = @daily;

    DELETE
g
    FROM sgs.grade_entry g
    JOIN sgs.component c ON c.id = g.component_id
    JOIN sgs.template_version v ON v.id = c.template_version_id
    WHERE v.template_id = @daily;

    -- By component, not by owning term: derivation_source.component_id is its
    -- own foreign key. The guard above has already established that every such
    -- row belongs to the daily journal itself.
    DELETE
s
    FROM sgs.derivation_source s
    JOIN sgs.component sc ON sc.id = s.component_id
    JOIN sgs.template_version sv ON sv.id = sc.template_version_id
    WHERE sv.template_id = @daily;

    DELETE
s
    FROM sgs.derivation_source s
    JOIN sgs.derivation_term t ON t.id = s.term_id
    JOIN sgs.derivation_rule dr ON dr.id = t.rule_id
    JOIN sgs.component c ON c.id = dr.component_id
    JOIN sgs.template_version v ON v.id = c.template_version_id
    WHERE v.template_id = @daily;

    DELETE
t
    FROM sgs.derivation_term t
    JOIN sgs.derivation_rule dr ON dr.id = t.rule_id
    JOIN sgs.component c ON c.id = dr.component_id
    JOIN sgs.template_version v ON v.id = c.template_version_id
    WHERE v.template_id = @daily;

    DELETE
dr
    FROM sgs.derivation_rule dr
    JOIN sgs.component c ON c.id = dr.component_id
    JOIN sgs.template_version v ON v.id = c.template_version_id
    WHERE v.template_id = @daily;

DELETE
FROM sgs.template_assignment
WHERE template_id = @daily;

-- class_subject may pin a version. Unlikely for a class-wide journal, but
-- fk_cs_template_version is real and would fail the delete below.
UPDATE cs
SET cs.template_version_id = NULL FROM sgs.class_subject cs
    JOIN sgs.template_version v
ON v.id = cs.template_version_id
WHERE v.template_id = @daily;

DELETE
c
    FROM sgs.component c
    JOIN sgs.template_version v ON v.id = c.template_version_id
    WHERE v.template_id = @daily;

DELETE
FROM sgs.template_version
WHERE template_id = @daily;
DELETE
FROM sgs.grading_template
WHERE id = @daily;
END

-- ---- and the day periods go too ------------------------------------------
--
-- Everything that points at a day, in dependency order. absence_notice is
-- already clear: its column was dropped two batches ago.

DELETE
pub
FROM sgs.publication pub
JOIN sgs.period p ON p.id = pub.period_id
WHERE p.depth = 3 AND p.scheme_id = @scheme;

DELETE
cps
FROM sgs.class_period_setting cps
JOIN sgs.period p ON p.id = cps.period_id
WHERE p.depth = 3 AND p.scheme_id = @scheme;

UPDATE v
SET v.effective_from_period_id = NULL FROM sgs.template_version v
JOIN sgs.period p
ON p.id = v.effective_from_period_id
WHERE p.depth = 3 AND p.scheme_id = @scheme;

DELETE
FROM sgs.period
WHERE scheme_id = @scheme
  AND depth = 3;

COMMIT TRANSACTION;
GO

SELECT (SELECT COUNT(*) FROM sgs.daily_absence)          AS daily_absence_rows,
       (SELECT COUNT(*) FROM sgs.period WHERE depth = 3) AS day_periods_left,
       (SELECT COUNT(*)
        FROM sgs.grading_template
        WHERE name = N'გაცდენები (დღიური)')              AS daily_journal_left;
GO
