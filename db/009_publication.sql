-- Publication log and change requests.
--
-- Only needed for a database created before phase 3; 001_schema.sql now carries
-- both tables. Extracted from it verbatim so the two cannot drift.
--
-- The per-cell columns on grade_entry (004_publication.sql) remain the
-- mechanism for what parents can see. sgs.publication is the audit trail: who
-- released which scope and when. Deriving that by scanning grade_entry for
-- distinct timestamps would be slower and lossy, because a republish overwrites
-- the previous stamp.

SET
XACT_ABORT ON;
SET
NOCOUNT ON;
-- A filtered index requires QUOTED_IDENTIFIER and ANSI_NULLS ON at creation
-- time. sqlcmd leaves QUOTED_IDENTIFIER OFF, which fails with Msg 1934; the
-- JDBC driver sets it ON, so the application is unaffected either way.
SET
QUOTED_IDENTIFIER ON;
SET
ANSI_NULLS ON;

IF
NOT EXISTS (SELECT 1 FROM sys.sequences s JOIN sys.schemas c ON c.schema_id = s.schema_id
               WHERE c.name = 'sgs' AND s.name = 'publication_seq')
CREATE SEQUENCE sgs.publication_seq START WITH 1 INCREMENT BY 50;
GO

IF NOT EXISTS (SELECT 1 FROM sys.sequences s JOIN sys.schemas c ON c.schema_id = s.schema_id
               WHERE c.name = 'sgs' AND s.name = 'grade_change_request_seq')
CREATE SEQUENCE sgs.grade_change_request_seq START WITH 1 INCREMENT BY 50;
GO

IF OBJECT_ID('sgs.publication') IS NULL
CREATE TABLE sgs.publication
(
    id                  bigint    NOT NULL,
    cell_count          int       NOT NULL,
    from_change_request bit       NOT NULL,
    published_at        datetime2 NOT NULL,
    published_by        bigint,
    class_group_id      bigint    NOT NULL,
    period_id           bigint    NOT NULL,
    subject_id          bigint,
    PRIMARY KEY (id)
);
GO

IF OBJECT_ID('sgs.grade_change_request') IS NULL
CREATE TABLE sgs.grade_change_request
(
    id                      bigint      NOT NULL,
    decided_at              datetime2,
    decided_by              bigint,
    decision_comment        nvarchar(1024),
    previous_special_value  nvarchar(16),
    previous_value          numeric(6, 2),
    reason                  nvarchar(1024) NOT NULL,
    requested_at            datetime2   NOT NULL,
    requested_by            bigint,
    requested_special_value nvarchar(16),
    requested_value         numeric(6, 2),
    status                  varchar(16) NOT NULL,
    grade_entry_id          bigint      NOT NULL,
    PRIMARY KEY (id)
);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_publication_class_period')
CREATE INDEX ix_publication_class_period
    ON sgs.publication (class_group_id, period_id, published_at);
IF
NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_publication_at')
CREATE INDEX ix_publication_at ON sgs.publication (published_at);
IF
NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_change_request_status')
CREATE INDEX ix_change_request_status
    ON sgs.grade_change_request (status, requested_at);
IF
NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_change_request_entry')
CREATE INDEX ix_change_request_entry ON sgs.grade_change_request (grade_entry_id);
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_publication_class')
ALTER TABLE sgs.publication
    ADD CONSTRAINT fk_publication_class
        FOREIGN KEY (class_group_id) REFERENCES sgs.class_group;
IF
NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_publication_period')
ALTER TABLE sgs.publication
    ADD CONSTRAINT fk_publication_period
        FOREIGN KEY (period_id) REFERENCES sgs.period;
IF
NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_publication_subject')
ALTER TABLE sgs.publication
    ADD CONSTRAINT fk_publication_subject
        FOREIGN KEY (subject_id) REFERENCES sgs.subject;
IF
NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_change_request_entry')
ALTER TABLE sgs.grade_change_request
    ADD CONSTRAINT fk_change_request_entry
        FOREIGN KEY (grade_entry_id) REFERENCES sgs.grade_entry;
GO

-- One open request per cell. See 002_indexes.sql for why it is filtered.
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'uq_open_change_request')
    CREATE UNIQUE
NONCLUSTERED INDEX uq_open_change_request
        ON sgs.grade_change_request (grade_entry_id)
        WHERE status = 'PENDING';
GO
