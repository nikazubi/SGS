-- Subject column order, moved from Java into data.
--
-- Order used to come from ExcelUtils.subjectPattern, a hardcoded list of 39
-- Georgian subject names; anything absent sorted to the end. Checked against the
-- live data, 20 of 51 subjects did not match - including five the list was
-- trying to cover, because it spells them with a slash and no row in
-- dbo.subject contains a slash at all:
--
--     pattern "ალგებრა / გეომეტრია"   vs   data  ალგებრა  გეომეტრია
--
-- So a fifth of the subjects landed at the end of every export in arbitrary
-- order, and the order differed between classes.
--
-- 006_migrate_from_dbo.sql filled sort_index with ROW_NUMBER() over subject_id,
-- which is equally arbitrary. This reseeds it: the known teaching order first,
-- then everything else alphabetically after it. Editable from then on, rather
-- than a code change.

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

IF
OBJECT_ID('tempdb..#subject_order') IS NOT NULL
DROP TABLE #subject_order;
CREATE TABLE #subject_order
(
    ordinal int NOT NULL,
    name    nvarchar(256) NOT NULL
);

INSERT INTO #subject_order (ordinal, name)
VALUES (0, N'ქართული ენა'),
       (1, N'ქართული ლიტერატურა'),
       (2, N'ქართული სტილისტიკა'),
       (3, N'ქართული გრამატიკა'),
       (4, N'ავტორის საათი'),
       (5, N'ავტორის საათი / წერის საათი'),
       (6, N'მათემატიკა'),
       (7, N'ალგებრა / გეომეტრია'),
       (8, N'ალგორითმიკა'),
       (9, N'პრე-კალკულუსი'),
       (10, N'კალკულუსი'),
       (11, N'ბიზნესი / ეკონომიკა'),
       (12, N'ბლოკური პროგრამირება'),
       (13, N'ინგლისური ენა'),
       (14, N'ინგლისური ლიტერატურა'),
       (15, N'ინგლისური გრამატიკა და ლექსიკა'),
       (16, N'ბიზნეს ინგლისური'),
       (17, N'საკომუნიკაციო ინგლისური'),
       (18, N'ინგლისური აკადემიური წერა'),
       (19, N'გერმანული ენა'),
       (20, N'რუსული ენა'),
       (21, N'ფიზიკა'),
       (22, N'ქიმია'),
       (23, N'ბიოლოგია'),
       (24, N'ისტორია'),
       (25, N'ისტორია / ჩვენი საქართველო'),
       (26, N'გეოგრაფია'),
       (27, N'სამოქალაქო განათლება'),
       (28, N'ინფორმაციული და საკომუნიკაციო ტექნოლოგიები'),
       (29, N'რობოტიკა / ინჟინერია'),
       (30, N'არტ ხელოვნება'),
       (31, N'არტ სახელოსნო'),
       (32, N'სამართალის შესავალი'),
       (33, N'სასცენო ხელოვნება'),
       (34, N'მუსიკა'),
       (35, N'სპორტი');

-- Matched on a slash-insensitive, whitespace-collapsed form, so that
-- "ალგებრა / გეომეტრია" and "ალგებრა  გეომეტრია" are recognised as the same
-- subject rather than one of them falling off the end.
IF
OBJECT_ID('tempdb..#normalised') IS NOT NULL
DROP TABLE #normalised;
SELECT o.ordinal,
       REPLACE(REPLACE(REPLACE(o.name, N'/', N' '), N'  ', N' '), N'  ', N' ') AS norm
INTO #normalised
FROM #subject_order o;

UPDATE cs
SET sort_index = x.new_index FROM sgs.class_subject cs
JOIN (
    SELECT cs2.id,
           ROW_NUMBER() OVER (
               PARTITION BY cs2.class_group_id
               ORDER BY ISNULL(n.ordinal, 9999), s.name) AS new_index
    FROM sgs.class_subject cs2
    JOIN sgs.subject s ON s.id = cs2.subject_id
    LEFT JOIN #normalised n
           ON n.norm = LTRIM(RTRIM(
                  REPLACE(REPLACE(REPLACE(s.name, N'/', N' '), N'  ', N' '), N'  ', N' ')))
) x
ON x.id = cs.id;

COMMIT TRANSACTION;

SELECT 'class_subject_rows' AS entity, COUNT(*) AS n
FROM sgs.class_subject
UNION ALL
SELECT 'subjects_matched_to_known_order',
       (SELECT COUNT(DISTINCT s.id)
        FROM sgs.subject s
                 JOIN #normalised n ON n.norm = LTRIM(RTRIM(
                REPLACE(REPLACE(REPLACE(s.name, N'/', N' '), N'  ', N' '), N'  ', N' '))))
UNION ALL
SELECT 'subjects_total', (SELECT COUNT(*) FROM sgs.subject);
GO
