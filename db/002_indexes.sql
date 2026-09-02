-- Indexes that JPA cannot express, applied after 001_schema.sql.
--
-- SQL Server INCLUDE columns have no JPA annotation, so this one is written by
-- hand rather than generated. It matters more than the others: it serves the
-- working-set read that every grade save begins with, and covering it means the
-- whole grid is answered from the index without touching the table.
--
-- For reference, the same read against the legacy schema was a full scan -
-- dbo.grades is a heap with no primary key at all.

IF
NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_grade_grid_covering')
CREATE
NONCLUSTERED INDEX ix_grade_grid_covering
    ON sgs.grade_entry (period_id, subject_id, enrollment_id)
    INCLUDE (component_id, value, special_value, row_version, is_override, source,
             template_version_id);
GO

-- Only one open change request per cell.
--
-- A filtered unique index rather than a check-then-insert: two teachers
-- submitting at the same moment is exactly the case where reading first and
-- writing second lets both through. JPA cannot express the WHERE clause, so it
-- lives here rather than on the entity.
--
-- Decided requests are deliberately outside the index - a cell can be disputed
-- again after an earlier request was approved or rejected.
-- A filtered index requires QUOTED_IDENTIFIER and ANSI_NULLS ON at creation
-- time. sqlcmd leaves QUOTED_IDENTIFIER OFF, which fails with Msg 1934; the
-- JDBC driver sets it ON, so the application is unaffected either way.
SET QUOTED_IDENTIFIER ON;
SET
ANSI_NULLS ON;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'uq_open_change_request')
CREATE UNIQUE
NONCLUSTERED INDEX uq_open_change_request
    ON sgs.grade_change_request (grade_entry_id)
    WHERE status = 'PENDING';
GO
