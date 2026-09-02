-- One *pending* notice per student per day, enforced rather than hoped for.
--
-- db/024 dropped uq_absence_notice(enrollment_id, absence_date) because it made
-- a cancelled notice block every later one - a mis-click corrected in the
-- morning meant a real absence that afternoon was silently never reported.
--
-- But dropping it left nothing enforcing the invariant the code still relies on.
-- AbsenceNotifier.queue is a read-then-insert with no lock, so two staff marking
-- the same child at the same moment can both miss the lookup and insert: the
-- parent gets two emails, and every later queue() for that student and day
-- throws from an Optional that now finds two rows - turning each subsequent mark
-- into a 500 *after* the grade has already been written.
--
-- A filtered unique index is the right shape: unique among pending notices,
-- silent about resolved ones. It keeps the db/024 fix and restores the guarantee.

SET
XACT_ABORT ON;
SET
NOCOUNT ON;
SET
QUOTED_IDENTIFIER ON;
SET
ANSI_NULLS ON;

IF
NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'uq_absence_notice_pending')
CREATE UNIQUE INDEX uq_absence_notice_pending
    ON sgs.absence_notice (enrollment_id, absence_date) WHERE sent_at IS NULL;
GO

SELECT 'pending-unique index' AS what,
       CASE
           WHEN EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'uq_absence_notice_pending')
               THEN 1
           ELSE 0 END         AS ok;
GO
