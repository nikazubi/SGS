-- Carry the teacher across from the legacy subject rows.
--
-- Only needed for a database migrated before 006 learned to do this;
-- 006_migrate_from_dbo.sql now populates teacher_name in the same pass.
--
-- The legacy dbo.subject table is really "subject taught by teacher X" - 143
-- rows, 51 distinct names, ინგლისური ენა appearing 16 times. Folding those into
-- one subject each is right, but it destroys the teacher association unless it
-- is moved first. class_subject is the grain it actually belongs at, and all
-- 658 class/subject pairs have exactly one teacher, so nothing is ambiguous.
--
-- It stays a name rather than a reference: only 3 of the 98 teacher names match
-- a row in dbo.system_user_table. Most teachers have no login, and the accounts
-- that do exist are spelled in Latin while these are in Georgian.
-- teaching_assignment is the structured form, for when they have accounts.

SET
XACT_ABORT ON;
SET
NOCOUNT ON;
BEGIN
TRANSACTION;

IF
NOT EXISTS (SELECT 1 FROM sys.columns
               WHERE object_id = OBJECT_ID('sgs.class_subject') AND name = 'teacher_name')
ALTER TABLE sgs.class_subject
    ADD teacher_name nvarchar(256) NULL;
GO

UPDATE cs
SET teacher_name = src.teacher_name FROM sgs.class_subject cs
JOIN (
    SELECT cg.id AS class_group_id, s.id AS subject_id,
           -- Stored as "პედაგოგი: <name>"; the label is not part of the name.
           MAX(NULLIF(LTRIM(RTRIM(REPLACE(
                 CAST(ls.teacher AS nvarchar(400)), N'პედაგოგი:', N''))), N'')) AS teacher_name
    FROM dbo.class_subject lcs
    JOIN dbo.academy_class ac ON ac.id = lcs.academy_class_id
    JOIN dbo.subject ls ON ls.id = lcs.subject_id
    JOIN sgs.school sc ON sc.ordinal = ac.class_level
    JOIN sgs.class_group cg ON cg.school_id = sc.id AND cg.name = ac.class_name
    JOIN sgs.subject s ON s.name = LTRIM(RTRIM(ls.name))
    GROUP BY cg.id, s.id
) src
ON src.class_group_id = cs.class_group_id AND src.subject_id = cs.subject_id
WHERE cs.teacher_name IS NULL;

COMMIT TRANSACTION;

SELECT 'class_subject' AS entity, COUNT(*) AS n
FROM sgs.class_subject
UNION ALL
SELECT 'with_teacher', COUNT(*)
FROM sgs.class_subject
WHERE teacher_name IS NOT NULL;
GO
