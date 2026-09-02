-- Bring students, classes and subjects across from the legacy dbo schema.
--
-- Grades are deliberately NOT migrated. This year's data is cleared before the
-- new version goes live, and the legacy GradeType taxonomy does not map onto
-- configurable components anyway.
--
-- The legacy data does not translate cleanly, and this script does not pretend
-- otherwise. Three things have to be reconciled:
--
--   1. dbo.academy_class.class_level is the SCHOOL (1 primary, 2 basic,
--      3 secondary), not the grade. The grade is the number at the front of
--      class_name: 5ა, 7ტ1, 10ჰ2. All 47 rows parse.
--
--   2. dbo.subject is really "subject taught by teacher X" - it carries a
--      teacher column, so ინგლისური ენა appears 16 times. 143 rows collapse to
--      51 distinct subjects. The new model separates the subject from who
--      teaches it, so the rows are folded and class_subject is remapped - with
--      the teacher carried onto class_subject, which is the grain it actually
--      lives at. All 658 class/subject pairs have exactly one teacher, so
--      nothing has to be reconciled, but folding first and asking later would
--      have destroyed the association permanently.
--
--   3. Eight usernames are duplicated and seven students have no class. Both
--      would violate constraints the new schema actually enforces, so they are
--      handled explicitly rather than left to fail halfway through.
--
-- Run once, against a database that already has 001_schema.sql. Idempotent:
-- re-running it is a no-op.

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

-- ---- 1. the year --------------------------------------------------------

IF
NOT EXISTS (SELECT 1 FROM sgs.academic_year WHERE code = N'2025-26')
INSERT INTO sgs.academic_year (id, code, starts_on, ends_on, is_current)
VALUES (NEXT VALUE FOR sgs.academic_year_seq, N'2025-26', '2025-09-01', '2026-06-30', 1);

DECLARE
@year bigint = (SELECT id FROM sgs.academic_year WHERE code = N'2025-26');

-- ---- 2. the period tree -------------------------------------------------
--
-- Three trimesters under a year. This is the structure the school actually
-- uses and the only one the client brief mentions.

IF
NOT EXISTS (SELECT 1 FROM sgs.period_scheme WHERE academic_year_id = @year)
INSERT INTO sgs.period_scheme (id, name, academic_year_id)
VALUES (NEXT VALUE FOR sgs.period_scheme_seq, N'ტრიმესტრები', @year);

DECLARE
@scheme bigint = (SELECT TOP 1 id FROM sgs.period_scheme WHERE academic_year_id = @year);

IF
NOT EXISTS (SELECT 1 FROM sgs.period WHERE scheme_id = @scheme AND code = N'YEAR')
INSERT INTO sgs.period (id, scheme_id, parent_id, code, label, ordinal, depth, kind, starts_on, ends_on)
VALUES (NEXT VALUE FOR sgs.period_seq, @scheme, NULL, N'YEAR', N'წელი', 0, 0, N'YEAR',
        '2025-09-01', '2026-06-30');

DECLARE
@yearPeriod bigint = (SELECT id FROM sgs.period WHERE scheme_id = @scheme AND code = N'YEAR');

INSERT INTO sgs.period (id, scheme_id, parent_id, code, label, ordinal, depth, kind, starts_on, ends_on)
SELECT NEXT VALUE FOR sgs.period_seq, @scheme, @yearPeriod, v.code, v.label, v.ordinal, 1, N'ROLLUP', v.starts_on, v.ends_on
FROM (VALUES
    (N'T1', N'I ტრიმესტრი', 0, CAST ('2025-09-01' AS date), CAST ('2025-12-05' AS date)), (N'T2', N'II ტრიმესტრი', 1, CAST ('2025-12-08' AS date), CAST ('2026-03-13' AS date)), (N'T3', N'III ტრიმესტრი', 2, CAST ('2026-03-16' AS date), CAST ('2026-06-30' AS date))
    ) v(code, label, ordinal, starts_on, ends_on)
WHERE NOT EXISTS (SELECT 1 FROM sgs.period p WHERE p.scheme_id = @scheme AND p.code = v.code);

-- ---- 3. schools ---------------------------------------------------------
--
-- class_level 1/2/3, which is what the legacy column actually held.

INSERT INTO sgs.school (id, code, name, ordinal)
SELECT NEXT VALUE FOR sgs.school_seq, v.code, v.name, v.ordinal
FROM (VALUES
    (N'PRIMARY', N'დაწყებითი სკოლა', 1), (N'BASIC', N'საბაზო სკოლა', 2), (N'SECONDARY', N'საშუალო სკოლა', 3)
    ) v(code, name, ordinal)
WHERE NOT EXISTS (SELECT 1 FROM sgs.school s WHERE s.code = v.code);

-- ---- 4. subjects, folded by name ----------------------------------------
--
-- 143 legacy rows, 51 distinct names. The teacher moves to class_subject
-- below; teaching_assignment needs a system_user_id and only 3 of the 98
-- teacher names match an account, so it stays empty until they have logins.

INSERT INTO sgs.subject (id, name, short_name, is_active, created_at, updated_at)
SELECT NEXT VALUE FOR sgs.subject_seq, x.name, NULL, 1, SYSUTCDATETIME(), SYSUTCDATETIME()
FROM (SELECT DISTINCT LTRIM(RTRIM(name)) AS name FROM dbo.subject WHERE name IS NOT NULL) x
WHERE NOT EXISTS (SELECT 1 FROM sgs.subject s WHERE s.name = x.name);

-- legacy subject id -> new subject id, via the name
IF
OBJECT_ID('tempdb..#subject_map') IS NOT NULL
DROP TABLE #subject_map;
SELECT ls.id AS legacy_id, s.id AS new_id
INTO #subject_map
FROM dbo.subject ls
         JOIN sgs.subject s ON s.name = LTRIM(RTRIM(ls.name));

-- ---- 5. classes ---------------------------------------------------------
--
-- The grade is the leading integer of class_name; the school comes from
-- class_level.

IF
OBJECT_ID('tempdb..#class_map') IS NOT NULL
DROP TABLE #class_map;
CREATE TABLE #class_map
(
    legacy_id bigint PRIMARY KEY,
    new_id    bigint
);

INSERT INTO sgs.class_group (id, school_id, academic_year_id, period_scheme_id, level, name)
SELECT NEXT VALUE FOR sgs.class_group_seq, sc.id, @year, @scheme, CAST (LEFT (ac.class_name, PATINDEX('%[^0-9]%', ac.class_name + 'x') - 1) AS smallint), ac.class_name
FROM dbo.academy_class ac
    JOIN sgs.school sc
ON sc.ordinal = ac.class_level
WHERE NOT EXISTS (
    SELECT 1 FROM sgs.class_group cg
    WHERE cg.academic_year_id = @year
  AND cg.school_id = sc.id
  AND cg.name = ac.class_name);

-- Mapped back by name, which is unique within a school and year by constraint.
INSERT INTO #class_map (legacy_id, new_id)
SELECT ac.id, cg.id
FROM dbo.academy_class ac
         JOIN sgs.school sc ON sc.ordinal = ac.class_level
         JOIN sgs.class_group cg ON cg.academic_year_id = @year
    AND cg.school_id = sc.id
    AND cg.name = ac.class_name;

-- ---- 6. class_subject, remapped and deduplicated ------------------------
--
-- Folding the subjects can turn two legacy rows into one, e.g. a class taking
-- ინგლისური ენა from two teachers.

INSERT INTO sgs.class_subject (id, class_group_id, subject_id, sort_index,
                               teacher_name, template_version_id)
SELECT NEXT VALUE FOR sgs.class_subject_seq, x.class_group_id, x.subject_id, ROW_NUMBER() OVER (PARTITION BY x.class_group_id ORDER BY x.subject_id), x.teacher_name, NULL
FROM (
    SELECT cm.new_id AS class_group_id, sm.new_id AS subject_id,
    -- The stored form is "პედაგოგი: <name>"; the label is not part of
    -- the name. MAX over a group that is already single-valued.
    MAX (NULLIF (LTRIM(RTRIM(REPLACE(
    CAST (ls.teacher AS nvarchar(400)), N'პედაგოგი:', N''))), N'')) AS teacher_name
    FROM dbo.class_subject cs
    JOIN #class_map cm ON cm.legacy_id = cs.academy_class_id
    JOIN #subject_map sm ON sm.legacy_id = cs.subject_id
    JOIN dbo.subject ls ON ls.id = cs.subject_id
    GROUP BY cm.new_id, sm.new_id
    ) x
WHERE NOT EXISTS (
    SELECT 1 FROM sgs.class_subject e
    WHERE e.class_group_id = x.class_group_id
  AND e.subject_id = x.subject_id);

-- ---- 7. students --------------------------------------------------------
--
-- Eight usernames are duplicated in the legacy data, where nothing enforced
-- uniqueness. The new schema does, so the later rows are suffixed with their
-- legacy id rather than being dropped - a name nobody can log in with is
-- recoverable, a deleted student is not.

IF
OBJECT_ID('tempdb..#student_src') IS NOT NULL
DROP TABLE #student_src;
SELECT s.id    AS legacy_id,
       s.first_name,
       s.last_name,
       s.owner_mail,
       s.password,
       s.personal_number,
       s.academy_class_id,
       CASE
           WHEN ROW_NUMBER() OVER (PARTITION BY s.username ORDER BY s.id) = 1
            THEN s.username
           ELSE s.username + '.' + CAST(s.id AS varchar(20))
           END AS username
INTO #student_src
FROM dbo.students s;

INSERT INTO sgs.student (id, username, password_hash, first_name, last_name,
                         personal_number, guardian_email, is_active, created_at, updated_at)
SELECT NEXT VALUE FOR sgs.student_seq, src.username, ISNULL(src.password, N'{noop}!disabled'), ISNULL(src.first_name, N''), ISNULL(src.last_name, N''), src.personal_number, src.owner_mail, 1, SYSUTCDATETIME(), SYSUTCDATETIME()
FROM #student_src src
WHERE NOT EXISTS (SELECT 1 FROM sgs.student e WHERE e.username = src.username)
-- Also skipped when the personal number is already present. Re-running after
-- db/015 has merged duplicate records must not reintroduce the record that
-- was merged away: the number is the identity, the username is not.
  AND NOT EXISTS (
    SELECT 1 FROM sgs.student e
    WHERE e.personal_number IS NOT NULL
  AND e.personal_number = RIGHT (N'00000000000'
    + LTRIM(RTRIM(src.personal_number))
    , 11));

-- ---- 8. enrollments -----------------------------------------------------
--
-- Seven legacy students have no class. An enrollment is what ties a student to
-- a class for a year and the grid is keyed by it, so those students migrate but
-- are left unenrolled rather than being attached to an arbitrary class.

INSERT INTO sgs.enrollment (id, student_id, class_group_id, academic_year_id, joined_on, left_on)
SELECT NEXT VALUE FOR sgs.enrollment_seq, st.id, cm.new_id, @year, '2025-09-01', NULL
FROM #student_src src
    JOIN sgs.student st
ON st.username = src.username
    JOIN #class_map cm ON cm.legacy_id = src.academy_class_id
WHERE NOT EXISTS (
    SELECT 1 FROM sgs.enrollment e
    WHERE e.student_id = st.id
  AND e.academic_year_id = @year);

-- ---- what happened ------------------------------------------------------

SELECT 'schools' AS entity, COUNT(*) AS n
FROM sgs.school
UNION ALL
SELECT 'periods', COUNT(*)
FROM sgs.period
WHERE scheme_id = @scheme
UNION ALL
SELECT 'subjects', COUNT(*)
FROM sgs.subject
UNION ALL
SELECT 'classes', COUNT(*)
FROM sgs.class_group
WHERE academic_year_id = @year
UNION ALL
SELECT 'class_subject', COUNT(*)
FROM sgs.class_subject
UNION ALL
SELECT 'class_subject_with_teacher',
       (SELECT COUNT(*) FROM sgs.class_subject WHERE teacher_name IS NOT NULL)
UNION ALL
SELECT 'students', COUNT(*)
FROM sgs.student
UNION ALL
SELECT 'enrollments', COUNT(*)
FROM sgs.enrollment
WHERE academic_year_id = @year
UNION ALL
SELECT 'students_left_unenrolled',
       (SELECT COUNT(*) FROM #student_src WHERE academy_class_id IS NULL)
UNION ALL
SELECT 'usernames_disambiguated',
       (SELECT COUNT(*)
        FROM #student_src src
                 JOIN dbo.students s ON s.id = src.legacy_id
        WHERE src.username <> s.username);

COMMIT TRANSACTION;
GO
