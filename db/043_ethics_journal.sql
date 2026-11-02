-- The ethical-norms journal, promoted from db/demo/130_ethics_journal.sql.
--
-- The code for this has always been there: the parent console renders whatever
-- journals exist and knows nothing about ethics specifically, and db/035 even
-- reserved ordinal 4 of the parent menu for it. What was missing was the
-- journal itself, which only ever existed in the demo seed - so db/035 looked
-- for "the second MONTH + PERIODS template", found none, and silently skipped
-- its row. The live menu runs 1, 2, 3, 5.
--
-- Shape, from the brief (section 4, "Student's assessment by ethical norms"):
--
--   # | Student | Sept-Oct | Nov | Trim I | Dec | Jan-Feb | Mar | Trim II |
--       Apr | May | Trim III | Year
--
-- One typed mark per reporting period, with trimester and year roll-ups.
-- Students down, periods across - GridMode PERIODS, the same grid the absence
-- register uses, because the brief draws both tables the same way.
--
-- STILL OPEN, and worth asking the school: the legacy screen carried five
-- weekly criteria and the brief shows one value per month. This follows the
-- brief. If the weekly detail is wanted, it is a different component set and
-- this needs revisiting before anyone types a term of marks into it.
--
-- Differs from the demo seed in three ways, all deliberate:
--   * created hidden from parents - see the long note on is_parent_visible;
--   * no per-class template_assignment rows, because production has none for
--     any journal and they would not scope it anyway;
--   * adds the parent_menu_item row db/035 could not, at its reserved
--     ordinal 4. Harmless while the journal is hidden - views() drops a menu
--     row whose journal the child cannot see - and it means switching ethics
--     on later is one UPDATE rather than another migration.
--
-- Idempotent and transactional: safe to re-run, and leaves nothing behind if
-- any step fails.

SET XACT_ABORT ON;
SET NOCOUNT ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;

BEGIN TRANSACTION;

DECLARE @name   nvarchar(200) = N'შეფასება ეთიკური ნორმების მიხედვით';
DECLARE @year   bigint = (SELECT id FROM sgs.academic_year WHERE is_current = 1);
DECLARE @scheme bigint = (SELECT TOP 1 ps.id FROM sgs.period_scheme ps
                          WHERE ps.academic_year_id = @year ORDER BY ps.id);

IF @year IS NULL OR @scheme IS NULL
BEGIN
    RAISERROR('No current academic year or period scheme.', 16, 1);
    ROLLBACK TRANSACTION;
    RETURN;
END

-- ---- template -------------------------------------------------------------
--
-- school_id NULL, matching the two journals already live: the school a class
-- belongs to decides nothing here, the per-class assignment below does.
--
-- Every NOT NULL column named explicitly. db/001 is regenerated from the
-- entities and Hibernate emits no DEFAULT constraints, so a column the model
-- gains breaks this statement rather than silently taking a default.

IF NOT EXISTS (SELECT 1 FROM sgs.grading_template WHERE name = @name)
INSERT INTO sgs.grading_template (id, uuid, name, description, frequency, subject_scoped,
                                  sort_index, is_archived, is_parent_visible, chart_key,
                                  grid_mode, locks_on_publish,
                                  school_id, created_at, updated_at)
VALUES (NEXT VALUE FOR sgs.grading_template_seq,
        LOWER(CONVERT(nvarchar(36), NEWID())),
        @name,
        N'ერთი შეფასება საანგარიშო პერიოდში',
        N'MONTH', 0, 5, 0,
        -- Hidden from parents on purpose, and this is the important line.
        --
        -- ParentViewService.journals() lets a primary-school parent see a
        -- journal only when isRegister() says so, and isRegister() is
        -- gridMode == PERIODS. Ethics is PERIODS - that is the brief's table
        -- shape - so the proxy reads it as a register and it would appear on
        -- all 23 primary classes' landing pages, which the brief scopes to
        -- basic and secondary. The proxy's own comment predicts exactly this:
        -- "a transposed gradebook would arrive on the primary landing page
        -- uninvited".
        --
        -- Creating it hidden lets staff be set up and marks entered now. Once
        -- the primary filter can tell a register from a transposed gradebook,
        -- turning it on is:
        --   UPDATE sgs.grading_template SET is_parent_visible = 1
        --    WHERE name = N'შეფასება ეთიკური ნორმების მიხედვით';
        0,
        -- No chart. GRADE_TREND plots a subject's marks and this is not
        -- subject-scoped; a journal naming no chart renders a complete page
        -- without one, which is honest until somebody decides what an ethics
        -- chart should show.
        NULL,
        N'PERIODS',
        -- Marks accumulate through the year and a coordinator will publish
        -- more than once, exactly as with the absence register. Locking on
        -- publish would put an approval between them and every amendment.
        0,
        NULL, SYSUTCDATETIME(), SYSUTCDATETIME());

DECLARE @template bigint = (SELECT id FROM sgs.grading_template WHERE name = @name);

IF NOT EXISTS (SELECT 1 FROM sgs.template_version
               WHERE template_id = @template AND version_no = 1)
INSERT INTO sgs.template_version (id, template_id, version_no, status, period_scheme_id,
                                  effective_from_period_id, activated_at, created_at, updated_at)
VALUES (NEXT VALUE FOR sgs.template_version_seq, @template, 1, N'ACTIVE', @scheme,
        NULL, SYSUTCDATETIME(), SYSUTCDATETIME(), SYSUTCDATETIME());

DECLARE @version bigint = (SELECT id FROM sgs.template_version
                           WHERE template_id = @template AND version_no = 1);

-- ---- components, and the two roll-ups --------------------------------------
--
-- Skipped wholesale on a re-run: the components existing means the derivation
-- rules below do too, and inserting a second copy of those would double-count
-- every average.

IF NOT EXISTS (SELECT 1 FROM sgs.component WHERE template_version_id = @version)
BEGIN
    -- The scale is the school's ordinary 0..10. Whole numbers for the typed
    -- mark, two decimals for the averages - the same distinction the academic
    -- journal draws between a mark somebody awarded and a figure computed.
    INSERT INTO sgs.component (id, template_version_id, code, label, ordinal, group_label,
                               kind, period_kind, subject_scoped, scale_min, scale_max,
                               decimals, allow_special_values, allow_override,
                               parent_visible, summary_column)
    SELECT NEXT VALUE FOR sgs.component_seq, @version, v.code, v.label, v.ordinal, NULL,
           v.kind, v.period_kind, 0, 0, 10, v.decimals, 0, 1, 1, 0
    FROM (VALUES
        (N'ETHICS',      N'ეთიკური ნორმების შეფასება', 0, N'INPUT',   N'REPORTING', 0),
        (N'ETHICS_TRIM', N'ტრიმესტრი',                 1, N'DERIVED', N'ROLLUP',    2),
        (N'ETHICS_YEAR', N'წლიური',                    2, N'DERIVED', N'YEAR',      2)
        ) v(code, label, ordinal, kind, period_kind, decimals);

    DECLARE @mark   bigint = (SELECT id FROM sgs.component
                              WHERE template_version_id = @version AND code = N'ETHICS');
    DECLARE @trim   bigint = (SELECT id FROM sgs.component
                              WHERE template_version_id = @version AND code = N'ETHICS_TRIM');
    DECLARE @yearly bigint = (SELECT id FROM sgs.component
                              WHERE template_version_id = @version AND code = N'ETHICS_YEAR');

    -- The trimester: the average of its own reporting periods, one level down.
    -- Plain and unweighted - (Sept-Oct + Nov) / 2 - confirmed by the school.
    -- The reporting periods are not equal lengths, so the alternative was to
    -- weight Sept-Oct double for being two months. It is not weighted.
    --
    -- IGNORE rather than counting a blank as nought: a period nobody assessed
    -- is not a zero, and scoring a child on it would be inventing a mark.
    DECLARE @rule bigint = NEXT VALUE FOR sgs.derivation_rule_seq;
    DECLARE @term bigint = NEXT VALUE FOR sgs.derivation_term_seq;

    INSERT INTO sgs.derivation_rule (id, component_id, chain_order, type, null_policy,
                                     renormalize_weights, rounding_mode, decimals)
    VALUES (@rule, @trim, 0, N'AVERAGE', N'IGNORE', 0, N'HALF_UP', 2);

    INSERT INTO sgs.derivation_term (id, rule_id, ordinal, weight, source_kind, reduce,
                                     period_ref, period_id, label)
    VALUES (@term, @rule, 0, 1, N'COMPONENT', N'AVERAGE', N'CHILDREN', NULL,
            N'საანგარიშო პერიოდები');

    INSERT INTO sgs.derivation_source (id, term_id, component_id)
    VALUES (NEXT VALUE FOR sgs.derivation_source_seq, @term, @mark);

    -- The year: the average of the reporting periods, not of the three
    -- trimester figures - that would be an average of averages and would
    -- weight a two-period trimester the same as a three-period one.
    --
    -- DESCENDANTS, not CHILDREN: the reporting periods are the year's
    -- grandchildren, with the trimesters in between holding no ethics mark at
    -- all. CHILDREN would reach the trimesters, find nothing, and average to
    -- nothing - the bug db/023 fixed for the absence totals.
    SET @rule = NEXT VALUE FOR sgs.derivation_rule_seq;
    SET @term = NEXT VALUE FOR sgs.derivation_term_seq;

    INSERT INTO sgs.derivation_rule (id, component_id, chain_order, type, null_policy,
                                     renormalize_weights, rounding_mode, decimals)
    VALUES (@rule, @yearly, 0, N'AVERAGE', N'IGNORE', 0, N'HALF_UP', 2);

    INSERT INTO sgs.derivation_term (id, rule_id, ordinal, weight, source_kind, reduce,
                                     period_ref, period_id, label)
    VALUES (@term, @rule, 0, 1, N'COMPONENT', N'AVERAGE', N'DESCENDANTS', NULL,
            N'საანგარიშო პერიოდები');

    INSERT INTO sgs.derivation_source (id, term_id, component_id)
    VALUES (NEXT VALUE FOR sgs.derivation_source_seq, @term, @mark);
END

-- ---- no per-class assignments, deliberately --------------------------------
--
-- template_assignment is not how a journal reaches a class. When a class has
-- none, TemplateVersionResolver falls back to the journal's ACTIVE version, so
-- an absent assignment excludes nothing - it only pins a version. Production
-- carries zero assignment rows and both live journals work from that fallback;
-- adding 42 for this one would make it the odd journal out while changing
-- nothing about who can open it.
--
-- It also could not have been used for scoping: see the visibility note above
-- for how primary is actually filtered.

-- ---- the parent menu row db/035 reserved -----------------------------------
--
-- ONE period, not ALL: the brief's ethics screen is a month picker and a single
-- figure, which is what ONE means for a journal whose rows are periods. No
-- chart, matching the template.

IF NOT EXISTS (SELECT 1 FROM sgs.parent_menu_item WHERE template_id = @template)
INSERT INTO sgs.parent_menu_item (id, template_id, ordinal, label, period_kind,
                                  subject_mode, chart_key, chart_column,
                                  created_at, updated_at)
VALUES (NEXT VALUE FOR sgs.parent_menu_item_seq, @template, 4, @name,
        N'REPORTING', N'ONE', NULL, NULL, SYSUTCDATETIME(), SYSUTCDATETIME());

COMMIT TRANSACTION;

-- ---- what it looks like now -------------------------------------------------

SELECT c.code, c.label, c.kind, c.period_kind, c.decimals
FROM sgs.component c
WHERE c.template_version_id = (SELECT id FROM sgs.template_version
                               WHERE template_id = (SELECT id FROM sgs.grading_template
                                                    WHERE name = N'შეფასება ეთიკური ნორმების მიხედვით')
                                 AND version_no = 1)
ORDER BY c.ordinal;

SELECT pv.ordinal, pv.label, t.name AS journal, pv.period_kind, pv.subject_mode
FROM sgs.parent_menu_item pv
         JOIN sgs.grading_template t ON t.id = pv.template_id
ORDER BY pv.ordinal;

SELECT COUNT(*) AS classes_assigned
FROM sgs.template_assignment ta
         JOIN sgs.grading_template t ON t.id = ta.template_id
WHERE t.name = N'შეფასება ეთიკური ნორმების მიხედვით';
GO
