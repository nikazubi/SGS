-- DESTRUCTIVE. Deletes every class, and everything that only exists because a
-- child was in a class, so the school can create its classes again through the
-- roster screen.
--
-- WHY. The classes on the database came from db/006, a one-off copy of
-- dbo.academy_class matched to schools by name and grade. The school decided to
-- start the class list clean in sgs.class_group - the one class table the
-- system reads - rather than carry the migrated rows forward.
--
-- DELETED, all years:
--   classes              class_group, class_subject (and teacher names),
--                        teaching_assignment, class_period_setting (permitted
--                        absence hours), template_assignment (a class with no
--                        assignment falls back to the journal's active version),
--                        publication, staff_class_grant
--   enrollments          enrollment, enrollment_placement - every child's place
--                        in a class. Children are placed again on the Students
--                        screen.
--   per-child records    grade_entry, grade_change_request, daily_absence,
--                        absence_notice, homework_seen
--   class content        every post aimed at a class (homework, schedule, menu,
--                        characterization) with its lines, links and targets.
--                        School-wide posts - news with no class - stay.
--
-- KEPT: students, subjects, academic years, period schemes and periods,
-- journals and their columns, schools, staff accounts, school-wide news,
-- post images, and everything in dbo.
--
-- TAKE A BACKUP FIRST. Nothing here can be undone.
--
-- Runs as one transaction: it deletes everything or nothing. Refuses to run
-- unless @confirm below is edited to the exact phrase.

SET
XACT_ABORT ON;
SET
NOCOUNT ON;
SET
QUOTED_IDENTIFIER ON;
SET
ANSI_NULLS ON;
GO

DECLARE @confirm nvarchar(64) = N'change me';

IF @confirm <> N'DELETE ALL CLASSES'
BEGIN
    RAISERROR ('Refusing to run: set @confirm to ''DELETE ALL CLASSES'' after taking a backup.', 16, 1);
    RETURN;
END;

IF OBJECT_ID('sgs.staff_class_grant') IS NULL
BEGIN
    RAISERROR ('Run db/036_staff_class_grant.sql first.', 16, 1);
    RETURN;
END;

-- What is about to go, so the output of the run is its own record.
SELECT 'class_group' AS entity, COUNT(*) AS rows_before FROM sgs.class_group
UNION ALL SELECT 'enrollment', COUNT(*) FROM sgs.enrollment
UNION ALL SELECT 'grade_entry', COUNT(*) FROM sgs.grade_entry
UNION ALL SELECT 'daily_absence', COUNT(*) FROM sgs.daily_absence
UNION ALL SELECT 'class posts', COUNT(*) FROM sgs.post WHERE class_group_id IS NOT NULL
UNION ALL SELECT 'student (kept)', COUNT(*) FROM sgs.student
UNION ALL SELECT 'subject (kept)', COUNT(*) FROM sgs.subject;

BEGIN TRANSACTION;

-- Children of each child's records, leaves first.
DELETE FROM sgs.grade_change_request;
DELETE FROM sgs.grade_entry;
DELETE FROM sgs.daily_absence;
DELETE FROM sgs.absence_notice;
DELETE FROM sgs.homework_seen;
DELETE FROM sgs.post_target;
DELETE FROM sgs.enrollment_placement;

-- Class content. A post aimed at a class means nothing without it.
DELETE FROM sgs.post_line WHERE post_id IN (SELECT id FROM sgs.post WHERE class_group_id IS NOT NULL);
DELETE FROM sgs.post_link WHERE post_id IN (SELECT id FROM sgs.post WHERE class_group_id IS NOT NULL);
DELETE FROM sgs.post WHERE class_group_id IS NOT NULL;

DELETE FROM sgs.enrollment;

-- The class itself and everything configured on it.
DELETE FROM sgs.teaching_assignment;
DELETE FROM sgs.class_subject;
DELETE FROM sgs.class_period_setting;
DELETE FROM sgs.template_assignment;
DELETE FROM sgs.publication;
DELETE FROM sgs.staff_class_grant;
DELETE FROM sgs.class_group;

COMMIT TRANSACTION;

SELECT 'class_group' AS entity, COUNT(*) AS rows_after FROM sgs.class_group
UNION ALL SELECT 'enrollment', COUNT(*) FROM sgs.enrollment
UNION ALL SELECT 'grade_entry', COUNT(*) FROM sgs.grade_entry
UNION ALL SELECT 'student (kept)', COUNT(*) FROM sgs.student
UNION ALL SELECT 'subject (kept)', COUNT(*) FROM sgs.subject;
GO
