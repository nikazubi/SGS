# The demo database

A small, disposable database to click around in: two classes, two students each,
one administrator, and the journals the system needs to work.

```powershell
.\db\demo\reset.ps1        # PowerShell
```

```bash
bash db/demo/reset.sh       # Git Bash
```

Drops `SGS_DEMO` and rebuilds it from nothing in about half a minute. Run it
whenever a test session has made a mess — nothing in it is precious, so there
is never a reason to repair it by hand.

> From PowerShell use the `.ps1`, not `bash db/demo/reset.sh`. On this machine
> `bash` on PATH is `C:\Windows\system32\bash.exe` — the **WSL** launcher,
> not Git Bash — so the script runs inside Ubuntu, against a different
> filesystem and with no reason to see the same Docker CLI. The wrapper locates
> Git Bash and does nothing else.

The school's own database, `SGS`, is never touched. The two sit side by side in
the same container; only `application.yml` decides which one the application
talks to.

---

## Logging in

| Console                   | Username     | Password     |
|---------------------------|--------------|--------------|
| Staff (`admin-console`)   | `admin`      | `admin`      |
| Parent (`client-console`) | `beridze`    | `nino2025`   |
|                           | `maisuradze` | `giorgi2025` |
|                           | `beridze`    | `mariam2025` |
|                           | `chkheidze`  | `luka2025`   |

The two `beridze` logins are **not a mistake**. ნინო and მარიამ are sisters and
share a username with different passwords, which is the school's actual rule:
the personal number is unique, the (username, password) pair is unique, and
neither half of the pair identifies anybody on its own. Logging in as each
should show a different child — which is the point of seeding it this way,
because the legacy portal looked a student up by username alone and served
whichever row came back first.

The staff password is stored in plain text and the student passwords as unsalted
MD5. Neither is this seed's doing — it is what the running system stores, and
both are in `FOLLOW-UPS.md`.

---

## What is in it

**Two classes, deliberately in different schools**, so the primary/basic split
is visible without a second database:

| Class | School              | Students                      |
|-------|---------------------|-------------------------------|
| 3ა    | დაწყებითი (PRIMARY) | ნინო ბერიძე, გიორგი მაისურაძე |
| 8ბ    | საბაზო (BASIC)      | მარიამ ბერიძე, ლუკა ჩხეიძე    |

Five subjects each, with a teacher name against every one.

**Three journals:**

| Journal                            | Frequency | Grid    | Assigned to | From                          |
|------------------------------------|-----------|---------|-------------|-------------------------------|
| ტრიმესტრული შეფასება               | TRIMESTER | columns | both        | `db/007` + `db/018`           |
| გაცდენილი საათები                  | MONTH     | periods | both        | `db/022`                      |
| შეფასება ეთიკური ნორმების მიხედვით | MONTH     | columns | 8ბ only     | `db/demo/130` — **demo only** |

The first two are the shipped seed. The third is not, and `130_ethics_journal.sql`
explains at length why it cannot be shipped as it stands: the brief scopes the
workspace to basic and secondary, the school has never said whether ethics is
weekly or monthly, and the week level of the period tree no longer exists.

Daily absence is **not** a journal. It is `sgs.daily_absence` — one row per child
per day absent — with its own register page.

**Periods:** one year, three trimesters, ten months, and nothing below. Days and
weeks are both created and then deleted on the way through, which is exactly
what happened to the school's own database.

**Absence allowances** are set for every month of both classes, with June
deliberately lower. That matters: when the months do not all share one
allowance, the parent's chart drops its dashed reference line and colours each
bar against its own number, and with a single uniform figure that path never
runs.

---

## The year is 2025-26, which is in the past

Deliberate, and the more useful of the two options.

Every shipped script hardcodes these dates — `db/013`'s month list especially —
so using them means the whole chain runs unmodified. It also makes the register
testable: daily absence refuses a future date, so on a year that has not started
yet there is no day anywhere that can be marked, while on a finished one every
school day can.

The cost is that anything defaulting to today's month lands outside the year.
Pick a month from the picker.

---

## What is *not* in it

* **No grades, no homework, no news, no absences.** Entering them is the thing
  being tested; a seed full of them would be in the way.
* **The legacy `dbo` tables are empty apart from the login.** The staff account,
  its permission group and the two class names are seeded because the login path
  reads them; legacy students, subjects and grades are not, because every new
  screen reads `sgs` and none of them touch `dbo`. The old pages in the staff
  console will therefore look empty — that is the cutover gap, not a fault.
* **`teaching_assignment` is empty**, as in production. Teachers are names on
  `class_subject`, not accounts.

---

## Which database the application talks to

`core/src/main/resources/application.yml`:

```yaml
url: jdbc:sqlserver://localhost:1433;databaseName=SGS_DEMO;
```

Change `SGS_DEMO` back to `SGS` for the school's migrated data. That is the only
difference between the two.

> **The dialect is not interchangeable in the same way.** `application.yml` now
> pins `SQLServer2012Dialect`, and it has to. The older `SQLServerDialect`
> reports no sequence support, so Hibernate quietly substitutes a *table*
> generator for every sequence in the new model: it tries to create
> `sgs.grade_entry_seq` as a table, collides with the real sequence, and every
> insert then fails with `Invalid object name`. The application still starts,
> because `ddl-auto: update` logs schema failures as warnings — it simply cannot
> write anything. A database created *before* that change also carries a
> `hibernate_sequence` **table** for the legacy entities where the newer dialect
> expects a sequence. A fresh database has neither problem. `FOLLOW-UPS.md` has
> the migration.

---

## Building the application

Always with `clean`:

```bash
export JAVA_HOME="/c/Program Files/Java/jdk-11.0.10"
./mvnw -B -DskipTests clean install
java -jar core/target/core-0.0.1-SNAPSHOT.war
```

`core/target/core-0.0.1-SNAPSHOT/` is an exploded war that Maven does not tidy
between builds, so rebuilding without `clean` packages whatever was there
before — including classes whose source has since been renamed. Two controllers
with the same simple name is a `ConflictingBeanDefinitionException` and the
application does not start at all. No test catches it, because tests read
`target/classes` rather than the war.
