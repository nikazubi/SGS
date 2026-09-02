# Phase 5 — journals as data

The school has run many journals over the years and changes them often. So the unit of
configuration stops being "the columns inside a fixed set of screens" and becomes **the
journal itself**: created from the UI, named by the user, appearing in the menu on its
own, with its own columns, formulas and frequency.

This supersedes the earlier plan (a template editor for three hardcoded scopes). It is
the same engine underneath — phase 1 modelled components, rules, terms and sources as
data already — but `TemplateScope` stops being an enum in Java.

---

## 1. What a journal is

|                     |                                                                                                                                                   |
|---------------------|---------------------------------------------------------------------------------------------------------------------------------------------------|
| **Identity**        | A `bigint` primary key for storage, plus a **UUID** as the stable external key. The name is the menu label and can change; nothing references it. |
| **Name**            | Becomes the menu entry and the tab title.                                                                                                         |
| **Frequency**       | How often it is filled in — once a year, per trimester, per month, per week.                                                                      |
| **Shape**           | One grid per subject, or one grid for the whole class.                                                                                            |
| **Columns**         | Names, grouping, scale, decimals, and per column whether it is typed or calculated.                                                               |
| **Formulas**        | Per calculated column, referencing columns in this journal **or any other**.                                                                      |
| **Editable anyway** | Per column, independent of whether it is calculated. Defaults to true.                                                                            |

Creating a journal adds a menu entry. Nothing else does — adding a column or changing a
formula changes what the existing grid shows, which is what phase 2 was built for.

### Why identity is a UUID and not the name

The name is the menu label, so a school renaming a journal is routine. If the name were
the identity, every formula referencing it, every assignment and every stored grade
would break. The `bigint` key stays for storage — phase 1 chose sequences over identity
columns because that is what keeps JDBC insert batching available — and the UUID is what
the API and the URLs use.

---

## 2. Frequency, and why periods survive

Periods were nearly cut. The argument for cutting them is good: with no periods, a
trimester journal is simply 36 columns instead of 12, rollups become ordinary formulas
(`ANNUAL = (T1 + T2 + T3) / 3`), and the question of *which month a trimester maps to*
disappears entirely.

They survive on one number. The ethics journal is 5 criteria × up to 6 weeks plus a
monthly figure, worked one month at a time — **~279 columns** across a year if flattened.
The alternative, one journal per month, means configuring nine near-identical journals
every year, which is worse than what it replaces.

So a period is exactly one thing: *the same columns again, for a different slice of
time*. The wizard never says "period scheme". It asks:

> **How often is this filled in?** — once a year · per trimester · per month · per week

**Once a year** is the default and gives what a plain table would: one grid, no period
dropdown anywhere. The others generate the repetition and the teacher picks the
occurrence, as today.

Frequency maps to depth in the period tree, which is seeded consistently:

```
depth 0   YEAR         ONCE_A_YEAR
depth 1   T1 T2 T3     TRIMESTER
depth 2   months       MONTH
depth 3   (unused)        WEEK
```

---

## 3. Cross-journal references

A formula may read a column in another journal. The picker is **journal → column**, and
a plain mirror is the trivial case of the same mechanism: one term, weight 1.

### Alignment is asked, never inferred

An earlier draft aligned periods by date overlap, so a trimester would find the months
inside it. That was machinery invented to solve a problem periods created. Dropped.

* **Same frequency on both sides** → the same occurrence. Ethics-October reads
  Academic-October. Nothing to ask.
* **Different frequency** → the picker asks which occurrence, explicitly. A third
  dropdown appears.

That is `PeriodRef.SPECIFIC`, already in the model and already handled by the evaluator.
No date arithmetic, no ragged-edge tie-breaks, and the user says what they mean rather
than the system guessing.

Subjects align the same way: a class-wide journal read from a per-subject one resolves
to the subject-less cell; a per-subject journal read from a class-wide one averages
across subjects, which `ALL_SUBJECTS` already does.

### What this costs in the engine

The largest change since phase 1, because today a formula can only see its own version.

1. **Working set** — the grid loads one journal's cells for a period. A cross-journal
   formula needs the other journal's cells too, in the shape `loadComponentsAcrossSubjects`
   already uses for rating.
2. **Recompute fan-out** — saving an ethics mark must now recompute an academic column.
   A fourth axis alongside component, period and subject.
3. **Cycle detection** — `TemplateGraph` validates one version. A → B → A has to be
   caught at save time, so the graph spans journals.
4. **Version resolution** — a cross-journal source names a component; which *version* of
   the other journal applies is resolved the same way a grid resolves its own, so a
   pinned period keeps reading the rules its marks were entered under.

---

## 4. Editing and versions

Unchanged from the earlier design, and it matters more here, not less: a school that
changes journals often is a school that will change one mid-year.

**A version that has data is never edited in place.** No `grade_entry` rows referencing
it means edit directly; otherwise editing forks a `DRAFT`, which is activated when
saved. The word "version" never appears in the UI unless a period is pinned and the user
is being asked about recalculating.

`TemplateVersionStatus.LOCKED` — *"referenced by a publication, so its shape can no
longer change"* — is declared and has never been set by anything. Publishing sets it now.

### Activation and migration are different

* **Activate** — future periods use the new version. Existing periods stay pinned.
  Nothing recalculates.
* **Migrate a period** — deliberate, and always recalculates.

The prompt says what it will do: *"412 cells across 3 subjects will be recalculated, and
24 marks in 2 removed columns will be deleted."* Answering no leaves the period on the
old version. It never moves a period and keeps numbers the new rules did not produce.

Both scopes are offered: one (class, period), and **all open periods at once** — the
useful and dangerous one, so it previews first. The preview is the migration with the
write suppressed, so it cannot disagree with what happens.

Overrides survive migration; they are sticky by decision 23.

---

## 5. The API

The editor posts a whole version rather than issuing per-column CRUD, because that is
what a wizard and a spreadsheet-shaped editor both produce, and it lets validation see
the entire structure at once.

```
GET    /api/gradebook/journals                     the menu, and the index page
POST   /api/gradebook/journals                     the wizard
GET    /api/gradebook/journals/{uuid}
PUT    /api/gradebook/journals/{uuid}              rename, reorder, archive
GET    /api/gradebook/journals/{uuid}/versions/current
PUT    /api/gradebook/journals/{uuid}/versions/{id}   the whole structure; validates
POST   /api/gradebook/journals/{uuid}/versions/{id}/activate
GET    /api/gradebook/journals/columns             every journal and column, for the picker
POST   /api/gradebook/journals/migrate/preview     what would change
POST   /api/gradebook/journals/migrate             apply it
```

Behind a new `MANAGE_TEMPLATES` permission. Decision 3 put configurability behind one,
and reusing `MANAGE_GRADES` would let everyone who enters marks change how they are
calculated.

---

## 6. What this replaces

`TemplateScope` (`ACADEMIC | ETHICS | ABSENCE`) was the three legacy journals as an enum,
and `TemplateVersionResolver` hardcoded `ACADEMIC`, so the machinery served academic
grades only. Both go: a journal is a row, and the resolver takes the journal it is
resolving for.

This also closes the roadmap gap — the ethics and absence journals had no phase and were
never going to be ported as code. They become two rows and their columns.

---

## 7. Open

* Whether the school computes across journals today or only views them side by side.
  The legacy annual page displays rating, behaviour and absence together; no arithmetic
  mixing them has been found. The reference is being built because it is wanted, not to
  reproduce something.
* Whether the ethics journal keeps its 5-criteria weekly detail or collapses to one
  value per month — still open with the client, and now a configuration answer rather
  than a code one.
