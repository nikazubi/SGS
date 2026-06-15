# Backend performance notes

## ✅ Done now — database indexes (safe, high value)
Almost every grade query filters `GRADES` by some combination of `class_id`, `student_id`, `subject_id`,
`grade_type`, `exact_month`, `identifier`. Without indexes these are full table scans that get slower as the
table grows.

- Declared `@Index` on `Grade` and `AbsenceGrade` entities (applies on fresh schema creation).
- Added `core/src/main/resources/db/indexes.sql` — **run once on the production DB** to add the indexes to
  the existing tables (Hibernate `ddl-auto=update` does not reliably add indexes to existing tables).

This is the single biggest, lowest-risk win for the parent/teacher dashboards.

## ⚠️ Known N+1 — `GradeRepositoryCustomImpl.findGradeBySemester` (NOT changed yet)
Inside the `for (student) { for (subject) { ... } }` loops, a separate `diagnostics` query is fired per
(student × subject), plus per-student behaviour-average (2 queries) and absence-semester queries. For a class
of 25 students × ~12 subjects this is ~300+ small queries per semester/annual page load.

**Why not fixed in this pass:** this feeds the semester/annual report calculation, there are no tests, and it
runs against production. The new indexes turn each of those small queries into fast index seeks, which
removes most of the pain without touching the (intricate, magic-key) calculation logic.

**Safe fix when we're ready (with the user watching the output):** pre-fetch all DIAGNOSTICS_1..4 for the
class+year+months in ONE query, group by `(studentId, subjectId)` preserving `createTime desc` order, then
look up from the map instead of querying per subject. Behaviour is identical because the original also takes
the latest-by-createTime of each diagnostic type. Same approach for the behaviour/absence per-student queries.

## ⚠️ Entity mapping — `@OneToOne` should be `@ManyToOne(LAZY)` (NOT changed yet)
`Grade` (and `AbsenceGrade`) map `student`, `subject`, `academyClass` as `@OneToOne`, which:
1. is semantically wrong (many grades reference one student/subject/class), and
2. is **eager** by default, so every grade row drags its student+subject+class along even when unused.

**Why not fixed in this pass:** switching to `@ManyToOne(fetch = LAZY)` is the correct change, but the grade
service methods are **not** `@Transactional` and the controllers/mappers access these associations *after* the
transaction closes (during JSON serialization). Making them lazy without first adding `@Transactional`
boundaries or explicit `join fetch` would cause `LazyInitializationException` in production.

**Safe path:** (a) change to `@ManyToOne`, keep `fetch = EAGER` first (correct cardinality, same behaviour),
then (b) introduce `@Transactional(readOnly = true)` on the read service methods / add `join fetch` to the
QueryDSL reads, then (c) flip to `LAZY`. Do it as its own change with the app running so we can watch for
lazy-init errors.

## Other quick wins (cheap, low risk) — candidates
- `findGradeBySemester` calls `academyClassRepository.findById(classId)` and then ignores most of it except
  `getIsTransit()`; fine, but the per-loop work could hoist the `dateYear/dateMonth` predicates (already done).
- Consider `fetchFirst()` / `LIMIT` on queries that only use `.get(0)` to avoid materialising full lists
  (several `find...` methods order by date desc and take the first element).
