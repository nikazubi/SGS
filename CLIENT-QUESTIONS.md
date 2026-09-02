# Questions for the client

Open questions that only the school can answer. Each records what we have built in
the meantime, so the cost of a different answer is visible before it is asked.

**This file is added to on request only.** Questions are not moved here because they
seem worth asking — they go here when we decide to put them to the school.

---

## 1. A mark entered after the period was published

**Status:** built the permissive way. May need redoing.

### The situation

A student misses the final test and sits it in December. By then the trimester has
already been published to parents, and that student's cell for the final test was
empty when it was.

Nothing that parents have seen is being changed — the cell was blank. So the teacher
simply enters the mark, and it reaches parents at the next publication.

But entering it moves the trimester assessment, and **that** number parents have
already seen. So the next publication changes a published grade on the strength of a
mark that never passed the director.

### The question

> When a teacher enters a mark that was blank at the time of publication, and doing
> so changes a grade parents have already been shown — should that need the
> director's approval, or is it ordinary catch-up work?

### The two answers

|                                | What happens                                                                                             | Cost                                                                                                                      |
|--------------------------------|----------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------|
| **Permissive** — what we built | The teacher enters the mark. Everything it affects is republished at the next publication. No request.   | A published grade can change without the director seeing it, if the cause was a blank cell being filled.                  |
| **Strict**                     | Filling the blank is allowed, but anything it changes that was already published needs a change request. | Every late test, medical absence or transfer-in generates director requests — routine catch-up becomes an approval queue. |

### What we did

The permissive reading. A cell is locked only if it was itself published
(`published_at is not null`); a blank cell was not, so it stays editable, and the
recomputed values it affects are released at the next publication.

### If the answer is "strict"

The change is contained. It is one rule in the write path — reject an edit when any
**published** cell downstream of it would move — plus a way to raise a request for a
cell that does not yet have a value. The dependency graph already knows what is
downstream, so nothing structural moves. Roughly a day, not a redesign.

---

## 2. What should the yearly bulk export contain?

**Status:** not built. Waiting on the answer before designing it.

### The situation

Every year the school archives grades by running an Excel export and copying the
file to their own storage. Today that means going class by class: each export
takes **one class** and produces one `IB_Mthiebi_<class>.xlsx`, so 47 classes is
47 downloads.

We want one button that does the whole thing. What we do not know is *which*
export they are actually collecting, and the five are quite different.

### The question

> Which Excel export do you download each year for your records, and what do you
> need in it — the summary grade per subject, or the full detail behind it?
>
> And should the archive be split per class (as now), or would per subject or
> per student be more useful to you?

### What exists today, so the answer can name one

| Export          | Scope                         | What one sheet holds                                                                                                                                                 |
|-----------------|-------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Monthly**     | one class                     | Row per student, **one column per subject**, that month's mark. Last row lists each subject's teacher.                                                               |
| **Semester**    | one class                     | Two-tier header: each **subject merged across its month columns**, month names beneath — plus semester, creativity, behaviour and absence columns. The detailed one. |
| **Annual**      | one class                     | Same shape; per subject the columns are semester I, semester II, final exam, annual, rating.                                                                         |
| **Dashboard**   | one class **and one subject** | The monthly journal for that subject: summary tasks, restoration, month mark, %, school work 1–7.                                                                    |
| **Word report** | one class                     | Landscape A4, four subjects per page, month columns plus სემესტრული and შემოქმედობითობა.                                                                             |

Most likely **Semester** or **Annual**, since those are the ones holding a full
year of detail — but it is worth confirming rather than assuming.

**Also worth asking:** in the current code all four Excel hooks live on a page
that is commented out of the menu, and the Word route is commented out of the
parent console. In this version none of them are reachable, so either the school
is on an older deployment or they have stopped using them. Confirm they still
can before we rebuild what they no longer use.

### What we did

Nothing yet. The single-class exports are being rewritten in phase 4 regardless,
since they are the same report parameterised differently. The bulk button is
deliberately on hold: building it before knowing which export it should batch
risks batching the wrong one 47 times.

---

## 3. Is the ethics journal one mark per period, or the weekly detail?

**Status:** built as one mark per period, in a demo seed rather than a shipped one.

### The situation

The brief's table for **მოსწავლის შეფასება ეთიკური ნორმების მიხედვით** is:

| # | Student | Sept-Oct | Nov | **Trim I** | Dec | Jan-Feb | Mar | **Trim II** | Apr | May | **Trim III** | **Year** |
|---|---------|----------|-----|------------|-----|---------|-----|-------------|-----|-----|--------------|----------|

One value per reporting period, with trimester and year roll-ups.

The system the school uses today does something much larger: **five criteria,
assessed weekly**, up to six entries a month each, with a monthly figure per
criterion, a weekly average and an overall monthly mark. Around 35 `BEHAVIOUR_*`
grade types.

The brief's own note flags the gap: either the weekly detail is being dropped, or
the brief is only showing the summary view.

### The question

> Does the ethics journal keep the five criteria assessed weekly, or does it
> become one mark per reporting period as the brief's table shows?

### Why it is not a small difference

The period tree stops at reporting periods. Weeks existed once, were replaced by
dated school days, and those were removed when daily absence moved out of the
journal model. **A weekly journal has no level to hang its columns off**, so
answering "weekly" means restoring a level to the period tree - a decision about
every journal, not only this one - before the ethics journal can be configured at
all.

Answering "one per period" costs nothing: it is already built.

### What we did

Seeded it as the brief shows - one input per reporting period, a trimester
average, a year average - and deliberately kept it in `db/demo/` rather than the
shipped migration chain, so that a guess does not look like a decision. If the
answer is "as the brief shows", promoting it is a rename.

### Also worth asking at the same time

The five criteria themselves. If the school is content with one mark, are the
criteria (uniform, lateness, care of classroom equipment, hygiene, conduct) gone
entirely, or does the coordinator still want somewhere to record them?

---

## 4. How is the ethics year mark worked out?

**Status:** built one way. The other is a one-line change, and the difference is small.

### The situation

The trimester figure is settled: the plain average of its reporting periods,
unweighted - `(Sept-Oct + Nov) / 2`. Sept-Oct is two calendar months and November
is one, and it is **not** weighted for that. A period is a period.

The year has the same choice one level up, and it has not been answered:

|                               | Formula                              | For 10, 10, 10, 10, 10, 4, 4 |
|-------------------------------|--------------------------------------|------------------------------|
| **By period** - what we built | the seven reporting periods averaged | **8.29**                     |
| **By trimester**              | the three trimester figures averaged | **8.00**                     |

They differ because the trimesters are not equal: Trimester I has two reporting
periods, Trimester II has three, Trimester III has two. Averaging the trimesters
makes a period in Trimester II count about a fifth less than one in Trimester I.

### The question

> Is the ethics year mark the average of the seven reporting periods, or the
> average of the three trimester marks?

### How much it matters

Not much: at most about **0.3 on a 0-10 scale**, and only for a child whose marks
swing between trimesters. It is an informational mark rather than one reported to
the government.

Worth asking anyway because it is cheaper to answer than to change: journals fork
a version when a formula is edited, and marks already computed stay pinned to the
version they were computed under - so a change made after a term of data may only
affect what is entered afterwards.

### What we did

Averaged the seven reporting periods, which is the reading consistent with the
trimester rule the school has confirmed - each period counting once.

The same question exists for the absence register and does not matter there:
summing seven reporting periods and summing three trimester totals give the same
number.
