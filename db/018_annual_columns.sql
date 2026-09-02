-- The three columns the client brief's section 4 shows and the seeded journal
-- did not have.
--
-- Two of them are not new to the school, which is worth recording because the
-- brief's own assessment said all three were:
--
--   * Final exam            -- legacy GradeType.FINAL_EXAM
--   * Overall academic      -- legacy GradeServiceImpl:565, which averages
--                              semester 1, semester 2 and the final exam
--   * Academic project      -- genuinely new; nothing in the legacy system
--
-- The formulas come from the school directly: the project mark is typed in, and
-- the overall assessment is the average of the annual assessment and the final
-- exam. Seeded rather than left empty so the grid is not blank on day one -
-- they are ordinary configuration and the school can change either from the
-- formula editor without a script.
--
-- Note the difference from legacy: it averaged two *semesters* and the exam.
-- There are no semesters any more (decision 2), so this averages the annual
-- assessment and the exam, which is what the school asked for.

SET
XACT_ABORT ON;
SET
NOCOUNT ON;
SET
QUOTED_IDENTIFIER ON;
SET
ANSI_NULLS ON;

DECLARE
@template bigint =
    (SELECT id FROM sgs.grading_template WHERE name = N'ტრიმესტრული შეფასება');

IF
@template IS NULL
BEGIN
    RAISERROR
('No trimester journal - run 007_seed_template.sql first.', 16, 1);
    RETURN;
END

DECLARE
@version bigint =
    (SELECT id FROM sgs.template_version WHERE template_id = @template AND version_no = 1);

IF
@version IS NULL
BEGIN
    RAISERROR
('No version 1 of the trimester journal.', 16, 1);
    RETURN;
END

BEGIN
TRANSACTION;

-- ---- the columns ---------------------------------------------------------
--
-- All three sit at the year, beside ANNUAL and RATING: they are settled once a
-- year, not once a trimester. period_kind YEAR is what puts them on the annual
-- grid and keeps them off every trimester grid.

INSERT INTO sgs.component (id, template_version_id, code, label, ordinal, group_label,
                           kind, period_kind, subject_scoped, scale_min, scale_max, decimals,
                           allow_special_values, allow_override, parent_visible, summary_column)
SELECT NEXT VALUE FOR sgs.component_seq, @version, v.code, v.label, v.ordinal, NULL, v.kind, N'YEAR', 1, 0, 10, 0, 1, 1, 1, 0
FROM (VALUES
    (N'FINAL_EXAM', N'ფინალური გამოცდა', 13, N'INPUT'), (N'OVERALL', N'საბოლოო აკადემიური შეფასება', 14, N'DERIVED'), (N'PROJECT', N'აკადემიური პროექტის შეფასება', 15, N'INPUT')
    ) v(code, label, ordinal, kind)
WHERE NOT EXISTS (SELECT 1 FROM sgs.component c
    WHERE c.template_version_id = @version
  AND c.code = v.code);

-- The rating is a whole-school aggregate and belongs after the subject columns
-- the brief lists, not in the middle of them.
UPDATE sgs.component
SET ordinal = 16
WHERE template_version_id = @version
  AND code = N'RATING'
  AND ordinal <> 16;

-- ---- OVERALL = average of the annual assessment and the final exam --------

DECLARE
@overall   bigint = (SELECT id FROM sgs.component
                             WHERE template_version_id = @version AND code = N'OVERALL');
DECLARE
@annual    bigint = (SELECT id FROM sgs.component
                             WHERE template_version_id = @version AND code = N'ANNUAL');
DECLARE
@finalExam bigint = (SELECT id FROM sgs.component
                             WHERE template_version_id = @version AND code = N'FINAL_EXAM');

IF
NOT EXISTS (SELECT 1 FROM sgs.derivation_rule WHERE component_id = @overall)
BEGIN
    DECLARE
@rule bigint = NEXT VALUE FOR sgs.derivation_rule_seq;
    DECLARE
@term bigint;

    -- IGNORE, not AS_ZERO: a student who has not sat the exam yet should read
    -- as their annual assessment, not as half of it.
INSERT INTO sgs.derivation_rule (id, component_id, chain_order, type, null_policy,
                                 renormalize_weights, rounding_mode, decimals)
VALUES (@rule, @overall, 0, N'AVERAGE', N'IGNORE', 0, N'HALF_UP', 0);

SET
@term = NEXT VALUE FOR sgs.derivation_term_seq;
INSERT INTO sgs.derivation_term (id, rule_id, ordinal, weight, source_kind, reduce,
                                 period_ref, period_id, label)
VALUES (@term, @rule, 0, 1, N'COMPONENT', N'FIRST_NON_NULL', N'SAME', NULL,
        N'წლიური აკადემიური შეფასება');
INSERT INTO sgs.derivation_source (id, term_id, component_id)
VALUES (NEXT VALUE FOR sgs.derivation_source_seq, @term, @annual);

SET
@term = NEXT VALUE FOR sgs.derivation_term_seq;
INSERT INTO sgs.derivation_term (id, rule_id, ordinal, weight, source_kind, reduce,
                                 period_ref, period_id, label)
VALUES (@term, @rule, 1, 1, N'COMPONENT', N'FIRST_NON_NULL', N'SAME', NULL,
        N'ფინალური გამოცდა');
INSERT INTO sgs.derivation_source (id, term_id, component_id)
VALUES (NEXT VALUE FOR sgs.derivation_source_seq, @term, @finalExam);
END

COMMIT TRANSACTION;
GO

SELECT c.ordinal, c.code, c.label, c.kind
FROM sgs.component c
         JOIN sgs.template_version v ON v.id = c.template_version_id
         JOIN sgs.grading_template t ON t.id = v.template_id
WHERE t.name = N'ტრიმესტრული შეფასება'
  AND v.version_no = 1
  AND c.period_kind = N'YEAR'
ORDER BY c.ordinal;
GO
