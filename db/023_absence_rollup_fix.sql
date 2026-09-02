-- The absence journals' yearly totals could never compute.
--
-- db/022 seeded DAYS_ABSENT and HOURS_YEAR as YEAR components summing a
-- CHILDREN term. CHILDREN is exactly one level: the year's children are
-- trimesters, and no day or month mark lives on a trimester - so the sums saw
-- nothing, silently, on every recompute. Worse, the recompute side resolved
-- CHILDREN as one hop *up* from the changed cell, persisting DAYS_ABSENT rows
-- at the month and HOURS_YEAR rows at the trimester: invisible data at levels
-- those columns do not live at.
--
-- CHILDREN was right for the trimester journal, whose annual mark really does
-- read the trimesters directly one level below. It was never right for a total
-- three levels above the days it counts.
--
-- PeriodRef.DESCENDANTS now spans any distance, and this points the two seeded
-- rollups at it. It also removes the wrong-level rows the old rules wrote.

SET
XACT_ABORT ON;
SET
NOCOUNT ON;
SET
QUOTED_IDENTIFIER ON;
SET
ANSI_NULLS ON;

BEGIN
TRANSACTION;

-- ---- point the two rollups at DESCENDANTS --------------------------------

UPDATE t
SET t.period_ref = N'DESCENDANTS' FROM sgs.derivation_term t
JOIN sgs.derivation_rule r
ON r.id = t.rule_id
    JOIN sgs.component c ON c.id = r.component_id
WHERE c.code IN (N'DAYS_ABSENT'
    , N'HOURS_YEAR')
  AND t.period_ref = N'CHILDREN';

-- ---- remove what the broken rules persisted ------------------------------
--
-- Only derived rows, and only where the component does not belong at that
-- period's level. A manually entered mark is never touched: nothing here has a
-- source of MANUAL, because these two columns are computed and cannot be typed.

DELETE
g
FROM sgs.grade_entry g
JOIN sgs.component c ON c.id = g.component_id
JOIN sgs.period p    ON p.id = g.period_id
WHERE c.code IN (N'DAYS_ABSENT', N'HOURS_YEAR')
  AND c.period_kind = N'YEAR'
  -- A YEAR component belongs at depth 0 and nowhere else.
  AND p.depth <> 0
  AND g.source <> N'MANUAL';

COMMIT TRANSACTION;
GO

SELECT c.code, t.period_ref, COUNT(*) AS terms
FROM sgs.derivation_term t
         JOIN sgs.derivation_rule r ON r.id = t.rule_id
         JOIN sgs.component c ON c.id = r.component_id
WHERE c.code IN (N'DAYS_ABSENT', N'HOURS_YEAR')
GROUP BY c.code, t.period_ref;
GO
