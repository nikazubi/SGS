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

IF EXISTS (SELECT 1 FROM sgs.parent_menu_item)
BEGIN
SELECT 'already seeded' AS result;
RETURN;
END
GO

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
