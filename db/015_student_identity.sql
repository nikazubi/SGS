-- Student identity: one record per child, and a login that identifies exactly one.
--
-- Two rules, both enforced here:
--
--   personal_number            unique
--   (username, password_hash)  unique
--
-- Usernames may repeat and passwords may repeat; the pair may not. That is what
-- makes a login resolve to one student, and it is why the parent token is keyed
-- by student id rather than by username - a username is no longer an identity.
--
-- The legacy data breaks both rules. 006 migrated it faithfully, suffixing the
-- eight duplicate usernames, which was the wrong repair: nine of the eleven
-- duplicate personal numbers are the SAME CHILD entered twice, so suffixing
-- turned one child into two accounts with their enrollments split.
--
-- None of the affected records carry grades or absences, so merging loses
-- nothing.

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

-- ---- 0. drop the constraint that was the wrong rule ----------------------
--
-- 001 made username unique on its own, which is what forced 006 to suffix the
-- duplicates. Uniqueness belongs on the login pair, not on half of it.

IF
EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = 'uq_student_username')
ALTER TABLE sgs.student DROP CONSTRAINT uq_student_username;

-- ---- 1. find the duplicates ----------------------------------------------
--
-- Normalised, because eight legacy numbers lost a leading zero:
-- 1617064292 and 01617064292 are the same child.

IF
OBJECT_ID('tempdb..#dupes') IS NOT NULL
DROP TABLE #dupes;
SELECT s.id, RIGHT (N'00000000000' + LTRIM(RTRIM(s.personal_number)), 11) AS pn, s.first_name, s.last_name, (SELECT COUNT (*) FROM sgs.enrollment e WHERE e.student_id = s.id) AS enrollments
INTO #dupes
FROM sgs.student s
WHERE s.personal_number IS NOT NULL AND LTRIM(RTRIM(s.personal_number)) <> N'';

DELETE
FROM #dupes
WHERE pn NOT IN (SELECT pn FROM #dupes GROUP BY pn HAVING COUNT(*) > 1);

-- ---- 2. separate the real duplicates from the mis-keyed records ----------
--
-- Only an exact match on BOTH names is treated as one child recorded twice.
--
-- Deliberately conservative, because the first attempt at this was not and it
-- deleted a real student. Matching on surname alone merged სოსო and საბა
-- ბაბუნაშვილი - two brothers, who of course share one. Matching on first name
-- alone would merge ანდრია ლომიაშვილი with ანდრია იაშვილი, who are unrelated
-- children whose records collided on a mistyped number.
--
-- Anything short of an exact match goes to the school. A near-miss like
-- აბდულლაევი / აბდულლაევ is very likely the same child, but "very likely" is
-- not a licence to delete somebody's record.

IF
OBJECT_ID('tempdb..#same_child') IS NOT NULL
DROP TABLE #same_child;
SELECT pn
INTO #same_child
FROM #dupes
GROUP BY pn
HAVING COUNT(DISTINCT LTRIM(RTRIM(first_name)) + N'|' + LTRIM(RTRIM(last_name))) = 1;

-- ---- 3. merge: keep one record per child ---------------------------------
--
-- The survivor is the row that is actually enrolled; where both are, the
-- higher id, which is the later-created record and so the current placement.
-- Four pairs are decided that way rather than by evidence, and are reported at
-- the end so the school can confirm the class is right.

IF
OBJECT_ID('tempdb..#keep') IS NOT NULL
DROP TABLE #keep;
SELECT pn, MAX(id) AS keep_id
INTO #keep
FROM (SELECT d.pn,
             d.id,
             ROW_NUMBER() OVER (PARTITION BY d.pn
                              ORDER BY CASE WHEN d.enrollments > 0 THEN 0 ELSE 1 END, d.id DESC)
               AS rank_in_pair
      FROM #dupes d
      WHERE d.pn IN (SELECT pn FROM #same_child)) ranked
WHERE rank_in_pair = 1
GROUP BY pn;

IF
OBJECT_ID('tempdb..#drop_ids') IS NOT NULL
DROP TABLE #drop_ids;
SELECT d.id
INTO #drop_ids
FROM #dupes d
         JOIN #keep k ON k.pn = d.pn
WHERE d.id <> k.keep_id;

-- The losing record's enrollment goes with it. Its grades would too, but none
-- of these students have any - checked before writing this.
DELETE
FROM sgs.enrollment
WHERE student_id IN (SELECT id FROM #drop_ids);
DELETE
FROM sgs.student
WHERE id IN (SELECT id FROM #drop_ids);

-- ---- 4. undo 006's username suffixes -------------------------------------
--
-- They only existed to dodge a unique constraint on username alone, which is
-- not the rule: duplicate usernames are fine as long as the password differs.

UPDATE sgs.student
SET username = LEFT (username, CHARINDEX(N'.', username) - 1)
WHERE username LIKE N'%.%'
  AND username NOT LIKE N'%@%'
  AND TRY_CAST(SUBSTRING (username
    , CHARINDEX(N'.'
    , username) + 1
    , 20) AS bigint) IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sgs.student o
    WHERE o.username = LEFT (sgs.student.username
    , CHARINDEX(N'.'
    , sgs.student.username) - 1)
  AND o.password_hash = sgs.student.password_hash
  AND o.id <> sgs.student.id);

-- ---- 5. the two records whose personal number belongs to someone else -----
--
-- Different children sharing a number: one of them is wrong and the data does
-- not say which. The wrong number is worse than no number - it would block the
-- unique constraint and it identifies the wrong child - so the later record's
-- is cleared for the school to fill in.

IF
OBJECT_ID('tempdb..#unresolved') IS NOT NULL
DROP TABLE #unresolved;
SELECT d.id, d.pn, d.first_name, d.last_name
INTO #unresolved
FROM #dupes d
WHERE d.pn NOT IN (SELECT pn FROM #same_child);

UPDATE sgs.student
SET personal_number = NULL
WHERE id IN (SELECT id
             FROM #unresolved
             WHERE id NOT IN (SELECT MIN(id) FROM #unresolved GROUP BY pn));

-- ---- 6. normalise, then enforce ------------------------------------------

UPDATE sgs.student
SET personal_number = RIGHT (N'00000000000' + LTRIM(RTRIM(personal_number)), 11)
WHERE personal_number IS NOT NULL
  AND LEN(LTRIM(RTRIM(personal_number))) = 10;
GO

-- Filtered, because a plain UNIQUE constraint in SQL Server permits only one
-- NULL - and a cleared number has to be allowed on more than one record.
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'uq_student_personal_number')
    CREATE UNIQUE
NONCLUSTERED INDEX uq_student_personal_number
        ON sgs.student (personal_number)
        WHERE personal_number IS NOT NULL;

-- The login pair. Duplicate usernames are fine; duplicate passwords are fine;
-- the combination has to pick out one student, or a parent could be shown
-- somebody else's child.
IF
NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'uq_student_login')
    CREATE UNIQUE
NONCLUSTERED INDEX uq_student_login
        ON sgs.student (username, password_hash);
GO

COMMIT TRANSACTION;

SELECT 'students_now' AS result, CAST(COUNT(*) AS varchar) AS value
FROM sgs.student
UNION ALL
SELECT 'merged_away', CAST((SELECT COUNT(*) FROM #drop_ids) AS varchar)
UNION ALL
SELECT 'personal_number_cleared_for_review',
       CAST((SELECT COUNT(*)
             FROM #unresolved
             WHERE id NOT IN (SELECT MIN(id) FROM #unresolved GROUP BY pn)) AS varchar);

SELECT 'NEEDS THE SCHOOL: same personal number, names do not match exactly' AS note;
SELECT CAST(id AS varchar) + N' | ' + first_name + N' ' + last_name + N' | was ' + pn
FROM #unresolved
ORDER BY pn, id;
GO
