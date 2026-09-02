-- Printed scale offset, per component.
--
-- Replaces the exports' isDecimalSystem flag, which added a hardcoded 3 to
-- every value except rating, behaviour and absence - an IB 7-point to Georgian
-- 10-point conversion with the mapping written into the export code, applied by
-- a boolean query parameter.
--
-- As a column it is configuration: a component not on the academic scale leaves
-- it null, and a different mapping later needs no export change. It is applied
-- when printing only; nothing stored ever moves.

SET
NOCOUNT ON;

IF
NOT EXISTS (SELECT 1 FROM sys.columns
               WHERE object_id = OBJECT_ID('sgs.component') AND name = 'output_offset')
ALTER TABLE sgs.component
    ADD output_offset numeric(6, 2) NULL;
GO
