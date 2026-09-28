-- Which classes a member of staff is limited to, by class group.
--
-- Until now the scope lived in dbo.system_user_table_academy_class_list, which
-- points at dbo.academy_class. The roster screens write sgs.class_group and
-- never touch dbo, so a class created through the console could not be chosen
-- on the user form at all, and ClassScopeGuard had to translate every legacy
-- grant into a class group by name.
--
-- This table names the class group itself. No rows for a user still means
-- unrestricted - the grant is a narrowing, not a requirement.
--
-- No backfill: db/037 deletes every class and they are created again through
-- the roster screen, so there is nothing to carry over. Classes are granted on
-- the user form once they exist. The dbo tables are left in place as data;
-- no code reads them any more.
--
-- Idempotent. Run after 001_schema.sql and before 037.

SET
XACT_ABORT ON;
SET
NOCOUNT ON;
SET
QUOTED_IDENTIFIER ON;
SET
ANSI_NULLS ON;
GO

IF NOT EXISTS (SELECT 1 FROM sys.sequences q
               JOIN sys.schemas c ON c.schema_id = q.schema_id
               WHERE q.name = 'staff_class_grant_seq' AND c.name = 'sgs')
CREATE SEQUENCE sgs.staff_class_grant_seq AS bigint START WITH 1 INCREMENT BY 50;
GO

IF OBJECT_ID('sgs.staff_class_grant') IS NULL
CREATE TABLE sgs.staff_class_grant
(
    id             bigint NOT NULL
        CONSTRAINT pk_staff_class_grant PRIMARY KEY,
    system_user_id bigint NOT NULL,
    class_group_id bigint NOT NULL,
    CONSTRAINT uq_staff_class_grant UNIQUE (system_user_id, class_group_id),
    CONSTRAINT fk_staff_grant_class FOREIGN KEY (class_group_id)
        REFERENCES sgs.class_group
);
GO

-- system_user_id has to be the same type as dbo.system_user_table.id for the
-- foreign key below, and that differs by database: numeric(19,0) on the
-- school's server (an old Hibernate default), bigint on the demo one. Match
-- whatever is there. The unique constraint covers the column, so it is
-- dropped and recreated around the change.
DECLARE @type nvarchar(64) = (
    SELECT CASE WHEN t.name IN ('numeric', 'decimal')
                THEN t.name + '(' + CAST(c.precision AS nvarchar) + ',' + CAST(c.scale AS nvarchar) + ')'
                ELSE t.name END
    FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
    WHERE c.object_id = OBJECT_ID('dbo.system_user_table') AND c.name = 'id');
DECLARE @current nvarchar(64) = (
    SELECT CASE WHEN t.name IN ('numeric', 'decimal')
                THEN t.name + '(' + CAST(c.precision AS nvarchar) + ',' + CAST(c.scale AS nvarchar) + ')'
                ELSE t.name END
    FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id
    WHERE c.object_id = OBJECT_ID('sgs.staff_class_grant') AND c.name = 'system_user_id');

IF @type <> @current
BEGIN
    IF EXISTS (SELECT 1 FROM sys.objects WHERE name = 'uq_staff_class_grant')
        ALTER TABLE sgs.staff_class_grant DROP CONSTRAINT uq_staff_class_grant;
    EXEC ('ALTER TABLE sgs.staff_class_grant ALTER COLUMN system_user_id ' + @type + ' NOT NULL');
END;

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE name = 'uq_staff_class_grant')
ALTER TABLE sgs.staff_class_grant
    ADD CONSTRAINT uq_staff_class_grant UNIQUE (system_user_id, class_group_id);
GO

-- Separate from the table because 001_schema.sql also creates it, from the
-- entity, and cannot know about dbo. Cascades so deleting a user needs no
-- knowledge of this table: the user delete path is plain JPA on the dbo entity.
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_staff_grant_user')
ALTER TABLE sgs.staff_class_grant
    ADD CONSTRAINT fk_staff_grant_user FOREIGN KEY (system_user_id)
        REFERENCES dbo.system_user_table (id) ON DELETE CASCADE;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_staff_class_grant_class')
CREATE INDEX ix_staff_class_grant_class
    ON sgs.staff_class_grant (class_group_id);
GO
