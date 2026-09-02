-- A resolved absence notice must not block the next genuine one.
--
-- uq_absence_notice(enrollment_id, absence_date) looked like sensible
-- de-duplication and was a silent failure mode: a mark made and withdrawn in
-- the morning leaves a cancelled row, and a real absence that afternoon reused
-- it instead of creating a new one - so the parent was never told, with nothing
-- anywhere recording that a message had been skipped.
--
-- De-duplication now lives in the query, which matches only *pending* notices.
-- An index replaces the constraint, because the lookup is still by student and
-- date.

SET
XACT_ABORT ON;
SET
NOCOUNT ON;
SET
QUOTED_IDENTIFIER ON;
SET
ANSI_NULLS ON;

IF
EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = 'uq_absence_notice')
ALTER TABLE sgs.absence_notice DROP CONSTRAINT uq_absence_notice;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_absence_notice_student')
CREATE INDEX ix_absence_notice_student
    ON sgs.absence_notice (enrollment_id, absence_date);
GO

SELECT 'unique constraint gone' AS what,
       CASE
           WHEN EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = 'uq_absence_notice')
               THEN 0
           ELSE 1 END           AS ok;
GO
