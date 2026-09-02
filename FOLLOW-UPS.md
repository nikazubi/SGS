# Follow-ups

Things to come back to. Not open design questions — those are in
`CLIENT-QUESTIONS.md` — and not bugs, which get fixed rather than listed. These
are decisions already taken that leave something outstanding, and work that is
deliberately deferred.

---

## 1. Three students whose identity the data cannot settle

`db/015_student_identity.sql` enforces one personal number per child. Eleven were
duplicated; eight were plainly the same child recorded twice and were merged.
**Three are not, and their personal number has been cleared** so the constraint
holds. Each needs someone at the school to say which number belongs to whom.

| Who                                          | Why it could not be decided                                                                                                                             |
|----------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------|
| **სოსო** and **საბა ბაბუნაშვილი**            | Brothers. Same personal number, same password, guardian emails both *Sultanishvili* — the second record was copied from the first and the name changed. |
| **ანდრია ლომიაშვილი** and **ანდრია იაშვილი** | Same personal number, unrelated guardian emails (`lomiashvilieka.1@` and `dajichachanidze1@`). A copied record carrying somebody else's number.         |
| **სალიმ აბდულლაევი** / **სალიმ აბდულლაევ**   | Almost certainly one child — the surname differs by a trailing ი. Left alone because "almost certainly" is not a licence to delete a student record.    |

Until they are filled in, those three log in and are served normally; only the
personal number is blank.

**How this got here:** the first merge rule treated a shared surname as a shared
identity and deleted საბა ბაბუნაშვილი, who is his brother's sibling rather than
his duplicate. He was restored from `dbo`, which was never modified, and the rule
now requires an exact match on both names. Worth remembering if the rule is ever
loosened.

### Four merges decided by a tie-break rather than by evidence

Where both records of a pair were enrolled, the survivor was the higher id — the
later-created record, and so presumably the current placement. That is a guess
for `01252009159`, `01617064292`, `01808070009` and `24301052673`. None carried
grades, so the only cost of being wrong is a student sitting in the wrong class,
which the school will spot. Worth a glance when they next look at class lists.

---

## 2. Passwords are unsalted MD5

Stored as `md5Hex(password).toUpperCase()` — inherited, and copied verbatim by
the migration so that 913 families were not locked out on the day we switched.

It is weak: unsalted, so identical passwords produce identical hashes (which is
how the duplicate records above were spotted), and a common password falls to a
rainbow table immediately. The whole table is compromised at once if it ever
leaks.

**The migration that needs nobody to reset anything:** on the next successful
login, verify against the MD5 as now, then re-store the password as bcrypt and
mark the row. Both forms are accepted during the changeover; the weak hashes
drain away as people log in, and whatever is left after a term can be reset
deliberately for a small number of accounts rather than all of them.

Needs the school to agree, because a handful of families who never log in will
eventually need a reset.

---

## 3. The legacy parent API is unauthenticated

`SecurityConfiguration` has `/client/**` as `permitAll()`. Every legacy parent
endpoint is reachable without a token.

Not fixed because those endpoints die at cutover and the new `/api/parent/**` is
authenticated. But if cutover slips, or the old console runs alongside the new
one for a term, this is the thing to close first.

---

## 4. Deferred deliberately

* **Real URLs in the staff console** (decision 20). Agreed in principle, never
  attributed to a phase. The tab shell carries every page, so replacing it is its
  own piece of work.
* ~~**The bulk export button**~~ — **answered.** The school picks a trimester and a
  year, every export shape gets a bulk variant, and the output is a zip. Built in
  phase 7; `CLIENT-QUESTIONS.md` §2 can be closed.
* **The 9-to-10 mapping, once the school decides it.** They grade out of 7 today and
  must report to the government out of 10; they are moving to a 9-point scale and do
  not yet know how it converts. The formula built in phase 7 is `multiplier` and
  `offset` — a straight line, which covers any proportional mapping and any mapping
  fixed by two points. **If what they settle on is a lookup table** (say 9→10, 8→9,
  7→7, 6→5) rather than a line, it cannot be expressed and becomes a table of
  ranges instead: one entity and one branch in `GradeConversionService`. Small, but
  worth knowing before they commit — so ask for the actual table, not just the rule.
* **The annual export's წლიური column has never been populated.**
  `adjustMonthNamesForAnual` names key 5, but `getAnualGrades` only ever writes keys
  1–4, so that column has been blank in every annual export the school has run. Not
  a rewrite decision — something they should be told about their current system.
* **`TeachingAssignment` is empty.** It needs a `system_user_id`, and only 3 of
  98 teacher names match an account. The names live on
  `class_subject.teacher_name` and are displayed; the structured form waits until
  teachers have logins.
* ~~**`application.yml` still pins `SQLServerDialect`**~~ — **changed, and it was
  not cosmetic.** Written up below.
* **The parent console is not responsive.** It has to open properly on a phone,
  which is where a parent actually reads it. Fixed-width layouts, the news
  card's side-by-side image, the five weekday cards and the landing grid all
  need a narrow form; the calendar and the absence register scroll inside their
  own container rather than reflow. Written up screen by screen in
  `PARENT-COMPONENTS.md`. It applies to the shared components, so it is not part
  of the primary theme and should not wait for it.
* **`node_modules/.yarn-integrity` is still tracked.** `git rm --cached` it.

---

## 6. The dialect change, and the one migration it still needs

`application.yml` used to pin `org.hibernate.dialect.SQLServerDialect` — the
2000-era one. It is now `SQLServer2012Dialect`, and that was a fix, not tidying.

**What it was actually doing.** `SQLServerDialect` does not override
`supportsSequences()`, so it inherits `false`. Hibernate's `SequenceStyleGenerator`
reads that and silently builds a `TableStructure` instead of a `SequenceStructure`
— for *every* `@SequenceGenerator` in the new model. At startup it tried

```
create table sgs.grade_entry_seq (next_val numeric(19,0))
```

against a database where `sgs.grade_entry_seq` is a real sequence, got *"There
is already an object named 'grade_entry_seq'"*, and, because `ddl-auto: update`
logs schema failures as warnings, carried on and started normally. Every insert
into an `sgs` table then failed with *"Invalid object name
`sgs.grade_entry_seq`"*.

So the rewritten system **could not write a single row**, and had not been able
to at any point. Nothing caught it:

* the integration tests override the dialect to `SQLServer2012Dialect`;
* `ApplicationWiringIT` boots the context but does not write;
* startup succeeds, so the log looks noisy rather than broken.

Worth remembering as a shape: a setting that only misbehaves in the configuration
nothing tests. The tests were right to override the dialect and wrong to be the
only place the real one was ever exercised.

**The migration that is still owed.** On a database created before this change,
the *legacy* entities are the mirror image of the problem. `GenerationType.AUTO`
under the old dialect produced a `dbo.hibernate_sequence` **table**; under the
new one Hibernate wants a **sequence** of that name, cannot create it because
the table is there, and every legacy insert fails the same way round.

The school's `SGS` database has that table. Before cutover it needs:

```sql
DECLARE @next bigint = (SELECT next_val + 50 FROM dbo.hibernate_sequence);
DROP TABLE dbo.hibernate_sequence;
EXEC('CREATE SEQUENCE dbo.hibernate_sequence AS bigint START WITH '
     + @next + ' INCREMENT BY 50');
```

Start *above* the table's current value, not at it — the table generator hands
out its value and then increments, so the last id issued may be the one sitting
in the column. Not written as a numbered script yet, because it should be
applied in the same maintenance window as the cutover rather than sitting in the
chain waiting to be run early.

`SGS_DEMO`, being built from nothing, has neither problem: Hibernate creates
`dbo.hibernate_sequence` as a sequence on first boot and every generator lines
up.

---

## 7. Operational notes worth not rediscovering

* A **filtered index** requires `QUOTED_IDENTIFIER ON` for *every* DML statement
  on the table it covers, not only when it is created. `sqlcmd` leaves it off and
  fails with `Msg 1934`; the JDBC driver sets it, so the application never sees
  this. Four scripts set it explicitly for that reason.
* `db/006` skips students whose personal number already exists, so re-running it
  after a merge cannot resurrect the record that was merged away.
* **Build the war with `clean`.** `core/target/core-0.0.1-SNAPSHOT/` is an
  exploded war that Maven does not tidy between builds, so a rebuild without it
  packages whatever was there before - including classes whose source has since
  been renamed. A stale `gradebook/AbsenceController.class` next to the live
  `controllers/AbsenceController` is a `ConflictingBeanDefinitionException` and
  the application does not start at all. No test sees it: tests read
  `target/classes`, which is correct; only the war is wrong.
* **Chrome fills the signed-in admin's own credentials into the new-student form.** A text
  field followed by a password field is a login form as far as the password manager is
  concerned, and it dispatches real input events doing it, so React's state ends up holding
  them and Save lights up. Saved unnoticed, that makes a child whose login is the admin's.
  `autocomplete="off"`, `autocomplete="new-password"` and readonly-until-focus were each
  tried and each ignored. What works is clearing the two fields a beat after the dialog
  opens — state written after autofill has finished. Worth knowing before adding any
  other credential field to this console.
* **A demo database exists**: `db/demo/reset.sh` builds `SGS_DEMO` from nothing
  in about half a minute - schema, migrations, two classes, four students, three
  journals and an administrator. `db/demo/README.md` says what is in it. The
  school's own `SGS` is never touched.
