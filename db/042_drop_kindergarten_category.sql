-- Removes the news category "საბავშვო ბაღი".
--
-- db/020 seeded it as the one category the school had named. In November 2026
-- the school asked for it to go. 020 no longer seeds it; this takes it out of a
-- database that already has it.
--
-- News filed under it is kept and loses only the label: category_id is cleared,
-- so those items show with no category and drop out of no list. The report
-- below says how many. The published snapshot also names the category, but
-- nothing reads that field - the label shown comes from category_id - so it is
-- left alone.
--
-- Matched on the exact name. A category the school renamed it to is theirs and
-- is not touched.
--
-- Idempotent: a second run finds nothing to do.

SET
XACT_ABORT ON;
SET
NOCOUNT ON;
SET
QUOTED_IDENTIFIER ON;
SET
ANSI_NULLS ON;
GO

DECLARE
@category bigint = (SELECT id FROM sgs.post_category WHERE name = N'საბავშვო ბაღი');
DECLARE
@unlabelled int = 0;

IF
@category IS NOT NULL
BEGIN
BEGIN
TRANSACTION;
UPDATE sgs.post
SET category_id = NULL
WHERE category_id = @category;
SET
@unlabelled = @@ROWCOUNT;
DELETE
FROM sgs.post_category
WHERE id = @category;
COMMIT TRANSACTION;
END

SELECT CASE WHEN @category IS NULL THEN N'not there - nothing to do' ELSE N'removed' END
                   AS kindergarten_category,
       @unlabelled AS news_left_without_category;
GO
