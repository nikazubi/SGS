-- The yearly absence totals inherited the wrong scale.
--
-- db/022 shared one literal row for both components of each journal, so
-- DAYS_ABSENT - a count of days across the whole year - was given the daily
-- column's 0..1 scale, and HOURS_YEAR inherited the single month's cap of 500.
--
-- Derived writes skip range validation, so the totals compute and store
-- correctly today. But both columns allow an override, and an override is
-- validated: a director correcting a yearly count to 7 is rejected as out of
-- range on a 0..1 column. Allowed but unusable, and wrong metadata either way.

SET
XACT_ABORT ON;
SET
NOCOUNT ON;
SET
QUOTED_IDENTIFIER ON;
SET
ANSI_NULLS ON;

-- ~217 school days in a year, so 400 is ample headroom.
UPDATE sgs.component
SET scale_max = 400
WHERE code = N'DAYS_ABSENT'
  AND (scale_max IS NULL OR scale_max < 400);

-- Nine months of up to 500 hours each.
UPDATE sgs.component
SET scale_max = 5000
WHERE code = N'HOURS_YEAR'
  AND (scale_max IS NULL OR scale_max < 5000);
GO

SELECT code, scale_min, scale_max
FROM sgs.component
WHERE code IN (N'DAYS_ABSENT', N'HOURS_YEAR', N'ABSENT', N'HOURS_MISSED')
ORDER BY code;
GO
