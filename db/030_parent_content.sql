-- Phase 11: the parent side of homework, news and absence.
--
-- Two things, and only one of them is a table.
--
--   1. homework_seen - which posts a child's parent has opened. A row means
--      seen; no row means unseen.
--   2. The monthly absence register becomes parent-visible. It was seeded with
--      is_parent_visible = 0 in db/022 and nothing ever turned it on, so the
--      journal whose whole purpose is the parent's green-to-red diagram was
--      invisible to them.
--
-- Nothing is needed for news or for the primary/basic/secondary split. News is
-- institution-wide by the school's own decision, its categories already exist
-- (db/020), and the split keys on school.code, which db/006 already populated
-- for every class.

SET
XACT_ABORT ON;
SET
NOCOUNT ON;
SET
QUOTED_IDENTIFIER ON;
SET
ANSI_NULLS ON;

-- ---- what the parent has read --------------------------------------------
--
-- Seen, not unseen, and per post rather than per date. Both inversions are
-- deliberate and the reasoning is on HomeworkSeen; the short version is that
-- recording the negative would make one new homework post write a row for every
-- child in the class, and recording it per date would let a second assignment
-- added to an already-opened day arrive without ever announcing itself.
--
-- Keyed on enrollment rather than student so a child who moves class starts
-- clean on work that was never theirs.

IF
NOT EXISTS (SELECT 1 FROM sys.sequences q
               JOIN sys.schemas c ON c.schema_id = q.schema_id
               WHERE q.name = 'homework_seen_seq' AND c.name = 'sgs')
CREATE SEQUENCE sgs.homework_seen_seq AS bigint START WITH 1 INCREMENT BY 50;
GO

IF OBJECT_ID('sgs.homework_seen') IS NULL
CREATE TABLE sgs.homework_seen
(
    id            bigint    NOT NULL
        CONSTRAINT pk_homework_seen PRIMARY KEY,
    enrollment_id bigint    NOT NULL,
    post_id       bigint    NOT NULL,
    seen_at       datetime2 NOT NULL,
    -- What makes the console's debounced batch safe: a re-send, a double tap
    -- and a retry after a dropped response all land on the same state instead
    -- of accumulating duplicates.
    CONSTRAINT uq_homework_seen UNIQUE (enrollment_id, post_id),
    CONSTRAINT fk_homework_seen_enrollment FOREIGN KEY (enrollment_id)
        REFERENCES sgs.enrollment,
    CONSTRAINT fk_homework_seen_post FOREIGN KEY (post_id)
        REFERENCES sgs.post
);
GO

-- The calendar asks "which of this month's posts has this child not opened",
-- so the lookup is by enrollment across a set of posts.
IF NOT EXISTS (SELECT 1 FROM sys.indexes
               WHERE name = 'ix_homework_seen_enrollment'
                 AND object_id = OBJECT_ID('sgs.homework_seen'))
CREATE INDEX ix_homework_seen_enrollment
    ON sgs.homework_seen (enrollment_id, post_id);
GO

-- ---- the absence register reaches parents --------------------------------
--
-- By shape, not by name, for the reason db/029 gives: grading_template has no
-- unique constraint on name and the console permits renames.

UPDATE sgs.grading_template
SET is_parent_visible = 1
WHERE id = (SELECT TOP 1 id
            FROM sgs.grading_template
            WHERE frequency = N'MONTH'
              AND grid_mode = N'PERIODS'
            ORDER BY id);
GO

SELECT name, frequency, grid_mode, is_parent_visible, locks_on_publish
FROM sgs.grading_template
ORDER BY sort_index;
GO
