-- The trimester grading template.
--
-- Structure only. The seven ongoing marks, the initial-knowledge test, the
-- progress test, the final test and the trimester assessment are what the
-- school's current grid holds, so those are certain.
--
-- THE WEIGHTS ARE PROVISIONAL. The school has not said how the trimester
-- assessment is actually calculated, and it is configuration rather than code:
-- once they answer, it is edited in the template editor and nothing here is
-- touched. They are seeded so the grid has something to compute rather than as
-- a claim about the school's rules.
--
--   TRIMESTER_GRADE = 0.50 ongoing average + 0.20 initial knowledge
--                   + 0.30 final test          <-- provisional
--
-- Precision, however, is not a guess. Of 853 grades in the live data only 13
-- are fractional, and every one of them is a behaviour average or a percentage
-- - no TRIMESTER_* value has ever carried a decimal, and 7.00 alone accounts
-- for 633 of them. So the academic grades round to whole numbers: the engine
-- rounds once, when it calculates, and every surface then shows the same
-- number. Rounding later, in an export, would make the spreadsheet disagree
-- with the screen.
--
-- ONGOING_AVG and RATING keep their decimals: they are explicitly averages
-- rather than marks.
--
-- Run after 006_migrate_from_dbo.sql. Idempotent.
--
-- Kept in step with the journal columns phase 5 added (uuid, frequency,
-- subject_scoped, sort_index, is_archived) and the assignment's move from
-- `scope` to a journal reference. A database built straight from 001 has those
-- columns already; 012 only exists for one upgraded through the earlier shape.

SET
XACT_ABORT ON;
SET
NOCOUNT ON;
-- A filtered index (db/015) requires QUOTED_IDENTIFIER ON for any DML on the
-- table it covers, not only when it is created. sqlcmd leaves it OFF and fails
-- with Msg 1934; the JDBC driver sets it, so the application is unaffected.
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
('No period scheme for the current year - run 006_migrate_from_dbo.sql first.', 16, 1);
ROLLBACK TRANSACTION;
RETURN;
END

-- ---- template and version ------------------------------------------------

IF
NOT EXISTS (SELECT 1 FROM sgs.grading_template WHERE name = N'ტრიმესტრული შეფასება')
-- grid_mode and locks_on_publish are named explicitly, and must stay that way.
-- db/001 is regenerated from the entities by SchemaExportTest, and Hibernate
-- emits no DEFAULT constraints - so every NOT NULL column the model gains has
-- to appear here or a fresh install dies on this statement. It has happened
-- twice: once for grid_mode, once for locks_on_publish.
INSERT INTO sgs.grading_template (id, uuid, name, description, frequency, subject_scoped,
                                  sort_index, is_archived, is_parent_visible, chart_key,
                                  grid_mode, locks_on_publish,
                                  school_id, created_at, updated_at)
VALUES (NEXT VALUE FOR sgs.grading_template_seq,
        LOWER(CONVERT(nvarchar(36), NEWID())),
        N'ტრიმესტრული შეფასება',
        N'აკადემიური დისციპლინების ტრიმესტრული შეფასება',
        N'TRIMESTER', 1, 0, 0,
        -- This is the journal parents already see today.
        1, N'GRADE_TREND',
        -- Students down, the template's columns across; and grades freeze when
        -- they are published, which is what change requests exist for.
        N'COMPONENTS', 1,
        NULL, SYSUTCDATETIME(), SYSUTCDATETIME());

DECLARE
@template bigint = (SELECT id FROM sgs.grading_template WHERE name = N'ტრიმესტრული შეფასება');

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

-- ---- columns -------------------------------------------------------------
--
-- period_kind says which level of the period tree a column lives at;
-- subject_scoped says whether it is held per subject. Together they decide
-- which screen a column appears on.

INSERT INTO sgs.component (id, template_version_id, code, label, ordinal, group_label,
                           kind, period_kind, subject_scoped, scale_min, scale_max, decimals,
                           allow_special_values, allow_override, parent_visible, summary_column)
SELECT NEXT VALUE FOR sgs.component_seq, @version, v.code, v.label, v.ordinal, v.group_label, v.kind, v.period_kind, v.subject_scoped, 0, 10, v.decimals, 1, v.allow_override, 1, 0
FROM (VALUES
    (N'ONGOING_1', N'I', 0, N'მიმდინარე შეფასება', N'INPUT', N'ROLLUP', CAST (1 AS bit), 2, CAST (1 AS bit)), (N'ONGOING_2', N'II', 1, N'მიმდინარე შეფასება', N'INPUT', N'ROLLUP', 1, 2, 1), (N'ONGOING_3', N'III', 2, N'მიმდინარე შეფასება', N'INPUT', N'ROLLUP', 1, 2, 1), (N'ONGOING_4', N'IV', 3, N'მიმდინარე შეფასება', N'INPUT', N'ROLLUP', 1, 2, 1), (N'ONGOING_5', N'V', 4, N'მიმდინარე შეფასება', N'INPUT', N'ROLLUP', 1, 2, 1), (N'ONGOING_6', N'VI', 5, N'მიმდინარე შეფასება', N'INPUT', N'ROLLUP', 1, 2, 1), (N'ONGOING_7', N'VII', 6, N'მიმდინარე შეფასება', N'INPUT', N'ROLLUP', 1, 2, 1), (N'ONGOING_AVG', N'მიმდინარე საშუალო', 7, NULL, N'DERIVED', N'ROLLUP', 1, 2, 1), (N'INITIAL_KNOWLEDGE', N'საწყისი ცოდნის განმსაზღვრელი ტესტი', 8, NULL, N'INPUT', N'ROLLUP', 1, 2, 1), (N'PROGRESS', N'პროგრეს ტესტი', 9, NULL, N'INPUT', N'ROLLUP', 1, 2, 1), (N'FINAL_TEST', N'ფინალური ტესტი', 10, NULL, N'INPUT', N'ROLLUP', 1, 2, 1), (N'TRIMESTER_GRADE', N'ტრიმესტრის შეფასება', 11, NULL, N'DERIVED', N'ROLLUP', 1, 0, 1), (N'ANNUAL', N'წლიური შეფასება', 12, NULL, N'DERIVED', N'YEAR', 1, 0, 1), (N'RATING', N'რეიტინგი', 13, NULL, N'DERIVED', N'YEAR', 0, 2, 1)
    ) v(code, label, ordinal, group_label, kind, period_kind, subject_scoped, decimals, allow_override);

-- ---- helper: component id by code ---------------------------------------

DECLARE
@avg       bigint = (SELECT id FROM sgs.component WHERE template_version_id=@version AND code=N'ONGOING_AVG');
DECLARE
@initial   bigint = (SELECT id FROM sgs.component WHERE template_version_id=@version AND code=N'INITIAL_KNOWLEDGE');
DECLARE
@finalTest bigint = (SELECT id FROM sgs.component WHERE template_version_id=@version AND code=N'FINAL_TEST');
DECLARE
@trimester bigint = (SELECT id FROM sgs.component WHERE template_version_id=@version AND code=N'TRIMESTER_GRADE');
DECLARE
@annual    bigint = (SELECT id FROM sgs.component WHERE template_version_id=@version AND code=N'ANNUAL');
DECLARE
@rating    bigint = (SELECT id FROM sgs.component WHERE template_version_id=@version AND code=N'RATING');

DECLARE
@rule bigint, @term bigint;

-- ---- ONGOING_AVG = average of the seven, blanks ignored -----------------

SET
@rule = NEXT VALUE FOR sgs.derivation_rule_seq;
INSERT INTO sgs.derivation_rule (id, component_id, chain_order, type, null_policy,
                                 renormalize_weights, rounding_mode, decimals)
VALUES (@rule, @avg, 0, N'AVERAGE', N'IGNORE', 0, N'HALF_UP', 2);

SET
@term = NEXT VALUE FOR sgs.derivation_term_seq;
INSERT INTO sgs.derivation_term (id, rule_id, ordinal, weight, source_kind, reduce,
                                 period_ref, period_id, label)
VALUES (@term, @rule, 0, 1, N'GROUP', N'AVERAGE', N'SAME', NULL, N'მიმდინარე 1-7');

INSERT INTO sgs.derivation_source (id, term_id, component_id)
SELECT NEXT VALUE FOR sgs.derivation_source_seq, @term, c.id
FROM sgs.component c
WHERE c.template_version_id = @version AND c.code LIKE 'ONGOING[_][1-7]';

-- ---- TRIMESTER_GRADE (weights provisional) ------------------------------
--
-- renormalize_weights is on: a student with no final test is scored on what
-- they did sit, rather than quietly capped at 70% of the scale.

SET
@rule = NEXT VALUE FOR sgs.derivation_rule_seq;
INSERT INTO sgs.derivation_rule (id, component_id, chain_order, type, null_policy,
                                 renormalize_weights, rounding_mode, decimals)
VALUES (@rule, @trimester, 0, N'WEIGHTED_SUM', N'IGNORE', 1, N'HALF_UP', 0);

SET
@term = NEXT VALUE FOR sgs.derivation_term_seq;
INSERT INTO sgs.derivation_term (id, rule_id, ordinal, weight, source_kind, reduce, period_ref, period_id, label)
VALUES (@term, @rule, 0, 0.50, N'COMPONENT', N'FIRST_NON_NULL', N'SAME', NULL, N'მიმდინარე საშუალო');
INSERT INTO sgs.derivation_source (id, term_id, component_id)
VALUES (NEXT VALUE FOR sgs.derivation_source_seq, @term, @avg);

SET
@term = NEXT VALUE FOR sgs.derivation_term_seq;
INSERT INTO sgs.derivation_term (id, rule_id, ordinal, weight, source_kind, reduce, period_ref, period_id, label)
VALUES (@term, @rule, 1, 0.20, N'COMPONENT', N'FIRST_NON_NULL', N'SAME', NULL, N'საწყისი ცოდნის ტესტი');
INSERT INTO sgs.derivation_source (id, term_id, component_id)
VALUES (NEXT VALUE FOR sgs.derivation_source_seq, @term, @initial);

SET
@term = NEXT VALUE FOR sgs.derivation_term_seq;
INSERT INTO sgs.derivation_term (id, rule_id, ordinal, weight, source_kind, reduce, period_ref, period_id, label)
VALUES (@term, @rule, 2, 0.30, N'COMPONENT', N'FIRST_NON_NULL', N'SAME', NULL, N'ფინალური ტესტი');
INSERT INTO sgs.derivation_source (id, term_id, component_id)
VALUES (NEXT VALUE FOR sgs.derivation_source_seq, @term, @finalTest);

-- ---- ANNUAL = average of the trimesters below the year ------------------

SET
@rule = NEXT VALUE FOR sgs.derivation_rule_seq;
INSERT INTO sgs.derivation_rule (id, component_id, chain_order, type, null_policy,
                                 renormalize_weights, rounding_mode, decimals)
VALUES (@rule, @annual, 0, N'AVERAGE', N'IGNORE', 0, N'HALF_UP', 0);

SET
@term = NEXT VALUE FOR sgs.derivation_term_seq;
INSERT INTO sgs.derivation_term (id, rule_id, ordinal, weight, source_kind, reduce, period_ref, period_id, label)
VALUES (@term, @rule, 0, 1, N'COMPONENT', N'AVERAGE', N'CHILDREN', NULL, N'ტრიმესტრები');
INSERT INTO sgs.derivation_source (id, term_id, component_id)
VALUES (NEXT VALUE FOR sgs.derivation_source_seq, @term, @trimester);

-- ---- RATING = the same, but across every subject the student takes ------

SET
@rule = NEXT VALUE FOR sgs.derivation_rule_seq;
INSERT INTO sgs.derivation_rule (id, component_id, chain_order, type, null_policy,
                                 renormalize_weights, rounding_mode, decimals)
VALUES (@rule, @rating, 0, N'AVERAGE', N'IGNORE', 0, N'HALF_UP', 2);

SET
@term = NEXT VALUE FOR sgs.derivation_term_seq;
INSERT INTO sgs.derivation_term (id, rule_id, ordinal, weight, source_kind, reduce, period_ref, period_id, label)
VALUES (@term, @rule, 0, 1, N'ALL_SUBJECTS', N'AVERAGE', N'CHILDREN', NULL, N'ყველა საგანი');
INSERT INTO sgs.derivation_source (id, term_id, component_id)
VALUES (NEXT VALUE FOR sgs.derivation_source_seq, @term, @trimester);

-- ---- assign it to every class -------------------------------------------
--
-- subject_id NULL means the template applies to every subject the class takes.
-- A class or subject that needs its own grid gets a narrower assignment later.

INSERT INTO sgs.template_assignment (id, class_group_id, subject_id, template_id,
                                     template_version_id, created_at, updated_at)
SELECT NEXT VALUE FOR sgs.template_assignment_seq, cg.id, NULL, @template, @version, SYSUTCDATETIME(), SYSUTCDATETIME()
FROM sgs.class_group cg
WHERE cg.academic_year_id = @year
  AND NOT EXISTS (SELECT 1 FROM sgs.template_assignment ta
    WHERE ta.class_group_id = cg.id
  AND ta.subject_id IS NULL
  AND ta.template_id = @template);

COMMIT TRANSACTION;

SELECT 'components' AS entity, COUNT(*) AS n
FROM sgs.component
WHERE template_version_id = @version
UNION ALL
SELECT 'rules', COUNT(*)
FROM sgs.derivation_rule r
         JOIN sgs.component c ON c.id = r.component_id
WHERE c.template_version_id = @version
UNION ALL
SELECT 'assignments', COUNT(*)
FROM sgs.template_assignment
WHERE template_version_id = @version;
GO
