# Parent views — putting the school's five doors back

**The problem in one line:** the rewrite collapsed five parent screens into one,
and a parent who used to have five labelled doors now has three and a picker.

The legacy parent console had five boxes, and the brief describes the same five
because the brief was describing what already existed:

| Legacy box                                                       | Route                  | Component          |
|------------------------------------------------------------------|------------------------|--------------------|
| მოსწავლის ტრიმესტრული შეფასება აკადემიური დისციპლინების მიხედვით | `/grades/:subjectName` | `Discipline`       |
| მოსწავლის შემაჯამებელი ტრიმესტრული შეფასება                      | `/trimester`           | `MonthlyGrade`     |
| მოსწავლის შეფასება ეთიკური ნორმების მიხედვით                     | `/ethicalPage`         | `EthicPage`        |
| მოსწავლის ტრიმესტრული და წლიური შეფასება                         | `/annual`              | `TsliuriShefaseba` |
| მოსწავლის მიერ გაცდენილი საათები                                 | `/absence-page`        | `AbsencePage`      |

**Three of those five are the same journal.** Per-subject detail, summary across
subjects, and the year view are the trimester journal at different settings —
which is why the rewrite drew them with one page and one box.

That collapse was right and is not being undone. `JournalPage` says why in its
own comment: *"drilling into a single subject is not a different page. It is this
page with one row, and it turns into cards on its own."* One row renders as
`RowCards`, many as `RowTable`. All five screens already work today.

What was lost is only the **entry points**. A parent used to arrive at the one
they wanted; now they arrive at a journal and have to find it.

---

## 1. What a view actually is

Everything that separates the five is already a value the system holds:

| Legacy box            | journal   | period tier | subjects | chart          |
|-----------------------|-----------|-------------|----------|----------------|
| per-subject trimester | trimester | ROLLUP      | one      | `GRADE_TREND`  |
| summary trimester     | trimester | ROLLUP      | all      | *(new)*        |
| ethical norms         | ethics    | ROLLUP      | all      | —              |
| trimester and annual  | trimester | YEAR        | all      | —              |
| hours missed          | absence   | YEAR        | all      | `ABSENCE_BARS` |

So a **parent view is a named entry point into `JournalPage`** — a journal, a
period tier, whether one subject or all, a chart, and the words the school puts
on the button. Nothing else.

No screen is built for this. Every one of the five already renders.

---

## 2. Why this is a table and not five literals

The obvious cheap answer is five entries in `AfterLoginPage`. It is the wrong
one, and the file itself records why: the five boxes it replaced *were*
hardcoded, and the first linked to `/grades/<subject NAME>`, so the page it
opened had to refetch every subject and match on a string. A rename broke it.

Putting five literals back re-creates that. Worse, it re-couples the console to
the school's journal set at exactly the moment the rewrite finished decoupling
them: the point of `parentVisible` is that ticking a box adds a parent screen
with nothing deployed.

**The rows go in the database. The editor screen does not get built yet.**

That is a deliberate middle, and it is the one this project already uses for
editorial decisions — `component.summary_column` is a migration (db/034), not a
field in the journal editor; `db/030` flips the absence register parent-visible
with an UPDATE. Changing a label is a one-line migration by a developer, which
is what it has been for years and what nobody has asked to change.

What the table buys over literals, even with no UI:

* the console stops knowing there are five — it renders the rows it is given;
* a renamed journal does not orphan a box, because the box holds a uuid and
  carries its own label;
* the demo database and production can differ without a branch;
* a sixth is an `INSERT`;
* when the school does ask to edit them, the editor is a form over rows that
  exist, and **no screen is rebuilt** — all seven boxes already lead to the same
  `JournalPage`.

---

## 3. The table

`sgs.parent_menu_item`, in `db/035_parent_menu.sql`:

```sql
CREATE TABLE sgs.parent_menu_item (
    id            bigint        NOT NULL PRIMARY KEY,
    template_id   bigint        NOT NULL,  -- the journal
    ordinal       int           NOT NULL,
    label         nvarchar(200) NOT NULL,  -- the words on the tile
    period_kind   nvarchar(20)  NULL,      -- REPORTING | ROLLUP | YEAR; null = the journal's own
    subject_mode  nvarchar(10)  NOT NULL,  -- ONE | ALL
    chart_key     nvarchar(40)  NULL,      -- overrides the journal's, null = none
    created_at    datetime2     NOT NULL,
    updated_at    datetime2     NOT NULL
);
```

**A menu item in code, a view in prose.** `ParentView` was already taken - it is
a journal's marks as a parent sees them, built by `ParentViewService`. A second
class of that name shadowed it and broke the compile, which was the name telling
the truth that these are two different things. So the entity is `ParentMenuItem`,
the wire shape is `ParentLandingView`, and the endpoint is
`GET /api/parent/views`.

`chart_key` is on the view rather than inherited, because the three trimester
views want three different answers: a trend for one subject, bars across
subjects, and nothing at all for the annual table. The journal's own `chartKey`
stays where it is and is what a journal with no views falls back to.

`period_kind` is nullable so a single-tier journal needs no opinion. It reuses
the enum db/033 established, where the kind names **the tier a column lives on**
rather than being a YEAR-or-not binary — which is what makes "the annual view"
expressible at all.

### Seeding

Five rows, matched **by journal shape rather than by name**, for the reason
db/029 and db/030 both give: `grading_template` has no unique constraint on name
and the console permits renames. The trimester journal is the `COMPONENTS` one,
absence is the first `MONTH` + `PERIODS`, ethics the second.

A journal with no rows keeps today's behaviour — one box, the journal's name,
its own chart. Nothing regresses for a journal the school invents later.

---

## 4. Labels

The legacy wording is kept, minus one word. Every label begins `მოსწავლის` —
"the student's" — which is redundant on a console that shows one child and puts
their name in the header, and costs a third of the longest tile:

|                                                                    | characters |
|--------------------------------------------------------------------|------------|
| `მოსწავლის ტრიმესტრული შეფასება აკადემიური დისციპლინების მიხედვით` | 62         |
| `ტრიმესტრული შეფასება აკადემიური დისციპლინების მიხედვით`           | 52         |

Row 2 of the grid is three tiles across, so each is a third of the width; at 62
characters the longest wraps to four or five lines and drags the row's height up
after it. The full wording still heads the page the tile opens.

This is a value in the seed, so restoring the long form is an UPDATE if the
school prefers it.

---

## 5. The landing grid

Seven boxes on a six-column track. Six columns rather than three so that a row
of two can split evenly instead of leaving a third empty.

Three across then two only divides for exactly five, so any other count falls
back to halves - even at any number, and never a hole. A sixth seeded journal
changes the rhythm rather than breaking the row.

```
news (span 4)                    homework (span 2)
დისციპლინების მიხ. (2)   შემაჯამებელი (2)   ტრიმ. და წლიური (2)
ეთიკური ნორმები (span 3)             გაცდენილი საათები (span 3)
```

The rows are not an arrangement of seven equal things — they group by what the
things are:

* **row 1** the two content modules;
* **row 2** one journal seen three ways;
* **row 3** the two monthly class-wide registers — ethics and absence are both
  `MONTH`, both non-subject-scoped, both `PERIODS`.

This reorders the legacy set slightly: ethics moves from third to fourth so the
three trimester views sit together. The school sees its own five labels in a
shape that matches what they are.

On a phone it collapses to one column in the same order, which is what
`primaryLanding.css` already does.

**Primary is unchanged.** It keeps its own grid; it draws the absence view and
no other, exactly as it draws the absence journal today.

---

## 5b. What ONE and ALL actually mean

`subject_mode` turned out to be the wrong name for the right idea. It decides
**how many rows**, and what a row is comes from the journal:

|                   | ONE                             | ALL                                |
|-------------------|---------------------------------|------------------------------------|
| rows are subjects | one subject, every column of it | every subject, the summary columns |
| rows are periods  | one period, with a picker       | every period at once               |

That single axis produces all four academic screens:

* **per-subject trimester** — ONE subject, so twelve cards for one subject;
* **summary trimester** — ALL subjects, so the one column marked for a summary,
  which is the legacy subject-and-mark table;
* **trimester and annual** — ALL at the year, so summary columns reaching one
  tier down: three trimester marks beside the four year ones;
* **ethics** — ONE period, so a month picker and a single figure, which is the
  brief's "month's mark" tile.

The absence register is ALL over periods, which is the whole year on one screen,
and is unchanged.

**The summary is the same rule at every tier.** `component.summary_column`
(db/034) says which columns are the headline; a view showing every row shows
those, at the tier being looked at plus, for a period above them, one column per
period below. Nothing here special-cases the year.

### The chart names its column

`chart_column` on the menu item, because no ordering of the columns gets both
summaries right: the trimester summary wants the trimester assessment, which is
its last column, and the annual table wants the final academic grade, which is
not. Which mark is the headline is an editorial choice, so it is stated. Null
leaves the chart to pick the last numeric column, which is what a journal nobody
has configured a button for still gets.

---

## 6. What has to be built

Most of this is wiring, because the screens exist.

|                                                                  |                            |
|------------------------------------------------------------------|----------------------------|
| migration + 5 seeded rows                                        | small                      |
| entity, repository, service, one endpoint                        | small                      |
| landing page renders views instead of journals                   | small                      |
| `JournalPage` reads period and subject mode off the query string | small                      |
| `SUBJECT_BARS` chart                                             | **the only new component** |

`GRADE_TREND` plots one subject across periods; the summary screenshot is a bar
per subject for one trimester, which is a different picture. The chart registry
is built for exactly this — its comment says adding one is "a file plus one
line", keyed by a stable name rather than a uuid so it is the same in every
environment.

### Scoping

Views inherit the school rule already in `ParentViewService.journals`: a view
whose journal a child's school does not get is not offered. Primary gets
registers only, so it keeps the absence view.

---

## 7. Open

* **Ethics reaches primary.** The register filter is `gridMode == PERIODS` and
  ethics is a `PERIODS` journal, so a primary child is offered it — the landing
  grid simply does not draw it. That predates this change and is the same
  question as TEST-RESULTS §3.A: whether a journal's class assignment should
  restrict who sees it or only pin a version. Unresolved, and not made worse
  here.
* **No editor.** Deliberate, per §2. The trigger for building one is the school
  asking to change a label, not a developer deciding they might.
* **`subject_mode = ONE`** lands on the first subject and lets the parent change
  it. Whether it should remember the last one they looked at is unasked.
