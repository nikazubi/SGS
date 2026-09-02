-- Publication, per cell.
--
-- Replaces the legacy timestamp cut-off. Parent queries used to filter
-- `grade.createTime < <close event>`, but grades are updated in place, so
-- createTime never moved and any edit made after publication reached parents
-- immediately - no change request, no director. A cut-off compared against a
-- mutable row cannot hold; the row changes underneath it.
--
-- published_at is not null  ->  locked: only a change request may alter it.
-- value <> published_value  ->  changed since publication.
--
-- Recomputation writes `value` only and is never blocked, so a published
-- derived cell may legitimately diverge from what parents were shown.

-- Upgrade-only: db/001 is regenerated from the entities and already declares
-- all three. Guarded anyway - it is the one column-adding script that was not,
-- so running the directory in numeric order died here with Msg 2705 rather than
-- skipping a script that had nothing to do.
IF
NOT EXISTS (SELECT 1 FROM sys.columns
               WHERE object_id = OBJECT_ID('sgs.grade_entry') AND name = 'published_value')
ALTER TABLE sgs.grade_entry
    ADD published_value numeric(6, 2) NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns
               WHERE object_id = OBJECT_ID('sgs.grade_entry')
                 AND name = 'published_special_value')
ALTER TABLE sgs.grade_entry
    ADD published_special_value varchar(16) NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns
               WHERE object_id = OBJECT_ID('sgs.grade_entry') AND name = 'published_at')
ALTER TABLE sgs.grade_entry
    ADD published_at datetime2 NULL;
GO
