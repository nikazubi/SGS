# Test run — 2 September 2026

`TEST-PLAN.md` executed against the seeded `SGS_DEMO` database, driving both
consoles with Playwright (Chromium). Every check below was run against the real
UI rather than the API, except where it says otherwise.

**78 checks executed: 71 pass, 7 fail. 41 not executed.**

Nine defects were found. **Seven are fixed** — the changes are in §4. Two are
decisions for the school rather than bugs, and are in §3.

---

## 1. What was run

| §                | Checks  | Pass | Fail | Not run |
|------------------|---------|------|------|---------|
| 3 Journals       | 1–22    | 20   | 1    | 1       |
| 4 Journal editor | 23–29   | 6    | 0    | 1       |
| 5 Roster         | 30–64   | 34   | 0    | 1       |
| 6 Content        | 65–86   | 8    | 0    | 14      |
| 7 Parent         | 87–100  | 3    | 3    | 8       |
| 8 Flows          | 101–114 | 14   | 0    | 0       |
| 9 Permissions    | 115–119 | 0    | 0    | 5       |

The fixture was reset between runs and restored afterwards: 4 students, 6
subjects, 3 journals, 2 classes, no marks, no posts.

---

## 2. The checks that failed

### 19 — the ethics journal is offered to 3ა, which is not assigned it

`/api/gradebook/classes` takes no journal, so **every class picker on every
journal offers every class**. Choosing 3ა on the ethics journal returns HTTP 200
and a fully editable grid; marks entered there would be stored against a
template version the class was never assigned.

This is not an oversight in the code. `TemplateVersionResolver` falls back to
the journal's active version deliberately, and says why: *a class created after
the journal was activated has no assignment, and falling back means a new class
can open every journal without an administrator re-activating each one by hand.*

**So assignment is a pin, not a restriction.** That is coherent; it is just not
what the plan assumed. See §3.

### 89 — a primary child's parent is shown the ethics journal

The same thing from the parent side. ნინო (3ა, primary) gets a box for
შეფასება ეთიკური ნორმების მიხედვით, which the fixture assigns to 8ბ alone.
The gradebook *is* correctly withheld from her.

### 90 — a basic child gets a different module set, not a larger one

|                                  | ნინო (3ა, primary) | ლუკა (8ბ, basic) |
|----------------------------------|--------------------|------------------|
| homework, news                   | yes                | yes              |
| schedule, menu, characterization | yes                | **no**           |
| grade journals                   | no                 | yes              |

`ParentContentService.modulesFor` adds SCHEDULE, MENU and CHARACTERIZATION only
when the school code is PRIMARY — deliberate, and documented in one place. The
plan said "the same modules **plus** the grade journals", which is wrong about
this build. Whether basic-school parents should really have no daily schedule,
no menu and no characterization is a question for the school. See §3.

### 100 — a failed request is indistinguishable from an empty one

Parent homework page, every API call aborted, page reloaded: the month calendar
draws normally with no day marked and an ellipsis that never resolves. There is
no error message. A parent whose connection has dropped is told, in effect, that
no homework was set.

The staff console does distinguish the three states — `AbsenceRegisterPage` and
`DailyRegisterPage` have a `Body` that keeps them apart, with a comment saying
why. The parent console's homework page does not.

### 25, 47, 57 — could not be completed

* **25** the formula editor's cycle refusal — the editor was reached and a column
  added, but a deliberate cycle was not constructed.
* **47** another academic year — the fixture has only 2025-26.
* **57** "marks for a removed subject are not destroyed" — the subject removed
  had no marks, so only the removal itself was verified.

---

## 3. Two decisions for you, not bugs

**A. Should a journal's class assignment restrict, or only pin?**
Today it pins: any class can open any journal, and the assignment decides *which
version* it gets. The consequence is checks 19 and 89 — a primary class is
offered a journal meant for the basic school, on both the staff and the parent
side. Making it restrict would mean the class pickers and the parent's boxes
filter on assignment, and new classes would need assigning before their journals
appear. I have not changed this.

**B. Should basic-school parents see the schedule, menu and characterization?**
They do not, by design. Worth confirming with the school before cutover.

---

## 4. The nine defects, and the seven fixes

### Fixed

**1. `AbsenceRegisterPage.js` did not compile.** `componentCode` was used in
`write` but was never a parameter — a half-finished edit from the summary-grid
work. The dev server's error overlay covered the whole console, so nothing at
all could be tested until this was fixed. Corrected in the same file: the cell
key gained the component (a period now carries several columns), the React keys
likewise, and `rowTotal` stopped counting the trimester and year columns on top
of the months it had already added — a student with 20 hours reported 60.

**2. Mark grids were served from a stale cache.** The query client keeps every
answer fresh for 60 seconds; the per-cell save path updates local state and
deliberately does not refetch. Enter marks, switch period, switch back inside
the minute and the grid redrew from the copy taken *before* the edits — no HTTP
request at all, the marks apparently gone. A teacher's obvious response is to
enter them again. `staleTime: 0` on the three editable grids.

**3. Parents could not log in.** `LoginForm.handleSubmit` in `client-console`
never called `e.preventDefault()`, so the browser submitted the form itself and
navigated away, aborting the request `login()` had just started; the page came
back to an empty form. This is the same fault found and fixed in the staff
console earlier — the parent console still had it.

**4. Activating a journal did not add its tab.** The index reads
`["JOURNALS","all"]`, the menu reads `["JOURNALS"]`. Every mutation called the
index's own refetch, so a new journal appeared in the list and reached the menu
only after a reload. It now invalidates the shared prefix, which covers create,
activate, rename and archive at once.

**5. A draft journal appeared in the menu.** The menu asked for every
non-archived journal, including one the wizard had just made with no columns,
whose tab could only ever report that the journal has no active version.
`JournalView` could not tell the two apart — `currentVersionId` falls back to the
newest version when none is active — so it now carries `currentVersionStatus`,
and the menu skips drafts.

**6. The journal wizard could only produce a working journal by accident.**
`ColumnEditor` hardcoded every new column's `periodKind` to `ROLLUP` and offered
no control to change it. ROLLUP means "sits on a trimester", so:

| journal frequency | its periods | a new column | result                                                     |
|-------------------|-------------|--------------|------------------------------------------------------------|
| ONCE_A_YEAR       | YEAR        | ROLLUP       | **no columns for this period** — the tab never drew a grid |
| MONTH             | REPORTING   | ROLLUP       | columns on the trimesters, not the months being filled in  |
| TRIMESTER         | ROLLUP      | ROLLUP       | right, by coincidence                                      |

The tier now comes from the journal's frequency.

**7. A journal could not be renamed.** No screen offered it, though the update
endpoint takes a name and `JournalSettings` was already sending one — unchanged,
straight back. Tabs are keyed by uuid precisely so that a rename is safe, which
`useNavigationData` says in as many words. The settings dialog now has name and
description fields.

Also: react-query's devtools had `initialIsOpen={true}`, and the open panel sits
over the bottom-left of every page — in a 1600-wide window it covers the login
button. Closed by default now.

### Not fixed — see §3

**8.** Journal assignment does not restrict the class pickers (checks 19, 89).

**9.** The parent console cannot distinguish a failed request from an empty one
(check 100). The staff console can; the pattern to copy is
`AbsenceRegisterPage`'s `Body`.

---

## 5. Worth knowing, not defects

* **Error text leaks the status code.** Refusals read `409, ეს პირადი ნომერი
  სხვა მოსწავლეს უკვე აქვს`. The message is right; the `409, ` prefix comes from
  `convertError(error, includeStatus = true)` and is not for teachers or
  parents.
* **უფლებათა ჯგუფები has no create control at all**, which is why §9 could not
  start — every one of its checks begins by making a permission group.
  სისტემური მომხმარებელი does have a visible one. §10 of the plan says
  "creating records there works" — true of one page, not the other.
* **A student who leaves disappears from periods they were present for.** After
  ტესტაძე left on 01.03.2026, the trimester-I grid (September–November, when
  they were enrolled and had a mark of 7) no longer lists them. The mark is
  still in the database, but no screen shows it.
* **Two DataGrids are mounted at once** — an open tab stays alive behind the one
  being shown, which is where the "data grid has an empty height" console
  warning comes from. Harmless.

---

## 6. What passed, in brief

The parts carrying the most new code all behave:

* **The brief's eleven-column register** draws in exactly the right order —
  სექტემბერი-ოქტომბერი, ნოემბერი, **I ტრიმესტრი**, დეკემბერი,
  იანვარი-თებერვალი, მარტი, **II ტრიმესტრი**, აპრილი, მაისი,
  **III ტრიმესტრი**, **წელი** — with the rollups shaded and read-only, and the
  settings rows offering inputs on the reporting periods only.
* **The two journals of the same shape compute differently, and correctly.**
  Hours: 12 and 8 give a trimester of 20. Ethics: 9 and 6 give **7.50**, not 15.
* **The roster reaches the gradebook.** A student created on the students page
  has a row in the trimester journal for their class — the whole point of the
  rewrite, and the thing the old pages got wrong.
* **The identity rules are exactly as specified.** A duplicate personal number is
  refused by name; a duplicate username *and* password is refused as a pair,
  saying the username may repeat but the password must differ; the same username
  with a different password is accepted.
* **Moving a child keeps the history and the marks.** `3ა — 01.09.2025 –
  14.01.2026`, `8ბ — 15.01.2026 – დღემდე`: the old placement closes the day
  before the new one opens. A backwards move is refused.
* **Publication, disputes and approval work end to end.** Unpublished marks are
  invisible to the parent; publishing releases exactly four; an edit afterwards
  is withheld; a rejected request changes nothing; an approved one changes the
  value, recomputes the average and the trimester grade, and the parent sees the
  new figures.
* **Daily absence** offers November's twenty weekdays and no weekends, and
  forty-five columns for the paired სექტემბერი-ოქტომბერი.
* **The schedule round trip**: two days filled on the staff side and published,
  and the parent sees all five weekdays with the empty ones drawn as a dash.

---

## 7. Re-running this

```powershell
.\db\demo\reset.ps1
```

```bash
export JAVA_HOME="/c/Program Files/Java/jdk-11.0.10"
./mvnw -B -DskipTests clean install
java -jar core/target/core-0.0.1-SNAPSHOT.war
```

Staff console on 3000 (`admin-console/src/front-ac`), parent console on 3001
(`client-console`, `PORT=3001`). Both need
`NODE_OPTIONS=--openssl-legacy-provider` on Node 20, not on 16.

The Playwright scripts were throwaway and live in the job's scratch directory,
not the repository. Each was written against one screen, and none of them assert
anything a person working through `TEST-PLAN.md` could not check by hand.
