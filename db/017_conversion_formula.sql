-- The one formula marks are printed on.
--
-- IB Mtiebi grades German-style, out of 7, but is legally required to report to
-- the government out of 10. That conversion currently lives as a hardcoded "+3"
-- inside two copy-pasted export methods, switched by a checkbox. The school is
-- moving to a 9-point scale and has not settled the 9-to-10 mapping, so it
-- becomes configuration.
--
-- Representation only. No grade is stored converted, nothing recomputes through
-- it, and the parent portal does not use it - so editing the formula cannot
-- corrupt anything and changing it is never a migration.
--
-- One row, for the whole school. Also drops component.output_offset (db/011),
-- which tried to express the same thing per column.

SET
XACT_ABORT ON;
SET
NOCOUNT ON;
SET
QUOTED_IDENTIFIER ON;
SET
ANSI_NULLS ON;

IF
NOT EXISTS (SELECT 1 FROM sys.sequences WHERE name = 'conversion_formula_seq')
CREATE SEQUENCE sgs.conversion_formula_seq AS bigint START WITH 1 INCREMENT BY 50;
GO

IF OBJECT_ID('sgs.conversion_formula') IS NULL
CREATE TABLE sgs.conversion_formula
(
    id           bigint    NOT NULL
        CONSTRAINT pk_conversion_formula PRIMARY KEY,
    name         nvarchar(128) NOT NULL,
    multiplier   numeric(9, 4) NULL,
    offset_value numeric(9, 4) NULL,
    created_at   datetime2 NOT NULL,
    created_by   bigint NULL,
    updated_at   datetime2 NOT NULL,
    updated_by   bigint NULL
);
GO

-- ---- seed the conversion the school runs on today -------------------------
--
-- 7 + 3 = 10. Seeded rather than left empty so the toggle works on day one; the
-- school replaces it from the UI when the 9-point mapping is decided.

IF NOT EXISTS (SELECT 1 FROM sgs.conversion_formula)
INSERT INTO sgs.conversion_formula (id, name, multiplier, offset_value,
                                    created_at, updated_at)
VALUES (NEXT VALUE FOR sgs.conversion_formula_seq,
        N'ათბალიანი', 1, 3, SYSUTCDATETIME(), SYSUTCDATETIME());
GO

-- ---- retire the earlier attempts -----------------------------------------
--
-- Wrapped in dynamic SQL, and not for style. SQL Server compiles a whole batch
-- before running any of it, and deferred name resolution covers missing *tables*
-- but not missing *columns* of a table that exists. An IF EXISTS guard around a
-- statement naming a dropped column therefore fails to compile - so this script
-- would break on its second run, which is when a migration is least expected to.

IF EXISTS (SELECT 1 FROM sys.columns
           WHERE object_id = OBJECT_ID('sgs.component') AND name = 'output_offset')
BEGIN
    DECLARE
@df nvarchar(128) = (
        SELECT dc.name FROM sys.default_constraints dc
        JOIN sys.columns col ON col.object_id = dc.parent_object_id
                            AND col.column_id = dc.parent_column_id
        WHERE dc.parent_object_id = OBJECT_ID('sgs.component') AND col.name = 'output_offset');
    IF
@df IS NOT NULL
        EXEC('ALTER TABLE sgs.component DROP CONSTRAINT ' + @df);

EXEC('ALTER TABLE sgs.component DROP COLUMN output_offset');
END
GO

-- A per-journal and per-column scale was built and then cut back: the school
-- wants one formula, not a scale attached to things. These drops matter only
-- for a database that saw the intermediate version.

IF EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_component_conversion_scale')
ALTER TABLE sgs.component DROP CONSTRAINT fk_component_conversion_scale;
GO

IF EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_template_conversion_scale')
ALTER TABLE sgs.grading_template DROP CONSTRAINT fk_template_conversion_scale;
GO

IF EXISTS (SELECT 1 FROM sys.columns
           WHERE object_id = OBJECT_ID('sgs.component') AND name = 'conversion_scale_id')
    EXEC('ALTER TABLE sgs.component DROP COLUMN conversion_scale_id');
GO

IF EXISTS (SELECT 1 FROM sys.columns
           WHERE object_id = OBJECT_ID('sgs.grading_template') AND name = 'conversion_scale_id')
    EXEC('ALTER TABLE sgs.grading_template DROP COLUMN conversion_scale_id');
GO

IF OBJECT_ID('sgs.conversion_band') IS NOT NULL
DROP TABLE sgs.conversion_band;
GO
IF OBJECT_ID('sgs.conversion_scale') IS NOT NULL
DROP TABLE sgs.conversion_scale;
GO
IF EXISTS (SELECT 1 FROM sys.sequences WHERE name = 'conversion_band_seq')
DROP SEQUENCE sgs.conversion_band_seq;
GO
IF EXISTS (SELECT 1 FROM sys.sequences WHERE name = 'conversion_scale_seq')
DROP SEQUENCE sgs.conversion_scale_seq;
GO

SELECT name, multiplier, offset_value
FROM sgs.conversion_formula;
GO
