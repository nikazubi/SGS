-- The ethical-norms journal, in the shape the brief actually shows.
--
-- ---------------------------------------------------------------------------
-- WHAT THE BRIEF ASKS FOR (section 4, "Student's assessment by ethical norms"):
--
--   # | Student | Sept-Oct | Nov | Trim I | Dec | Jan-Feb | Mar | Trim II |
--       Apr | May | Trim III | Year
--
-- One value per reporting period, with trimester and year roll-ups. No weeks,
-- and no five criteria - both of which an earlier version of this script had,
-- from the legacy BehaviourDashBoard rather than from the brief. The brief's own
-- note says it plainly: "the brief asks for one value per month, plus trimester
-- and year roll-ups", and flags that either the weekly detail is being dropped
-- or the brief shows only a summary. That is the one thing still worth asking.
--
-- The table's shape is students down and periods across, which is GridMode
-- PERIODS - the same grid the absence register uses, and for the same reason:
-- the absence table in section 3 has exactly these columns.
--
-- ---------------------------------------------------------------------------
-- WHAT IS NOT HERE, AND WHY
--
-- Nothing, now. An earlier version of this file could not express the trimester
-- columns: period_kind was read as "the year, or the journal's own level", so a
-- monthly journal had nowhere to put a trimester. It now names the kind of
-- period a column lives on, and the trimester total is an ordinary AVERAGE over
-- CHILDREN - the same shape ANNUAL has always used.
-- ---------------------------------------------------------------------------
--
-- Still db/demo/ rather than db/: the weekly-detail question above is
-- unanswered, and seeding a guess into the shipped chain would make it look
-- decided. Promoting this to a numbered migration is a rename once the school
-- confirms.
--
-- Run after 032_reporting_periods.sql and 100_roster.sql. Idempotent.

SET
XACT_ABORT ON;
SET
NOCOUNT ON;
SET
QUOTED_IDENTIFIER ON;
SET
ANSI_NULLS ON;
BEGIN
TRANSACTION;

DECLARE
@year   bigint = (SELECT id FROM sgs.academic_year WHERE is_current = 1);
DECLARE
@scheme bigint = (SELECT TOP 1 id FROM sgs.period_scheme WHERE academic_year_id = @year);

IF
@scheme IS NULL
BEGIN
    RAISERROR
('No period scheme - run db/demo/100_roster.sql first.', 16, 1);
ROLLBACK TRANSACTION;
RETURN;
END

-- ---- template and version ------------------------------------------------
--
-- Every NOT NULL column named explicitly: db/001 is regenerated from the
-- entities and Hibernate emits no DEFAULT constraints, so a column the model
-- gains breaks this statement rather than silently taking a default.

IF
NOT EXISTS (SELECT 1 FROM sgs.grading_template
               WHERE name = N'შეფასება ეთიკური ნორმების მიხედვით')
INSERT INTO sgs.grading_template (id, uuid, name, description, frequency, subject_scoped,
                                  sort_index, is_archived, is_parent_visible, chart_key,
                                  grid_mode, locks_on_publish,
                                  school_id, created_at, updated_at)
VALUES (NEXT VALUE FOR sgs.grading_template_seq,
        LOWER(CONVERT(nvarchar(36), NEWID())),
        N'შეფასება ეთიკური ნორმების მიხედვით',
        N'ერთი შეფასება საანგარიშო პერიოდში',
        N'MONTH', 0, 5, 0, 1,
        -- No chart. GRADE_TREND plots a subject's marks and this is not
        -- subject-scoped; a journal naming no chart renders a complete page
        -- without one, which is honest until somebody decides what an ethics
        -- chart should show.
        NULL,
        -- Students down, periods across - the brief's table.
        N'PERIODS', 1,
        NULL, SYSUTCDATETIME(), SYSUTCDATETIME());

DECLARE
@template bigint = (SELECT id FROM sgs.grading_template
                            WHERE name = N'შეფასება ეთიკური ნორმების მიხედვით');

IF
NOT EXISTS (SELECT 1 FROM sgs.template_version WHERE template_id = @template AND version_no = 1)
INSERT INTO sgs.template_version (id, template_id, version_no, status, period_scheme_id,
                                  effective_from_period_id, activated_at, created_at, updated_at)
VALUES (NEXT VALUE FOR sgs.template_version_seq, @template, 1, N'ACTIVE', @scheme,
        NULL, SYSUTCDATETIME(), SYSUTCDATETIME(), SYSUTCDATETIME());

DECLARE
@version bigint =
    (SELECT id FROM sgs.template_version WHERE template_id = @template AND version_no = 1);

IF
EXISTS (SELECT 1 FROM sgs.component WHERE template_version_id = @version)
BEGIN
COMMIT TRANSACTION;
SELECT 'already seeded' AS result;
RETURN;
END

-- ---- the mark, and the year's ---------------------------------------------
--
-- The scale is the school's ordinary 0..10. Whole numbers for the typed mark,
-- two decimals for the average, which is the same distinction the academic
-- journal draws between a mark somebody awarded and a figure computed from
-- several.

INSERT INTO sgs.component (id, template_version_id, code, label, ordinal, group_label,
                           kind, period_kind, subject_scoped, scale_min, scale_max, decimals,
                           allow_special_values, allow_override, parent_visible, summary_column)
SELECT NEXT VALUE FOR sgs.component_seq, @version, v.code, v.label, v.ordinal, NULL, v.kind, v.period_kind, 0, 0, 10, v.decimals, 0, 1, 1, 0
FROM (VALUES
    (N'ETHICS', N'ეთიკური ნორმების შეფასება', 0, N'INPUT', N'REPORTING', 0), (N'ETHICS_TRIM', N'ტრიმესტრი', 1, N'DERIVED', N'ROLLUP', 2), (N'ETHICS_YEAR', N'წლიური', 2, N'DERIVED', N'YEAR', 2)
    ) v(code, label, ordinal, kind, period_kind, decimals);

-- ---- the year is the average of the reporting periods ---------------------
--
-- DESCENDANTS, not CHILDREN: the reporting periods are the year's
-- grandchildren, with the trimesters in between holding no ethics mark at all.
-- CHILDREN would reach the trimesters, find nothing and average to nothing -
-- which is the bug db/023 fixed for the absence totals.
--
-- IGNORE rather than counting a blank as nought: a period nobody assessed is
-- not a zero, and scoring a child on it would be inventing a mark.

DECLARE
@mark bigint = (SELECT id FROM sgs.component
                        WHERE template_version_id = @version AND code = N'ETHICS');
DECLARE
@trim bigint = (SELECT id FROM sgs.component
                        WHERE template_version_id = @version AND code = N'ETHICS_TRIM');
DECLARE
@yearly bigint = (SELECT id FROM sgs.component
                          WHERE template_version_id = @version AND code = N'ETHICS_YEAR');

-- The trimester: the average of its own reporting periods, one level down.
--
-- Plain, unweighted - (Sept-Oct + Nov) / 2 - confirmed by the school. The
-- reporting periods are not equal lengths, so the alternative was to weight
-- Sept-Oct double for being two months; it is not weighted. A period is a
-- period.
DECLARE
@rule bigint = NEXT VALUE FOR sgs.derivation_rule_seq;
DECLARE
@term bigint = NEXT VALUE FOR sgs.derivation_term_seq;

INSERT INTO sgs.derivation_rule (id, component_id, chain_order, type, null_policy,
                                 renormalize_weights, rounding_mode, decimals)
VALUES (@rule, @trim, 0, N'AVERAGE', N'IGNORE', 0, N'HALF_UP', 2);

INSERT INTO sgs.derivation_term (id, rule_id, ordinal, weight, source_kind, reduce,
                                 period_ref, period_id, label)
VALUES (@term, @rule, 0, 1, N'COMPONENT', N'AVERAGE', N'CHILDREN', NULL,
        N'საანგარიშო პერიოდები');

INSERT INTO sgs.derivation_source (id, term_id, component_id)
VALUES (NEXT VALUE FOR sgs.derivation_source_seq, @term, @mark);

-- The year: the average of the reporting periods rather than of the three
-- trimester figures, which would be an average of averages and would weight a
-- two-period trimester the same as a three-period one.
SET
@rule = NEXT VALUE FOR sgs.derivation_rule_seq;
SET
@term = NEXT VALUE FOR sgs.derivation_term_seq;

INSERT INTO sgs.derivation_rule (id, component_id, chain_order, type, null_policy,
                                 renormalize_weights, rounding_mode, decimals)
VALUES (@rule, @yearly, 0, N'AVERAGE', N'IGNORE', 0, N'HALF_UP', 2);

INSERT INTO sgs.derivation_term (id, rule_id, ordinal, weight, source_kind, reduce,
                                 period_ref, period_id, label)
VALUES (@term, @rule, 0, 1, N'COMPONENT', N'AVERAGE', N'DESCENDANTS', NULL,
        N'საანგარიშო პერიოდები');

INSERT INTO sgs.derivation_source (id, term_id, component_id)
VALUES (NEXT VALUE FOR sgs.derivation_source_seq, @term, @mark);

-- ---- assigned to the basic class only --------------------------------------
--
-- The brief scopes this workspace to basic and secondary. It also makes the
-- demo show that assignment is per class: 8ბ has it and 3ა does not.

INSERT INTO sgs.template_assignment (id, class_group_id, subject_id, template_id,
                                     template_version_id, created_at, updated_at)
SELECT NEXT VALUE FOR sgs.template_assignment_seq, cg.id, NULL, @template, @version, SYSUTCDATETIME(), SYSUTCDATETIME()
FROM sgs.class_group cg
WHERE cg.academic_year_id = @year
  AND cg.name = N'8ბ'
  AND NOT EXISTS (SELECT 1 FROM sgs.template_assignment ta
    WHERE ta.class_group_id = cg.id
  AND ta.subject_id IS NULL
  AND ta.template_id = @template);

COMMIT TRANSACTION;

SELECT c.code, c.label, c.kind, c.period_kind, c.decimals
FROM sgs.component c
WHERE c.template_version_id = @version
ORDER BY c.ordinal;
GO
