-- Journals as data.
--
-- TemplateScope was the three legacy journals (ACADEMIC / ETHICS / ABSENCE)
-- written into a Java enum, and TemplateVersionResolver hardcoded ACADEMIC - so
-- the machinery served academic grades only and the other two could never have
-- run on it. The school has run many journals over the years and changes them
-- often, so a journal becomes a row it can create, name and see in the menu.
--
-- grading_template gains:
--   uuid            stable external key. The name is the menu label and is
--                   free to change; nothing may reference it.
--   frequency       how often it is filled in. ONCE_A_YEAR behaves like a
--                   plain table - one grid, no period dropdown anywhere.
--   subject_scoped  one grid per subject, or one for the whole class.
--   sort_index      menu order.
--   is_archived     removed from the menu without being deleted: grades point
--                   at a journal, so deleting one takes its history with it.
--
-- template_assignment is rekeyed from scope to the journal, so a class can keep
-- one journal on an older version while another moves on.

SET
XACT_ABORT ON;
SET
NOCOUNT ON;

-- ---- grading_template ----------------------------------------------------

IF
NOT EXISTS (SELECT 1 FROM sys.columns
               WHERE object_id = OBJECT_ID('sgs.grading_template') AND name = 'uuid')
ALTER TABLE sgs.grading_template
    ADD uuid nvarchar(36) NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns
               WHERE object_id = OBJECT_ID('sgs.grading_template') AND name = 'frequency')
ALTER TABLE sgs.grading_template
    ADD
        frequency varchar(16) NULL,
        subject_scoped bit         NULL,
        sort_index     int         NULL,
        is_archived    bit         NULL;
GO

-- Existing rows predate all of this. The one seeded journal is the academic
-- one: per subject, per trimester, first in the menu.
UPDATE sgs.grading_template
SET uuid           = ISNULL(uuid, LOWER(CONVERT(nvarchar(36), NEWID()))),
    frequency      = ISNULL(frequency, 'TRIMESTER'),
    subject_scoped = ISNULL(subject_scoped, 1),
    sort_index     = ISNULL(sort_index, 0),
    is_archived    = ISNULL(is_archived, 0);
GO

ALTER TABLE sgs.grading_template ALTER COLUMN uuid nvarchar(36) NOT NULL;
ALTER TABLE sgs.grading_template ALTER COLUMN frequency varchar(16) NOT NULL;
ALTER TABLE sgs.grading_template ALTER COLUMN subject_scoped bit NOT NULL;
ALTER TABLE sgs.grading_template ALTER COLUMN sort_index int NOT NULL;
ALTER TABLE sgs.grading_template ALTER COLUMN is_archived bit NOT NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = 'uq_template_uuid')
ALTER TABLE sgs.grading_template
    ADD CONSTRAINT uq_template_uuid UNIQUE (uuid);
GO

-- scope is superseded by the journal itself.
IF EXISTS (SELECT 1 FROM sys.columns
           WHERE object_id = OBJECT_ID('sgs.grading_template') AND name = 'scope')
ALTER TABLE sgs.grading_template DROP COLUMN scope;
GO

-- ---- template_assignment -------------------------------------------------

IF NOT EXISTS (SELECT 1 FROM sys.columns
               WHERE object_id = OBJECT_ID('sgs.template_assignment') AND name = 'template_id')
ALTER TABLE sgs.template_assignment
    ADD template_id bigint NULL;
GO

-- Denormalised from the version it points at.
UPDATE ta
SET template_id = tv.template_id FROM sgs.template_assignment ta
JOIN sgs.template_version tv
ON tv.id = ta.template_version_id
WHERE ta.template_id IS NULL;
GO

ALTER TABLE sgs.template_assignment ALTER COLUMN template_id bigint NOT NULL;
GO

IF EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = 'uq_assignment_class_subject_scope')
ALTER TABLE sgs.template_assignment DROP CONSTRAINT uq_assignment_class_subject_scope;
IF
EXISTS (SELECT 1 FROM sys.columns
           WHERE object_id = OBJECT_ID('sgs.template_assignment') AND name = 'scope')
ALTER TABLE sgs.template_assignment DROP COLUMN scope;
GO

IF NOT EXISTS (SELECT 1 FROM sys.key_constraints
               WHERE name = 'uq_assignment_class_subject_journal')
ALTER TABLE sgs.template_assignment
    ADD CONSTRAINT uq_assignment_class_subject_journal
        UNIQUE (class_group_id, subject_id, template_id);
IF
NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_assignment_template')
ALTER TABLE sgs.template_assignment
    ADD CONSTRAINT fk_assignment_template
        FOREIGN KEY (template_id) REFERENCES sgs.grading_template;
GO

SELECT 'journals' AS entity, COUNT(*) AS n
FROM sgs.grading_template
UNION ALL
SELECT 'assignments_keyed', COUNT(*)
FROM sgs.template_assignment
WHERE template_id IS NOT NULL;
GO
