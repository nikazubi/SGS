-- Optimistic locking on a change request.
--
-- Two directors opening the queue at once both pass the PENDING check
-- otherwise: the grade is published twice, the guardian emailed twice, or a
-- rejection overwrites the other's applied approval.

SET
NOCOUNT ON;
SET
QUOTED_IDENTIFIER ON;
SET
ANSI_NULLS ON;

IF
NOT EXISTS (SELECT 1 FROM sys.columns
               WHERE object_id = OBJECT_ID('sgs.grade_change_request') AND name = 'row_version')
BEGIN
ALTER TABLE sgs.grade_change_request
    ADD row_version int NULL;
END
GO

UPDATE sgs.grade_change_request
SET row_version = 0
WHERE row_version IS NULL;
GO

ALTER TABLE sgs.grade_change_request ALTER COLUMN row_version int NOT NULL;
GO
