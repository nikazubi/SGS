-- The trimester column the brief's tables have and the journals did not.
--
-- The brief shows three of its four grids with more than one level of period in
-- the same row:
--
--   Hours missed   Sept-Oct | Nov | TRIM I | Dec | Jan-Feb | Mar | TRIM II | ... | YEAR
--   Ethical norms  the same shape
--   Trim + final   TRIM I | TRIM II | TRIM III | Annual | Exam | Overall | Project
--
-- Two things were missing, and only one of them was arithmetic.
--
-- WHAT WAS NOT MISSING: the sum itself. A trimester total is a DERIVED column
-- with a SUM over CHILDREN, which is the same shape ANNUAL has used since
-- phase 1. The engine has always been able to compute this.
--
-- WHAT WAS: somewhere to put it. component.period_kind was read as a binary -
-- YEAR meant the root, anything else meant the journal's own level - so a
-- monthly journal could have a column on its months and a column on the year,
-- and nothing in between. It now means what it says: the kind of period the
-- column lives on. REPORTING sits on a reporting period, ROLLUP on a trimester,
-- YEAR on the year.
--
-- That reading makes one existing row wrong. HOURS_MISSED is typed against
-- reporting periods but was labelled ROLLUP, because under the old binary any
-- non-YEAR value meant the same thing. It is corrected here; the trimester
-- journal needs no correction, since its columns are ROLLUP and its periods are
-- trimesters, which agreed by accident and now agree on purpose.
--
-- HOURS_YEAR keeps summing DESCENDANTS rather than the new trimester column.
-- Summing the trimesters would give the same answer and adds a dependency for
-- nothing; db/023 chose DESCENDANTS deliberately after CHILDREN reached the
-- trimesters, found them empty and totalled to nothing.
--
-- Run after 026_absence_scales.sql. Idempotent.

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

-- ---- the correction ------------------------------------------------------

UPDATE sgs.component
SET period_kind = N'REPORTING'
WHERE code = N'HOURS_MISSED'
  AND period_kind = N'ROLLUP';

-- ---- the trimester total -------------------------------------------------

DECLARE
@version bigint = (
    SELECT TOP 1 v.id
    FROM sgs.template_version v
    JOIN sgs.grading_template t ON t.id = v.template_id
    WHERE t.name = N'გაცდენილი საათები' AND v.status = N'ACTIVE');

IF
@version IS NULL
BEGIN
COMMIT TRANSACTION;
SELECT 'no absence register - nothing to add' AS result;
RETURN;
END

IF
EXISTS (SELECT 1 FROM sgs.component
           WHERE template_version_id = @version AND code = N'HOURS_TRIMESTER')
BEGIN
COMMIT TRANSACTION;
SELECT 'already added' AS result;
RETURN;
END

-- Three reporting periods at most feed one trimester, and a period caps at 500
-- hours, so 1500 is the ceiling. Wide enough to be enterable as an override,
-- which is the only reason the scale matters on a derived column - the same
-- lesson db/026 recorded.
INSERT INTO sgs.component (id, template_version_id, code, label, ordinal, group_label,
                           kind, period_kind, subject_scoped, scale_min, scale_max, decimals,
                           allow_special_values, allow_override, parent_visible, summary_column)
VALUES (NEXT VALUE FOR sgs.component_seq, @version, N'HOURS_TRIMESTER',
             N'ტრიმესტრის ჯამი', 2, NULL,
             N'DERIVED', N'ROLLUP', 0, 0, 1500, 0, 0, 1, 1, 0);

DECLARE
@trimester bigint = (SELECT id FROM sgs.component
                             WHERE template_version_id = @version AND code = N'HOURS_TRIMESTER');
DECLARE
@hours bigint = (SELECT id FROM sgs.component
                         WHERE template_version_id = @version AND code = N'HOURS_MISSED');

DECLARE
@rule bigint = NEXT VALUE FOR sgs.derivation_rule_seq;
DECLARE
@term bigint = NEXT VALUE FOR sgs.derivation_term_seq;

INSERT INTO sgs.derivation_rule (id, component_id, chain_order, type, null_policy,
                                 renormalize_weights, rounding_mode, decimals)
VALUES (@rule, @trimester, 0, N'SUM', N'IGNORE', 0, N'HALF_UP', 0);

-- CHILDREN, not DESCENDANTS: the reporting periods are the trimester's own
-- children, one level down. DESCENDANTS would reach the same rows here and
-- would keep reaching if a level were ever added below them.
INSERT INTO sgs.derivation_term (id, rule_id, ordinal, weight, source_kind, reduce,
                                 period_ref, period_id, label)
VALUES (@term, @rule, 0, 1, N'COMPONENT', N'SUM', N'CHILDREN', NULL, N'საანგარიშო პერიოდები');

INSERT INTO sgs.derivation_source (id, term_id, component_id)
VALUES (NEXT VALUE FOR sgs.derivation_source_seq, @term, @hours);

COMMIT TRANSACTION;

SELECT c.code, c.label, c.kind, c.period_kind
FROM sgs.component c
WHERE c.template_version_id = @version
ORDER BY c.ordinal;
GO
