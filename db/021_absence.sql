-- Phase 10: the two absence journals.
--
-- Three things happen here.
--
-- 1. Weeks become dated days. Daily absence needs a period per school day, and
--    weeks were numbered *within* a month rather than dated, so days could not
--    hang off them. Checked before deciding: the week level had 40 rows and zero
--    grade entries, and the only journal is trimester-based - it was seeded
--    speculatively in db/013 and never used. Replacing an unused level is far
--    cheaper than adding a fourth one or relaxing the grid's scheme check.
--
-- 2. grading_template gains grid_mode. The absence register is students down,
--    *periods* across - the transpose of every other grid - and that is true of
--    both absence journals, so it is a journal property rather than two
--    hardcoded screens.
--
-- 3. absence_notice: who is owed a message about a day their child was absent.

SET
XACT_ABORT ON;
SET
NOCOUNT ON;
SET
QUOTED_IDENTIFIER ON;
SET
ANSI_NULLS ON;
-- DATEPART(weekday) is relative to DATEFIRST, which is a session setting that
-- varies by login language. Left to the default, a connection with DATEFIRST 1
-- silently generates Saturday columns and drops every Monday. Pinned so the
-- calendar this script builds does not depend on who runs it.
SET
DATEFIRST 7;

-- ---- grid mode -----------------------------------------------------------

IF
NOT EXISTS (SELECT 1 FROM sys.columns
               WHERE object_id = OBJECT_ID('sgs.grading_template') AND name = 'grid_mode')
BEGIN
ALTER TABLE sgs.grading_template
    ADD grid_mode varchar(16) NULL;
END
GO

UPDATE sgs.grading_template
SET grid_mode = 'COMPONENTS'
WHERE grid_mode IS NULL;
GO

ALTER TABLE sgs.grading_template ALTER COLUMN grid_mode varchar(16) NOT NULL;
GO

-- ---- notices -------------------------------------------------------------

IF NOT EXISTS (SELECT 1 FROM sys.sequences WHERE name = 'absence_notice_seq')
CREATE SEQUENCE sgs.absence_notice_seq AS bigint START WITH 1 INCREMENT BY 50;
GO

IF OBJECT_ID('sgs.absence_notice') IS NULL
CREATE TABLE sgs.absence_notice
(
    id            bigint    NOT NULL
        CONSTRAINT pk_absence_notice PRIMARY KEY,
    enrollment_id bigint    NOT NULL,
    period_id     bigint    NOT NULL,
    absence_date  date      NOT NULL,
    queued_at     datetime2 NOT NULL,
    -- Null while waiting. Set when the message goes, or when the job decides
    -- not to send one because the mark was withdrawn - is_cancelled says which.
    sent_at       datetime2 NULL,
    is_cancelled  bit       NOT NULL CONSTRAINT df_absence_notice_cancelled DEFAULT 0,
    -- Deliberately no unique constraint on (enrollment_id, absence_date). It
    -- looked like sensible de-duplication and was a silent failure: a mark made
    -- and withdrawn in the morning leaves a cancelled row, and a real absence
    -- that afternoon reused it rather than making a new one - so the parent was
    -- never told. De-duplication lives in the query, which matches only
    -- *pending* notices. See db/024 for the database this was fixed on.
    CONSTRAINT fk_absence_notice_enrollment FOREIGN KEY (enrollment_id)
        REFERENCES sgs.enrollment,
    CONSTRAINT fk_absence_notice_period FOREIGN KEY (period_id) REFERENCES sgs.period
);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_absence_notice_pending')
CREATE INDEX ix_absence_notice_pending ON sgs.absence_notice (sent_at, queued_at);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_absence_notice_student')
CREATE INDEX ix_absence_notice_student
    ON sgs.absence_notice (enrollment_id, absence_date);
GO

-- ---- days replace weeks --------------------------------------------------
--
-- Refused rather than silently destructive if anything ever did start using
-- weeks: this deletes period rows, and deleting a period a mark hangs off would
-- take the mark with it.

DECLARE
@scheme bigint = (SELECT TOP 1 s.id FROM sgs.period_scheme s
                          JOIN sgs.academic_year y ON y.id = s.academic_year_id
                          WHERE y.is_current = 1);

IF
@scheme IS NULL
BEGIN
    RAISERROR
('No period scheme - run 006_migrate_from_dbo.sql first.', 16, 1);
    RETURN;
END

IF
EXISTS (SELECT 1 FROM sgs.grade_entry g
           JOIN sgs.period p ON p.id = g.period_id
           WHERE p.depth = 3 AND p.scheme_id = @scheme)
BEGIN
    RAISERROR
('Depth 3 periods carry grades; refusing to replace them.', 16, 1);
    RETURN;
END

BEGIN
TRANSACTION;

DELETE
FROM sgs.period
WHERE scheme_id = @scheme
  AND depth = 3;

-- One row per school day of each month, Monday to Friday. Months sit at depth
-- 2 and carry the dates the days are cut from.
--
-- A recursive CTE rather than a loop: the date range is the month's own, so a
-- month whose boundaries move needs no change here.
WITH days AS (SELECT m.id AS month_id, m.starts_on AS d, m.ends_on AS last_day
              FROM sgs.period m
              WHERE m.scheme_id = @scheme
                AND m.depth = 2
                AND m.starts_on IS NOT NULL
              UNION ALL
              SELECT month_id, DATEADD(day, 1, d), last_day
              FROM days
              WHERE d < last_day)
INSERT
INTO sgs.period (id, scheme_id, parent_id, code, label, ordinal, depth, kind,
                        starts_on, ends_on)
SELECT NEXT VALUE FOR sgs.period_seq, @scheme, month_id, CONVERT (nvarchar(10), d, 23), CONVERT (nvarchar(10), d, 23), ROW_NUMBER() OVER (PARTITION BY month_id ORDER BY d), 3, N'REPORTING', d, d
FROM days
-- 1 = Sunday under the DATEFIRST 7 pinned above, so 1 and 7 are the weekend. The
-- school works Monday to Friday, and a register with weekend columns in it is
-- 30 columns where 22 will do.
WHERE DATEPART(weekday, d) NOT IN (1, 7)
OPTION (MAXRECURSION 400);

COMMIT TRANSACTION;
GO

SELECT depth, COUNT(*) AS periods, MIN(starts_on) AS first_day, MAX(starts_on) AS last_day
FROM sgs.period
GROUP BY depth
ORDER BY depth;
GO
