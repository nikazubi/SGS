-- Staff-authored content: homework now, and the other four modules on the same
-- table as they land.
--
-- The client brief asks for five things beyond grades - homework, the daily
-- schedule, the menu, student characterizations and news. They differ in about
-- four fields and agree on everything structural, so one table with a `kind`
-- rather than five that drift apart. Phase 8 creates only what homework uses;
-- the schedule's weekday lines and news's picture and category are nullable
-- additions phase 9 makes when it has something to put in them.
--
-- Publication is frozen: the school's answer was that any edit needs a
-- re-publish, so published_payload holds what parents were shown and the row
-- holds the working copy. Nothing reads the snapshot until phase 11.

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
NOT EXISTS (SELECT 1 FROM sys.sequences WHERE name = 'post_seq')
CREATE SEQUENCE sgs.post_seq AS bigint START WITH 1 INCREMENT BY 50;
GO
IF NOT EXISTS (SELECT 1 FROM sys.sequences WHERE name = 'post_target_seq')
CREATE SEQUENCE sgs.post_target_seq AS bigint START WITH 1 INCREMENT BY 50;
GO
IF NOT EXISTS (SELECT 1 FROM sys.sequences WHERE name = 'post_link_seq')
CREATE SEQUENCE sgs.post_link_seq AS bigint START WITH 1 INCREMENT BY 50;
GO

-- ---- post ----------------------------------------------------------------

IF OBJECT_ID('sgs.post') IS NULL
CREATE TABLE sgs.post
(
    id                      bigint      NOT NULL
        CONSTRAINT pk_post PRIMARY KEY,
    uuid                    nvarchar(36)  NOT NULL,
    kind                    varchar(24) NOT NULL,
    class_group_id          bigint NULL,
    subject_id              bigint NULL,
    event_date              date NULL,
    title                   nvarchar(512) NULL,
    body_html               nvarchar(MAX) NULL,
    status                  varchar(16) NOT NULL,
    published_at            datetime2 NULL,
    -- nvarchar(MAX), not (255). A snapshot carrying a rich-text body overflows
    -- 255 on the first real publish, which is what JPA defaults to unaided.
    published_payload       nvarchar(MAX) NULL,
    has_unpublished_changes bit         NOT NULL CONSTRAINT df_post_pending DEFAULT 0,
    is_archived             bit         NOT NULL CONSTRAINT df_post_archived DEFAULT 0,
    created_at              datetime2   NOT NULL,
    created_by              bigint NULL,
    updated_at              datetime2   NOT NULL,
    updated_by              bigint NULL,
    CONSTRAINT uq_post_uuid UNIQUE (uuid),
    CONSTRAINT fk_post_class FOREIGN KEY (class_group_id) REFERENCES sgs.class_group,
    CONSTRAINT fk_post_subject FOREIGN KEY (subject_id) REFERENCES sgs.subject
);
GO

-- The list screen: this class's homework for a subject, newest first.
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_post_class_kind')
CREATE INDEX ix_post_class_kind
    ON sgs.post (kind, class_group_id, subject_id, event_date);
GO

-- Phase 11's parent calendar: what was published, by date.
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_post_published')
CREATE INDEX ix_post_published ON sgs.post (kind, status, event_date);
GO

-- ---- targets -------------------------------------------------------------
--
-- No rows means the whole class, which is the common case - so the common case
-- costs nothing to store. Keyed on the enrollment rather than the student, or
-- homework set for a child would follow them into next year's class.

IF OBJECT_ID('sgs.post_target') IS NULL
CREATE TABLE sgs.post_target
(
    id            bigint NOT NULL
        CONSTRAINT pk_post_target PRIMARY KEY,
    post_id       bigint NOT NULL,
    enrollment_id bigint NOT NULL,
    CONSTRAINT uq_post_target UNIQUE (post_id, enrollment_id),
    CONSTRAINT fk_post_target_post FOREIGN KEY (post_id)
        REFERENCES sgs.post ON DELETE CASCADE,
    CONSTRAINT fk_post_target_enrollment FOREIGN KEY (enrollment_id)
        REFERENCES sgs.enrollment
);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_post_target_enrollment')
CREATE INDEX ix_post_target_enrollment ON sgs.post_target (enrollment_id);
GO

-- ---- links ---------------------------------------------------------------
--
-- Links stand in for file attachments: the school's server is short of space,
-- so they asked for links rather than uploads.

IF OBJECT_ID('sgs.post_link') IS NULL
CREATE TABLE sgs.post_link
(
    id      bigint NOT NULL
        CONSTRAINT pk_post_link PRIMARY KEY,
    post_id bigint NOT NULL,
    ordinal int    NOT NULL,
    url     nvarchar(2048) NOT NULL,
    label   nvarchar(256)  NULL,
    CONSTRAINT fk_post_link_post FOREIGN KEY (post_id)
        REFERENCES sgs.post ON DELETE CASCADE
);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_post_link_post')
CREATE INDEX ix_post_link_post ON sgs.post_link (post_id, ordinal);
GO

SELECT 'post' AS entity, COUNT(*) AS n
FROM sgs.post
UNION ALL
SELECT 'post_target', COUNT(*)
FROM sgs.post_target
UNION ALL
SELECT 'post_link', COUNT(*)
FROM sgs.post_link;
GO
