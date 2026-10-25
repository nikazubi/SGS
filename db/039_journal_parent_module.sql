-- Each journal says which module shows it to parents.
--
-- db/038 put "which class sees what" into rows, but which journals GRADES
-- covers was still guessed: a journal drawn periods-across (students down,
-- months across) was taken to be a register like absence and shown to every
-- child. Ethics is drawn periods-across too, so every primary child was offered
-- it - the brief gives ethics to basic and secondary only.
--
-- grading_template.parent_module states it instead:
--
--   null      every child           the absence register
--   GRADES    classes with GRADES   the trimester gradebook, ethics, and any
--                                   journal created from now on
--
-- Any module in sgs.parent_module_grant's list may be named here, so ethics can
-- later get a module of its own (an ETHICS value and its grant rows) without
-- touching the gradebook.
--
-- The absence register is recognised by its chart, ABSENCE_BARS, which db/022
-- seeds and the parent console already keys on. CHECK THE OUTPUT: the absence
-- register must read "(every child)". If it does not, primary 1-4 have lost it:
--
--   UPDATE sgs.grading_template SET parent_module = NULL WHERE id = <its id>;
--
-- Run after 022_absence_journals.sql and 038_parent_module_grant.sql. On a
-- fresh install, after every script that seeds a journal - the backfill below
-- runs once. Idempotent.

SET
XACT_ABORT ON;
SET
NOCOUNT ON;
SET
QUOTED_IDENTIFIER ON;
SET
ANSI_NULLS ON;
GO

-- 001_schema.sql already has the column on a fresh install, generated from the
-- entity.
IF COL_LENGTH('sgs.grading_template', 'parent_module') IS NULL
ALTER TABLE sgs.grading_template
    ADD parent_module varchar(32) NULL;
GO

-- The same list as ck_parent_module_grant_module (db/038): a journal naming a
-- module no class can be granted would be a journal nobody sees.
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints
               WHERE name = 'ck_grading_template_parent_module')
ALTER TABLE sgs.grading_template
    ADD CONSTRAINT ck_grading_template_parent_module
        CHECK (parent_module IS NULL
            OR parent_module IN ('HOMEWORK', 'NEWS', 'SCHEDULE', 'MENU', 'CHARACTERIZATION', 'GRADES'));
GO

-- Once. The default constraint is the marker that this has run: without one,
-- running the script again would put GRADES back on any journal somebody had
-- since deliberately opened to every child.
--
-- The default itself is for journals inserted by script, which name no module:
-- they start gated, as a journal made in the console does. JPA cannot express a
-- default, so 001_schema.sql does not have it and every install gets it here.
IF NOT EXISTS (SELECT 1 FROM sys.default_constraints
               WHERE name = 'df_grading_template_parent_module')
BEGIN
UPDATE sgs.grading_template
SET parent_module = 'GRADES'
WHERE parent_module IS NULL
  AND ISNULL(chart_key, N'') <> N'ABSENCE_BARS';

ALTER TABLE sgs.grading_template
    ADD CONSTRAINT df_grading_template_parent_module
        DEFAULT 'GRADES' FOR parent_module;
END
GO

SELECT id,
       name,
       ISNULL(chart_key, N'—')                 AS chart_key,
       ISNULL(parent_module, N'(every child)') AS shown_to
FROM sgs.grading_template
ORDER BY id;
GO
