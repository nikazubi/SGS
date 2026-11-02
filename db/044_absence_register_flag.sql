-- Names the absence register instead of guessing it from the grid shape.
--
-- Two places asked "is this the absence register?" and both answered it with
-- `grid_mode = 'PERIODS'`, which actually means "draw this transposed -
-- students down, periods across". That held only while the register was the
-- one transposed journal. db/038 added a second: the ethics journal is drawn
-- the same way because the brief's table has the same columns, and is academic
-- in every other respect.
--
-- What the proxy got wrong once ethics existed:
--
--   * ParentViewService.isRegister() decides what a primary-school parent may
--     see. Ethics would have appeared on all 23 primary classes' landing
--     pages, which the brief scopes to basic and secondary.
--
--   * AbsenceGridService drew the academic-hours and permitted-absence rows on
--     any transposed grid. ClassPeriodSetting is keyed per class and period
--     with no journal column, so those rows appeared on the ethics screen and
--     saving one would have overwritten the absence figures - the ones that
--     turn a parent's chart red.
--
-- Neither was hypothetical; both were reachable the moment db/038 ran. Ethics
-- shipped hidden (is_parent_visible = 0) precisely because of the first.
--
-- Run BEFORE deploying the war that reads the column: the entity maps it NOT
-- NULL, and ddl-auto would otherwise try to add it itself against existing
-- rows.
--
-- Idempotent.

SET XACT_ABORT ON;
SET NOCOUNT ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;

-- Added nullable, backfilled, then tightened - adding a NOT NULL column to a
-- populated table in one step needs a default this model does not declare.
IF NOT EXISTS (SELECT 1 FROM sys.columns
               WHERE object_id = OBJECT_ID('sgs.grading_template')
                 AND name = 'is_absence_register')
ALTER TABLE sgs.grading_template ADD is_absence_register bit NULL;
GO

UPDATE sgs.grading_template
SET is_absence_register = 0
WHERE is_absence_register IS NULL;
GO

-- Identified by chart_key, not by shape or name.
--
-- Shape is the thing that stopped being unique. The name can be edited in the
-- journal console, which is the reason db/029, db/030 and db/035 all refuse to
-- match on it. ABSENCE_BARS is the parent chart that only the register draws,
-- it is set by db/014, and nothing in the console can change it.
DECLARE @register bigint = (
    SELECT TOP 1 id FROM sgs.grading_template
    WHERE chart_key = N'ABSENCE_BARS' AND is_archived = 0 ORDER BY id);

IF @register IS NULL
BEGIN
    -- Better to stop than to flag the wrong journal: leaving every template at
    -- 0 hides the absence register from primary parents, which is a visible
    -- failure, where flagging the wrong one would quietly expose the other.
    RAISERROR('No journal carries chart_key = ABSENCE_BARS; flag not set.', 16, 1);
    RETURN;
END

UPDATE sgs.grading_template SET is_absence_register = 1 WHERE id = @register;
GO

-- Tighten to match the entity, now that every row holds a value.
IF EXISTS (SELECT 1 FROM sys.columns
           WHERE object_id = OBJECT_ID('sgs.grading_template')
             AND name = 'is_absence_register' AND is_nullable = 1)
ALTER TABLE sgs.grading_template ALTER COLUMN is_absence_register bit NOT NULL;
GO

SELECT id,
       name,
       grid_mode,
       is_absence_register,
       is_parent_visible
FROM sgs.grading_template
ORDER BY id;
GO
