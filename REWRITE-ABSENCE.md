# Phase 10 — the two absence registers

Daily absence (students × school days of a month) and monthly absence (students × months
of the year). They started as two journals sharing one mechanism. They are now two
different mechanisms, which is the substance of this document.

---

## 1. Why they were split

A journal cell carries a value. A tick is not a value.

Storing "absent" as the number `1` meant "present" had to be the absence of a number — a
blank. And a blank cell in a journal already meant something else: *a mark nobody has
entered yet*. One representation, two meanings, and no way for any reader to tell them
apart.

Every serious defect in the register came out of that. Publishing had to decide whether a
blank was an assertion or a gap. The write path had to decide whether writing into a blank
was creating a mark or amending a published one. The notifier had to ask "is any cell on
this day greater than zero" to answer "is this child still marked absent". Each of those
answered the question its own way, and each was individually defensible.

The attempted fix made it worse: publishing a register would write a row for every student
and every day, so the blank became explicit and the lock had something to fire on. That is
`publishes_blanks`, and the code implementing it inserted duplicates and violated
`uq_grade_cell` on the second publish of any period.

**In `sgs.daily_absence` a row means absent and no row means present.** There is no third
state, so nothing has to interpret one.

## 2. What the daily register is now

```sql
daily_absence (id, enrollment_id, absence_date, marked_at, marked_by)
  unique (enrollment_id, absence_date)
```

What is deliberately not there:

* **No value, scale or special value.** Nothing to validate, round, or reject as out of
  range. The `NaN`-deletes-the-cell bug is unrepresentable rather than fixed.
* **No row version.** Marking is insert-or-delete and therefore idempotent — two
  coordinators marking the same child converge, instead of one of them losing an optimistic
  lock conflict over a boolean.
* **No publication columns.** The daily register is a staff working document. What reaches
  a parent is the email, the same day.
* **No period.** A date. This is what took the register out of the period tree: "days
  absent in March" is a range query, not a three-level descent past trimesters that hold
  nothing.

`marked_at` and `marked_by` are the only record of who did what. With the director's
approval gone, nothing else is — and one timestamp and one user id are cheap enough that
going without would be a choice rather than a saving.

**The day periods are gone with it.** `db/021` replaced the unused week level with 217
dated days for this; nothing else ever used depth 3, so they are unused again, exactly like
the weeks they replaced. The register's columns are now the weekdays between a month's own
two dates, computed in Java — where the weekend filter cannot depend on a session's
`DATEFIRST` setting, which was a real defect in the script that generated them.

The yearly "days absent" total was a `DERIVED` column with a `SUM` rule over a
`DESCENDANTS` term, recomputed and persisted on every mark. It is `COUNT(*)` over an index.

## 3. What the monthly register still is

A journal, in every respect, and rightly:

* it holds a real typed number (academic hours missed),
* it has a yearly total,
* it is genuinely published — it is the parent-facing half, and the source of the brief's
  green-to-red diagram.

It keeps `GridMode.PERIODS` (students down, months across), the transposed read, and the
two `ClassPeriodSetting` values — total academic hours for the month, and the permitted
number missed.

The two registers stay **independent**, as the school confirmed: one counts days and one
counts hours, and converting between them needs an hours-per-day figure nobody has. That
independence is what made the split clean — there is no rollup crossing the seam.

## 4. Publishing without freezing

`grading_template.locks_on_publish` replaces `publishes_blanks`.

They had been one idea. Publishing a journal released its cells to parents *and* made them
read-only, so a later edit needed the director. For grades that is the whole point.

For the register it is wrong, and only the workflow shows it: missed hours accumulate
through a month. The coordinator publishes, more hours are missed, they publish again.
That is the ordinary path, not an exception to it — so a lock would put an approval
between them and every top-up, dozens of times a month, for a number nobody disputes.

Change requests are not removed; grades still use them. They are simply never reached by a
journal that does not lock, which is what makes wiring approval back in a matter of
flipping this flag.

Publication itself is unchanged in meaning: parents see the published figure and nothing
newer until it is published again.

### What the school asked for, and what changed

They asked for the director's approval on absence, and the reason given was that
publication sends email so the director should see a change to something parents were
already told. That reason did not hold: **the email fires when a cell is marked, not when
it is published**, so approval sat on a path that never reached a parent anyway. Told that,
the school was not firm about it — they had not been to begin with.

## 5. Notifying parents

Unchanged in behaviour, simpler underneath. **Fifteen minutes, coalesced, and re-read
before sending.** Marking autosaves in about a second, so sending immediately would mean a
mis-click tells a parent their child was absent with no unsend.

Marking queues a notice; a scheduled job takes anything past the window and re-reads before
sending — so a correction always wins whenever it lands, rather than depending on beating a
timer. Several absences on one day collapse into one message.

The re-read used to be a query over `grade_entry` loading every cell on a day period and
asking whether any held a number above zero. It is now `does a row exist`.

`absence_notice` loses `period_id`: the enrollment and the date are the whole key.

## 6. One owner for period reach

`PeriodRef.DESCENDANTS` spans any distance, where `CHILDREN` is exactly one level. The
monthly register needs it — the year is two levels above the months, with trimesters in
between that hold no absence at all.

`DESCENDANTS` was never the problem. **Six services each decided independently which
periods a change touches**, in six inline expressions that had to agree and were never
checked against one another. When `DESCENDANTS` arrived only four learned it, so:

* the write path's working set loaded a neighbourhood, and a yearly total was computed from
  one month of data and stored over the year — silently, with nothing failing;
* the explain trace still loaded the old set, so explaining a yearly total reported every
  source `EMPTY` — an explanation contradicting the number it explained.

`PeriodReach` now owns all five questions, which are genuinely different:

|              |                                                                    |
|--------------|--------------------------------------------------------------------|
| `sources`    | evaluating a cell, where its inputs are read from                  |
| `dependents` | a cell changed; which periods hold rollups that read it            |
| `workingSet` | everything to load first, so no rule sees a fraction of its inputs |
| `subtree`    | a period and everything beneath it — what publication releases     |
| `atDepth`    | one level beneath a period — the columns of a transposed grid      |

`dependents` is the one that had been wrong most often: "one level up" is the inverse of
`CHILDREN` alone, and applying it to `DESCENDANTS` persisted a yearly total onto a month, a
level that column does not live at.

`PeriodReachTest` asserts the property none of the six copies could express: **if
evaluating X reads P, then changing P must recompute X.**

`PeriodTree.maxDepth()` replaces a `MAX_DEPTH = 3` constant that existed in two services.
It was written when depth 3 held days; days are gone, and a hardcoded 3 would still be
looking for them.

## 7. Tests

194 green — 73 unit, 121 integration.

* **The daily register:** a month's school days as columns with no weekend; a mark against
  its own date; marking twice changing nothing; clearing removing the row rather than
  blanking it; a weekend refused; a date outside the academic year refused; an enrollment
  from another class ignored; days absent over a range as a count.
* **The monthly register:** months found two levels under the year; a period at or below
  the journal's level refused rather than drawn empty; the two settings round-tripping, and
  a null clearing one rather than storing a null.
* **Publication:** a published month still amendable (the lock is off); publishing writing
  no rows for months nobody filled in; publishing the same period twice not being an error
  — the case the old `materialiseBlanks` broke outright.
* **The notification window:** nothing sent inside it; one notice per student per day; a
  withdrawn mark cancelled with no mail sent at all; a standing mark sent; a mail failure
  leaving the notice pending for retry; a cancelled notice not blocking a real absence later
  the same day; a student with no address cancelled rather than recorded as told.
* **The reach:** each ref resolving as specified and in calendar order, the subtree,
  `maxDepth` measured from the tree, the working set widening only for a graph that needs
  it, a YEAR column narrowed to the root, and the inverse property above - asserted in
  **both** directions, so a reach that resolves to a level nothing reads from fails too.
* **Scope:** the change-request queue narrowed to the caller own classes.
* **The guards:** a future date refused; a change request refused on a journal that does not
  lock; each month carrying its own two figures rather than one pair for the year.
* **`DescendantRollupTest`** still pins a three-level reach. No seeded journal needs one now
  that daily absence has left the tree, but the engine supports it and something should say
  so.

Still not covered, and worth knowing: `db/028`'s migration of existing marks is verified by
running it, not by a test.

## 8. Migrations

|                            |                                                                                                                                                                                                                                  |
|----------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `028_daily_absence.sql`    | creates `daily_absence`, migrates the marks out of `grade_entry`, retires the daily journal and its rollup, deletes the depth-3 day periods, drops `absence_notice.period_id`. Refuses if anything still hangs off a day period. |
| `029_publication_lock.sql` | adds `locks_on_publish`, sets the monthly register to `0`, drops `publishes_blanks`.                                                                                                                                             |

`027_publishes_blanks.sql` is **deleted**, not superseded — it only ever existed to support
the mechanism this removed, and it never shipped.

---

## 9. What this left open

The **daily register has no audit trail** beyond `marked_at` / `marked_by`. Dropping the
director's approval means nothing records that a mark changed, only who touched it last.
Decision 14 already defers `grade_entry_history`; this is the same call, made explicitly
rather than by omission.

The **parent side is phase 11** and unchanged by any of this. The monthly register publishes
and writes its snapshot now, because retrofitting that into a term of accumulated data is
the alternative.
