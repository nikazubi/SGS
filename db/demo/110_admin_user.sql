-- One administrator, so there is somebody to log in as.
--
-- The staff login is still the legacy one: SystemUser in dbo.system_user_table,
-- authorities from the comma-separated permission string on its group. None of
-- that has been rewritten yet, so this seeds dbo rather than sgs - the two live
-- side by side until cutover.
--
-- THE PASSWORD IS STORED IN PLAIN TEXT, and that is not this script's doing.
-- SecurityConfiguration registers NoOpPasswordEncoder, so the column holds
-- exactly what is typed at the login box. (Student passwords are different -
-- those are unsalted MD5, see db/demo/100_roster.sql.) Both are on the list to
-- replace: FOLLOW-UPS.md section 2.
--
-- ---------------------------------------------------------------------------
-- WHY THIS CREATES TABLES. The legacy tables are not in db/001_schema.sql -
-- that file is generated from the *new* entities only. On a fresh database the
-- legacy ones appear when the application first boots, because ddl-auto is
-- `update`. That leaves a chicken and egg: no tables to seed until the app has
-- run, and no way to log in until they are seeded.
--
-- So the tables the login path needs are created here if they are missing, in
-- the shape Hibernate makes them TODAY: bigint ids, datetime2 timestamps,
-- nvarchar for every string, everything nullable. `update` never drops or
-- alters, so when the application starts it finds them and leaves them alone.
--
-- The types matter and were got wrong once. The school's own database holds
-- numeric(19,0) and varchar in these columns, because it was created years ago
-- under an older dialect; copying that shape here left Hibernate's own
-- `students` table, which it makes with a bigint key, unable to take a foreign
-- key to this `academy_class` - six constraints failed at every startup. What
-- belongs here is what the current mapping emits, not what the old database
-- happens to hold.
--
-- IDS START AT 900001. hibernate_sequence is left entirely to Hibernate, which
-- creates it starting at 1 and hands out ids in blocks of 50. Nothing it
-- generates will reach 900001 in the life of a test database, so seeded rows
-- and created ones cannot collide.
-- ---------------------------------------------------------------------------

SET
XACT_ABORT ON;
SET
NOCOUNT ON;
SET
QUOTED_IDENTIFIER ON;
SET
ANSI_NULLS ON;
GO

IF OBJECT_ID('dbo.system_groups') IS NULL
CREATE TABLE dbo.system_groups
(
    id               bigint NOT NULL
        CONSTRAINT pk_system_groups PRIMARY KEY,
    create_time      datetime2 NULL,
    last_update_time datetime2 NULL,
    active           bit NULL,
    name             nvarchar(255) NULL,
    -- 500, because it holds every permission name joined by commas.
    permissions      nvarchar(500) NULL
);
GO

IF OBJECT_ID('dbo.system_user_table') IS NULL
CREATE TABLE dbo.system_user_table
(
    id               bigint NOT NULL
        CONSTRAINT pk_system_user_table PRIMARY KEY,
    create_time      datetime2 NULL,
    last_update_time datetime2 NULL,
    active           bit NULL,
    email            nvarchar(255) NULL,
    name             nvarchar(255) NULL,
    password         nvarchar(255) NULL,
    username         nvarchar(255) NULL
);
GO

IF OBJECT_ID('dbo.system_user_table_groups') IS NULL
CREATE TABLE dbo.system_user_table_groups
(
    system_user_table_id bigint NOT NULL,
    groups_id            bigint NOT NULL
);
GO

-- The legacy class list, which is how a staff account is narrowed to particular
-- classes - see ClassScopeGuard, which matches these names against
-- sgs.class_group. Created and seeded with the same two class names as the new
-- roster so that scoping can be tried; the administrator below is deliberately
-- attached to neither, because an empty list means unrestricted.

IF OBJECT_ID('dbo.academy_class') IS NULL
CREATE TABLE dbo.academy_class
(
    id               bigint NOT NULL
        CONSTRAINT pk_academy_class PRIMARY KEY,
    create_time      datetime2 NULL,
    last_update_time datetime2 NULL,
    class_level      bigint NULL,
    class_name       nvarchar(255) NULL,
    is_transit       bit NULL
);
GO

IF OBJECT_ID('dbo.system_user_table_academy_class_list') IS NULL
CREATE TABLE dbo.system_user_table_academy_class_list
(
    system_user_table_id  bigint NOT NULL,
    academy_class_list_id bigint NOT NULL
);
GO

-- ---- the group -----------------------------------------------------------
--
-- Every permission the server defines, which is the whole of AuthConstants.
-- Named individually rather than selected from anywhere: there is no table of
-- permissions, only this string, so adding one to AuthConstants means adding it
-- here too.

IF NOT EXISTS (SELECT 1 FROM dbo.system_groups WHERE id = 900001)
INSERT INTO dbo.system_groups (id, create_time, last_update_time, active, name, permissions)
VALUES (900001, SYSUTCDATETIME(), SYSUTCDATETIME(), 1, N'ადმინისტრატორი',
        N'MANAGE_STUDENT,VIEW_STUDENT,MANAGE_ACADEMY_CLASS,VIEW_ACADEMY_CLASS,'
      + N'MANAGE_SUBJECT,VIEW_SUBJECT,ADD_GRADES,MANAGE_GRADES,MANAGE_SYSTEM_USER,'
      + N'MANAGE_CHANGE_REQUESTS,VIEW_CHANGE_REQUESTS,MANAGE_CLOSED_PERIOD,'
      + N'MANAGE_TEMPLATES,VIEW_SYSTEM_USER_GROUP,MANAGE_TOTAL_ABSENCE,'
      + N'MANAGE_HOMEWORK,MANAGE_SCHEDULE,MANAGE_MENU,MANAGE_CHARACTERIZATION,'
      + N'MANAGE_NEWS');
GO

-- ---- the user ------------------------------------------------------------
--
-- active must be 1 and the group list must not be empty: UserDetailsImpl reads
-- getActive() into a boolean and loops over getGroups() without a null check,
-- so either being absent is a 500 at the login box rather than a refusal.

IF NOT EXISTS (SELECT 1 FROM dbo.system_user_table WHERE username = N'admin')
INSERT INTO dbo.system_user_table (id, create_time, last_update_time, active, email, name,
                                   password, username)
VALUES (900001, SYSUTCDATETIME(), SYSUTCDATETIME(), 1, N'admin@example.invalid',
        N'ადმინისტრატორი', N'admin', N'admin');
GO

IF NOT EXISTS (SELECT 1 FROM dbo.system_user_table_groups
               WHERE system_user_table_id = 900001 AND groups_id = 900001)
INSERT INTO dbo.system_user_table_groups (system_user_table_id, groups_id)
VALUES (900001, 900001);
GO

-- ---- the legacy class rows -----------------------------------------------
--
-- class_level is the school (1 primary, 2 basic, 3 secondary), which is what
-- the legacy column actually held - not the grade.

INSERT INTO dbo.academy_class (id, create_time, last_update_time, class_level, class_name, is_transit)
SELECT v.id, SYSUTCDATETIME(), SYSUTCDATETIME(), v.class_level, v.class_name, 0
FROM (VALUES (900001, 1, N'3ა'), (900002, 2, N'8ბ')) v(id, class_level, class_name)
WHERE NOT EXISTS (SELECT 1 FROM dbo.academy_class a WHERE a.id = v.id);
GO

SELECT u.username, u.active, g.name AS [
group], g.permissions
FROM dbo.system_user_table u
    JOIN dbo.system_user_table_groups ug
ON ug.system_user_table_id = u.id
    JOIN dbo.system_groups g ON g.id = ug.groups_id
WHERE u.username = N'admin';
GO
