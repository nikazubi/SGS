-- The release log names the journal it released.
--
-- sgs.publication recorded the class, the period and the subject of a release,
-- but not which journal: every release went through one journal's own button,
-- so the screen it was pressed on said it. Once one action can release several
-- journals of a class at once - the history page's bulk release - a log of
-- "8ბ, I ტრიმესტრი, 48" cannot say whether those were grades or hours.
--
-- Nullable. Rows written before this have no journal and are left that way:
-- nothing recorded which one they were, and guessing one from the cells would
-- be wrong whenever two journals shared a period. They show as "—".
--
-- Idempotent. Run after 009_publication.sql.

SET
XACT_ABORT ON;
SET
NOCOUNT ON;
SET
QUOTED_IDENTIFIER ON;
SET
ANSI_NULLS ON;
GO

-- 001_schema.sql already has the column on a fresh install, generated from the
-- entity.
IF COL_LENGTH('sgs.publication', 'template_id') IS NULL
ALTER TABLE sgs.publication
    ADD template_id bigint NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_publication_template')
ALTER TABLE sgs.publication
    ADD CONSTRAINT fk_publication_template
        FOREIGN KEY (template_id) REFERENCES sgs.grading_template (id);
GO

SELECT COUNT(*)                                             AS releases,
       SUM(CASE WHEN template_id IS NULL THEN 1 ELSE 0 END) AS without_journal
FROM sgs.publication;
GO
