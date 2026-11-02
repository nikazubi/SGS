-- News for particular classes.
--
-- News was school-wide: every parent saw every item, which the school confirmed
-- when the module was built. They then asked for news aimed at one class or a
-- few. sgs.post_audience lists the classes an item is for, one row each. An item
-- with no rows is for every class, so every item written before this keeps
-- reaching everyone and nothing existing needs touching.
--
-- Classes rather than children: a child who joins the class later sees its
-- news. Classes are per academic year, so an item aimed at a class stops
-- reaching those children when the year rolls over - agreed with the school.
--
-- Idempotent. Run after 001_schema.sql has created sgs.post and
-- sgs.class_group. Deploying the backend first is harmless too: ddl-auto: update
-- creates the same table, and each step below checks before it writes.

SET
XACT_ABORT ON;
SET
NOCOUNT ON;
SET
QUOTED_IDENTIFIER ON;
SET
ANSI_NULLS ON;
GO

-- 001_schema.sql already has the table on a fresh install, generated from the
-- entity.
IF OBJECT_ID('sgs.post_audience') IS NULL
CREATE TABLE sgs.post_audience
(
    post_id        bigint NOT NULL,
    class_group_id bigint NOT NULL,
    CONSTRAINT pk_post_audience PRIMARY KEY (post_id, class_group_id)
);
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_post_audience_post')
ALTER TABLE sgs.post_audience
    ADD CONSTRAINT fk_post_audience_post
        FOREIGN KEY (post_id) REFERENCES sgs.post (id);
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_post_audience_class')
ALTER TABLE sgs.post_audience
    ADD CONSTRAINT fk_post_audience_class
        FOREIGN KEY (class_group_id) REFERENCES sgs.class_group (id);
GO

-- A parent's news list asks "is my class among them?"; the primary key leads
-- with the post, so the class needs its own.
IF NOT EXISTS (SELECT 1 FROM sys.indexes
               WHERE name = 'ix_post_audience_class'
                 AND object_id = OBJECT_ID('sgs.post_audience'))
CREATE INDEX ix_post_audience_class ON sgs.post_audience (class_group_id);
GO

SELECT (SELECT COUNT(*) FROM sgs.post WHERE kind = 'NEWS')     AS news_items,
       (SELECT COUNT(DISTINCT post_id) FROM sgs.post_audience) AS aimed_at_classes;
GO
