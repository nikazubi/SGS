-- The brief's two absence numbers, per class per month.
--
-- TOTAL_ACADEMIC_HOURS   how many hours the month holds
-- PERMITTED_MISSED_HOURS how many a student may miss before it is a problem
--
-- The second is what turns a bar on the parent's absence chart from green to
-- red, and it is deliberately per month rather than per year: a child can be
-- inside September's allowance and past October's, and the colours have to be
-- able to say so.
--
-- Seeded on every reporting period so the screens have something to draw. The
-- last one is set lower for both classes, which is true - May runs to the end
-- of teaching - and also makes the chart exercise the case that matters: when the months do not
-- all share one allowance, the dashed reference line is dropped and each bar is
-- judged against its own number. With a single uniform figure that code path
-- never runs.
--
-- Run after 032_reporting_periods.sql (which creates the periods) and
-- 100_roster.sql (which creates the classes). Idempotent.

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

DECLARE
@year   bigint = (SELECT id FROM sgs.academic_year WHERE is_current = 1);
DECLARE
@scheme bigint = (SELECT TOP 1 id FROM sgs.period_scheme WHERE academic_year_id = @year);

IF
@scheme IS NULL
BEGIN
    RAISERROR
('No period scheme - run db/demo/100_roster.sql first.', 16, 1);
ROLLBACK TRANSACTION;
RETURN;
END

INSERT INTO sgs.class_period_setting (id, class_group_id, period_id, setting_key, setting_value,
                                      created_at, updated_at)
SELECT NEXT VALUE FOR sgs.class_period_setting_seq, c.id, m.id, k.setting_key, CASE WHEN k.setting_key = N'TOTAL_ACADEMIC_HOURS'
    THEN CASE WHEN m.code = N'R7' THEN v.short_total ELSE v.total
END
            ELSE CASE WHEN m.code = N'R7' THEN v.short_permitted ELSE v.permitted
END
END
,
       SYSUTCDATETIME(), SYSUTCDATETIME()
FROM sgs.class_group c
JOIN (VALUES
        (N'3ა', 100, 10, 60, 6),
        (N'8ბ', 120, 12, 70, 7)
     ) v(class_name, total, permitted, short_total, short_permitted)
  ON v.class_name = c.name
JOIN sgs.period m ON m.scheme_id = @scheme AND m.depth = 2
CROSS JOIN (VALUES (N'TOTAL_ACADEMIC_HOURS'), (N'PERMITTED_MISSED_HOURS')) k(setting_key)
WHERE c.academic_year_id = @year
  AND NOT EXISTS (SELECT 1 FROM sgs.class_period_setting e
                  WHERE e.class_group_id = c.id AND e.period_id = m.id
                    AND e.setting_key = k.setting_key);

COMMIT TRANSACTION;

SELECT c.name AS class, p.label AS month, s.setting_key, s.setting_value
FROM sgs.class_period_setting s
    JOIN sgs.class_group c
ON c.id = s.class_group_id
    JOIN sgs.period p ON p.id = s.period_id
ORDER BY c.name, p.ordinal, s.setting_key;
GO
