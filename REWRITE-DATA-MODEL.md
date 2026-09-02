# Rewrite — Phase 1 data model

Draft for discussion. Nothing here is committed and nothing needs to be compatible with the
current schema: the grades table will be empty at cutover, and the old version is retired the
moment this ships.

Constraints this is designed against:

* Spring Boot 2.4.3 / Java 11 / Hibernate 5.4 / QueryDSL 4.1.3 / MS SQL Server — unchanged.
* ~1,500 students (12 levels × ~5 classes × ~25), ~25–36 subjects, ≈1.6M grade rows per year.
* Grids **and** derivation formulas configurable from the UI, behind a permission.
* Existing exports retained (4 Excel + 1 Word), rendered from the template.
* New modules (homework, meals, timetable, characterization, posts) come *after* this.

---

## 1. What this replaces

| Gone                                                         | Replaced by                                |
|--------------------------------------------------------------|--------------------------------------------|
| `GradeType` enum (95 values) + `startsWith(prefix)` matching | `component` rows in a template             |
| `exactMonth` + the Feb→Jan / Oct→Sep normalisation           | `period` tree                              |
| `identifier` (overloaded trimester number)                   | `period_id`                                |
| `AcademyClass.isTransit`                                     | a different template assigned to the class |
| `AbsenceGrade` table, `TotalAbsence` table                   | components + class-period settings         |
| `ClosedPeriod` + `createTime < timestamp` filtering          | `publication` + snapshots                  |
| Synthetic subjects 7777 / 8888 / 9999                        | real components                            |
| Negative integer map keys (`-1`, `-7`, `-9`, `-10`)          | named components                           |
| `subjectPattern` hardcoded in 3 places                       | `class_subject.sort_index`                 |
| `-50` sentinel meaning "ჩთ"                                  | `special_value` column                     |
| `Subject.teacher` free-text string                           | `teaching_assignment`                      |
| Semester / diagnostics / monthly weighted machinery          | template configuration                     |

---

## 2. Academic structure

```sql
school            (id, code, name)              -- primary | basic | secondary
academic_year     (id, code, starts_on, ends_on, is_current)

class_group       (id, school_id, academic_year_id, level SMALLINT, name,
                   period_scheme_id, UNIQUE(academic_year_id, school_id, name))

student           (id, first_name, last_name, personal_number, birth_date,
                   username, password_hash, guardian_email, is_active)

enrollment        (id, student_id, class_group_id, academic_year_id,
                   joined_on, left_on NULL,
                   UNIQUE(student_id, academic_year_id))

subject           (id, name, short_name, is_active)

class_subject     (id, class_group_id, subject_id, sort_index,
                   template_version_id NULL,      -- per-subject template override
                   UNIQUE(class_group_id, subject_id))

teaching_assignment (id, class_subject_id, system_user_id, role)
```

Three deliberate changes from today:

**`enrollment` replaces the FK on `Student`.** Today a student belongs to one class through a join
column on `STUDENTS`, so there is no way to represent last year's class. This makes year-over-year
progression and history possible at no cost. It also gives every grade a stable anchor: a grade
belongs to *a student in a class in a year*, not to a student who might later be moved.

**`teaching_assignment` replaces `Subject.teacher`.** The current string field means the same
subject cannot have different teachers in different classes — which it obviously does. Exports and
the "Subject … Teacher" header block in the client brief both need the per-class value.

**`sort_index` on `class_subject`** replaces the three copies of the hardcoded Georgian subject
list (`SubjectOrderUtils`, `ExcelUtils`, `MonthlyGradePage/Helper.js`). Ordering becomes data, and
can differ per class.

---

## 3. Periods

The reporting calendar becomes a tree, not hardcoded month arithmetic.

```sql
period_scheme (id, name, academic_year_id)

period        (id, scheme_id, parent_id NULL, code, label,
               ordinal, kind, starts_on, ends_on,
               UNIQUE(scheme_id, code))
              -- kind: REPORTING | ROLLUP | YEAR
```

The scheme from the client brief:

```
YEAR
├── T1 ── SEP_OCT, NOV
├── T2 ── DEC, JAN_FEB, MAR
└── T3 ── APR, MAY
```

Sep+Oct and Jan+Feb being single reporting periods is now **data**, not a date-mangling rule buried
in `insertStudentGrade` and repeated in five query builders.

Different schemes per school are supported, which is what primary school will need when its modules
land. A component declares which *level* of the tree it lives at — academic components sit at
T1/T2/T3, absence and ethics sit at the monthly reporting level, rollups sit above.

---

## 4. Grading templates

```sql
grading_template  (id, name, school_id NULL, level NULL, description)

template_version  (id, template_id, version_no, status, effective_from_period_id,
                   period_scheme_id, created_by, created_at, published_at NULL,
                   UNIQUE(template_id, version_no))
                  -- status: DRAFT | ACTIVE | LOCKED | ARCHIVED

component         (id, template_version_id, code, label, ordinal,
                   group_label NULL,          -- e.g. "მიმდინარე შეფასება"
                   period_level,              -- which tier of the period tree
                   subject_scoped BIT,        -- false for ethics / absence
                   kind,                      -- INPUT | DERIVED
                   scale_min, scale_max, decimals,
                   allow_special_values BIT,
                   parent_visible BIT,
                   allow_override BIT,
                   UNIQUE(template_version_id, code))
```

`group_label` gives the merged column-group header the grid already draws for
"მიმდინარე შეფასება". `parent_visible` is what lets staff-only working columns exist without
leaking to the parent portal — something the current system has no way to express.

### Derivation

```sql
derivation_rule   (id, component_id UNIQUE, type, null_policy,
                   renormalize_weights BIT, rounding_mode, decimals)
                  -- type:        WEIGHTED_SUM | AVERAGE | SUM | MIN | MAX
                  --            | LATEST | FIRST_NON_NULL | COUNT
                  -- null_policy: IGNORE | AS_ZERO | BLOCK

derivation_term   (id, rule_id, ordinal, weight DECIMAL(6,4),
                   source_kind,        -- COMPONENT | GROUP | ALL_SUBJECTS
                   reduce,             -- for GROUP/ALL_SUBJECTS: AVERAGE | SUM | MIN | MAX | ...
                   period_ref,         -- SAME | CHILDREN | SPECIFIC
                   period_id NULL)     -- when period_ref = SPECIFIC

derivation_source (id, term_id, component_id)   -- 1 row for COMPONENT, N for GROUP
```

This expresses exactly the shape you described — *column 3 = column 1 × x% + column 2 × y%*, with
the user choosing which columns participate:

```
TRIMESTER_GRADE  =  0.50 × avg(ONGOING_1 … ONGOING_7)      ← one GROUP term, 7 sources
                 +  0.20 × INITIAL_KNOWLEDGE                ← one COMPONENT term
                 +  0.30 × FINAL_TEST
```

and the cross-period rollups the brief's annual table needs:

```
ANNUAL   = AVERAGE over period_ref = CHILDREN of TRIMESTER_GRADE
OVERALL  = 0.80 × ANNUAL + 0.20 × FINAL_EXAM
```

A term's source being a **group reduced to one number** is what makes the old
*"average the 8 classwork columns, then take 25% of that"* rule expressible — and, more to the
point, makes the next rule they invent expressible too.

`ALL_SUBJECTS` exists because one real rule aggregates *across* subjects rather than within one:
the legacy **rating** column is the average of a student's marks over every subject in the period.
Every other source kind resolves inside a single subject, so this needed its own kind. It also
covers anything else student-wide that gets invented later.

**No expression language.** A rule is rows, so it can be validated when saved (cycles, dangling
refs, weight totals), rendered back in Georgian in the UI, and executed in bulk. `allow_override`
on the component is the escape hatch: `TRIMESTER_GRADE` is typed by hand today despite looking
derived, so make that a first-class, flagged, audited state rather than a lie.

### Assignment

```sql
template_assignment (id, template_version_id, class_group_id,
                     subject_id NULL,        -- NULL = all subjects in the class
                     scope,                  -- ACADEMIC | ETHICS | ABSENCE
                     UNIQUE(class_group_id, subject_id, scope))
```

`isTransit` disappears here: a transit class is simply a class assigned a different template.

---

## 5. Grade data

```sql
grade_entry (
  id                  BIGINT,
  enrollment_id       BIGINT      NOT NULL,
  subject_id          BIGINT      NULL,        -- NULL for ethics / absence
  period_id           BIGINT      NOT NULL,
  component_id        BIGINT      NOT NULL,
  template_version_id BIGINT      NOT NULL,
  value               DECIMAL(6,2) NULL,
  special_value       VARCHAR(16)  NULL,       -- 'NA' (ჩთ), etc.
  source              TINYINT     NOT NULL,    -- MANUAL | DERIVED
  is_override         BIT         NOT NULL DEFAULT 0,
  created_at, created_by, updated_at, updated_by
)
```

**Indexes:**

```sql
UNIQUE (enrollment_id, subject_id, period_id, component_id)   -- upsert key
       (period_id, subject_id, enrollment_id) INCLUDE (component_id, value, special_value)
       (component_id, period_id)                              -- recompute sweeps
       (template_version_id)                                  -- version scoping
```

That first index is the entire reason a save can become one statement. Note SQL Server's `UNIQUE`
treats NULLs as equal for uniqueness (unlike Postgres), which is the behaviour we want for the
nullable `subject_id`; if it proves awkward, two filtered unique indexes do the same job.

`special_value` retires the `-50` sentinel. Each template declares its allowed special codes and
how each behaves in aggregation (excluded / counted as zero / blocks the derived value), so "ჩთ"
stops being a magic number that some screens render as text and others average into a mark.

### Class-level period settings

The brief's absence screen needs two numbers that are per class-and-month, not per student:

```sql
class_period_setting (id, class_group_id, period_id, key, value,
                      UNIQUE(class_group_id, period_id, key))
                     -- TOTAL_ACADEMIC_HOURS, PERMITTED_MISSED_HOURS
```

`PERMITTED_MISSED_HOURS` drives the brief's green→red threshold colouring.

---

## 6. Publication — replacing closed periods

Today "publishing" is a timestamp and every student-facing query appends
`grade.createTime < thatTimestamp`. That has a nasty property: a grade edited after publication
silently disappears from the parent's view, and there is no record of what was actually sent.

Instead, publish by **snapshot**:

```sql
publication      (id, class_group_id, period_id, scope, published_at,
                  published_by, note)

published_grade  (id, publication_id, enrollment_id, subject_id NULL,
                  component_id, value, special_value)
```

* Staff always read live data; parents always read the newest snapshot.
* Republishing is an explicit, diffable act.
* "What did we send them in December?" becomes answerable.
* A `template_version` moves to `LOCKED` the moment a publication references it — which is exactly
  the brief's rule that the coordinator loses edit rights *once results have been sent to the
  client*, and it falls out of the model rather than needing separate enforcement.

At ~1.6M rows/year and a handful of publications, the storage cost is irrelevant.

---

## 7. Recompute engine

Components in a template version form a DAG (validated acyclic when the version is saved).

**On write** of component `C` for `(enrollment, subject, period)`:

1. Resolve the transitive dependents of `C` — including across period tiers, so a T1 edit reaches
   the YEAR row.
2. Recompute in topological order, honouring `null_policy`, `renormalize_weights` and rounding.
3. Skip writing any cell with `is_override = 1`, but still propagate its value downstream.
4. Write everything in one transaction; return the changed set.

At your scale a closure is a handful of rows, so this is sub-millisecond work. A full class
recompute is 25 × ~14 ≈ 350 rows. A template-version change triggers a scoped background job.

**Write API:**

```
POST /api/grades/batch
{ classGroupId, subjectId, periodId,
  entries: [ { enrollmentId, componentCode, value | specialValue } ] }

→ { applied: [...], derived: [ { enrollmentId, componentCode, value } ], conflicts: [...] }
```

One request, one transaction, `MERGE` upsert, and the derived values come back so the client
patches its cache instead of refetching the grid. This is what turns ~8 round trips plus a full
reload per cell into one background request per row.

Ids come from a **per-table sequence with `allocationSize = 50`**, not `IDENTITY` — Hibernate
disables JDBC insert batching when ids are identity-generated. This requires switching the pinned
dialect from `org.hibernate.dialect.SQLServerDialect` (the SQL Server 2000-era dialect, which does
not support sequences at all) to `SQLServer2012Dialect`.

---

## 8. Why this de-risks the open client questions

Several things we flagged as unanswered in `CLIENT-BRIEF-2026.md` stop being architectural
decisions and become configuration:

| Open question                                                     | Effect on the model                                                   |
|-------------------------------------------------------------------|-----------------------------------------------------------------------|
| Is ethics one value per month, or the 5-criteria weekly detail?   | Either. More components at a weekly period tier, or fewer at monthly. |
| What is the formula for the trimester / annual / overall columns? | Configured, not coded. Wrong guesses are edits, not releases.         |
| The unlabelled 14th column                                        | Add or remove a component.                                            |
| 7-point vs 10-point scale                                         | `scale_min` / `scale_max` / `decimals` per component.                 |
| Is `Academic project assessment` input or derived?                | Flip `kind`.                                                          |

That is the main argument for doing configuration *first*, as you've sequenced it: it converts a
set of blocking questions into questions we can answer late.

---

## 9. Authorization — permission groups, not roles

Decided: **keep the current model.** A group is a freely-named set of permissions plus a scope
grant. No fixed role hierarchy. The brief's "General Director", "Quality Service", "School Head",
"Administrator" and "Coordinator" all become *example groups someone creates in the UI*, not types
in the code — which is the more configurable option and costs nothing.

```sql
permission        (code PK, label, category)        -- catalogue, seeded by the app
system_group      (id, name, is_active)
group_permission  (group_id, permission_code)       -- join, not a CSV string
system_user       (id, username, password_hash, name, email, is_active)
user_group        (user_id, group_id)

user_scope        (id, user_id, kind, school_id NULL, class_group_id NULL)
                  -- kind: ALL_SCHOOLS | SCHOOL | CLASS
```

Two refinements over today:

**Permissions become a join table, not a comma-separated string.** Same flexibility, but the UI can
enumerate the catalogue instead of hardcoding `PERMISSION_OPTIONS`, and a typo can't silently grant
nothing.

**Scope grants gain two levels.** Today scope is only an explicit list of classes
(`SystemUser.academyClassList`). The brief needs a director to see three whole schools — that works
by enumerating ~60 classes, but it's tedious and breaks whenever a class is added. `ALL_SCHOOLS` /
`SCHOOL` / `CLASS` covers every case in the brief with one mechanism, and `CLASS` remains exactly
what exists today.

Note that class scope is now year-bound (`class_group` belongs to an academic year), so scopes need
a "carry forward to next year" bulk action rather than silently going stale.

### Permissions to add

Existing 14 stay. New ones the rewrite needs:

| Code                      | Gates                                          |
|---------------------------|------------------------------------------------|
| `VIEW_GRADING_TEMPLATE`   | see template configuration                     |
| `MANAGE_GRADING_TEMPLATE` | edit columns, formulas, scales — the config UI |
| `PUBLISH_GRADES`          | run a publication                              |
| `EDIT_PUBLISHED_GRADES`   | edit a period after it has been published      |
| `OVERRIDE_DERIVED_GRADE`  | type over a computed cell                      |

`EDIT_PUBLISHED_GRADES` is how the brief's rule — *the coordinator loses edit rights once results
are sent to the client* — gets expressed without inventing roles: publication locks the period, and
that permission is what gets you past the lock. Ordinary coordinators simply don't have it.

---

## 10. Change requests — kept

Confirmed as a must-keep, so it stays a first-class workflow rather than being replaced by
role-based locking.

```sql
change_request (id, grade_entry_id, requested_by, requested_at,
                old_value, old_special, new_value, new_special,
                reason, status, decided_by, decided_at, decision_note)
               -- status: PENDING | APPROVED | REJECTED
```

Three fixes to how it works today:

1. It currently looks the grade up by **the change request's own id**
   (`ChangeRequestServiceImpl`, with a `//TODO incorrect code` beside it). The FK above fixes that.
2. Approving writes the new value straight to the grade. In the new model it must go through the
   same write path as a normal edit, so **derived values recompute** — today they wouldn't.
3. It pairs naturally with the publication lock: a teacher without `EDIT_PUBLISHED_GRADES` raising
   a request is the intended path once a period is closed.

Because a change request records old → new, who and when, it also gives a real audit trail for
exactly the changes that matter — which is what makes §11 acceptable.

---

## 11. Audit — last-writer now, history later

Decided: `grade_entry` carries `created_at/by` and `updated_at/by` only. No per-change history
table in phase 1.

This is safe to defer because adding it later is additive: a `grade_entry_history` table plus an
insert on the write path, with no change to the read model or to any existing row. Worth doing the
moment someone asks "who changed this 6 to a 7", and the change-request table already covers the
disputed cases in the meantime.

---

## 12. Frontends — two, not one

Confirmed: the staff console and the parent console stay separate applications. They serve
different audiences, different auth, and different deployment sensitivity.

What should **not** stay duplicated is the plumbing — today both apps carry their own copies of the
DataGrid wrapper, formik field wrappers, axios instance, and five contexts, which have already
drifted apart. A small shared package (or simply one copied-once, deliberately-owned module) for
the grid and the API client is worth it, since the grid is where all the performance work lands.

---

## 13. Open items

| #  | Item                                                                                                                                                                                                                                                                             | Status                                                                                                                                              |
|----|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| 1  | Annual wipe                                                                                                                                                                                                                                                                      | closed — it's a workaround but stays this round; `academic_year` + `enrollment` remain in the schema, and the wipe becomes an explicit purge action |
| 2  | Portal login                                                                                                                                                                                                                                                                     | closed — **one login per child**, as today; `guardian_email` stays on `student`, no guardian entity                                                 |
| 3  | Ethics granularity                                                                                                                                                                                                                                                               | closed — a seeding choice, not a design one; the template decides                                                                                   |
| 4  | Exports' `isDecimal` +3 shift                                                                                                                                                                                                                                                    | closed — dropped; re-add as a component scale setting if it proves real                                                                             |
| 5  | **Do any rules drop or ignore marks** — "discard the lowest of the seven", or "use the final exam if it beats the trimester average"? Nothing in the legacy code does, and neither shape is currently expressible. Not blocking; decides whether two more rule types are needed. | **client — the only one left**                                                                                                                      |
| 6  | Deployment target                                                                                                                                                                                                                                                                | closed — versions unchanged                                                                                                                         |
| 7  | Audit depth                                                                                                                                                                                                                                                                      | closed — last-writer (§11)                                                                                                                          |
| 8  | Change requests                                                                                                                                                                                                                                                                  | closed — kept (§10)                                                                                                                                 |
| 9  | Role model                                                                                                                                                                                                                                                                       | closed — permission groups (§9)                                                                                                                     |
| 10 | One frontend or two                                                                                                                                                                                                                                                              | closed — two (§12)                                                                                                                                  |
