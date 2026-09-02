-- Months and weeks, so that a journal can actually be filled in monthly or weekly.
--
-- SUPERSEDED IN PART BY db/032. The ten calendar months below are not what the
-- school reports on: the brief gives seven reporting periods, pairing September
-- with October and January with February, putting March in Trimester II rather
-- than III, and stopping at May. db/032 rebuilds this level to that shape. The
-- months are left here because 021 cuts its school days from their dates and
-- because rewriting a shipped script is worse than correcting it with another.
--
-- The wizard offers four frequencies and only two levels of the period tree
-- existed - a journal set to MONTH had no periods to hang off, so the option
-- was a lie. Frequency maps to depth:
--
--     depth 0   year         ONCE_A_YEAR
--     depth 1   trimesters   TRIMESTER
--     depth 2   months       MONTH
--     depth 3   weeks        WEEK
--
-- Months sit under the trimester their teaching falls in rather than by strict
-- date containment: the trimester boundaries cut across December and March, and
-- a school does not split a month's journal in two.
--
-- Weeks are numbered within their month rather than dated. The school works a
-- month at a time and the fourth week of October is what a teacher looks for,
-- not the week commencing the 22nd.

SET
XACT_ABORT ON;
SET
NOCOUNT ON;
-- A filtered index (db/015) requires QUOTED_IDENTIFIER ON for any DML on the
-- table it covers, not only when it is created. sqlcmd leaves it OFF and fails
-- with Msg 1934; the JDBC driver sets it, so the application is unaffected.
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
('No period scheme - run 006_migrate_from_dbo.sql first.', 16, 1);
ROLLBACK TRANSACTION;
RETURN;
END

-- ---- months ---------------------------------------------------------------

IF
OBJECT_ID('tempdb..#months') IS NOT NULL
DROP TABLE #months;
CREATE TABLE #months
(
    trimester nvarchar(8),
    code      nvarchar(32),
    label     nvarchar(128),
    ordinal   int,
    starts_on date,
    ends_on   date
);

INSERT INTO #months
VALUES (N'T1', N'M09', N'სექტემბერი', 0, '2025-09-01', '2025-09-30'),
       (N'T1', N'M10', N'ოქტომბერი', 1, '2025-10-01', '2025-10-31'),
       (N'T1', N'M11', N'ნოემბერი', 2, '2025-11-01', '2025-11-30'),
       (N'T2', N'M12', N'დეკემბერი', 3, '2025-12-01', '2025-12-31'),
       (N'T2', N'M01', N'იანვარი', 4, '2026-01-01', '2026-01-31'),
       (N'T2', N'M02', N'თებერვალი', 5, '2026-02-01', '2026-02-28'),
       (N'T3', N'M03', N'მარტი', 6, '2026-03-01', '2026-03-31'),
       (N'T3', N'M04', N'აპრილი', 7, '2026-04-01', '2026-04-30'),
       (N'T3', N'M05', N'მაისი', 8, '2026-05-01', '2026-05-31'),
       (N'T3', N'M06', N'ივნისი', 9, '2026-06-01', '2026-06-30');

INSERT INTO sgs.period (id, scheme_id, parent_id, code, label, ordinal, depth, kind,
                        starts_on, ends_on)
SELECT NEXT VALUE FOR sgs.period_seq, @scheme, t.id, m.code, m.label, m.ordinal, 2, N'REPORTING', m.starts_on, m.ends_on
FROM #months m
    JOIN sgs.period t
ON t.scheme_id = @scheme AND t.code = m.trimester
WHERE NOT EXISTS (SELECT 1 FROM sgs.period p
    WHERE p.scheme_id = @scheme
  AND p.code = m.code);

-- ---- weeks ----------------------------------------------------------------
--
-- Four per month. The ethics journal runs to six in a long month; adding the
-- extra two is a row each, not a schema change.

INSERT INTO sgs.period (id, scheme_id, parent_id, code, label, ordinal, depth, kind,
                        starts_on, ends_on)
SELECT NEXT VALUE FOR sgs.period_seq, @scheme, m.id, m.code + N'W' + CAST (w.n AS nvarchar(2)), N'კვირა ' + CAST (w.n AS nvarchar(2)), w.n - 1, 3, N'REPORTING', NULL, NULL
FROM sgs.period m
    CROSS JOIN (VALUES (1), (2), (3), (4)) w(n)
WHERE m.scheme_id = @scheme
  AND m.depth = 2
  AND NOT EXISTS (SELECT 1 FROM sgs.period p
    WHERE p.scheme_id = @scheme
  AND p.code = m.code + N'W' + CAST (w.n AS nvarchar(2)));

COMMIT TRANSACTION;

SELECT CAST(depth AS varchar) + ' — ' + kind AS level, COUNT(*) AS n
FROM sgs.period
WHERE scheme_id = @scheme
GROUP BY depth, kind
ORDER BY depth;
GO
