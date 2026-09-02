# Phase 4 — exports

Four Excel exports and one Word export, rebuilt as **two shapes parameterised by the
template**, rather than five methods with the columns written into them.

---

## 1. Why they cannot be reproduced literally

Decision 6 says the existing exports are kept. That cannot mean column for column,
because three of the five are built around periods the new model does not have.
Decision 2 dropped the monthly and semester machinery; the period tree is
`YEAR → T1 / T2 / T3`.

* **Monthly** takes a `createDate` and titles itself with a month name. There is no
  monthly period.
* **Semester** lays out months `9, 11, 12` for the first semester and `1, 3, 4, 5`
  for the second, plus semester, creativity, behaviour and absence columns.
* **Annual** lays out semester I, semester II, final exam, annual, rating.

What survives is their *shape*. Everything the five do is one of two layouts:

| Legacy export        | Shape                    | Parameters                     |
|----------------------|--------------------------|--------------------------------|
| Monthly              | **Matrix**               | one component, one period      |
| Semester             | **Detail**               | one subject, one period        |
| Annual               | **Matrix** at year level | one component per child period |
| Dashboard            | **Detail**               | one subject, one period        |
| Word semester report | **Detail**, paginated    | one subject, one period        |

* **Matrix** — rows are students, columns are subjects, each cell one component's value.
* **Detail** — rows are students, columns are the template's components for a subject.

Both take their columns from the template version in force, so a school that adds a
column gets it in the export without a deployment. The page being replaced hardcoded
eleven; the monthly and annual screens hardcoded every subject name as well.

---

## 2. What the legacy code got wrong

Worth recording, because two of these are live defects rather than design choices.

### Subject order has never worked

Column order comes from `ExcelUtils.subjectPattern`, a hardcoded list of 39 Georgian
subject names. Anything not in it sorts to the end.

Checked against the live data: **20 of 51 subjects do not match.** Five of those are
entries the list is actively trying to cover —

```
pattern: "ალგებრა / გეომეტრია"        data: ალგებრა  გეომეტრია
pattern: "რობოტიკა / ინჟინერია"       data: რობოტიკა  ინჟინერია
pattern: "ისტორია / ჩვენი საქართველო"  data: ისტორია  ჩვენი საქართველო
```

**No row in `dbo.subject` contains a slash at all.** The list has never matched those,
so every export drops a fifth of the subjects at the end in arbitrary order, and the
order differs between classes.

There is also `ალგებრა  გეომეტრია` and `ალგებრა გეომეტრია` — the same subject twice,
differing by one space.

Order moves to `class_subject.sort_index`, which is data. The migration filled it with
`ROW_NUMBER() OVER (ORDER BY subject_id)` — arbitrary — so it is reseeded from the
pattern where the names match and alphabetically otherwise.

### Decimals were truncated

```java
return String.valueOf(val.longValue());
```

`6.7` exports as `6`. But this turns out to be the right answer reached the wrong way:
of 853 grades in the live data only **13 are fractional, and every one is a behaviour
average or a percentage**. No `TRIMESTER_*` value has ever carried a decimal, and
`7.00` alone accounts for 633 of them — the IB 1–7 scale.

So academic grades are whole numbers, and the fix belongs in the template rather than
the export: `decimals = 0` on the component means the engine rounds once when it
calculates and every surface shows the same number (decision 25). Rounding in an
export would make the spreadsheet disagree with the screen.

`db/007_seed_template.sql` now sets `TRIMESTER_GRADE` and `ANNUAL` to 0 decimals.
`ONGOING_AVG` and `RATING` keep theirs — they are explicitly averages, not marks.

### Smaller ones

* `list.get(0)` — an empty class throws `IndexOutOfBoundsException`.
* The column set comes from the **first student's** subject list, so students with
  different subjects misalign.
* `for (i = 0; i <= sheet.getRow(1).getLastCellNum(); i++)` autosizes one column past
  the end.
* The Excel base path is literally `/test`.
* `/export/semester-word` has no `@Secured` annotation at all.
* Values of `0` render as blank, so a genuine zero is indistinguishable from no mark.

---

## 3. The scale shift

```java
isDecimalSystem && !rating && !behaviour && !absence
    ? val.add(new BigDecimal(3)).longValue()
    : val.longValue()
```

An **IB 7-point → Georgian 10-point conversion**: 7 + 3 = 10. Rating, behaviour and
absence are excluded because they are not on the academic scale.

Phase 4 replaced the boolean with `component.output_offset`, a per-column decimal
applied when printing.

**Phase 7 supersedes that** — see `REWRITE-CONVERSION.md`. The school grades out of 7 and
must report to the government out of 10, they are moving to a 9-point scale, and the
mapping is not settled — so it becomes **one configurable formula for the school**, not a
number on a column. `output_offset` is dropped. Exports apply the formula when the caller
ticks the box, and no longer round what it produced.

Three live values sit outside 1–7 (`TRIMESTER_FINAL_EXAM_GRADE = 9`,
`TRIMESTER_ONGOING_GRADE_2 = 9`, `TRIMESTER_GRADE = 8`) — either entry errors or a
class already grading on 10. The seeded template allows 0–10 so they would pass today.

---

## 4. Working values, not published

Exports read `value`, not `published_value`.

An export is a staff document produced from the journal on screen, and handing back a
spreadsheet that disagrees with what the person is looking at is worse than handing
back one that is ahead of what parents can see. Staff-facing, behind `MANAGE_GRADES`.

---

## 5. Bulk export

**Answered** (previously `CLIENT-QUESTIONS.md` §2): the school picks a **trimester** and
a **year**, and every export shape gets a bulk variant. The output is a **zip**.

There is no semester anywhere in this. The school corrected the wording — trimesters are
the only reporting periods, which is what decision 2 already assumed.

### Scope

Every class the caller is scoped to, via `ClassScopeGuard`. An unrestricted user gets
the school; a coordinator gets their class, and the same button works for both without
a second endpoint.

The zip is foldered by class. The matrix shape yields one workbook per class (~15). The
detail shape is per class *per subject*, so roughly 15 × 12 ≈ 180 workbooks.

### Synchronous, and measured

The zip streams as it is built, so memory stays flat regardless of how many workbooks go
into it.

Generation runs on the request rather than as a background job. 180 small workbooks is a
few seconds of POI plus the query time, and a job queue — a table, a polling endpoint, a
progress UI, a retention policy — is a lot of machinery to build against an estimate.
The honest order is to build the simple thing, measure it, and add the job only if the
measurement asks for it. If it exceeds ~30s, it gets one.

---

## 6. Open

* Whether the `+3` mapping is confirmed or merely inferred.
* Whether `scale_max` should be 7 rather than 10, which would flag the three
  out-of-range values.
* The annual export's **წლიური** column has never been populated —
  `adjustMonthNamesForAnual` names key 5, `getAnualGrades` only ever writes keys 1–4.
  Blank in every annual export the school has run. Worth telling them.
