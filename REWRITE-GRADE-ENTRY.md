# Phase 2 — the grade entry screen

The screen teachers spend their working day in. Phase 1 built the engine and the write path;
this phase makes them reachable, and replaces the page that currently hardcodes eleven columns
in JSX with one that renders whatever the template says.

Read `REWRITE-DATA-MODEL.md` and `REWRITE-RECOMPUTE-ENGINE.md` first — this document assumes the
model and the evaluator, and only covers what phase 2 adds.

---

## 1. What phase 2 delivers

|              |                                                                                                                                                                                                                                       |
|--------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Backend**  | A read endpoint that returns a whole grid in one call; filter endpoints on the new model; publication columns on `grade_entry`; write-path enforcement of the publication lock; a `dbo` → `sgs` data migration and a template seeder. |
| **Frontend** | The grade entry page in `admin-console/src/front-ac`, rendering columns from the template, saving by debounced batch, showing derived/override/conflict/locked cell states, with an explain panel.                                    |

Deliberately **not** here: the publish action, the change-request flow, the template editor,
exports, the parent console. Those are phases 3–6.

---

## 2. The read endpoint

```
GET /api/gradebook/grid?classGroupId={id}&subjectId={id}&periodId={id}
```

One request, one response, the entire screen. The old page needed a grid fetch plus a subject
fetch plus a class fetch, and then refetched the grid after every single cell edit.

```jsonc
{
  "period":          { "id": 12, "code": "T1", "label": "I ტრიმესტრი", "kind": "ROLLUP" },
  "templateVersion": { "id": 3, "templateName": "ტრიმესტრული შეფასება", "versionNo": 2,
                       "status": "ACTIVE", "pinned": true },

  "columnGroups": [
    { "label": "მიმდინარე შეფასება",
      "componentCodes": ["ONGOING_1", "ONGOING_2", "…", "ONGOING_7"] }
  ],

  "columns": [
    { "code": "ONGOING_1", "label": "I", "ordinal": 0, "kind": "INPUT",
      "editable": true, "decimals": 2, "scaleMin": 0, "scaleMax": 10,
      "allowSpecialValues": true, "groupLabel": "მიმდინარე შეფასება",
      "dependents": ["ONGOING_AVG", "TRIMESTER_GRADE"] },

    { "code": "TRIMESTER_GRADE", "label": "ტრიმესტრის შეფასება", "ordinal": 11,
      "kind": "DERIVED", "editable": true, "allowOverride": true, "decimals": 1,
      "dependsOn": ["ONGOING_AVG", "INITIAL_KNOWLEDGE", "FINAL_TEST"] }
  ],

  "specialValues": [ { "code": "CHT", "label": "ჩთ", "behaviour": "EXCLUDE" } ],

  "students": [
    { "enrollmentId": 501, "studentId": 91, "firstName": "ნინო",
      "lastName": "აბაშიძე", "index": 1 }
  ],

  "cells": [
    { "enrollmentId": 501, "componentCode": "ONGOING_1", "value": 8.00,
      "specialValue": null, "source": "MANUAL", "override": false,
      "rowVersion": 3, "published": false }
  ],

  "capabilities": { "canEdit": true, "canOverride": true, "canExplain": true }
}
```

### Why it is shaped this way

**Cells are a flat array, not nested inside students.** The client indexes it once into a
`Map` keyed `enrollmentId:componentCode`. The current page does this instead, inside `renderCell`:

```js
const found = row.grades?.find(g => g.gradeType === gradeType);   // TrimesterDashBoard.js:28
```

A linear scan per cell per render — 25 students × 11 columns, each scanning an 11-element array,
on **every** re-render. Flat plus a Map makes it a hash lookup.

**`rowVersion` travels with every cell.** It is what `GradeEntryUpdate.expectedVersion` needs for
per-cell optimistic concurrency. Without it in the read payload the client cannot write safely.

**`dependents` / `dependsOn` are included.** Static per template version and free to compute —
`TemplateGraph` already holds the edges. The client uses them to know which derived cells to dim
while a flush is in flight. This is a **dependency list, not evaluation logic**: no part of the
evaluator moves into JavaScript (decision 29).

**Template version resolution is shared with the write path.** Both must agree on which version is
in force, or the UI renders columns the write path then rejects. `GradeWriteService`'s pinning
logic (`findTemplateVersionIdsInPeriod`, falling back to the current assignment) is extracted to a
`TemplateVersionResolver` used by read, write and explain alike. `pinned: true` in the response
means the period already has marks and is therefore locked to that version — the template editor
in phase 5 will need to know this before offering to migrate.

### Which columns appear

Filtered from the template version by:

```
component.periodKind == period.kind        &&        component.subjectScoped == (subjectId != null)
```

For (9A, mathematics, trimester I) that yields the seven ongoing marks, the ongoing average,
initial-knowledge, progress, final test and the trimester grade — exactly the legacy grid, but
derived from configuration rather than from JSX.

Cross-subject components (`subjectScoped = false`, e.g. `RATING`) are excluded from a subject grid
by the same rule; they belong to the class-level annual matrix, which is a different screen.

### Filter endpoints

None of these exist on the new model — `GradebookController` currently has only `batch` and
`explain`. The toolbar needs classes, subjects for a class, periods for a scheme, and students.
Small read-only endpoints, scoped by the caller's permission grants.

---

## 3. Publication

### The legacy mechanism, and why it cannot be carried forward

A "close" is an event row — `ClosedPeriod(academyClassId, gradePrefix, lastUpdateTime)`. Parent
queries take the newest one for the class and filter:

```java
.and(qGrade.createTime.before(latest))     // GradeRepositoryCustomImpl:419, 453, 468, 483
```

"Everything before X." But `insertStudentGrade` updates the row **in place**:

```java
existing.setValue(grade.getValue());       // GradeServiceImpl:82
gradeRepository.save(existing);
```

`createTime` never moves. So a mark entered in October, published in October and then edited in
November still satisfies `createTime < cutoff`, and **parents see the new value immediately** —
no publication, no change request, no director. Setting a value to null is worse: the row is
deleted outright (`GradeServiceImpl:79`) and the grade simply vanishes for parents.

The only guard is a UI check, and it lives on the *behaviour* page. On the trimester screen the
change-request modal is wired but nothing opens it. On the screen where marks are actually
entered, there is no lock at all.

A timestamp cut-off compared against a mutable row cannot work. The row changes underneath it.

### What replaces it

Publication becomes **per cell and explicit**. `grade_entry` gains three columns:

```
published_value            DECIMAL(6,2)  NULL
published_special_value    NVARCHAR(16)  NULL
published_at               DATETIME2     NULL
```

* Teachers read and write `value`. Parents read `published_value`.
* Publishing a scope copies the former into the latter and stamps `published_at`.
* **Locked** is `published_at is not null` — a column read, not a subquery against an event table.
* **Changed since publication** is `value <> published_value`. The state that needs a director is
  now representable instead of invisible.
* Clearing a mark sets `value` to null; `published_value` stands until republished. Deletes stop
  leaking.

Cost is roughly 20 bytes a row — about 19 MB a year at our volumes.

### The one semantic that matters here

**The lock blocks direct user edits. It never blocks recomputation.**

Recompute writes `value` only, never `published_value`. So if an input changes, a published derived
cell recalculates on the working side and simply diverges from what parents were shown — which is
precisely the "changed since publication" state that phase 3's change-request flow exists to
resolve. Nothing needs to decide policy in phase 2, and nothing silently reaches parents.

---

## 4. Write path enforcement

`GradeWriteService` already rejects per cell rather than failing the batch — a contested mark must
not discard a teacher's other twenty. Publication joins that path.

`CellConflict` gains a reason so the client can say something useful:

| Reason             | Meaning                                             |
|--------------------|-----------------------------------------------------|
| `VERSION_CONFLICT` | Someone else changed this cell first (exists today) |
| `PUBLISHED`        | Published; changing it needs a change request       |
| `NOT_EDITABLE`     | Derived column with `allowOverride = false`         |

`NOT_EDITABLE` currently throws and fails the whole batch (`GradeWriteService:146`). That was
defensible when it could only be a client bug, but once columns come from live configuration a
template edit can make it a legitimate race. Demoting it to a per-cell rejection keeps the other
cells safe.

**The invariant ships with the write path it protects.** Phase 3 adds the publish action and the
change-request flow, but the rule that a published cell cannot be edited directly lands here, so
the hole cannot reopen in between.

---

## 5. Data

The `sgs` tables are empty. Integration tests build their own fixtures; there is nothing for a real
grid to render. Phase 2 needs:

* **`db/003_migrate_from_dbo.sql`** — students, classes, subjects and enrollments across from the
  legacy `dbo` tables. Grades are *not* migrated: this year's data is wiped before the new version
  goes live (decision 10), and the legacy `GradeType` taxonomy does not map onto components anyway.
* **A template seeder** for the trimester grid — structure only. The real weights are still unknown
  and stay unknown until the school says; guessing them in a seeder would be worse than leaving
  them to be entered once (this is why phase 1 shipped without one).

---

## 6. The frontend

### Grid component: MUI X DataGrid stays

Weighed against a hand-built table, on the requirement that it look as polished as it does now and
keep pagination, grouping and the rest.

Two objections raised against DataGrid during design were **wrong**, and the decision turns on that:

* **Enter already moves down a column.** `useGridCellEditing.js` sets `cellToFocusAfter = 'below'`
  on Enter, `'right'` on Tab, `'left'` on shift-Tab. The spreadsheet behaviour teachers want is
  built in.
* **`processRowUpdate` does not fight batching.** It only fights it if a network call is awaited
  inside it — which is exactly what the current code does. Made local and synchronous it becomes
  the ideal autosave trigger.

One correction in the other direction: `experimentalFeatures={{columnGrouping: true}}` is **not**
leftover from v5. In the installed 6.9.2 the flag is load-bearing —
`useGridColumnGrouping.js:41` returns early without it. It works; it keeps the flag.

**What we lose:** column pinning is Pro-only, so the student-name column cannot freeze. Irrelevant
for a 12-column entry grid where nothing scrolls sideways; it will matter on the wide annual matrix
and gets addressed there. Range selection and fill-down are also out — no Excel paste is needed.

**What we gain:** no visual regression, and pagination, sorting, grouping, keyboard navigation,
overlays and footer all keep working. Rebuilding those by hand is weeks whose best possible outcome
is parity.

### Wrapper fixes — `main/components/grid/DataGrid.js`

```js
rowBuffer={5000}                  // renders every row  → virtualization off
columnBuffer={columns.length}     // renders every col  → virtualization off
paginationMode={"server"}         // …with client-side rows passed in
pageSize={5000}
```

Virtualization is disabled outright. At 25 × 12 that is harmless — rendering was never the
bottleneck, the network was — but the annual matrix is ~1,400 cells and this is why it crawls.
Configuration, not architecture.

Two further things about the wrapper, found while working in it:

* `sx` arrived through `{...props}` **after** `sx={DataGridStyles(colorGroups)}`, so any page
  passing its own `sx` silently replaced the base styles rather than adding to them. Now merged.
* `pageSize`, `rowsPerPageOptions`, `onPageChange` and `onPageSizeChange` **do not exist in v6** —
  they became `paginationModel` and `pageSizeOptions`. The wrapper still passes the v5 names, so
  they are inert and every grid actually runs on the default page size. It works because
  `paginationMode="server"` makes DataGrid render the rows it is given without slicing. Left alone
  for now: fixing it changes behaviour on all thirteen legacy pages, and it is invisible at these
  row counts. Worth doing when those pages are ported.

Community DataGrid also forces `pagination: true`, so `pagination={false}` is silently ignored —
`fullyHideFooter` is what actually keeps the pager off the entry grid.

### Autosave

Teachers are used to the value sticking when they leave a cell, and that stays. What changes is
that leaving a cell is no longer a transaction.

```
processRowUpdate  →  update local state, mark cell dirty, return newRow   (synchronous)
                     ↓
              debounce 800ms
                     ↓
        POST /grades/batch  with every dirty cell
                     ↓
     apply returned `derived`, clear dirty, surface conflicts
```

* Flush also on grid blur, filter change, navigation and `beforeunload`.
* Edits made while a flush is in flight accumulate into the **next** batch; they are never merged
  into the request already on the wire.
* A dirty/saving/saved indicator, with an unsaved count.

Entering marks for a class goes from 25 posts plus 25 full-grid refetches to roughly one request.

### Derived cells

When a flush goes out, every derived cell reachable from a dirty input — via `dependents` from the
read payload — dims to a "recalculating" state and is filled from `GradeWriteResult.derived`.

No evaluator in JavaScript. The dimming is cosmetic; the numbers are always the server's. At an
800 ms debounce the round trip lands while the teacher is typing the next cell.

### Cell states

| State              | Rendering               | Behaviour                               |
|--------------------|-------------------------|-----------------------------------------|
| Manual             | plain                   | editable                                |
| Derived            | subtly tinted           | editable if `allowOverride`             |
| Overridden         | tinted + corner marker  | context menu → **revert to calculated** |
| Recalculating      | dimmed                  | not editable while in flight            |
| Conflict           | outlined                | keep-mine / take-theirs                 |
| Locked (published) | greyed, lock affordance | read-only, reason shown                 |
| Special value      | `ჩთ` etc.               | from `specialValues`                    |

Overrides are sticky (decision 23): held through recomputes, still feeding downstream, released
only by an explicit revert. Locked rendering is built here and stays inert until phase 3 publishes
anything.

Both the revert and the explain hang off a right-click. v6 has no `onCellContextMenu` event and
`onCellClick` never fires for a right-click, so the handler sits on the container and resolves the
target from the cell's `data-field` and the row's `data-id`. Double-click was the obvious place for
explain and is the wrong one: it is how DataGrid enters edit mode.

### Explain

Right-clicking a derived cell offers **"როგორ გამოითვალა?"**, opening a side panel with the working from
`GET /api/gradebook/grades/explain` — which terms fed in, which were skipped and why, and the
weights after renormalisation. The same `evaluate()` call in trace mode, never a second
implementation.

---

## 7. What the legacy data turned out to be

Found while writing the migration, and worth recording because the old schema
actively misleads on all three.

**`academy_class.class_level` is the school, not the grade.** It holds 1/2/3 —
primary, basic, secondary. The real grade is the leading integer of
`class_name`: `5ა`, `7ტ1`, `10ჰ2`. All 47 rows parse.

**`dbo.subject` is really "subject taught by teacher X".** It carries a free-text
`teacher` column, so `ინგლისური ენა` appears 16 times and `ქართული ლიტერატურა`

12. **143 rows fold to 51 distinct subjects.** The new model separates the
    subject from who teaches it, so the migration folds them and remaps
    `class_subject`.

The teacher moves onto **`class_subject.teacher_name`** as part of the same
pass, because folding is what destroys the association and migration is a
one-way door. It is filled on 142 of 143 rows as `პედაგოგი: <name>`, and all
**658 class/subject pairs have exactly one teacher**, so nothing needs
reconciling — but only once folded first.

It stays a name rather than a reference: **3 of the 98 teacher names match a
`system_user_table` row**. Most teachers have no login, the accounts that do
exist are spelled in Latin while these are Georgian, and some entries name two
co-teachers (`ღვინიაშვილი სოფო / ოთხმეზური არჩილ`). `TeachingAssignment`
requires a `system_user_id` and stays empty until they have accounts; the name
is what the grid header shows, as the old page did.

**Eight usernames are duplicated and seven students have no class.** Nothing
enforced either in the legacy schema; the new one enforces both. Duplicates are
suffixed with the legacy id (a login nobody can use is recoverable, a deleted
student is not) and the unenrolled students migrate without an enrollment.

### The collation bug

The database collation is `SQL_Latin1_General_CP1_CI_AS`, which has **no code
page for Georgian**. Every string column Hibernate generated was `varchar`:

```
varchar  = [??????????]
nvarchar = [მათემატიკა]
```

The write succeeds and nothing errors — the text is simply gone. All 37 string
columns in `sgs` were affected. Phase 1's tests inserted Georgian but asserted
only on numbers, so it passed straight through them.

Fixed with `hibernate.use_nationalized_character_data`, plus `db/005_nvarchar.sql`
for schemas already created (12 of the columns were indexed, so the constraints
are rebuilt around the change). `GradeGridServiceIT` now asserts Georgian
survives the round trip rather than assuming it.

---

## 8. Open items

* **Routing.** Real URLs replacing the tab-host shell were agreed in principle (decision 20) but
  are **not attributed to a phase**. Deferred from phase 2: the tab shell carries all thirteen
  existing pages, and swapping it while replacing the grade grid doubles the blast radius for no
  user-visible gain.
* **Publication scope** — class-wide across every subject as legacy does it, or per subject.
  Answered in phase 3.
* **Cells blank at publication** — a missed test sat later, inside an already-published period.
  Under §3 the cell is unlocked and the teacher simply enters it, invisible to parents until the
  next publish. Whether that is right, or whether any edit inside a published period needs the
  director, is a phase 3 question.
* **Column widths** are not in the template model. The legacy page hardcodes them per column. For
  now they are derived from label length and `decimals`; a `width` field on `GradeComponent` is
  cheap to add if the school wants control in phase 5.
