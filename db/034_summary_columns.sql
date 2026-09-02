-- The report card: which columns belong in the cross-period summary.
--
-- The brief's second table for basic and secondary reads, per subject:
--
--   # | Student | Trim I | Trim II | Trim III | Annual | Final exam |
--       Overall | Academic project
--
-- Three of those columns are one component - the trimester assessment - shown
-- at three periods, and the rest are the year's own. That is a transposed grid
-- over two levels, which is what the register already draws since db/033 gave
-- period_kind its real meaning.
--
-- What the model could not say is *which* columns belong in such a summary.
-- Nothing implies it: every column of the seeded journal is parent-visible, and
-- ONGOING_AVG and TRIMESTER_GRADE are both derived and both ungrouped. A report
-- card is an editorial selection, so component.summary_column states it.
--
-- Off by default, so the absence register and anything built in the wizard have
-- no summary until somebody decides they should.
--
-- Run after 018_annual_columns.sql, which adds the exam, overall and project
-- columns this table is mostly made of. Idempotent.

SET
XACT_ABORT ON;
SET
NOCOUNT ON;
SET
QUOTED_IDENTIFIER ON;
SET
ANSI_NULLS ON;

IF
NOT EXISTS (SELECT 1 FROM sys.columns
               WHERE object_id = OBJECT_ID('sgs.component') AND name = 'summary_column')
BEGIN
ALTER TABLE sgs.component
    ADD summary_column bit NULL;
END
GO

UPDATE sgs.component
SET summary_column = 0
WHERE summary_column IS NULL;
GO

-- Hibernate emits no DEFAULT constraints, so the column is made NOT NULL only
-- after every existing row has a value - the same order db/021 used for
-- grid_mode and db/029 for locks_on_publish.
IF EXISTS (SELECT 1 FROM sys.columns
           WHERE object_id = OBJECT_ID('sgs.component') AND name = 'summary_column'
             AND is_nullable = 1)
BEGIN
ALTER TABLE sgs.component ALTER COLUMN summary_column bit NOT NULL;
END
GO

-- ---- the brief's seven ---------------------------------------------------
--
-- TRIMESTER_GRADE supplies the three trimester columns by being shown at each
-- of them; the rest are year columns and supply one each. RATING is left out
-- deliberately: it is a ranking across every subject, and this table is per
-- subject, so a rating column in it would be the same number repeated beside
-- marks it has nothing to do with.

UPDATE sgs.component
SET summary_column = 1
WHERE code IN (N'TRIMESTER_GRADE', N'ANNUAL', N'FINAL_EXAM',
               N'OVERALL', N'PROJECT');
GO

SELECT c.code, c.label, c.period_kind, c.summary_column
FROM sgs.component c
         JOIN sgs.template_version v ON v.id = c.template_version_id
         JOIN sgs.grading_template t ON t.id = v.template_id
WHERE t.name = N'ტრიმესტრული შეფასება'
ORDER BY c.ordinal;
GO
