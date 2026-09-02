-- Publishing and freezing become two things.
--
-- They were one, and conflating them is what made the absence register hard.
-- Publishing a journal released its cells to parents *and* made them read-only,
-- so any later edit needed the director. For grades that is the entire point.
--
-- For a register it is wrong in a way that only shows up in the workflow: the
-- monthly figure is missed academic hours, which accumulate through the month.
-- The coordinator publishes, more hours are missed, they publish again. That is
-- the normal path, not an exception to it - so a lock would put an approval
-- between them and every top-up, dozens of times a month, for a number nobody
-- disputes. The school was never firm about wanting approval on absence.
--
-- So publishes_blanks is replaced by locks_on_publish. It is one boolean traded
-- for another, and the difference is what each one costs:
--
--   publishes_blanks meant publishing a register wrote a grade_entry row for
--   every student and every day in the period - hundreds per publish, tens of
--   thousands a year - purely so the read-only check had a row to fire on. That
--   is what db/027 existed for, and the code implementing it inserted duplicates
--   and violated uq_grade_cell on the second publish of any period.
--
--   locks_on_publish is read by exactly one `if` in the write path and creates
--   nothing. With the freeze gone there are no blanks to assert: a day missing
--   from the published set was, at publish time, not marked absent, which is
--   precisely what "blank means present" says.
--
-- Publication itself is unchanged. Parents see the published figure and nothing
-- newer until it is published again.
--
-- Change requests are not removed - grades still use them. They are simply never
-- reached for a journal that does not lock, which is what makes wiring approval
-- back in later a matter of flipping this flag.

SET
XACT_ABORT ON;
SET
NOCOUNT ON;
SET
QUOTED_IDENTIFIER ON;
SET
ANSI_NULLS ON;

-- ---- the new flag --------------------------------------------------------

IF
NOT EXISTS (SELECT 1 FROM sys.columns
               WHERE object_id = OBJECT_ID('sgs.grading_template') AND name = 'locks_on_publish')
ALTER TABLE sgs.grading_template
    ADD locks_on_publish bit NULL;
GO

-- Grades lock. That is the default and every existing journal is one.
UPDATE sgs.grading_template
SET locks_on_publish = 1
WHERE locks_on_publish IS NULL;
GO

ALTER TABLE sgs.grading_template ALTER COLUMN locks_on_publish bit NOT NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.default_constraints WHERE name = 'df_template_locks_on_publish')
ALTER TABLE sgs.grading_template
    ADD CONSTRAINT df_template_locks_on_publish DEFAULT 1 FOR locks_on_publish;
GO

-- The monthly register does not. The daily one is gone entirely (db/028).
--
-- Identified by shape, not by name. grading_template has no unique constraint
-- on name and the console lets a journal be renamed, so a rename before this
-- migration would have left the register locking on publish - the exact
-- workflow this script exists to remove - with nothing to notice it.
--
-- MONTH + PERIODS is the register: PERIODS is the transposed grid, which no
-- grades journal uses, and MONTH distinguishes it from anything later.
--
-- Bounded to the oldest such journal - the seeded one. Without the bound this
-- clears the lock on *every* transposed monthly journal, so one built later in
-- the console would silently stop freezing on publish, which is a decision for
-- whoever builds it rather than for this migration.
UPDATE sgs.grading_template
SET locks_on_publish = 0
WHERE id = (SELECT TOP 1 id
            FROM sgs.grading_template
            WHERE frequency = N'MONTH'
              AND grid_mode = N'PERIODS'
            ORDER BY id);
GO

-- ---- the old one goes ----------------------------------------------------
--
-- EXEC because SQL Server compiles the whole batch: deferred name resolution
-- covers a missing table, not a missing column, so a bare reference to a dropped
-- column fails to compile even inside an IF that never runs.

DECLARE
@df sysname = (SELECT dc.name FROM sys.default_constraints dc
                       JOIN sys.columns c ON c.object_id = dc.parent_object_id
                                         AND c.column_id = dc.parent_column_id
                       WHERE dc.parent_object_id = OBJECT_ID('sgs.grading_template')
                         AND c.name = 'publishes_blanks');
IF
@df IS NOT NULL
    EXEC('ALTER TABLE sgs.grading_template DROP CONSTRAINT ' + @df);
GO

-- Only once the replacement is actually in place. Each statement here is its
-- own batch, and nothing aborts the script, so an earlier failure would
-- otherwise leave the table with neither column.
IF EXISTS (SELECT 1 FROM sys.columns
           WHERE object_id = OBJECT_ID('sgs.grading_template') AND name = 'publishes_blanks')
   AND EXISTS (SELECT 1 FROM sys.columns
               WHERE object_id = OBJECT_ID('sgs.grading_template')
                 AND name = 'locks_on_publish' AND is_nullable = 0)
    EXEC('ALTER TABLE sgs.grading_template DROP COLUMN publishes_blanks');
GO

SELECT name, frequency, grid_mode, locks_on_publish
FROM sgs.grading_template
ORDER BY sort_index;
GO
