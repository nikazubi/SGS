-- What parents can see.
--
-- parent_visible is off by default and set per journal by an admin. A journal
-- the school creates is a staff working document until someone decides
-- otherwise - an internal tracking grid appearing on the parent portal the
-- moment it is created is the wrong way round.
--
-- chart_key names the chart the parent view draws, if any. The chart itself is
-- code (it has to know what is an axis and what is a series); which journal
-- gets which is data. Keyed by a stable name rather than by uuid, because uuids
-- differ between environments and a uuid-keyed registry would need different
-- code in each.

SET
XACT_ABORT ON;
SET
NOCOUNT ON;

IF
NOT EXISTS (SELECT 1 FROM sys.columns
               WHERE object_id = OBJECT_ID('sgs.grading_template') AND name = 'is_parent_visible')
ALTER TABLE sgs.grading_template
    ADD is_parent_visible bit NULL;
IF
NOT EXISTS (SELECT 1 FROM sys.columns
               WHERE object_id = OBJECT_ID('sgs.grading_template') AND name = 'chart_key')
ALTER TABLE sgs.grading_template
    ADD chart_key nvarchar(32) NULL;
GO

-- The migrated academic journal is what parents already see today, so it keeps
-- its visibility and the grade chart the current console draws.
UPDATE sgs.grading_template
SET is_parent_visible = ISNULL(is_parent_visible, 1),
    chart_key         = ISNULL(chart_key, N'GRADE_TREND');
GO

ALTER TABLE sgs.grading_template ALTER COLUMN is_parent_visible bit NOT NULL;
GO

SELECT name, is_parent_visible, ISNULL(chart_key, N'—') AS chart_key
FROM sgs.grading_template;
GO
