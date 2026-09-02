# Phase 3 — publication and change requests

The document teachers work in becomes the document parents read, and stops being
freely editable at that moment.

This is the flow the school actually depends on, described by the client as: teachers
work on the journal privately, publish it to parents in a batch, and after that any
change goes through a request the director signs off. Phase 2 built the lock; this
phase builds the two actions on either side of it.

Read `REWRITE-GRADE-ENTRY.md` §3 first — the per-cell publication columns and why the
legacy timestamp cut-off could not be carried forward.

---

## 1. What phase 3 delivers

|                       |                                                                                                                                          |
|-----------------------|------------------------------------------------------------------------------------------------------------------------------------------|
| **Publish**           | Release a (class, period) to parents, optionally narrowed to one subject. Logged as an event. Emails guardians outside the transaction.  |
| **Change requests**   | Raise, list, approve and reject a change to a published cell. Approval writes the value **and** republishes everything downstream of it. |
| **Parent visibility** | Reads come from `published_value`, so parents see the last published snapshot and never work in progress.                                |

Not here: the parent console itself (phase 6), the template editor (phase 5), exports
(phase 4).

---

## 2. What the legacy flow does, and what carries forward

`createClosedPeriod` takes a list of class ids and writes close events for `GENERAL`,
`BEHAVIOUR` and `TRANSIT` at once. There is no period parameter, because the mechanism
is a timestamp: publishing means *everything entered up to now is now visible*. It is
scoped to the publisher's own `academyClassList`.

Three things about it do not carry forward.

**Change-request creation is broken.** In `ChangeRequestServiceImpl`:

```java
Grade prevGrade = gradeRepository.findById(changeRequest.getId()).orElseThrow(); //TODO incorrect code
```

It looks up a `Grade` by the `ChangeRequest`'s id. The `TODO` is in the original. With
the modal also unreachable from the trimester page, the creation path is dead in
practice — the queue only ever fills from the behaviour page.

**Both endpoints require `MANAGE_CHANGE_REQUESTS`, including create.** A teacher who
may enter grades but not approve changes cannot raise a request at all. Requester and
approver are the same permission, which inverts the flow the school described.

**Emails are sent inline, one per student, inside the publish loop** — around 900
synchronous sends for a full publish, in the request thread, inside the transaction.
An SMTP timeout can therefore fail a publish that has already happened.

What does carry forward is the shape: a batch release, an audit list of releases, and a
director's queue with an explanation on both sides.

---

## 3. Publish

```
POST /api/gradebook/publish
    { classGroupId, periodId, subjectId? }
```

**Scope is (class, period), with subject optional.** The normal action is "publish
trimester I for 9A" across every subject. The subject filter exists for when one
teacher is late and the rest should not be held back.

Legacy had neither parameter — period was implicit in the timestamp and subject was
never a unit at all. Both are explicit now because publication is per cell.

### What it does

1. Load every `grade_entry` in scope.
2. Copy `value` → `published_value` and `special_value` → `published_special_value`.
3. Stamp `published_at`.
4. Write one `publication` row recording the event.
5. **After the transaction commits**, email the guardians of students in scope.

Cells with no value are skipped: publishing a blank would freeze it as published and
lock a cell that never held anything.

### The publication log

```
publication(id, class_group_id, period_id, subject_id?, published_at,
            published_by, cell_count)
```

The per-cell columns stay the mechanism. This is the audit trail — who released what
and when — and it is what `ClosePeriodDashBoard` lists today. Deriving that list by
scanning `grade_entry` for distinct timestamps would be both slower and lossy, since a
republish overwrites the previous stamp.

### Republishing

Publishing the same scope again is normal, not an error. It picks up marks entered
since, and pushes recomputed values whose inputs have moved. It is the ordinary way a
period stays current between change requests.

### No unpublish

Retracting a grade parents have already seen is worse than correcting it forward, and a
retraction has no story to tell the parent who saw the old number. A mistaken publish is
fixed by a change request like anything else. Trivially added later if the school asks.

### Email

Moved out of the transaction and off the request thread. A failed send must never roll
back a publish: whether marks are visible cannot depend on an SMTP server answering.
Sends are logged, and failure is reported rather than retried silently.

---

## 4. Change requests

### The model

```
grade_change_request(
    id,
    grade_entry_id      -> sgs.grade_entry     the cell, not a copy of its coordinates
    previous_value, previous_special_value     what parents were shown, captured at raise time
    requested_value, requested_special_value
    status              PENDING | APPROVED | REJECTED
    reason                                     the teacher's explanation
    decision_comment                           the director's, emailed to the guardian
    requested_by, requested_at,
    decided_by,   decided_at)
```

A foreign key to `grade_entry`, because the cell always exists — it is published, so it
has a row. That is what the legacy design got wrong: it stored a `Grade` reference
resolved from the wrong id, and duplicated `prevValue` with nothing tying the two
together.

`previous_value` is still stored rather than read back at approval time. It is what the
*requester saw*, and if it no longer matches when the director decides, something moved
underneath the request and the director should be told rather than silently overruled.

**One open request per cell**, enforced by a filtered unique index:

```sql
CREATE UNIQUE INDEX uq_open_change_request ON sgs.grade_change_request (grade_entry_id)
    WHERE status = 'PENDING';
```

A constraint rather than a check-then-insert, because two teachers submitting at once
is exactly when the check would pass twice.

### Permissions

| Action           | Permission                                   |
|------------------|----------------------------------------------|
| Raise a request  | `ADD_GRADES` — a teacher must be able to ask |
| List the queue   | `VIEW_CHANGE_REQUESTS`                       |
| Approve / reject | `MANAGE_CHANGE_REQUESTS`                     |

Splitting create away from manage is the fix for the legacy inversion.

### Approval republishes the dependency closure

The part that matters.

Approving a change to `ONGOING_3` writes the new value, and the engine recomputes
`ONGOING_AVG` and `TRIMESTER_GRADE` on the working side as usual. If approval published
only the cell that was asked about, parents would see marks of 7, 8 and 9 beside an
average that matches none of them.

So approval publishes **the changed cell and every published cell downstream of it**.
The dependency graph already knows which those are.

Downstream cells that were *not* published are left alone — they are not yet visible, and
publishing them here would release them ahead of their period.

Rejection changes nothing at all. The working value stays as it was; the teacher is told.

---

## 5. Parent reads

Everything a parent sees comes from `published_value` and `published_special_value`. A
cell with `published_at` null is invisible to them, whatever its working value.

This replaces `qGrade.createTime.before(latest)` in the four legacy queries, which did
not hold: grades are updated in place, so `createTime` never moved and post-publication
edits reached parents immediately. See `REWRITE-GRADE-ENTRY.md` §3.

---

## 6. Open

* **A mark entered after publication, where filling it changes an already-published
  grade.** Built permissively — see `CLIENT-QUESTIONS.md` §1. If the school wants the
  strict reading it is one rule in the write path plus a way to request a cell that has
  no value yet; the dependency graph already knows what is downstream.
* **Publishing a period that is still empty** is allowed and does nothing. Whether the
  UI should warn is cosmetic.
* **Behaviour and absence** are not published by this endpoint. Legacy released
  `GENERAL`, `BEHAVIOUR` and `TRANSIT` together; those live outside the grade template
  and are picked up when their own screens are rewritten.
