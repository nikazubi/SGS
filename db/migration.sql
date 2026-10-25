-- =============================================================================
-- migration.sql - bring a production database up to date for the October 2026
-- parent-portal work.
--
-- WHAT IT DOES
--
--   1. Landing tiles (db/035). The five buttons on the parent landing page -
--      three views of the trimester journal, ethics, absence. Seeds only if
--      sgs.parent_menu_item is empty, so a database that already has them is
--      left as it is.
--   2. Who sees which parent pages (db/038). sgs.parent_module_grant: a row per
--      rule - school, grade range, module. Seeded with what the code used to
--      hard-wire, plus grades 5 and 6 of primary seeing grades.
--   3. Which module shows each journal (db/039). grading_template.parent_module:
--      the absence register for every child, the gradebook and ethics only for
--      classes with GRADES. Stops ethics reaching primary 1-4.
--   4. Class levels. Grades 5 and 6 are recognised by class_group.level. A class
--      saved on the Classes screen with საფეხური left blank has level 0 and
--      would never match; this fills it from the class name ("5ა" -> 5), the
--      rule db/006 used, for those classes only.
--   5. A report to read before deploying - see "CHECK THE OUTPUT" below.
--
-- WHAT IT DOES NOT DO
--
--   It deletes nothing and changes no grade, student, enrollment or account.
--   It does not run db/037 (which deletes every class) or anything else from
--   the numbered chain. It assumes the rewrite is already in place: the sgs
--   schema, the three schools, the journals.
--
-- ORDER: RUN THIS BEFORE DEPLOYING THE NEW BACKEND.
--
--   The backend starts with ddl-auto: update. Deployed first, it creates
--   sgs.parent_module_grant empty and grading_template.parent_module blank -
--   and an empty rule table means every parent is refused every page. If that
--   has already happened, run this straight away: it handles both states.
--
-- HOW
--
--   sqlcmd -S <server> -d <database> -U <user> -P <password> -b -f 65001 -i db/migration.sql
--
--   or open it in SSMS and execute (no SQLCMD mode needed). Safe to run again:
--   every step checks before it writes, and a second run changes nothing.
--
-- CHECK THE OUTPUT
--
--   - Journals: the absence register (გაცდენილი საათები) must read
--     "(every child)". If it does not, primary 1-4 have lost it - see db/039.
--   - Classes: every 5th and 6th grade class must say "yes" under
--     parents_see_grades, and no class should show level 0.
--
-- Assembled from db/035_parent_menu.sql, db/038_parent_module_grant.sql and
-- db/039_journal_parent_module.sql, unchanged. A fresh install runs those
-- through the normal chain instead (PROJECT-STATE.md section 7).
-- =============================================================================

SET
NOCOUNT ON;
GO

-- ---- preflight --------------------------------------------------------------
-- Stops here, writing nothing, on a database without the rewrite. NOEXEC makes
-- every batch below compile and do nothing, in sqlcmd and SSMS alike.

IF OBJECT_ID('sgs.school') IS NULL
    OR OBJECT_ID('sgs.class_group') IS NULL
    OR OBJECT_ID('sgs.grading_template') IS NULL
BEGIN
    RAISERROR
('migration.sql: no sgs schema here - is this the right database? Nothing was changed.', 16, 1);
    SET
NOEXEC ON;
END
ELSE IF (SELECT COUNT(*) FROM sgs.school WHERE code IN (N'PRIMARY', N'BASIC', N'SECONDARY')) < 3
BEGIN
    RAISERROR
('migration.sql: sgs.school is missing PRIMARY, BASIC or SECONDARY. Nothing was changed.', 16, 1);
    SET
NOEXEC ON;
END
GO


-- =============================================================================
-- 1. Landing tiles - db/035_parent_menu.sql
-- =============================================================================

-- The five doors the school's parents already know.
--
-- The legacy parent console had five buttons, and the brief describes the same
-- five because the brief was describing what existed:
--
--   მოსწავლის ტრიმესტრული შეფასება აკადემიური დისციპლინების მიხედვით
--   მოსწავლის შემაჯამებელი ტრიმესტრული შეფასება
--   მოსწავლის შეფასება ეთიკური ნორმების მიხედვით
--   მოსწავლის ტრიმესტრული და წლიური შეფასება
--   მოსწავლის მიერ გაცდენილი საათები
--
-- Three of those are one journal. Per-subject detail, the summary across
-- subjects and the year view are the trimester journal at different settings,
-- which is why the rewrite draws all three with one page: JournalPage turns a
-- single row into cards by itself, so "drill into a subject" was never a second
-- screen. The screens were never lost. The labelled doors in front of them were.
--
-- A view is a door: a journal, a tier of period, one subject or all, a chart,
-- and the school's own wording. Everything it names already existed - this adds
-- no behaviour, only the entry points.
--
-- WHY THE CHART IS ON THE VIEW AND NOT INHERITED. The three trimester views
-- want three different answers: a trend for one subject, bars across subjects,
-- and nothing at all on the annual table. grading_template.chart_key stays as
-- it is and remains the fallback for a journal with no views.
--
-- WHY THE LABEL IS ITS OWN COLUMN. Three views share a journal, and the school
-- words them differently. It also means renaming a journal in the editor cannot
-- silently retitle a parent's screen.
--
-- A journal with no rows here keeps today's behaviour - one button carrying its
-- own name and chart - so a journal invented later needs no migration to show up.
--
-- Run after 034_summary_columns.sql. Idempotent.

SET
XACT_ABORT ON;
SET
NOCOUNT ON;
SET
QUOTED_IDENTIFIER ON;
SET
ANSI_NULLS ON;

IF
NOT EXISTS (SELECT 1 FROM sys.sequences
               WHERE object_id = OBJECT_ID('sgs.parent_menu_item_seq'))
BEGIN
CREATE SEQUENCE sgs.parent_menu_item_seq AS bigint START WITH 1 INCREMENT BY 50;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.tables
               WHERE object_id = OBJECT_ID('sgs.parent_menu_item'))
BEGIN
CREATE TABLE sgs.parent_menu_item
(
    id           bigint    NOT NULL,
    template_id  bigint    NOT NULL,
    ordinal      int       NOT NULL,
    label        nvarchar(200) NOT NULL,
    -- REPORTING | ROLLUP | YEAR, or null for the journal's own level. The
    -- enum db/033 settled, where the kind names the tier a column lives on.
    period_kind  nvarchar(20)  NULL,
    -- ONE | ALL
    subject_mode nvarchar(10)  NOT NULL,
    chart_key    nvarchar(40)  NULL,
    -- Which column that chart plots, by component code. Null lets the chart
    -- choose - the last numeric one - which is right for the trimester
    -- summary and wrong for the annual table, where the mark worth
    -- comparing is the final academic grade rather than the last column.
    chart_column nvarchar(64)  NULL,
    created_at   datetime2 NOT NULL,
    updated_at   datetime2 NOT NULL,
    CONSTRAINT pk_parent_menu_item PRIMARY KEY (id),
    CONSTRAINT fk_parent_menu_template FOREIGN KEY (template_id)
        REFERENCES sgs.grading_template (id)
);

CREATE INDEX ix_parent_menu_ordinal ON sgs.parent_menu_item (ordinal, id);
END
GO

-- ---- the five ------------------------------------------------------------
--
-- Matched by journal SHAPE, never by name: grading_template has no unique
-- constraint on name and the console permits renames, which is the reason
-- db/029 and db/030 both give for doing it this way.
--
--   trimester journal  the COMPONENTS one
--   absence register   the first MONTH + PERIODS, by id
--   ethics             the second MONTH + PERIODS, by id
--
-- Labels are the school's own, minus the leading "მოსწავლის". It is on all five,
-- and it is redundant on a console that shows one child and puts their name in
-- the header - and the longest label is three tiles to a row, where sixty-two
-- characters wraps to five lines and drags the whole row down after it. The
-- full wording still heads the page the button opens.
--
-- The guard is on the INSERT itself (below). It used to be a batch of its own,
-- "IF EXISTS ... RETURN", which ends only that batch - the INSERT after the GO
-- ran anyway, so a second run seeded all five buttons twice.

DECLARE
@trimester bigint = (
    SELECT TOP 1 id FROM sgs.grading_template
    WHERE grid_mode = N'COMPONENTS' AND is_archived = 0 ORDER BY id);

DECLARE
@absence bigint = (
    SELECT TOP 1 id FROM sgs.grading_template
    WHERE frequency = N'MONTH' AND grid_mode = N'PERIODS' AND is_archived = 0
    ORDER BY id);

DECLARE
@ethics bigint = (
    SELECT id FROM sgs.grading_template
    WHERE frequency = N'MONTH' AND grid_mode = N'PERIODS' AND is_archived = 0
      AND id <> ISNULL(@absence, -1)
    ORDER BY id OFFSET 0 ROWS FETCH NEXT 1 ROWS ONLY);

-- A database without one of these simply gets fewer rows. The console falls
-- back to one button per journal for anything not named here, so a partial
-- seed is a smaller landing page rather than a broken one.

IF
NOT EXISTS (SELECT 1 FROM sgs.parent_menu_item)
INSERT INTO sgs.parent_menu_item (id, template_id, ordinal, label, period_kind,
                                  subject_mode, chart_key, chart_column, created_at, updated_at)
SELECT NEXT VALUE FOR sgs.parent_menu_item_seq, v.template_id, v.ordinal, v.label, v.period_kind, v.subject_mode, v.chart_key, v.chart_column, SYSUTCDATETIME(), SYSUTCDATETIME()
FROM (VALUES
    -- ONE subject: the seven ongoing marks, the three tests and the trimester
    -- assessment, drawn as cards because it is one row.
    (@trimester, 1, N'ტრიმესტრული შეფასება აკადემიური დისციპლინების მიხედვით', N'ROLLUP', N'ONE', N'GRADE_TREND', NULL),
    -- Every subject for one trimester: one mark each, and the bar across them.
    (@trimester, 2, N'შემაჯამებელი ტრიმესტრული შეფასება', N'ROLLUP', N'ALL', N'SUBJECT_BARS', N'TRIMESTER_GRADE'),
    -- The brief's second table: three trimesters beside the four year columns,
    -- which is what component.summary_column marks (db/034). The legacy screen
    -- carried a chart and this keeps one, plotting the final academic grade -
    -- the headline of the table rather than its last column.
    (@trimester, 3, N'ტრიმესტრული და წლიური შეფასება', N'YEAR', N'ALL', N'SUBJECT_BARS', N'OVERALL'),
    -- ONE period: the brief's ethics screen is a month picker and a single
    -- figure, which is what ONE means for a journal whose rows are periods.
    (@ethics, 4, N'შეფასება ეთიკური ნორმების მიხედვით', N'REPORTING', N'ONE', NULL, NULL),
    -- The register wants its whole year on screen, so ALL.
    (@absence, 5, N'გაცდენილი საათები', N'YEAR', N'ALL', N'ABSENCE_BARS', NULL)
    ) AS v(template_id, ordinal, label, period_kind, subject_mode, chart_key, chart_column)
WHERE v.template_id IS NOT NULL;
GO

SELECT pv.ordinal,
       pv.label,
       t.name                     AS journal,
       pv.period_kind,
       pv.subject_mode,
       ISNULL(pv.chart_key, N'—') AS chart_key
FROM sgs.parent_menu_item pv
         JOIN sgs.grading_template t ON t.id = pv.template_id
ORDER BY pv.ordinal;
GO

-- =============================================================================
-- 2. Who sees which parent pages - db/038_parent_module_grant.sql
-- =============================================================================

-- What the parent portal shows, as rows.
--
-- Until now it was two rules in code. ParentContentService gave every school
-- homework and news, and primary the schedule, the menu and the child's
-- description; ParentViewService withheld every journal from primary except
-- the registers. The school then asked for grades 5 and 6 of primary to keep
-- the primary pages and see grades as well - which neither rule could say
-- without a third, so the rule became data instead.
--
-- A row says "classes matching this get this module". A child gets every module
-- whose rows their class matches; rows only ever add. A blank criterion matches
-- everything:
--
--   school_id   null for every school
--   level_from  null for no lower bound   } class_group.level, the grade a
--   level_to    null for no upper bound   } class is in, inclusive
--
-- Keyed on school and level, not on the class: classes belong to one academic
-- year and rollover creates new ones every September, so a row naming a class
-- would stop applying the following year. A level range keeps applying.
--
-- GRADES is what the gradebook and ethics name in grading_template.parent_module
-- (db/039). The absence register names no module and reaches every child, as it
-- always has.
--
-- Changing who gets what is an INSERT or a DELETE here. For example, to give
-- grade 4 of primary grades as well:
--
--   UPDATE sgs.parent_module_grant SET level_from = 4
--   WHERE module = 'GRADES'
--     AND school_id = (SELECT id FROM sgs.school WHERE code = N'PRIMARY');
--
-- CAUTION: a class saved without a level has level 0, and no ranged row ever
-- matches it. A grade 5 class created with საფეხური left blank gets no grades.
--
-- Run after 006_migrate_from_dbo.sql, which seeds the three schools. Idempotent.

SET
XACT_ABORT ON;
SET
NOCOUNT ON;
SET
QUOTED_IDENTIFIER ON;
SET
ANSI_NULLS ON;
GO

IF NOT EXISTS (SELECT 1 FROM sys.sequences q
               JOIN sys.schemas c ON c.schema_id = q.schema_id
               WHERE q.name = 'parent_module_grant_seq' AND c.name = 'sgs')
CREATE SEQUENCE sgs.parent_module_grant_seq AS bigint START WITH 1 INCREMENT BY 50;
GO

IF OBJECT_ID('sgs.parent_module_grant') IS NULL
CREATE TABLE sgs.parent_module_grant
(
    id         bigint      NOT NULL
        CONSTRAINT pk_parent_module_grant PRIMARY KEY,
    school_id  bigint NULL
        CONSTRAINT fk_parent_module_grant_school REFERENCES sgs.school (id),
    level_from smallint NULL,
    level_to   smallint NULL,
    module     varchar(32) NOT NULL,
    created_at datetime2   NOT NULL,
    created_by bigint NULL,
    updated_at datetime2   NOT NULL,
    updated_by bigint NULL
);
GO

-- Outside the CREATE above: on a fresh install 001_schema.sql has already made
-- the table from the entity, and JPA cannot express either constraint.
--
-- The module list pins what the console and ParentModule know. A misspelt
-- module would otherwise be a row that grants nothing and says nothing, and an
-- unknown value would fail every parent request when Hibernate read it back.
-- Adding a module means widening this, and ck_grading_template_parent_module
-- (db/039), in the migration that first grants it.
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints
               WHERE name = 'ck_parent_module_grant_module')
ALTER TABLE sgs.parent_module_grant
    ADD CONSTRAINT ck_parent_module_grant_module
        CHECK (module IN ('HOMEWORK', 'NEWS', 'SCHEDULE', 'MENU', 'CHARACTERIZATION', 'GRADES'));
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints
               WHERE name = 'ck_parent_module_grant_levels')
ALTER TABLE sgs.parent_module_grant
    ADD CONSTRAINT ck_parent_module_grant_levels
        CHECK (level_from IS NULL OR level_to IS NULL OR level_from <= level_to);
GO

-- ---- the rules -----------------------------------------------------------
--
-- The first seven are exactly what the code did before; the last is the
-- change the school asked for. Schools are matched by code, which db/006 seeds
-- and nothing renames.
--
-- The guard is on the INSERT itself, in the same batch. A separate
-- "IF EXISTS ... RETURN" batch would only end itself: the INSERT after the GO
-- runs regardless, and a second run seeded every rule twice.

IF NOT EXISTS (SELECT 1 FROM sgs.parent_module_grant)
INSERT INTO sgs.parent_module_grant (id, school_id, level_from, level_to, module,
                                     created_at, updated_at)
SELECT NEXT VALUE FOR sgs.parent_module_grant_seq, s.id, v.level_from, v.level_to, v.module, SYSUTCDATETIME(), SYSUTCDATETIME()
FROM (VALUES
    -- Every school.
    (NULL, NULL, NULL, 'HOMEWORK'), (NULL, NULL, NULL, 'NEWS'),
    -- The brief's primary-only pages.
    (N'PRIMARY', NULL, NULL, 'SCHEDULE'), (N'PRIMARY', NULL, NULL, 'MENU'), (N'PRIMARY', NULL, NULL, 'CHARACTERIZATION'),
    -- Basic and secondary grade.
    (N'BASIC', NULL, NULL, 'GRADES'), (N'SECONDARY', NULL, NULL, 'GRADES'),
    -- And so do the two oldest years of primary, alongside its own pages.
    (N'PRIMARY', 5, 6, 'GRADES')
    ) AS v(school_code, level_from, level_to, module)
    LEFT JOIN sgs.school s
ON s.code = v.school_code
-- A row naming a school this database does not have is skipped, not widened
-- to every school.
WHERE v.school_code IS NULL OR s.id IS NOT NULL;
GO

SELECT ISNULL(s.code, N'(every school)')               AS school,
       ISNULL(CAST(g.level_from AS nvarchar(4)), N'—') AS level_from,
       ISNULL(CAST(g.level_to AS nvarchar(4)), N'—')   AS level_to,
       g.module
FROM sgs.parent_module_grant g
         LEFT JOIN sgs.school s ON s.id = g.school_id
ORDER BY s.ordinal, g.level_from, g.module;
GO


-- =============================================================================
-- 3. Which module shows each journal - db/039_journal_parent_module.sql
-- =============================================================================

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

-- =============================================================================
-- 4. Class levels
-- =============================================================================
--
-- Only classes whose level is unset (0, or anything outside 1-12), and only
-- when the class name starts with a grade between 1 and 12. A name that does
-- not ("მოსამზადებელი") is left alone and listed in the report below, to be
-- set on the Classes screen.

UPDATE c
SET level = g.grade FROM sgs.class_group c
CROSS APPLY (SELECT TRY_CAST(LEFT(c.name, PATINDEX('%[^0-9]%', c.name + 'x') - 1) AS smallint)
                 AS grade) g
WHERE c.level NOT BETWEEN 1
  AND 12
  AND g.grade BETWEEN 1
  AND 12;

SELECT @@ROWCOUNT AS class_levels_filled_in;
GO

-- =============================================================================
-- 5. Report
-- =============================================================================

-- This year's classes, and whether their parents see grades. Every 5th and 6th
-- grade class should say yes; level 0 means the class needs its საფეხური set.
SELECT s.code            AS school,
       c.name            AS class,
       c.level           AS level,
       CASE
           WHEN EXISTS (SELECT 1
                        FROM sgs.parent_module_grant g
                        WHERE g.module = 'GRADES'
                          AND (g.school_id IS NULL OR g.school_id = c.school_id)
                          AND (g.level_from IS NULL OR g.level_from <= c.level)
                          AND (g.level_to IS NULL OR g.level_to >= c.level))
               THEN N'yes'
           ELSE N'—' END AS parents_see_grades
FROM sgs.class_group c
         JOIN sgs.school s ON s.id = c.school_id
         JOIN sgs.academic_year y ON y.id = c.academic_year_id
WHERE y.is_current = 1
ORDER BY s.ordinal, c.level, c.name;
GO

SET NOEXEC OFF;
GO
