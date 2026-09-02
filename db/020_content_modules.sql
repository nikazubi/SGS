-- The other four content modules: schedule, menu, characterization and news.
--
-- All on the phase 8 `post` table. Only news brings new structure - a picture
-- and a category - and only schedule and menu bring `post_line`, the weekday
-- rows of a standing document.
--
-- The school confirmed schedule and menu are entered once for the year and
-- adjusted occasionally, so there is exactly one of each per class: no week key,
-- no period, no versions.

SET
XACT_ABORT ON;
SET
NOCOUNT ON;
SET
QUOTED_IDENTIFIER ON;
SET
ANSI_NULLS ON;

-- ---- sequences -----------------------------------------------------------

IF
NOT EXISTS (SELECT 1 FROM sys.sequences WHERE name = 'post_line_seq')
CREATE SEQUENCE sgs.post_line_seq AS bigint START WITH 1 INCREMENT BY 50;
GO
IF NOT EXISTS (SELECT 1 FROM sys.sequences WHERE name = 'post_category_seq')
CREATE SEQUENCE sgs.post_category_seq AS bigint START WITH 1 INCREMENT BY 50;
GO
IF NOT EXISTS (SELECT 1 FROM sys.sequences WHERE name = 'post_image_seq')
CREATE SEQUENCE sgs.post_image_seq AS bigint START WITH 1 INCREMENT BY 50;
GO

-- ---- schedule and menu rows ----------------------------------------------

IF OBJECT_ID('sgs.post_line') IS NULL
CREATE TABLE sgs.post_line
(
    id        bigint NOT NULL
        CONSTRAINT pk_post_line PRIMARY KEY,
    post_id   bigint NOT NULL,
    -- 1 = Monday to 5 = Friday, as ISO numbers them. The school works Mon-Fri.
    weekday   int    NOT NULL,
    ordinal   int    NOT NULL,
    -- Free text, not a time type: the brief asks for a hand-typed time or
    -- range, and parsing it would only create a way to reject what was meant.
    time_text nvarchar(64)   NULL,
    text      nvarchar(1024) NULL,
    CONSTRAINT fk_post_line_post FOREIGN KEY (post_id)
        REFERENCES sgs.post ON DELETE CASCADE
);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_post_line_post')
CREATE INDEX ix_post_line_post ON sgs.post_line (post_id, weekday, ordinal);
GO

-- ---- news categories -----------------------------------------------------
--
-- A table behind an autocomplete rather than a free-text tag. Typing feels the
-- same; the unique name is what stops one category becoming two because someone
-- left a double space in it.

IF OBJECT_ID('sgs.post_category') IS NULL
CREATE TABLE sgs.post_category
(
    id          bigint    NOT NULL
        CONSTRAINT pk_post_category PRIMARY KEY,
    uuid        nvarchar(36)  NOT NULL,
    name        nvarchar(128) NOT NULL,
    is_archived bit       NOT NULL CONSTRAINT df_post_category_archived DEFAULT 0,
    created_at  datetime2 NOT NULL,
    created_by  bigint NULL,
    updated_at  datetime2 NOT NULL,
    updated_by  bigint NULL,
    CONSTRAINT uq_post_category_name UNIQUE (name)
);
GO

-- The one the school named. Anything else they add from the console.
IF NOT EXISTS (SELECT 1 FROM sgs.post_category)
INSERT INTO sgs.post_category (id, uuid, name, is_archived, created_at, updated_at)
VALUES (NEXT VALUE FOR sgs.post_category_seq,
        LOWER(CONVERT(nvarchar(36), NEWID())), N'საბავშვო ბაღი', 0,
        SYSUTCDATETIME(), SYSUTCDATETIME());
GO

-- ---- news pictures -------------------------------------------------------
--
-- Its own table so that listing news does not drag image bytes through a query
-- that only wants dates and titles. What is stored is never what was uploaded:
-- the file is decoded, scaled to 1600px on its long edge and re-encoded, which
-- is both how a 4 MB phone photo becomes ~200 KB and the only validation worth
-- having - a file that will not decode is not an image.

IF OBJECT_ID('sgs.post_image') IS NULL
CREATE TABLE sgs.post_image
(
    id           bigint    NOT NULL
        CONSTRAINT pk_post_image PRIMARY KEY,
    uuid         nvarchar(36)   NOT NULL,
    content_type nvarchar(64)   NOT NULL,
    byte_size    int       NOT NULL,
    width        int       NOT NULL,
    height       int       NOT NULL,
    bytes        varbinary(MAX) NOT NULL,
    created_at   datetime2 NOT NULL,
    created_by   bigint NULL,
    updated_at   datetime2 NOT NULL,
    updated_by   bigint NULL,
    CONSTRAINT uq_post_image_uuid UNIQUE (uuid)
);
GO

-- ---- post gains the two news columns -------------------------------------

IF NOT EXISTS (SELECT 1 FROM sys.columns
               WHERE object_id = OBJECT_ID('sgs.post') AND name = 'category_id')
ALTER TABLE sgs.post
    ADD category_id bigint NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns
               WHERE object_id = OBJECT_ID('sgs.post') AND name = 'image_id')
ALTER TABLE sgs.post
    ADD image_id bigint NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_post_category')
ALTER TABLE sgs.post
    ADD CONSTRAINT fk_post_category
        FOREIGN KEY (category_id) REFERENCES sgs.post_category;
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_post_image')
ALTER TABLE sgs.post
    ADD CONSTRAINT fk_post_image
        FOREIGN KEY (image_id) REFERENCES sgs.post_image;
GO

SELECT 'post_line' AS entity, COUNT(*) AS n
FROM sgs.post_line
UNION ALL
SELECT 'post_category', COUNT(*)
FROM sgs.post_category
UNION ALL
SELECT 'post_image', COUNT(*)
FROM sgs.post_image;
GO
