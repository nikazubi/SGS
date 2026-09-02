-- A small roster to test against: two classes, two students each.
--
-- This is db/006_migrate_from_dbo.sql's replacement for a machine that has no
-- legacy database to migrate from. Sections 1 to 3 - the year, the period tree
-- and the three schools - are copied from it verbatim and must stay that way:
-- every later script keys off "the current academic year" and "the top period
-- scheme", and db/013's month list is hardcoded to these exact dates.
--
-- WHY 2025-26 AND NOT THE YEAR WE ARE ACTUALLY IN. It is the year the shipped
-- scripts seed, so all of them run unmodified - but it is also the more useful
-- of the two for testing, because the whole year is in the past. Daily absence
-- refuses a future date, so on a year that has not started yet there is no day
-- anywhere that can be marked; on a finished one every school day can.
--
-- The roster below is invented. Nothing here is real: the personal numbers are
-- sequential, the guardian addresses are on a domain that cannot receive mail
-- (.invalid is reserved by RFC 2606, so a stray absence notice bounces rather
-- than reaching a stranger), and the passwords are printed in db/demo/README.md.
--
-- Idempotent. Run after 001_schema.sql and 002_indexes.sql.

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

INSERT INTO sgs.school (id, code, name, ordinal)
SELECT NEXT VALUE FOR sgs.school_seq, v.code, v.name, v.ordinal
FROM (VALUES
    (N'PRIMARY', N'დაწყებითი სკოლა', 1), (N'BASIC', N'საბაზო სკოლა', 2), (N'SECONDARY', N'საშუალო სკოლა', 3)
    ) v(code, name, ordinal)
WHERE NOT EXISTS (SELECT 1 FROM sgs.school s WHERE s.code = v.code);

-- ---- 4. two classes, deliberately in different schools ------------------
--
-- One primary and one basic, because the two are not the same on the parent
-- side: primary gets the content modules and the absence register but no
-- gradebook. Staff-side they behave identically, so this costs nothing and
-- makes the difference visible without needing a second database.
--
-- `level` is the grade, which in the legacy data was the leading digit of the
-- class name. Kept consistent with that here.

INSERT INTO sgs.class_group (id, school_id, academic_year_id, period_scheme_id, level, name)
SELECT NEXT VALUE FOR sgs.class_group_seq, sc.id, @year, @scheme, v.level, v.name
FROM (VALUES
    (N'PRIMARY', CAST (3 AS smallint), N'3ა'), (N'BASIC', CAST (8 AS smallint), N'8ბ')
    ) v(school_code, level, name)
    JOIN sgs.school sc
ON sc.code = v.school_code
WHERE NOT EXISTS (SELECT 1 FROM sgs.class_group cg
    WHERE cg.academic_year_id = @year
  AND cg.school_id = sc.id
  AND cg.name = v.name);

DECLARE
@c3a bigint = (SELECT id FROM sgs.class_group WHERE academic_year_id = @year AND name = N'3ა');
DECLARE
@c8b bigint = (SELECT id FROM sgs.class_group WHERE academic_year_id = @year AND name = N'8ბ');

-- ---- 5. subjects --------------------------------------------------------

INSERT INTO sgs.subject (id, name, short_name, is_active, created_at, updated_at)
SELECT NEXT VALUE FOR sgs.subject_seq, v.name, v.short_name, 1, SYSUTCDATETIME(), SYSUTCDATETIME()
FROM (VALUES
    (N'ქართული ენა და ლიტერატურა', N'ქართული'), (N'მათემატიკა', N'მათემ.'), (N'ინგლისური ენა', N'ინგლ.'), (N'ბუნებისმეტყველება', N'ბუნება'), (N'ისტორია', N'ისტ.'), (N'სპორტი', N'სპორტი')
    ) v(name, short_name)
WHERE NOT EXISTS (SELECT 1 FROM sgs.subject s WHERE s.name = v.name);

-- ---- 6. what each class takes, and who teaches it -----------------------
--
-- sort_index is the teaching order the subject list is drawn in - db/010
-- derives it from the legacy data, and it is set by hand here. teacher_name is
-- a name rather than a user: teaching_assignment needs a login, and in the live
-- data only 3 of 98 teachers have one.

INSERT INTO sgs.class_subject (id, class_group_id, subject_id, sort_index, teacher_name, template_version_id)
SELECT NEXT VALUE FOR sgs.class_subject_seq, v.class_group_id, s.id, v.sort_index, v.teacher, NULL
FROM (VALUES
    (@c3a, N'ქართული ენა და ლიტერატურა', 0, N'ნათელა ქავთარაძე'), (@c3a, N'მათემატიკა', 1, N'ირმა გელაშვილი'), (@c3a, N'ინგლისური ენა', 2, N'თამარ წიკლაური'), (@c3a, N'ბუნებისმეტყველება', 3, N'ნათელა ქავთარაძე'), (@c3a, N'სპორტი', 4, N'დავით ქურდაძე'), (@c8b, N'ქართული ენა და ლიტერატურა', 0, N'ეკატერინე ჯანაშია'), (@c8b, N'მათემატიკა', 1, N'ზურაბ კიკნაძე'), (@c8b, N'ინგლისური ენა', 2, N'თამარ წიკლაური'), (@c8b, N'ისტორია', 3, N'ლევან მაჭავარიანი'), (@c8b, N'სპორტი', 4, N'დავით ქურდაძე')
    ) v(class_group_id, subject_name, sort_index, teacher)
    JOIN sgs.subject s
ON s.name = v.subject_name
WHERE NOT EXISTS (SELECT 1 FROM sgs.class_subject cs
    WHERE cs.class_group_id = v.class_group_id
  AND cs.subject_id = s.id);

-- ---- 7. four students ---------------------------------------------------
--
-- ნინო and მარიამ ბერიძე are sisters and share the username "beridze" with
-- different passwords. That is deliberate, and it is the school's own rule: the
-- personal number is unique, the (username, password) pair is unique, and
-- neither half of the pair identifies anybody on its own. It is also the shape
-- that broke the legacy parent login, which looked a student up by username
-- alone and served whichever row came back first.
--
-- password_hash is unsalted uppercase MD5, which is what the live system
-- stores. The plaintexts are in db/demo/README.md.

INSERT INTO sgs.student (id, username, password_hash, first_name, last_name,
                         personal_number, guardian_email, is_active, created_at, updated_at)
SELECT NEXT VALUE FOR sgs.student_seq, v.username, v.password_hash, v.first_name, v.last_name, v.personal_number, v.guardian_email, 1, SYSUTCDATETIME(), SYSUTCDATETIME()
FROM (VALUES
    -- nino2025
    (N'beridze', N'D92703C73D36784CD16087A32848E7BD', N'ნინო', N'ბერიძე', N'01001000001', N'beridze.family@example.invalid'),
    -- giorgi2025
    (N'maisuradze', N'DB0255094D471720B4FD90C62B6EFFF8', N'გიორგი', N'მაისურაძე', N'01001000002', N'maisuradze.family@example.invalid'),
    -- mariam2025
    (N'beridze', N'441A69AE31B05D3EECB0A6B5CDDF06AC', N'მარიამ', N'ბერიძე', N'01001000003', N'beridze.family@example.invalid'),
    -- luka2025
    (N'chkheidze', N'C3F56A23991CCCF8B70B830D250408FF', N'ლუკა', N'ჩხეიძე', N'01001000004', N'chkheidze.family@example.invalid')
    ) v(username, password_hash, first_name, last_name, personal_number, guardian_email)
WHERE NOT EXISTS (SELECT 1 FROM sgs.student e WHERE e.personal_number = v.personal_number);

-- ---- 8. enrollments -----------------------------------------------------
--
-- Matched on the personal number rather than the username, because two of them
-- share a username by design.

INSERT INTO sgs.enrollment (id, student_id, class_group_id, academic_year_id, joined_on, left_on)
SELECT NEXT VALUE FOR sgs.enrollment_seq, st.id, v.class_group_id, @year, '2025-09-01', NULL
FROM (VALUES
    (N'01001000001', @c3a), (N'01001000002', @c3a), (N'01001000003', @c8b), (N'01001000004', @c8b)
    ) v(personal_number, class_group_id)
    JOIN sgs.student st
ON st.personal_number = v.personal_number
WHERE NOT EXISTS (SELECT 1 FROM sgs.enrollment e
    WHERE e.student_id = st.id
  AND e.academic_year_id = @year);

COMMIT TRANSACTION;

SELECT 'schools' AS entity, COUNT(*) AS n
FROM sgs.school
UNION ALL
SELECT 'periods', COUNT(*)
FROM sgs.period
WHERE scheme_id = @scheme
UNION ALL
SELECT 'classes', COUNT(*)
FROM sgs.class_group
WHERE academic_year_id = @year
UNION ALL
SELECT 'subjects', COUNT(*)
FROM sgs.subject
UNION ALL
SELECT 'class_subject', COUNT(*)
FROM sgs.class_subject
UNION ALL
SELECT 'students', COUNT(*)
FROM sgs.student
UNION ALL
SELECT 'enrollments', COUNT(*)
FROM sgs.enrollment
WHERE academic_year_id = @year;
GO
