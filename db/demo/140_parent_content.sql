-- The parent portal with something in it: a schedule, a menu, news, and a login
-- to see them with.
--
-- db/demo/100_roster.sql deliberately seeds no content - "entering them is the
-- thing being tested; a seed full of them would be in the way". That was right
-- while the content screens were being built and is wrong now: the parent
-- console has five boxes and a fresh demo database draws all of them empty, so
-- the first thing anyone demonstrating it has to do is spend ten minutes typing
-- a week of lessons into the staff console.
--
-- ---------------------------------------------------------------------------
-- WHY THIS WRITES post_line AND NOT ONLY published_payload
--
-- A standing document is stored twice and both copies matter:
--
--   post_line          the rows the staff editor edits
--   published_payload  the JSON snapshot the parent reads
--
-- ParentContentService.standingDoc reads the snapshot; PostService.toView reads
-- the rows. A seed that wrote only the snapshot - which is what this database
-- was in when this file was written - gives parents a full week and staff a
-- blank editor, and the next person who opens that editor and presses publish
-- rebuilds the snapshot from nothing and wipes the parent's week. So the rows
-- are the seeded thing and the snapshot is built from them, in that order, the
-- same way PostService does it.
--
-- Section 3 backfills rows from a snapshot for exactly that case.
--
-- ---------------------------------------------------------------------------
-- WHY THE TEST CHILD IS IN 3a AND NOT 8b
--
-- ParentContentService.modulesFor gates SCHEDULE, MENU and CHARACTERIZATION on
-- school code PRIMARY. A login in the basic school is a perfectly good parent
-- login that cannot see three of the five boxes and is refused by their
-- endpoints, which looks like a bug the first time somebody meets it.
--
-- Run after 100_roster.sql and 020_content_modules.sql. Idempotent.

SET
XACT_ABORT ON;
SET
NOCOUNT ON;
SET
QUOTED_IDENTIFIER ON;
SET
ANSI_NULLS ON;
GO

BEGIN
TRANSACTION;

DECLARE
@year bigint = (SELECT TOP 1 id FROM sgs.academic_year ORDER BY is_current DESC, id);
DECLARE
@c3a bigint = (SELECT TOP 1 cg.id FROM sgs.class_group cg
                       JOIN sgs.school s ON s.id = cg.school_id
                       WHERE s.code = N'PRIMARY' AND cg.academic_year_id = @year
                       ORDER BY cg.id);
DECLARE
@now datetime2 = SYSUTCDATETIME();

IF
@c3a IS NULL
    THROW 50000, 'No PRIMARY class found - run db/demo/100_roster.sql first', 1;

-- ---- 1. a test login -----------------------------------------------------
--
-- Named so nobody mistakes it for one of the school's own: the four children in
-- 100_roster.sql have plausible Georgian names because they exercise the
-- duplicate-username rule, and a fifth plausible name would just be a fifth
-- thing to keep straight.
--
-- password_hash is unsalted uppercase MD5 - what the live system stores, and
-- what ParentAuthController hashes against. This is MD5('test123').

INSERT INTO sgs.student (id, username, password_hash, first_name, last_name,
                         personal_number, guardian_email, is_active, created_at, updated_at)
SELECT NEXT VALUE FOR sgs.student_seq, N'testparent', N'CC03E747A6AFBBCBF8BE7668ACFEBEE5', N'ტესტ', N'მოსწავლე', N'01001000099', N'test.parent@example.invalid', 1, @now, @now
WHERE NOT EXISTS (SELECT 1 FROM sgs.student s WHERE s.personal_number = N'01001000099');

DECLARE
@testStudent bigint =
    (SELECT id FROM sgs.student WHERE personal_number = N'01001000099');

INSERT INTO sgs.enrollment (id, student_id, class_group_id, academic_year_id, joined_on, left_on)
SELECT NEXT VALUE FOR sgs.enrollment_seq, @testStudent, @c3a, @year, (SELECT starts_on FROM sgs.academic_year WHERE id = @year), NULL
WHERE NOT EXISTS (SELECT 1 FROM sgs.enrollment e
    WHERE e.student_id = @testStudent
  AND e.academic_year_id = @year);

-- The open placement EnrollmentService would have written. Without it the child
-- has no current class as far as the placement history is concerned, and
-- db/031's uq_open_placement is what keeps it to one.
INSERT INTO sgs.enrollment_placement (id, enrollment_id, class_group_id, from_date, to_date,
                                      created_at, updated_at)
SELECT NEXT VALUE FOR sgs.enrollment_placement_seq, e.id, e.class_group_id, e.joined_on, NULL, @now, @now
FROM sgs.enrollment e
WHERE e.student_id = @testStudent
  AND NOT EXISTS (SELECT 1 FROM sgs.enrollment_placement p WHERE p.enrollment_id = e.id);

-- ---- 2. the standing documents -------------------------------------------
--
-- One SCHEDULE and one MENU per class, which is what the school confirmed and
-- what db/020's comment records: entered once for the year, no week key, no
-- versions. Created only when the class has none, so a database somebody has
-- already typed a real week into is left alone.

DECLARE
@schedule bigint = (SELECT TOP 1 id FROM sgs.post
                            WHERE kind = 'SCHEDULE' AND class_group_id = @c3a AND is_archived = 0);
DECLARE
@menu bigint = (SELECT TOP 1 id FROM sgs.post
                        WHERE kind = 'MENU' AND class_group_id = @c3a AND is_archived = 0);

IF
@schedule IS NULL
BEGIN
    DECLARE
@scheduleId bigint = NEXT VALUE FOR sgs.post_seq;

INSERT INTO sgs.post (id, uuid, kind, status, title, class_group_id,
                      is_archived, has_unpublished_changes, created_at, updated_at)
VALUES (@scheduleId, LOWER(CONVERT(nvarchar(36), NEWID())),
        'SCHEDULE', 'DRAFT', N'დღის რეჟიმი', @c3a, 0, 1, @now, @now);

-- weekday 1 = Monday to 5 = Friday, as ISO numbers them and as db/020 says.
-- time_text is free text on purpose: the brief asks for a hand-typed time,
-- and parsing it would only create a way to reject what was meant.
INSERT INTO sgs.post_line (id, post_id, weekday, ordinal, time_text, [text])
SELECT NEXT VALUE FOR sgs.post_line_seq, @scheduleId, v.weekday, v.ordinal, v.t, v.txt
FROM (VALUES
    (1, 0, N'09:00', N'ქართული ენა'), (1, 1, N'10:00', N'მათემატიკა'), (1, 2, N'11:00', N'ინგლისური ენა'), (1, 3, N'12:00', N'სპორტი'), (2, 0, N'09:00', N'მათემატიკა'), (2, 1, N'10:00', N'ბუნებისმეტყველება'), (2, 2, N'11:00', N'ქართული ენა'), (3, 0, N'09:00', N'ქართული ენა'), (3, 1, N'10:00', N'ხატვა'), (3, 2, N'11:00', N'მუსიკა'), (4, 0, N'09:00', N'მათემატიკა'), (4, 1, N'10:00', N'ინგლისური ენა'), (4, 2, N'11:00', N'ბუნებისმეტყველება'), (5, 0, N'09:00', N'სპორტი'), (5, 1, N'10:00', N'ქართული ენა')
    ) v(weekday, ordinal, t, txt);
END

IF
@menu IS NULL
BEGIN
    DECLARE
@menuId bigint = NEXT VALUE FOR sgs.post_seq;

INSERT INTO sgs.post (id, uuid, kind, status, title, class_group_id,
                      is_archived, has_unpublished_changes, created_at, updated_at)
VALUES (@menuId, LOWER(CONVERT(nvarchar(36), NEWID())),
        'MENU', 'DRAFT', N'კვება', @c3a, 0, 1, @now, @now);

-- No time_text: a menu is breakfast then lunch then snack, in order, and
-- the school does not put clock times on it.
INSERT INTO sgs.post_line (id, post_id, weekday, ordinal, time_text, [text])
SELECT NEXT VALUE FOR sgs.post_line_seq, @menuId, v.weekday, v.ordinal, NULL, v.txt
FROM (VALUES
    (1, 0, N'ფაფა, ხილი, რძე'), (1, 1, N'სუპი, კატლეტი, პიური'), (2, 0, N'ომლეტი, ყველი'), (2, 1, N'ბორში, მაკარონი'), (2, 2, N'ხილის სალათი'), (3, 0, N'ფაფა, თაფლი'), (3, 1, N'ხარჩო, პური'), (4, 0, N'ხაჭო, ხილი'), (4, 1, N'ქათმის სუპი, ბრინჯი'), (5, 0, N'ომლეტი, პური'), (5, 1, N'ლობიო, მწვანილი')
    ) v(weekday, ordinal, txt);
END

-- ---- 3. rows for a snapshot that has none --------------------------------
--
-- The repair described at the top: a published standing document whose lines
-- exist only inside its JSON. Reads the snapshot back into post_line so the
-- editor shows what the parent already sees, and so the next publish does not
-- replace a full week with an empty one.
--
-- OPENJSON rather than string surgery - the payload is JSON that PostService
-- wrote with Jackson, and the parent reader parses it the same way.

INSERT INTO sgs.post_line (id, post_id, weekday, ordinal, time_text, [text])
SELECT NEXT VALUE FOR sgs.post_line_seq, p.id, j.weekday, j.ordinal, j.timeText, j.[text]
FROM sgs.post p
    CROSS APPLY OPENJSON(p.published_payload, '$.lines')
WITH (weekday int '$.weekday', ordinal int '$.ordinal', timeText nvarchar(64) '$.timeText', [text] nvarchar(1024) '$.text') j
WHERE p.kind IN ('SCHEDULE'
    , 'MENU')
  AND p.published_payload IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sgs.post_line l WHERE l.post_id = p.id);

COMMIT TRANSACTION;
GO

-- ---- 4. publish them ------------------------------------------------------
--
-- The snapshot rebuilt from the rows, in the shape PostService.snapshot writes.
-- The parent reader takes title and lines from it and ignores the rest, but the
-- rest is written anyway so that a seeded post and a published one are the same
-- thing on disk.
--
-- INCLUDE_NULL_VALUES because time_text is null on every menu row: the reader
-- asks for the key and treats an empty one as "no time", which is not the same
-- as a key that is not there.
--
-- STRING_ESCAPE on the title rather than FOR JSON on the whole object, because
-- FOR JSON would name the columns and the reader wants a bare object.

UPDATE p
SET status                  = 'PUBLISHED',
    has_unpublished_changes = 0,
    published_at            = COALESCE(p.published_at, SYSUTCDATETIME()),
    updated_at              = SYSUTCDATETIME(),
    published_payload       =
        N'{"title":"' + STRING_ESCAPE(p.title, 'json')
            + N'","bodyHtml":null,"eventDate":null,"subjectId":null,'
            + N'"targets":[],"links":[],"lines":'
            + ISNULL((SELECT l.weekday, l.ordinal, l.time_text AS timeText, l.[text]
                      FROM sgs.post_line l
                      WHERE l.post_id = p.id
                      ORDER BY l.weekday, l.ordinal
                         FOR JSON PATH, INCLUDE_NULL_VALUES), N'[]')
            + N',"category":null,"imageUuid":null}' FROM sgs.post p
WHERE p.kind IN ('SCHEDULE'
    , 'MENU')
  AND p.is_archived = 0
  AND EXISTS (SELECT 1 FROM sgs.post_line l WHERE l.post_id = p.id);
GO

-- ---- 5. news --------------------------------------------------------------
--
-- For every class: no rows in sgs.post_audience (db/041), so every parent sees
-- all of it whatever school their child is in. The category is a label to
-- filter on rather than a visibility rule.
--
-- Matched on title rather than uuid, because a uuid generated here would never
-- match one generated on a previous run and the point is to not double them.

DECLARE
@now2 datetime2 = SYSUTCDATETIME();

INSERT INTO sgs.post_category (id, uuid, name, is_archived, created_at, updated_at)
SELECT NEXT VALUE FOR sgs.post_category_seq, LOWER (CONVERT (nvarchar(36), NEWID())), v.name, 0, @now2, @now2
FROM (VALUES (N'ღონისძიებები'), (N'განცხადებები'), (N'სასკოლო ცხოვრება')) v(name)
WHERE NOT EXISTS (SELECT 1 FROM sgs.post_category c WHERE c.name = v.name);

INSERT INTO sgs.post (id, uuid, kind, status, title, body_html, event_date, category_id,
                      is_archived, has_unpublished_changes, published_at, published_payload,
                      created_at, updated_at)
SELECT NEXT VALUE FOR sgs.post_seq, LOWER (CONVERT (nvarchar(36), NEWID())), 'NEWS', 'PUBLISHED', v.title, v.body, v.event_date, c.id, 0, 0, @now2, N'{"title":"' + STRING_ESCAPE(v.title, 'json')
    + N'","bodyHtml":"' + STRING_ESCAPE(v.body, 'json')
    + N'","eventDate":"' + CONVERT (nvarchar(10), v.event_date, 23)
    + N'","subjectId":null,"targets":[],"links":[],"lines":[],"category":"'
    + STRING_ESCAPE(v.category, 'json') + N'","imageUuid":null}', @now2, @now2
FROM (VALUES
    (N'ღია კარის დღე დაწყებით სკოლაში', N'<p>ოცდახუთ სექტემბერს დაწყებითი სკოლა მასპინძლობს ღია კარის დღეს. მშობლებს შეეძლებათ დაესწრონ გაკვეთილებს და შეხვდნენ პედაგოგებს.</p>', CAST ('2026-09-25' AS date), N'ღონისძიებები'), (N'სასადილოს ახალი მენიუ', N'<p>ოქტომბრიდან სასადილოში შემოდის განახლებული სეზონური მენიუ. კვირის სრული მენიუ ხელმისაწვდომია მშობლის პორტალზე.</p>', CAST ('2026-09-18' AS date), N'განცხადებები'), (N'შემოდგომის ექსკურსია მცხეთაში', N'<p>მესამე კლასები ექსკურსიაზე მიემგზავრებიან მცხეთაში. თანხმობის ფორმა გთხოვთ დააბრუნოთ პარასკევამდე.</p>', CAST ('2026-09-16' AS date), N'სასკოლო ცხოვრება'), (N'საშემოდგომო წიგნის კვირეული', N'<p>ბიბლიოთეკა აცხადებს წიგნის კვირეულს. ყოველდღე თერთმეტ საათზე გაიმართება ხმამაღალი კითხვის სესია.</p>', CAST ('2026-09-14' AS date), N'სასკოლო ცხოვრება')
    ) v(title, body, event_date, category)
    JOIN sgs.post_category c
ON c.name = v.category
WHERE NOT EXISTS (SELECT 1 FROM sgs.post p WHERE p.kind = 'NEWS' AND p.title = v.title);
GO

-- ---- what came out of it --------------------------------------------------

SELECT 'parent logins' AS entity, COUNT(*) AS n
FROM sgs.student
WHERE is_active = 1
UNION ALL
SELECT 'schedule lines', COUNT(*)
FROM sgs.post_line l
         JOIN sgs.post p ON p.id = l.post_id AND p.kind = 'SCHEDULE'
UNION ALL
SELECT 'menu lines', COUNT(*)
FROM sgs.post_line l
         JOIN sgs.post p ON p.id = l.post_id AND p.kind = 'MENU'
UNION ALL
SELECT 'news published', COUNT(*)
FROM sgs.post
WHERE kind = 'NEWS'
  AND status = 'PUBLISHED'
  AND is_archived = 0;
GO
