# Roster management — students, classes, subjects, teachers

**The problem in one line:** you can create a student, and the system that matters
cannot see it.

The surviving admin pages write `dbo.students`, `dbo.academy_class`,
`dbo.subject`. Everything the rewrite built reads `sgs.student`,
`sgs.class_group`, `sgs.subject`. `db/006` copied the roster across once and
nothing has copied it since, so a child added through the console today lands in
the legacy tables alone — invisible to the gradebook, the registers, homework
targeting and the parent portal.

The decision taken: **the `sgs` model stays as it is and the screens are rebuilt
to fit it.** Not a bridge, not a sync job. This document is what that means.

---

## 1. What the model already decides for us

Every entity exists. Nothing here needs a migration; what is missing is write
paths and screens. But the model is not neutral about how those screens look —
four constraints shape them before any design begins.

**A student is enrolled in a class *for a year*.** Not "in" a class.
`student → enrollment → class_group`, and the year sits on the enrollment.

**One enrollment per student per year** — `uq_enrollment_student_year`. This is
the sharpest one. It means "which class is this child in?" is only answerable
once you say which year, and it means a mid-year move is an *edit* to the
existing enrollment rather than a second row.

> **DECIDED: history is kept, and the constraint stays.** The school wants to
> know a child was in 5ა until December. The obvious way to give them that -
> a second enrollment row per move - was rejected, because enrollment is the
> spine: five tables key on it (`grade_entry`, `daily_absence`, `homework_seen`,
> `post_target`, `absence_notice`) across 27 Java files. Splitting a child's year
> across two enrollments splits their marks with it, and `ANNUAL = average of the
> trimesters` is computed per enrollment - a transferred child would get two half
> years and no annual mark at all. The absence yearly total has the same problem,
> and `enrollmentOf(studentId)` in the parent portal would start choosing between
> two rows.
>
> Instead the history sits **beside** the enrollment:
>
> ```
> enrollment_placement (enrollment_id, class_group_id, from_date, to_date)
> ```
>
> One row per stretch in a class, the current one with `to_date` null.
> `enrollment.class_group_id` stays as the current class, so **every existing
> query keeps working untouched** and the change is purely additive. A move is
> one transaction: close the open placement, open a new one, update the pointer.
>
> The cost, named so it is not forgotten: *current class* now lives in two
> places. They can only be allowed to disagree if something writes one without
> the other, so the move exists as a single service method and nothing else sets
> `class_group_id`.
>
> **Assumed, not answered:** a child who moves mid-trimester appears in the grid
> of the class they were in **at the end of the period**. Say so if the school
> prints registers differently.

**Identity is a pair, not a name.** `uq_student_personal_number` (filtered, so
blanks are allowed) and `uq_student_login` on `(username, password_hash)`.
Duplicate usernames are fine; duplicate passwords are fine; the combination is
not. Any student form has to enforce this, and has to explain it — a teacher
typing a second `beridze` should be told the pair must differ, not that the name
is taken.

**Names are unique within their scope.** A class name is unique per
`(academic_year, school)`; a subject name is globally unique.

---

## 2. Three screens

Fewer than the legacy five, because two of the legacy pages were compensating
for a model that had no `class_subject` and no enrollment.

Every screen carries **one year selector**, defaulting to the current year.
That is what makes the enrollment model tolerable in a UI: pick the year once at
the top, and everything below reads as it does today.

### Students

The roster. Filter by school, class, and active; search by name or personal
number.

The form is the legacy one plus honesty about the model: first name, last name,
personal number, username, password, guardian email, active — **and a class**,
which is not a field on the student at all but the enrollment for the selected
year. One picker, labelled "class", writing an `enrollment` row behind it. The
common case ("put this child in 5ა") stays one action; the model stays intact.

Leaving the school sets `left_on`. Moving class is a different action and a
different button: it closes the open `enrollment_placement`, opens a new one and
repoints the enrollment, so the child keeps one continuous year of marks and the
register still knows where they sat in October.

### Classes

Per year: name, school, level, period scheme. The unique constraint gives a real
error message rather than a stack trace.

Inside a class, **the subject list** — `class_subject`, with `sort_index` for
teaching order and `teacher_name`. This is the screen that does not exist today
in any form: those rows were written by `db/006` and reordered by `db/010`, and
nothing since has been able to touch them. Adding a subject to a class is
currently a SQL statement.

Teacher is two things and the screen should show both: a **name** for display
(`teacher_name`, what the live data has for 98 teachers) and optionally an
**account** (`teaching_assignment.system_user_id`, which only 3 of them have).
Pick an account where one exists, type a name where it does not.

### Subjects

Name, short name, active. Small, flat, global. The one thing it must do that the
legacy page does not: refuse to delete a subject a class still takes, rather than
leaving orphaned `class_subject` rows.

---

## 3. Year rollover

The piece with no UI, no endpoint and no plan, and the one the school hits
first — September is a new `academic_year`, a new `period_scheme` with its
trimesters and months, new `class_group` rows and a new enrollment for every
returning child.

Today that is `db/006` plus hand-written SQL. It cannot stay that way — but it
does not have to become clever either.

**DECIDED: they sit down and decide, and this is not worth much effort.** The
legacy system automated none of it and the school is used to that, so rollover
does only the part that cannot be done by hand: the `academic_year`, the
`period_scheme` and its thirty-odd periods, and - as a checkbox - a copy of last
year's class list with the level incremented. **No enrollments.** Children are
placed through the ordinary students screen.

Anything cleverer needs client input we do not have. It can be added later
without changing anything built here, because it would only be writing
enrollments that the screens already write.

---

## 4. What happens to the legacy pages

| Page               | Fate                       | Why                                                                                 |
|--------------------|----------------------------|-------------------------------------------------------------------------------------|
| `studentPage`      | **replaced, then deleted** | writes `dbo`; actively dangerous once the new one exists                            |
| `academyClassPage` | **replaced, then deleted** | same                                                                                |
| `subjectPage`      | **replaced, then deleted** | same                                                                                |
| `systemUserPage`   | **stays**                  | staff auth genuinely reads `dbo.system_user_table`; this page is correct, not stale |
| `systemUserGroup`  | **stays**                  | same — permissions are a legacy concept the rewrite never replaced                  |
| `closePeriod`      | **needs a look**           | period closing overlaps with publication locks; unresolved                          |

Deleted only once the replacement works. Two pages writing to two different
tables is worse than one page writing to the wrong one.

**The legacy `/client` parent API keeps reading `dbo`** and will not see students
created after this lands. That is correct: the new parent portal is its
replacement and both die at cutover.

---

## 5. Open questions

1. ~~Mid-year class transfer~~ — **answered:** history kept, via
   `enrollment_placement` beside the enrollment rather than a second enrollment.
2. ~~Do cohorts stay together between years?~~ — **answered:** no, and rollover
   stays deliberately dumb.
3. **Who may manage the roster?** `MANAGE_STUDENT` and `MANAGE_ACADEMY_CLASS`
   exist and are presumably right, but roster edits now have consequences they
   did not have before — moving a child moves their marks with them.
4. **What happens to a student who leaves?** `is_active = false`, `left_on`, or
   both? Their grades must survive either way, and the parent login should
   presumably stop working.

---

## 6. What was built

All of it, in this order.

1. **`enrollment_placement`** — the entity, `db/031` with its filtered unique index
   and a backfill, and `EnrollmentService`, which is the only writer of a child's
   class. `EnrollmentServiceIT`, 12 tests, including the one that justifies the
   whole design: a mark written before a move is still there, on the same
   enrollment, after it.
2. **`StudentService`, `ClassService`, `SubjectService`, `AcademicYearService`** —
   the identity rules enforced in code as well as in the database, so the console
   shows a sentence rather than a constraint name. `RosterServiceIT`, 17 tests.
3. **`RosterController`** under `/api/gradebook/roster`. Not `StudentController`:
   the legacy one already owns that bean name, and two controllers sharing one is
   an application that does not start at all.
4. **Three screens** — `roster/StudentsPage`, `roster/ClassesPage`,
   `roster/SubjectsPage`, each carrying the year selector.
5. **Rollover**, deliberately dumb: the year, the period tree copied and shifted,
   the class list optionally copied a level up, nobody enrolled.
6. **The three legacy pages deleted** — `studentPage`, `academyClassPage`,
   `subjectPage`, 31 files.

253 tests green.

### Verified against the running system, not only the tests

Driven over HTTP against the demo database: a student created; the duplicate
(username, password) pair refused with the message that explains the rule; a
sibling sharing a username accepted; a move on 15 January closing the old
placement on the 14th; and the history reading back 3ა 01.09 — 14.01, then
8ბ 15.01 onwards.

One fault was found only by opening the page, and no test could have caught it:
**Chrome fills the signed-in administrator's own username and password into the
new-student form**, dispatching real input events so React holds them and Save
lights up. Saved without looking, that makes a child whose login is the admin's.
Three standard defences were ignored by this Chrome; clearing the fields a beat
after the dialog opens is what works.

## 7. Still open

* **Which class a child who moved mid-trimester appears in.** Assumed: the one
  they were in at the end of the period. Unconfirmed by the school.
* **`closePeriod`** — overlaps with publication locks; not yet examined.
* **Teachers with accounts.** `teaching_assignment` can be written from the class
  screen, but the account picker is not built — the field takes a name today.
  Only 3 of 98 teachers have a login, so it waits until more of them do.
* **Who may manage the roster**, and **what happens to a student who leaves** —
  proceeding on the obvious defaults; correct them when the school says otherwise.
