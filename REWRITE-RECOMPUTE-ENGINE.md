# Rewrite — the recompute engine

Companion to `REWRITE-DATA-MODEL.md`. Draft for discussion; nothing committed.

This is the piece everything else leans on. Derived values are **materialised** — stored as rows,
never computed on read — so the engine is what keeps them true. It is also the only part of the
system that can silently produce wrong report cards, which is why it gets the tests.

---


> **Out of date since phase 10.** The absence rollup described below as a `CHILDREN` term is
> now `DESCENDANTS` (`db/023`), and the daily one no longer exists at all — daily absence
> left `grade_entry` for its own table and its yearly total is a `COUNT(*)` over a date
> range. The reach itself is owned by `PeriodReach`; see REWRITE-ABSENCE.md §6.

## 1. Cells

The unit of work is a **cell**:

```
cell = (enrollment, subject | NULL, period, component)
```

`subject` is NULL for components that aren't subject-scoped — ethics, absence, and any student-wide
aggregate such as *rating*.

A cell is `MANUAL` (typed by a person), `DERIVED` (written by the engine), or derived-but-overridden
(`is_override = 1`), which the engine treats as manual for writing and as derived for reading.

---

## 2. The dependency graph

Edges run **source component → dependent component**, read off `derivation_term` /
`derivation_source`. The graph belongs to a `template_version` and is immutable for that version, so
it is built once and cached.

**Validated when a template version is saved, not at runtime:**

| Check                                                                    | Failure                                               |
|--------------------------------------------------------------------------|-------------------------------------------------------|
| No cycles (Kahn / DFS)                                                   | reject with the cycle path                            |
| Every `derivation_source.component_id` exists in this version            | reject, naming the dangling ref                       |
| A term's `period_ref` is reachable from the component's own period level | reject                                                |
| `ALL_SUBJECTS` terms only on non-subject-scoped components               | reject                                                |
| Weights sum to 1.0                                                       | **warn**, don't reject — bonus schemes are legitimate |

Because validation happens at save time, evaluation at runtime has no error path for bad
configuration. That's deliberate: a teacher entering marks must never see a config error.

### Fan-out has three axes

A changed cell propagates along three independent axes, and this is the part that's easy to get
wrong:

| Axis          | Trigger                                                | Example                                                     |
|---------------|--------------------------------------------------------|-------------------------------------------------------------|
| **Component** | dependents in the same `(enrollment, subject, period)` | `ONGOING_3` → `ONGOING_AVG` → `TRIMESTER_GRADE`             |
| **Period**    | terms with `period_ref = CHILDREN`                     | `T1.TRIMESTER_GRADE` → `YEAR.ANNUAL`                        |
| **Subject**   | terms with `source_kind = ALL_SUBJECTS`                | any subject's `TRIMESTER_GRADE` → the subject-less `RATING` |

---

## 3. Recompute

```
recompute(changed: Set<Cell>) -> Set<Cell>:
    affected = {}
    frontier = changed
    while frontier:
        c = frontier.pop()
        for D in graph.dependentsOf(c.component):
            for t in resolveTargets(c, D):        # applies the period + subject mapping
                if t not in affected:
                    affected.add(t); frontier.push(t)

    for cell in topoSort(affected):               # see ordering below
        if cell.is_override: continue             # value stands, but still propagates
        v = evaluate(cell)
        if v != current(cell): write(cell, v)
    return written
```

**Ordering.** A plain component-level topological sort isn't enough, because `YEAR.ANNUAL` depends
on `T1/T2/T3.TRIMESTER_GRADE` — the same component at a deeper period. Sort the affected *cells*,
keyed by `(period depth descending, component topological index ascending)`: leaf periods resolve
before the rollups that consume them.

**Writes are conditional.** Only write when the value actually changes. This keeps `updated_at`
meaningful and stops a single edit rewriting half a class.

---

## 4. Evaluating one cell

```
evaluate(cell):
    rule = cell.component.rule
    terms = []
    for term in rule.terms:
        values  = resolveSources(term, cell)          # [] when nothing is present
        reduced = reduce(term.reduce, values, rule.null_policy)
        terms.append((reduced, term.weight))
    raw = aggregate(rule.type, terms, rule)
    return round(raw, rule.decimals, rule.rounding_mode)
```

**Rounding happens once, at the end.** Intermediates are carried at full precision. This matters:
rounding each step compounds error, and the legacy code did it inconsistently — `Math.round` in
some paths, `RoundingMode.HALF_UP` at varying scales in others.

### Blanks and special values

Two distinct concepts, deliberately kept separate:

* **Absent** — no row at all. Governed by `rule.null_policy`: `IGNORE` (drop it), `AS_ZERO`, or
  `BLOCK` (result becomes null).
* **Special** — a stored `special_value` such as `ჩთ`. Each special code carries its own
  behaviour on the template: `EXCLUDE`, `AS_ZERO`, or `BLOCK`.

`BLOCK` propagates: a blocked cell is null, so anything downstream sees an absent value and applies
its own `null_policy`.

### Weight renormalisation

When a term contributes nothing under `IGNORE` and `renormalize_weights` is on, drop the term and
rescale the remaining weights to their original total. Without this, a missing 30% final test
quietly caps a student at 70% of scale — which is the kind of bug nobody notices until report cards
go out.

### Overrides

An overridden cell is **not** recomputed, but it **is** read by everything downstream. It behaves as
an input from that point on. Clearing the override re-derives it and cascades.

---

## 5. The write path

```
POST /api/grades/batch
{ classGroupId, subjectId, periodId,
  entries: [ { enrollmentId, componentCode, value | specialValue, expectedVersion } ] }
```

1. Load the active `template_version` for `(class, subject, scope)`; reject unknown component codes.
2. Check the period isn't published — unless the caller holds `EDIT_PUBLISHED_GRADES`.
3. Optimistic check per entry against `expectedVersion`; mismatches are collected, not fatal.
4. `MERGE` the surviving manual entries.
5. `recompute()` over them.
6. Commit.
7. Return `{ applied, derived, conflicts }`.

```json
{ "applied":  [{ "enrollmentId": 41, "componentCode": "ONGOING_3", "value": 7, "version": 12 }],
  "derived":  [{ "enrollmentId": 41, "componentCode": "TRIMESTER_GRADE", "value": 6.4 },
               { "enrollmentId": 41, "componentCode": "RATING", "value": 6 }],
  "conflicts":[{ "enrollmentId": 55, "componentCode": "ONGOING_3",
                 "yours": 8, "theirs": 7, "theirVersion": 9 }] }
```

Returning `derived` is what lets the client patch its cache instead of refetching — the single
change that removes the current full-grid reload after every cell.

**The engine is the only write path.** No repository `save()` anywhere else may touch
`grade_entry`, or materialised values drift out of true. Worth enforcing with an architecture test.

### Conflicts

Optimistic, per cell, never per batch. Cells are independent, so a conflict only exists when two
people edit the *same* cell — rare, but silently overwriting a colleague's mark is exactly the bug
that ends trust in a gradebook. Rejected cells come back with the other value; the rest still apply.

Derived cells carry no version check — the server owns them.

---

## 6. Bulk recompute

Triggered by: activating a template version, adding a subject to a class, enrolling a student,
clearing an override, or an explicit *"recalculate class"*.

* Scope is `(class_group, period subtree, subject set)`.
* Runs as a background job, chunked per student, with progress.
* **Idempotent** — safe to re-run, since it only writes changed values.
* Never runs implicitly on read.

### Config changes mid-year — nothing recalculates on its own

**Decided: existing marks are never silently recalculated.** Activating a new template version
applies to periods that have not started; it does not reach back into ones that have.

The mechanism is that **a period stays on the version its marks were first entered under**. Every
`grade_entry` stores its `template_version_id`, and the write path resolves the version from the
marks already in that period rather than from the current assignment. So correcting an October mark
in February recomputes it under **October's** rules — which is the case that would otherwise have
gone wrong quietly, since the obvious implementation reaches for whatever version is active today.

Two consequences worth knowing:

* A period whose marks have gone out to parents cannot move underneath them.
* A period containing *two* versions is treated as an error, not reconciled. That state can only
  arise from a migration that stopped half way, and recomputing against either version would
  corrupt the marks belonging to the other.

Moving an existing period onto a newer version is therefore **a deliberate action, not a side
effect** — the template editor offers it as *"apply to periods already under way?"* with the two
outcomes spelled out:

| Answer           | Effect                                                                                                               |
|------------------|----------------------------------------------------------------------------------------------------------------------|
| **No** (default) | New rules apply to periods not yet started. Existing marks keep their values and their version.                      |
| **Yes**          | The chosen periods are migrated to the new version and recomputed as a background job, with a count of what changed. |

Components that no longer exist leave orphan rows — kept, not deleted, and filtered out of reads by
version. Deleting them would silently destroy data on a config change.

Because a cell records the version that produced it, the explain view can also say *which* rules
were used, so a value computed under a superseded version is inspectable rather than mysterious.

---

## 7. Performance envelope

Concrete numbers for the trimester template (7 ongoing + initial + progress + final + trimester,
plus annual/overall and rating):

| Action                                                | Derived cells written                                                  |
|-------------------------------------------------------|------------------------------------------------------------------------|
| One cell edit                                         | ~4 — `ONGOING_AVG → TRIMESTER_GRADE → ANNUAL → OVERALL`, plus `RATING` |
| One student row flushed (14 cells)                    | ~5 (the closure is shared)                                             |
| Whole class, one subject, one trimester (25 students) | ~125                                                                   |
| Whole class, all 25 subjects, one trimester           | ~3,100                                                                 |
| Full-year rebuild for a class                         | ~10,000                                                                |

Against ≈1.6M rows/year on an indexed table, all of these are sub-second except the last, which is
the background job. The closure is small because grading hierarchies are shallow — three or four
levels — and that is what makes materialising derived values affordable in the first place.

---

## 8. Failure behaviour

| Situation                                         | Behaviour                                                                                       |
|---------------------------------------------------|-------------------------------------------------------------------------------------------------|
| A rule can't produce a value (`BLOCK`, no inputs) | cell becomes null — **not** an error                                                            |
| A source component is missing from the version    | impossible after save-time validation; if it happens, fail the transaction naming the component |
| Recompute closure partly fails                    | whole transaction rolls back — never a half-consistent grid                                     |
| Cycle detected at runtime                         | impossible by construction; assert and fail loudly if ever hit                                  |

The distinction that matters: **missing data is normal and yields null; bad configuration is a bug
and fails loudly.** Conflating them is how you end up with zeros in report cards where a teacher
simply hadn't entered a mark yet.

---

## 9. Testing

The engine is pure logic over an in-memory graph with no database, so it tests cheaply. Cases worth
writing, drawn from the six real legacy rules:

1. Weighted sum, all inputs present
2. Weighted sum with a missing term — with and without renormalisation
3. Fallback chain: summary = avg(S1,S2) → avg(one, resit) → resit alone → null
4. Average ignoring blanks vs. counting them as zero
5. Special value `ჩთ` under each of `EXCLUDE` / `AS_ZERO` / `BLOCK`
6. Cross-period rollup, including a missing trimester
7. `ALL_SUBJECTS` rating with subjects that have no marks
8. Override held through a recompute, and released
9. Rounding: once at the end, not per step
10. Cycle and dangling-ref rejection at template save
11. Idempotence — recompute twice, second pass writes nothing

Plus integration tests on the batch endpoint for the publish lock and the conflict path.

---

## 10. Explaining a derived value

Decided: build it.

Because the engine already holds the full working set while evaluating, an explanation costs
nothing extra — it is the *same* evaluation run in trace mode:

```
GET /api/grades/explain?enrollmentId=&subjectId=&periodId=&componentCode=
```

```json
{ "component": "TRIMESTER_GRADE", "value": 6.4,
  "rule": "0.50 × საშუალო(მიმდინარე 1–7) + 0.20 × საწყისი ტესტი + 0.30 × ფინალური ტესტი",
  "terms": [
    { "label": "საშუალო(მიმდინარე 1–7)", "weight": 0.50, "reduced": 6.8,
      "used":     [{ "ONGOING_1": 7 }, { "ONGOING_2": 6 }, { "ONGOING_4": 8 }, { "ONGOING_5": 6 }],
      "excluded": [{ "ONGOING_3": "ცარიელი" }, { "ONGOING_6": "ცარიელი" },
                   { "ONGOING_7": "ჩთ — გამორიცხულია" }] },
    { "label": "საწყისი ტესტი", "weight": 0.20, "reduced": 4 },
    { "label": "ფინალური ტესტი", "weight": 0.30, "reduced": 7 }
  ],
  "raw": 6.42, "rounding": "HALF_UP, 1 decimal" }
```

Two rules for this to be worth having:

**It must run through the same code path as the calculation.** An explanation produced by separate
code is a lie waiting to happen — it will drift, and it will be believed. Trace mode is a flag on
`evaluate()`, not a second implementation.

**Nothing is stored.** It is recomputed on request, so it can never go stale against the value it
explains.

In the UI: click a derived cell → popover with the rule in Georgian, the inputs it used, and the
ones it skipped with the reason. That last part — *"it used five of your seven marks because two
are empty and one is ჩთ"* — is what stops the "the system calculated it wrong" tickets.

---

## 11. Supported calculation set

Confirmed scope: **the calculations that exist today, plus those in the client brief.** Nothing
speculative. Checked against both:

| Source | Rule                                                                              | Covered by               |
|--------|-----------------------------------------------------------------------------------|--------------------------|
| legacy | Summary = avg(S1,S2); else avg(one, resit); else resit alone                      | fallback chain           |
| legacy | Monthly = 50% summary + 25% homework + 25% classwork                              | `WEIGHTED_SUM`           |
| legacy | Semester = (diagnostics avg + month avg) / 2, month avg alone when no diagnostics | `AVERAGE` + `IGNORE`     |
| legacy | Annual = avg(sem1, sem2), blended with final exam only if present                 | `AVERAGE` + `IGNORE`     |
| legacy | Behaviour week average = mean of the 5 criteria                                   | `GROUP` + `AVERAGE`      |
| legacy | Rating = average across **all subjects**                                          | `ALL_SUBJECTS`           |
| brief  | Trimester assessment from ongoing / initial / progress / final                    | `WEIGHTED_SUM` + `GROUP` |
| brief  | Annual academic assessment = f(T1, T2, T3)                                        | `period_ref = CHILDREN`  |
| brief  | Overall academic assessment = f(annual, final exam)                               | `WEIGHTED_SUM`           |
| brief  | Academic project assessment                                                       | plain input              |
| brief  | Ethics: month → trimester → year                                                  | `CHILDREN` + `AVERAGE`   |
| brief  | Absence: month → trimester → year                                                 | `CHILDREN` + `SUM`       |

All of it fits. The engine is not over-built for the known requirement.

### Cost of adding something later

Worth knowing which future additions are cheap and which aren't:

* **A new `reduce` or rule `type`** — e.g. `AVERAGE_DROP_LOWEST(n)`, `MEDIAN`, `WEIGHTED_BY_PERIOD_LENGTH`
  — is an enum value, a function, and a dropdown entry. No schema change, no migration. Genuinely
  cheap, exactly as you assumed.
* **A new control-flow shape** — e.g. *"use the final exam if it beats the trimester average"* — is
  a conditional rather than an aggregation, so it needs a new node kind in the rule model. Still
  contained (additive tables, existing rules unaffected), but a day rather than an hour.

Neither is being built now.

---

## 12. Open

Nothing blocking. The engine scope is settled, and both extension paths above are additive if the
school later produces a rule that doesn't fit.
