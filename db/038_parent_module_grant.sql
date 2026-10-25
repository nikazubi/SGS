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
