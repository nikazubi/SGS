-- The monthly absence register.
--
-- An ordinary journal - the engine, the write path and publication all apply
-- unchanged. What makes it different is grid_mode: students down, *periods*
-- across, rather than the template's columns across.
--
-- It counts academic hours typed by the coordinator. Daily absence counts days
-- and is deliberately independent of it - converting between the two would need
-- an hours-per-day figure nobody has - and since db/028 it is not a journal at
-- all, but rows in sgs.daily_absence.
--
-- Not parent-visible yet: the parent side of everything lands in phase 11.

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
('No period scheme - run 006_migrate_from_dbo.sql first.', 16, 1);
    RETURN;
END

IF
NOT EXISTS (SELECT 1 FROM sgs.period WHERE scheme_id = @scheme AND depth = 3
                                          AND starts_on IS NOT NULL)
BEGIN
    RAISERROR
('No dated day periods - run 021_absence.sql first.', 16, 1);
    RETURN;
END

BEGIN
TRANSACTION;

-- ---- daily register: removed ------------------------------------------
--
-- This script used to create a DAY-frequency journal here, with an ABSENT
-- column and a DAYS_ABSENT yearly rollup. db/028 deletes all of it: daily
-- absence is its own table now, because a journal cell carries a value and a
-- tick is not one.
--
-- Removed rather than left in place. On an existing database 028 has already
-- retired it, so recreating it here would resurrect a journal nothing serves;
-- on a fresh install it was pure churn - created by this script and destroyed
-- two scripts later - and its INSERT predated locks_on_publish, so it would
-- have failed outright on the NOT NULL column.

-- ---- monthly register ----------------------------------------------------

IF
NOT EXISTS (SELECT 1 FROM sgs.grading_template WHERE name = N'გაცდენილი საათები')
BEGIN
INSERT INTO sgs.grading_template (id, uuid, name, description, frequency, subject_scoped,
                                  sort_index, is_archived, is_parent_visible, chart_key,
                                  grid_mode, locks_on_publish,
                                  school_id, created_at, updated_at)
VALUES (NEXT VALUE FOR sgs.grading_template_seq,
             LOWER(CONVERT(nvarchar(36), NEWID())),
             N'გაცდენილი საათები',
             N'თვეში გაცდენილი აკადემიური საათები - შეჰყავს კოორდინატორს',
             N'MONTH', 0, 11, 0, 0, N'ABSENCE_BARS',
           -- Published to parents, but NOT frozen: missed hours accumulate
           -- through a month and the coordinator republishes as they do, so an
           -- approval per top-up would sit on the ordinary path. db/029 sets
           -- this on an existing database; it is named here so a fresh install
           -- never has the wrong value even briefly.
             N'PERIODS', 0,
             NULL, SYSUTCDATETIME(), SYSUTCDATETIME());
END

DECLARE
@monthly bigint =
    (SELECT id FROM sgs.grading_template WHERE name = N'გაცდენილი საათები');

IF
NOT EXISTS (SELECT 1 FROM sgs.template_version WHERE template_id = @monthly AND version_no = 1)
INSERT INTO sgs.template_version (id, template_id, version_no, status, period_scheme_id,
                                  effective_from_period_id, activated_at, created_at, updated_at)
VALUES (NEXT VALUE FOR sgs.template_version_seq, @monthly, 1, N'ACTIVE', @scheme,
        NULL, SYSUTCDATETIME(), SYSUTCDATETIME(), SYSUTCDATETIME());

DECLARE
@monthlyVersion bigint =
    (SELECT id FROM sgs.template_version WHERE template_id = @monthly AND version_no = 1);

IF
NOT EXISTS (SELECT 1 FROM sgs.component WHERE template_version_id = @monthlyVersion)
INSERT INTO sgs.component (id, template_version_id, code, label, ordinal, group_label,
                           kind, period_kind, subject_scoped, scale_min, scale_max, decimals,
                           allow_special_values, allow_override, parent_visible, summary_column)
-- Likewise: a month caps at 500 hours, the year at nine of them.
SELECT NEXT VALUE FOR sgs.component_seq, @monthlyVersion, v.code, v.label, v.ordinal, NULL, v.kind, v.period_kind, 0, 0, v.scale_max, 0, 0, 1, 1, 0
FROM (VALUES
    (N'HOURS_MISSED', N'გაცდენილი საათები', 0, N'INPUT', N'ROLLUP', 500), (N'HOURS_YEAR', N'წლიური ჯამი', 1, N'DERIVED', N'YEAR', 5000)
    ) v(code, label, ordinal, kind, period_kind, scale_max);

DECLARE
@hours bigint = (SELECT id FROM sgs.component
                         WHERE template_version_id = @monthlyVersion AND code = N'HOURS_MISSED');
DECLARE
@hoursYear bigint = (SELECT id FROM sgs.component
                             WHERE template_version_id = @monthlyVersion AND code = N'HOURS_YEAR');

IF
NOT EXISTS (SELECT 1 FROM sgs.derivation_rule WHERE component_id = @hoursYear)
BEGIN
    DECLARE
@rule2 bigint = NEXT VALUE FOR sgs.derivation_rule_seq;
    DECLARE
@term2 bigint = NEXT VALUE FOR sgs.derivation_term_seq;

INSERT INTO sgs.derivation_rule (id, component_id, chain_order, type, null_policy,
                                 renormalize_weights, rounding_mode, decimals)
VALUES (@rule2, @hoursYear, 0, N'SUM', N'IGNORE', 0, N'HALF_UP', 0);

-- Two levels here rather than three, and equally out of CHILDREN's reach.
INSERT INTO sgs.derivation_term (id, rule_id, ordinal, weight, source_kind, reduce,
                                 period_ref, period_id, label)
VALUES (@term2, @rule2, 0, 1, N'COMPONENT', N'SUM', N'DESCENDANTS', NULL, N'თვეები');

INSERT INTO sgs.derivation_source (id, term_id, component_id)
VALUES (NEXT VALUE FOR sgs.derivation_source_seq, @term2, @hours);
END

-- ---- assign the register to every class ---------------------------------
--
-- subject_id NULL: absence is a class matter, not a subject one.

INSERT INTO sgs.template_assignment (id, class_group_id, subject_id, template_id,
                                     template_version_id, created_at, updated_at)
SELECT NEXT VALUE FOR sgs.template_assignment_seq, c.id, NULL, t.template_id, t.version_id, SYSUTCDATETIME(), SYSUTCDATETIME()
FROM sgs.class_group c
    CROSS JOIN (VALUES (@monthly, @monthlyVersion)) t(template_id, version_id)
WHERE c.academic_year_id = @year
  AND NOT EXISTS (SELECT 1 FROM sgs.template_assignment a
    WHERE a.class_group_id = c.id
  AND a.template_id = t.template_id
  AND a.subject_id IS NULL);

COMMIT TRANSACTION;
GO

SELECT t.name, t.frequency, t.grid_mode, COUNT(c.id) AS columns
FROM sgs.grading_template t
         JOIN sgs.template_version v ON v.template_id = t.id
         LEFT JOIN sgs.component c ON c.template_version_id = v.id
WHERE t.name = N'გაცდენილი საათები'
GROUP BY t.name, t.frequency, t.grid_mode;
GO
