-- Ten months become the brief's seven reporting periods.
--
-- The client brief states the calendar explicitly, and twice - once under the
-- absence table and once under the ethical-norms table, in the same shape:
--
--     Sept-Oct, Nov          -> Trimester I
--     Dec, Jan-Feb, Mar      -> Trimester II
--     Apr, May               -> Trimester III
--                            -> Year
--
-- db/013 seeded ten calendar months instead, which differs in four ways, all of
-- them visible to the school:
--
--   1. September and October were separate; the school reports them together.
--   2. January and February were separate; likewise.
--   3. MARCH SAT UNDER TRIMESTER III. The brief puts it in Trimester II, so a
--      March mark has been rolling up into the wrong trimester. This is the one
--      that changes a computed number rather than a column count.
--   4. June existed. The brief's table stops at May.
--
-- June is dropped rather than kept. Removing a period is only safe while
-- nothing points at it - a mark, a permitted-hours setting or an absence notice
-- would go with it - and that window closes the day the school starts entering
-- data. Adding it back later is one INSERT and disturbs nothing, because the
-- roll-ups aggregate over whatever children exist and an empty period changes
-- no computed value under the IGNORE null policy. Cheap to add, expensive to
-- remove: so it goes now and returns if the school turns out to teach in June.
--
-- WHY THE WHOLE LEVEL IS REBUILT rather than edited in place: every one of the
-- ten rows changes identity. Two pairs merge, one changes parent, one goes.
-- Renaming rows to fit would leave the marks attached to whichever old month
-- happened to become the new period, which is a silent reassignment of data.
-- Deleting and rebuilding makes the refusal below the only outcome when
-- anything is attached.
--
-- Run after 013_period_levels.sql. Idempotent: it refuses to run twice by
-- noticing the level already has the shape it is aiming for.

SET
XACT_ABORT ON;
SET
NOCOUNT ON;
SET
QUOTED_IDENTIFIER ON;
SET
ANSI_NULLS ON;

DECLARE
@year   bigint = (SELECT id FROM sgs.academic_year WHERE is_current = 1);
DECLARE
@scheme bigint = (SELECT TOP 1 id FROM sgs.period_scheme WHERE academic_year_id = @year);

IF
@scheme IS NULL
BEGIN
    RAISERROR
('No period scheme for the current year.', 16, 1);
    RETURN;
END

-- Already done: seven reporting periods and the first of them paired.
IF
EXISTS (SELECT 1 FROM sgs.period WHERE scheme_id = @scheme AND depth = 2 AND code = N'R1')
BEGIN
SELECT 'already reshaped' AS result;
RETURN;
END

-- ---- refuse rather than reassign -----------------------------------------
--
-- Everything that can point at a reporting period, checked before anything is
-- deleted. All guards live in this batch with the destructive work, because
-- RAISERROR ... RETURN only exits its own batch - a guard behind a GO would let
-- the deletion run anyway. db/028 was shaped around the same fact.

IF
EXISTS (SELECT 1 FROM sgs.grade_entry g
           JOIN sgs.period p ON p.id = g.period_id
           WHERE p.scheme_id = @scheme AND p.depth = 2)
BEGIN
    RAISERROR
('Marks exist against the monthly periods; refusing to rebuild the level.', 16, 1);
    RETURN;
END

IF
EXISTS (SELECT 1 FROM sgs.class_period_setting s
           JOIN sgs.period p ON p.id = s.period_id
           WHERE p.scheme_id = @scheme AND p.depth = 2)
BEGIN
    RAISERROR
('Absence settings exist against the monthly periods; clear them first.', 16, 1);
    RETURN;
END

IF
EXISTS (SELECT 1 FROM sgs.period child
           JOIN sgs.period parent ON parent.id = child.parent_id
           WHERE parent.scheme_id = @scheme AND parent.depth = 2)
BEGIN
    RAISERROR
('The monthly periods have children; nothing should sit below them.', 16, 1);
    RETURN;
END

BEGIN
TRANSACTION;

DELETE
FROM sgs.period
WHERE scheme_id = @scheme
  AND depth = 2;

-- ---- the seven -----------------------------------------------------------
--
-- Dates are the calendar span the period covers, which is what the daily
-- register cuts its school days from. A paired period is therefore two calendar
-- months wide, and its daily register is correspondingly wide - that is the
-- honest consequence of the school reporting September and October as one
-- thing.
--
-- March sits under Trimester II even though the trimester's own dates end on
-- the 13th. Months sit under the trimester their teaching belongs to rather
-- than by strict date containment - the same rule db/013 stated - and the brief
-- is explicit that March is Trimester II.

INSERT INTO sgs.period (id, scheme_id, parent_id, code, label, ordinal, depth, kind,
                        starts_on, ends_on)
SELECT NEXT VALUE FOR sgs.period_seq, @scheme, t.id, v.code, v.label, v.ordinal, 2, N'REPORTING', v.starts_on, v.ends_on
FROM (VALUES
    (N'T1', N'R1', N'სექტემბერი-ოქტომბერი', 0, CAST ('2025-09-01' AS date), CAST ('2025-10-31' AS date)), (N'T1', N'R2', N'ნოემბერი', 1, CAST ('2025-11-01' AS date), CAST ('2025-11-30' AS date)), (N'T2', N'R3', N'დეკემბერი', 2, CAST ('2025-12-01' AS date), CAST ('2025-12-31' AS date)), (N'T2', N'R4', N'იანვარი-თებერვალი', 3, CAST ('2026-01-01' AS date), CAST ('2026-02-28' AS date)), (N'T2', N'R5', N'მარტი', 4, CAST ('2026-03-01' AS date), CAST ('2026-03-31' AS date)), (N'T3', N'R6', N'აპრილი', 5, CAST ('2026-04-01' AS date), CAST ('2026-04-30' AS date)), (N'T3', N'R7', N'მაისი', 6, CAST ('2026-05-01' AS date), CAST ('2026-05-31' AS date))
    ) v(trimester, code, label, ordinal, starts_on, ends_on)
    JOIN sgs.period t
ON t.scheme_id = @scheme AND t.code = v.trimester;

COMMIT TRANSACTION;
GO

SELECT t.label AS trimester, p.code, p.label, p.starts_on, p.ends_on
FROM sgs.period p
         JOIN sgs.period t ON t.id = p.parent_id
WHERE p.depth = 2
ORDER BY p.ordinal;
GO
