-- Where a child sat, and when.
--
-- A stretch of an enrollment spent in one class. The open row - to_date null -
-- is where they are now; closed rows are where they were.
--
-- WHY NOT A SECOND ENROLLMENT. That was the obvious way to record a mid-year
-- move and it was rejected. Enrollment is the spine: grade_entry, daily_absence,
-- homework_seen, post_target and absence_notice all key on it. A child with two
-- enrollments in one year has their marks split down the middle of it, and
-- ANNUAL is an average of the trimesters computed per enrollment - so a
-- transferred child would end the year with two half years and no annual mark.
-- The absence yearly total has the same shape, and the parent portal's lookup of
-- "this student's enrollment" would start choosing between two rows.
--
-- So the year stays whole. enrollment.class_group_id remains the current class -
-- every existing query keeps working untouched - and this table answers what
-- that column cannot: where were they in October, and when did they move.
--
-- The current class is therefore recorded twice. One service method moves a
-- child and nothing else assigns class_group_id; see EnrollmentService.
--
-- Backfills one open placement per existing enrollment, so the history is
-- complete from the day this runs rather than starting empty.
--
-- Idempotent. Run after 001_schema.sql.

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
               WHERE q.name = 'enrollment_placement_seq' AND c.name = 'sgs')
CREATE SEQUENCE sgs.enrollment_placement_seq AS bigint START WITH 1 INCREMENT BY 50;
GO

IF OBJECT_ID('sgs.enrollment_placement') IS NULL
CREATE TABLE sgs.enrollment_placement
(
    id             bigint    NOT NULL
        CONSTRAINT pk_enrollment_placement PRIMARY KEY,
    enrollment_id  bigint    NOT NULL,
    class_group_id bigint    NOT NULL,
    from_date      date      NOT NULL,
    to_date        date NULL,
    created_at     datetime2 NOT NULL,
    created_by     bigint NULL,
    updated_at     datetime2 NOT NULL,
    updated_by     bigint NULL,
    CONSTRAINT fk_placement_enrollment FOREIGN KEY (enrollment_id)
        REFERENCES sgs.enrollment,
    CONSTRAINT fk_placement_class FOREIGN KEY (class_group_id)
        REFERENCES sgs.class_group
);
GO

-- One open placement per enrollment.
--
-- A filtered unique index rather than a check in the service: two coordinators
-- moving the same child at the same moment is exactly the case that read-then-
-- write lets through, and the result would be a child in two classes at once
-- with no way to tell which is current. The same pattern as uq_open_change_request
-- and the pending-notice index in db/025.
--
-- A filtered index needs QUOTED_IDENTIFIER ON at creation, which sqlcmd leaves
-- off; it is set above. The JDBC driver sets it, so the application is
-- unaffected.
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'uq_open_placement')
    CREATE UNIQUE
NONCLUSTERED INDEX uq_open_placement
        ON sgs.enrollment_placement (enrollment_id)
        WHERE to_date IS NULL;
GO

-- Answers "who was in this class on date D" without scanning.
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_placement_class_dates')
CREATE INDEX ix_placement_class_dates
    ON sgs.enrollment_placement (class_group_id, from_date, to_date);
GO

-- ---- backfill ------------------------------------------------------------
--
-- Every enrollment that exists today has been in its current class since the
-- day it started, as far as anything recorded knows. joined_on is the honest
-- from_date; where it is null the year's own start is, since an enrollment
-- cannot precede its year.

INSERT INTO sgs.enrollment_placement (id, enrollment_id, class_group_id, from_date, to_date,
                                      created_at, updated_at)
SELECT NEXT VALUE FOR sgs.enrollment_placement_seq, e.id, e.class_group_id, COALESCE (e.joined_on, y.starts_on),
    -- A child who has left keeps a closed placement: they are not in the
    -- class now, and an open row would say they were.
    e.left_on, SYSUTCDATETIME(), SYSUTCDATETIME()
FROM sgs.enrollment e
    JOIN sgs.academic_year y
ON y.id = e.academic_year_id
WHERE NOT EXISTS (SELECT 1 FROM sgs.enrollment_placement p WHERE p.enrollment_id = e.id);
GO

SELECT 'enrollments' AS entity, COUNT(*) AS n
FROM sgs.enrollment
UNION ALL
SELECT 'placements', COUNT(*)
FROM sgs.enrollment_placement
UNION ALL
SELECT 'open placements', COUNT(*)
FROM sgs.enrollment_placement
WHERE to_date IS NULL;
GO
