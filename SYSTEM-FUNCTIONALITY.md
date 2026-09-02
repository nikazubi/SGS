# SGS — School Grade System: Functional Reference

> Snapshot of the **existing** system as of commit `a2e7ee3` (branch `main`).
> Purpose: capture every piece of functionality the current codebase provides so the rewrite
> can reproduce it deliberately (and drop what deserves dropping). This describes *what is*,
> not *what should be*. Items marked **⚠︎** flag behaviour that is broken, dead, or dangerous
> and needs an explicit decision during the rewrite.

The product is the grading portal for **სკოლა პანსიონ IB მთიები** (IB Mtiebi boarding school),
deployed at `https://www.ibmthiebistudentrating.edu.ge`. All end-user copy is Georgian.

---

## 1. System shape

Maven multi-module monolith (Spring Boot 2.4.3, Java 11, MS SQL Server) + two React 17 SPAs.

| Module           | Packaging   | Role                                                                                                   |
|------------------|-------------|--------------------------------------------------------------------------------------------------------|
| `Sgs-model`      | jar         | JPA entities, QueryDSL repositories + custom query impls, exception types, `UserDetails` adapters      |
| `Sgs-server`     | jar         | Service interfaces + implementations (business logic), SMTP, exception handler                         |
| `core`           | **war**     | Spring Boot app: REST controllers, DTOs + MapStruct mappers, security config, JWT filter, Excel export |
| `admin-console`  | jar wrapper | React app at `src/front-ac` — staff/teacher/admin UI                                                   |
| `client-console` | jar wrapper | React app at `src/` — student/parent UI                                                                |
| `utils`          | —           | **⚠︎** empty directory, not a real module, not listed in `<modules>`                                   |

Both frontends are Create React App (`react-scripts` 4), MUI v5 + leftover Material-UI v4,
`react-query` v3, Formik, axios. Both proxy to `http://localhost:8080` in dev and read
`process.env.REACT_APP_BACKEND_BASE_URL` in production.

Backend entry point: `core/src/main/java/mthiebi/sgs/SgsApplication.java` (extends
`SpringBootServletInitializer` — deployable to an external Tomcat as `sgs-core.war`).

### Two consoles, one backend

* **Admin console** — authenticates a `SystemUser` against `/authenticate`, receives a JWT whose
  `scp` claim carries a comma-separated permission list. All non-`/client/**` endpoints are
  protected by `@Secured` on those permission strings.
* **Client console** — authenticates a `Student` against `/authenticate-student`, receives a JWT
  with an **empty** `scp` claim, and calls the `/client/**` endpoints. Those controllers identify
  the student by parsing the `authorization` header themselves.

---

## 2. Domain model

All entities extend `Audit` (`createTime`, `lastUpdateTime`, set in `@PrePersist`/`@PreUpdate`).
`spring.jpa.hibernate.ddl-auto: update` — **there are no migrations**; the schema is whatever
Hibernate has accreted over time.

> **Verified against a live snapshot** (`sps-mssql-db` container, database `SGS`): `GRADES`,
> `STUDENTS` and `CLOSED_PERIOD` are **heaps** — no clustered index and **no primary-key index at
> all**, so every lookup against them is a full table scan, `findById` included. Only
> `ABSENCE_GRADES` has a clustered PK. A `hibernate_sequence` table is present, confirming that
> `GenerationType.AUTO` fell back to a single shared table generator across every entity.

### `Student` → `STUDENTS`

`id, firstName, lastName, age, personalNumber, username, password, ownerMail`

* `password` is **MD5 hex, uppercased** (`DigestUtils.md5Hex(...).toUpperCase()`), recomputed on
  create and on every update.
* `ownerMail` is the parent/guardian address used for notification emails.
* `equals` blind-casts to `Student` (throws `ClassCastException` on a foreign type). **⚠︎**

### `Subject` → `SUBJECT`

`id, name, teacher` — `teacher` is free-text `nvarchar(max)`, not a link to a `SystemUser`.

### `AcademyClass` → `ACADEMY_CLASS`

`id, classLevel, className, isTransit` plus:

* `@OneToMany studentList` (join column `academy_class_id` on `STUDENTS`) — a student belongs to
  exactly one class.
* `@ManyToMany subjectList` via join table `class_subject`.
* `@OneToMany totalAbsences` → `TotalAbsence`.
* `isTransit` flips the whole class onto the `TRANSIT_*` grade-type family instead of `GENERAL_*`.
* Transient helpers `getActiveTotalAbsence()` / `getTotalAbsenceForYearAndMonth(y, m)`, which fold
  September+October and January+February into single periods.

### `Grade` → `GRADES`

`id, gradeType (enum, STRING), value (BigDecimal), student, subject, academyClass, exactMonth, identifier`

* `subject` is **nullable** — behaviour grades are stored with `subject = null`.
* `exactMonth` is the period bucket, normalised on write: **February → January**,
  **October → September**. The system therefore really has 7 grading months:
  Sep(+Oct), Nov, Dec, Jan(+Feb), Mar, Apr, May.
* `identifier` is an overloaded generic integer; today it means **trimester number (1–3)**.

### `AbsenceGrade` → `ABSENCE_GRADES`

`id, gradeType (AbsenceGradeType), value, student, subject, academyClass, exactMonth`
A parallel, separate store for absence hours per period — **not** the same table as `GRADES`.

### `TotalAbsence` → `TOTAL_ABSENCE`

`id, activePeriod (Date), academyClass, totalAcademyHour` — the denominator: how many academic
hours the class had that month, so an absence count can be shown as a proportion.

### `ClosedPeriod` → `CLOSED_PERIOD`

`id, academyClassId, gradePrefix` — one row per (class, prefix) where prefix ∈
`GENERAL | BEHAVIOUR | TRANSIT`. Its `lastUpdateTime` is the **publish cursor** (see §6).

### `ChangeRequest` → `CHANGE_REQUESTS`

`id, issuer (SystemUser), prevGrade (Grade), prevValue, newValue, status, description, directorDescription`

### `SystemUser` → `SYSTEM_USER_TABLE`

`id, username, password, name, email, active`, `@ManyToMany groups`, `@ManyToMany academyClassList`.
The class list is the **data-scoping mechanism**: a user only sees students, classes, subjects and
change requests belonging to their attached classes.

### `SystemUserGroup` → `SYSTEM_GROUPS`

`id, name, permissions (comma-separated string), active`. Transient `getListPermissions()` splits it.

### Enums

* **`ChangeRequestStatus`** — `APPROVED | PENDING | REJECTED`
* **`Permissions`** — a 9-value enum that is **⚠︎ dead**: the real permission vocabulary is
  `AuthConstants` (14 string constants), and the two disagree.
* **`AbsenceGradeType`** — `SEPTEMBER_OCTOBER(9), NOVEMBER(11), DECEMBER(12), JANUARY_FEBRUARY(1),
  MARCH(2), APRIL(3), MAY(4)`. **⚠︎** The `monthNumber` values for March/April/May are off by one,
  `getMonthByNumber` is effectively unusable, and every caller hand-rolls its own switch instead
  (there are three separate copies of that mapping: server, admin UI, client UI).

### `GradeType` — the central taxonomy (~95 values)

Grades are distinguished only by this enum, and code routinely does
`gradeType.toString().startsWith(prefix)`. Families:

| Family                                     | Members                                                                                                              | Meaning                                                                       |
|--------------------------------------------|----------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------|
| `GENERAL_SUMMARY_ASSIGMENT_*`              | `1, 2, RESTORATION, MONTH, PERCENT`                                                                                  | Summative assignments (შემაჯამებელი), incl. a resit and derived month/percent |
| `GENERAL_HOMEWORK_*`                       | `WRITE_ASSIGMENT_1..4`, `CREATIVE_ASSIGMENT`, `MONTHLY`, `PERCENT`                                                   | Homework                                                                      |
| `GENERAL_SCHOOL_WORK_*`                    | `1..8`, `MONTH`, `PERCENT`                                                                                           | Classwork                                                                     |
| `GENERAL_COMPLETE_MONTHLY`                 | —                                                                                                                    | The month's final subject mark                                                |
| `TRANSIT_*`                                | `SUMMARY_ASSIGMENT_1/2/RESTORATION/MONTH/PERCENT`, `SCHOOL_WORK_1..8/MONTH/MONTH_PERCENT`, `SCHOOL_COMPLETE_MONTHLY` | Mirror family for `isTransit` classes (no homework component)                 |
| `BEHAVIOUR_*`                              | 5 criteria × weeks 1–6 + `_MONTHLY` each, plus `WEEK_AVERAGE_1..6`, `WEEK_AVERAGE_MONTHLY`, `BEHAVIOUR_MONTHLY`      | Ethics/conduct: uniform, delays, classroom inventory, hygiene, behaviour      |
| `ABSENCE_DAILY`, `GENERAL_ABSENCE_MONTHLY` | —                                                                                                                    | Absence recorded as grades (parallel to `AbsenceGrade`)                       |
| `DIAGNOSTICS_1..4`                         | —                                                                                                                    | Diagnostic tests; 1–2 forced into December, 3–4 into June                     |
| `FINAL_EXAM`                               | —                                                                                                                    | Year-end exam, feeds the annual average                                       |
| `SHEMOKMEDEBITOBA`                         | —                                                                                                                    | "Creativity" — **⚠︎ present but every use site is commented out**             |
| `TRIMESTER_*`                              | `ONGOING_GRADE_1..7`, `INITIAL_KNOWLEDGE_GRADE`, `PROGRESS_GRADE`, `FINAL_EXAM_GRADE`, `GRADE`                       | The **current** grading model (newest work)                                   |

**⚠︎ Two grading models coexist.** The monthly/semester machinery (`GENERAL_*`, semesters,
diagnostics, annual) is the legacy model. The `TRIMESTER_*` family is the model the school
actually uses now — it drives the admin Trimester page and the entire student console. Both sets
of endpoints, services and repository queries are live simultaneously.

Sentinel value: **`-50` means "ჩთ"** (not attested) and is rendered as text, not a number.

---

## 3. Authentication, authorization, scoping

### Tokens

`UtilsJwt` (jjwt 0.9.1), HS512, secret from `jwt.secret` (**⚠︎ literal `secretkey123` committed in
`application.yml`**), 5-hour validity, subject = username, custom claim `scp` = comma-joined
authorities.

### Staff login — `POST /authenticate`

`AuthenticationManager` → `DaoAuthenticationProvider` → `UserDetailsServiceImpl` →
`UserDetailsImpl(SystemUser)`. Authorities = union of permissions across the user's **active**
groups. `isEnabled()` / `isAccountNonLocked()` = `SystemUser.active`.

**⚠︎ `PasswordEncoder` is `NoOpPasswordEncoder`** — staff passwords are stored and compared in
plaintext. `encryptPassword()` exists in `SystemUserServiceImpl` but is commented out at both call
sites.
**⚠︎ `AuthController` logs the submitted username *and password* at INFO on every login.**

### Student login — `POST /authenticate-student`

Bypasses Spring Security entirely: `studentRepository.authStudent(username, md5Upper(password))`.
Returns `ConsoleJwtResponse { jwtToken, student }` — the whole `StudentDTO`, **including the MD5
password hash**. **⚠︎**

### Filter chain

`JwtFilter` (registered before `UsernamePasswordAuthenticationFilter`) reads the `authorization`
header, extracts the username, and loads `UserDetails` via
`UserDetailsServiceImpl.loadUserByUsername(username, request)` — which branches on whether the
request URL contains `/client/`, returning `UserDetailImplStudent` (no authorities) or the staff
`UserDetailsImpl`. **⚠︎ It logs the full bearer token on every request.**

### `SecurityConfiguration`

Stateless, CSRF off, CORS `allowedOrigins("*")` for GET/POST/PUT/DELETE/HEAD.
Permit-all: `/authenticate`, `/authenticate-student`, `/client/**`, Swagger paths. Everything else
`.authenticated()`. `@EnableGlobalMethodSecurity(securedEnabled = true)` plus
`GrantedAuthorityDefaults("")` so `@Secured("MANAGE_GRADES")` works without a `ROLE_` prefix.

**⚠︎ `/client/**` is `permitAll`.** Those controllers still call
`utilsJwt.getUsernameFromHeader(authHeader)`, which throws on a missing/short header, so an
anonymous call fails — but there is no authorization check at all on that path: any valid student
token reaches any `/client/**` endpoint, and identity comes from the token subject.

### Permission vocabulary (`AuthConstants`)

`MANAGE_STUDENT`, `VIEW_STUDENT`, `MANAGE_ACADEMY_CLASS`, `VIEW_ACADEMY_CLASS`, `MANAGE_SUBJECT`,
`VIEW_SUBJECT`, `ADD_GRADES`, `MANAGE_GRADES`, `MANAGE_SYSTEM_USER`, `MANAGE_CHANGE_REQUESTS`,
`VIEW_CHANGE_REQUESTS`, `MANAGE_CLOSED_PERIOD`, `VIEW_SYSTEM_USER_GROUP`, `MANAGE_TOTAL_ABSENCE`.

Georgian labels for these live in `admin-console/.../systemUserGroup/permissions.js`.

**⚠︎ System-group CRUD (create/edit/delete) is guarded by `VIEW_SYSTEM_USER_GROUP`** — a view
permission gates write operations, so anyone who can see groups can grant themselves anything.

### Data scoping

Not enforced by the permission system but by `SystemUser.academyClassList`:
`getAcademyClasses(username, …)`, `students/get-students-by-name`, `subjects/get-subjects`,
`change-request/get-change-requests` and `close-period/create-closed-period` all filter to the
caller's attached classes. Endpoints ending `-without-validation` /
`-without-academy-class-filter` deliberately bypass this.

### `/refresh-token` and `/user-and-permissions`

* `POST /refresh-token` — validates the bearer token and reissues one. **⚠︎ Unused:** the admin
  console's `ENDPOINT_AUTHENTICATION_REFRESH` is the literal string `'ramerume'` and the whole
  refresh path in `utils/axios.js` is commented out. On any 401 the console just logs out.
* `GET /user-and-permissions` — returns `{ username, permissionList }`, read once at app start by
  `user-context.js` to drive menu visibility.

---

## 4. REST API surface

### Auth (`AuthController`, no prefix)

| Method | Path                    | Guard  | Notes                                |
|--------|-------------------------|--------|--------------------------------------|
| POST   | `/authenticate`         | public | `{username,password}` → `{jwtToken}` |
| POST   | `/authenticate-student` | public | → `{jwtToken, student}`              |
| POST   | `/refresh-token`        | bearer | reissue token                        |
| GET    | `/user-and-permissions` | bearer | `{username, permissionList}`         |

### Students (`/students`)

| Method | Path                                                            | Guard                                       |
|--------|-----------------------------------------------------------------|---------------------------------------------|
| POST   | `/create-student`                                               | `MANAGE_STUDENT`                            |
| PUT    | `/update-student`                                               | `MANAGE_STUDENT`                            |
| GET    | `/get-students?limit&page&id&firstName&lastName&personalNumber` | `VIEW_STUDENT`                              |
| GET    | `/get-students-by-name?queryKey`                                | `VIEW_STUDENT` — scoped to caller's classes |
| GET    | `/get-students-by-name-without-validation?queryKey`             | `VIEW_STUDENT` — unscoped                   |
| GET    | `/get-student/{id}`                                             | `VIEW_STUDENT`                              |
| DELETE | `/delete-students/{id}`                                         | `MANAGE_STUDENT`                            |

### Subjects (`/subjects`)

`POST /create-subject`, `PUT /update-subject` (`MANAGE_SUBJECT`);
`GET /get-subjects` (scoped), `GET /get-subjects-for-class?classId` (ordered by
`SubjectOrderUtils.subjectPattern`), `GET /get-subjects-without-academy-class-filter`,
`GET /get-subject/{id}` (`VIEW_SUBJECT`); `DELETE /delete-subject/{id}` (`MANAGE_SUBJECT`).

### Academy classes (`/academy-class`)

`POST /create-academy-class` (also appends the new class to the creator's `academyClassList`),
`PUT /update-academy-class`, `DELETE /delete-academy-class/{id}`,
`PUT /attach-students-to-academy-class?academyClassId`,
`PUT /attach-subjects-to-academy-class?academyClassId` (**⚠︎ broken — see §13**),
`GET /get-academy-class?queryKey` (unscoped), `GET /get-academy-classes?queryKey` (scoped),
`GET /get-academy-class/{id}`.

### Grades (`/grade`)

| Method | Path                                                                                 | Purpose                                                                                                                                                                                                                                  |
|--------|--------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| POST   | `/insert-student-grade`                                                              | Upsert. `value == null` **deletes** the grade. Resolves the class from the student. Trimester grades match on `(class, subject, student, identifier, gradeType)`; everything else on `(class, subject, student, gradeType, exactMonth)`. |
| GET    | `/get-grades?classId&subjectId&studentId&date`                                       | Flat list                                                                                                                                                                                                                                |
| GET    | `/get-grades-grouped?classId&subjectId&studentId&date&groupByClause&gradeTypePrefix` | `groupByClause ∈ STUDENT\|SUBJECT`; pads the result with empty `Grade`s for every missing `GradeType` matching the prefix, so the UI always receives a full grid                                                                         |
| GET    | `/get-grades-by-component?classId&studentId&yearRange&createDate&component`          | `component = monthly` → `Map<Student, Map<Subject, BigDecimal>>`                                                                                                                                                                         |
| GET    | `/get-grades-by-semester?…&component`                                                | `component ∈ firstSemester\|secondSemester\|anual` → `Map<Student, Map<Subject, Map<Integer, BigDecimal>>>`                                                                                                                              |
| GET    | `/get-grades-by-trimester?classId&trimester&subjectId&studentId`                     | Grouped by student, padded with empty `TRIMESTER_*` slots                                                                                                                                                                                |
| GET    | `/get-grades-years-grouped`                                                          | `["2023-2024", …]`, derived from min/max `YEAR(exactMonth)`                                                                                                                                                                              |

All guarded by `MANAGE_GRADES` except the insert (`ADD_GRADES`).

### Grade calculation (`/calculate-grade`) — **⚠︎ no `@Secured` at all**

* `GET /grades-monthly?academyClassId&subjectId&date` — recompute the month for a class+subject
* `GET /behaviour-monthly?academyClassId&date` — recompute behaviour for the month
* `GET /absence-monthly` — **empty method body**

### Absence (`/absence`)

* `POST /add-absence-grade` (`ADD_GRADES`) — upsert an `AbsenceGrade`; `value == null` deletes
* `GET /find-absence-grade?classId&studentId&yearRange` (`MANAGE_GRADES`) — grouped by student,
  padded with zero rows for every missing `AbsenceGradeType`
* `POST /create` (`MANAGE_TOTAL_ABSENCE`) — create `TotalAbsence` rows for a set of classes
* `GET /filter?academyClassId&activePeriod` (`MANAGE_TOTAL_ABSENCE`)
* `DELETE /{id}` (`MANAGE_TOTAL_ABSENCE`)

### Change requests (`/change-request`)

* `GET /get-change-requests?classId&studentId&date` (`VIEW_CHANGE_REQUESTS`) — scoped
* `POST /create-change-request` (`MANAGE_CHANGE_REQUESTS`)
* `PUT /change-request-status` (`MANAGE_CHANGE_REQUESTS`) — approval applies the new value to the
  grade and emails the guardian
* `GET /get-last-update-time` — **⚠︎ unsecured**, used for a "new requests" indicator

### Closed periods (`/close-period`)

* `GET /get-period-by-class?academyClassId&gradePrefix&gradeId` → boolean — **⚠︎ unsecured**
* `POST /create-closed-period` (`MANAGE_CLOSED_PERIOD`) — the "publish grades" action
* `GET /get-closed-period-ordered?academyClassId&dateFrom&dateTo` — **⚠︎ unsecured**

### System users (`/system-user`) — all `MANAGE_SYSTEM_USER`

`POST /add-User`, `PUT /update`, `PUT /statuschange/{userId}`, `GET /filter?username&name&active`,
`DELETE /delete/{userId}`.

### System user groups (`/system-user-group`) — all `VIEW_SYSTEM_USER_GROUP`

`GET /get-all`, `GET /filter?name&permission`, `POST /create`, `PUT /edit`, `DELETE /delete/{id}`.
**⚠︎ These return and accept the raw `SystemUserGroup` entity — no DTO.**

### Exports

* `GET /export/semester-word?classId&studentId&yearRange&createDate&component&isDecimal`
  → `.docx` bytes. **⚠︎ unsecured.**
* `GET /test/exportToExcel?classId&createDate&component&isDecimalSystem` (`MANAGE_GRADES`)
* `GET /test/exportToExcel/semester?classId&yearRange&component&…`
* `GET /test/exportToExcel/anual?…`
* `GET /test/exportToExcel/dashbord?…`

**⚠︎ The Excel base path is literally `/test`**, and `ExcelExportController` `@Autowired`s
`GradeController` and calls it as if it were a service.

### Student-facing (`/client/**`) — all `permitAll`, identity from the bearer token

| Path                                                                            | Returns                                                                                                                              |
|---------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------|
| `GET /client/subjects/get-subjects-for-student`                                 | the student's class's subject list                                                                                                   |
| `GET /client/grade/get-grades-for-student?gradeTypePrefix&subjectId&month&year` | per-subject month grades (prefix auto-switched to `TRANSIT` for transit classes)                                                     |
| `GET /client/grade/get-trimester-for-student?subjectId&trimester&year`          | one subject's `TRIMESTER_*` grades                                                                                                   |
| `GET /client/grade/get-trimester-for-student-by-subject?trimester&year`         | `TRIMESTER_GRADE` for every subject                                                                                                  |
| `GET /client/grade/get-grades-for-subject-monthly?subjectId&year`               | all monthly marks for a subject across the year                                                                                      |
| `GET /client/grade/get-grades-for-month?month&year`                             | all subjects' monthly marks + synthetic `behaviour`/`rating`/`absence` rows. **⚠︎ `monthlyGrades.get(0)` throws on an empty result** |
| `GET /client/grade/get-grades-years-grouped`                                    | year ranges                                                                                                                          |
| `GET /client/grade/get-grades-by-semester?yearRange&createDate&component`       | semester matrix                                                                                                                      |
| `GET /client/grade/get-grades-year?yearRange&createDate`                        | annual matrix (`component = "anual"`)                                                                                                |
| `GET /client/grade/get-grades-behaviour?month&year`                             | `BEHAVIOUR_*` grades for the month                                                                                                   |
| `GET /client/grade/get-grades-absence?yearRange&month`                          | `GENERAL_ABSENCE_MONTHLY` grades                                                                                                     |
| `GET /client/grade/get-transient`                                               | boolean: is the student in a transit class                                                                                           |
| `GET /client/absence?yearRange&month`                                           | `AbsenceGrade`s, padded to all 7 periods                                                                                             |
| `GET /client/absence/filter?month&year`                                         | `TotalAbsence` (denominator) for the class                                                                                           |

Every one of these applies the **closed-period cut-off** (§6).

---

## 5. Grade calculation rules

### Monthly subject mark — `calculateGradeMonthly(classId, subjectId, date)`

For each student in the class, load that month's grades for the subject, then:

**Normal class:**

1. `SUMMARY_MONTH` = average of `SUMMARY_ASSIGMENT_1` and `_2`. If only one exists, average it
   with `_RESTORATION` (if present). If neither exists, use `_RESTORATION` alone; if that is
   missing too, throw `GENERAL_SUMMERY_GRADES_NOT_PRESENT`.
2. `SUMMARY_PERCENT` = `SUMMARY_MONTH / 2` (50 % weight)
3. `HOMEWORK_MONTHLY` = plain average of all `GENERAL_HOMEWORK_*` (excluding `MONTHLY`/`PERCENT`);
   `HOMEWORK_PERCENT` = `/ 4` (25 %)
4. `SCHOOL_WORK_MONTH` = plain average of all `GENERAL_SCHOOL_WORK_*`;
   `SCHOOL_WORK_PERCENT` = `/ 4` (25 %)
5. `GENERAL_COMPLETE_MONTHLY` = rounded sum of the three percent components

**Transit class:** the same shape but with no homework component — summary and schoolwork are each
`/ 2` (50 %). It reads `TRANSIT_SUMMARY_ASSIGMENT_*` for the summary while still reading
`GENERAL_SCHOOL_WORK_*` and **writing** `GENERAL_*` result types. **⚠︎ inconsistent.**

`saveGrade` refuses to persist a `BigDecimal.ZERO` result, upserts an existing row of the same
type/month, and normalises Feb→Jan / Oct→Sep.

### Monthly behaviour — `calculateBehaviourMonthly(classId, date)`

Per student, for each of the five criteria (`APPEARING_IN_UNIFORM`, `STUDENT_DELAYS`,
`CLASSROOM_INVENTORY`, `STUDENT_HYGIENE`, `STUDENT_BEHAVIOR`): average the weekly entries →
`*_MONTHLY`. Then `WEEK_AVERAGE_1..6` = average of everything whose enum name starts with
`BEHAVIOUR` and *contains that digit* (**⚠︎ substring matching on enum names — fragile**).
Finally `BEHAVIOUR_MONTHLY` = average of the five criterion monthlies.

Behaviour grades carry `subject = null`. Months with a paired period (Sep+Oct, Jan+Feb) have
6 weeks; others have 4 — the client console re-derives this by slicing the array.

### Semester aggregation — `findGradeBySemester(classId, year, firstSemester)`

Selects `GENERAL_COMPLETE_MONTHLY` (or `TRANSIT_SCHOOL_COMPLETE_MONTHLY`) for months
`{9, 11, 12}` (first) or `{1, 3, 4, 5, 6}` (second) and builds
`Map<Student, Map<Subject, Map<Integer, BigDecimal>>>` where the integer key means:

| Key        | Meaning                                                                                                           |
|------------|-------------------------------------------------------------------------------------------------------------------|
| `1..12`    | that month's mark                                                                                                 |
| `-1`       | semester average: `(diagnosticAverage + monthAverage) / 2`, or just the month average if there are no diagnostics |
| `-2`       | `SHEMOKMEDEBITOBA` — **⚠︎ commented out everywhere, always absent**                                               |
| `-3`, `-4` | `DIAGNOSTICS_1`, `DIAGNOSTICS_2` (semester 1)                                                                     |
| `-5`, `-6` | `DIAGNOSTICS_3`, `DIAGNOSTICS_4` (semester 2)                                                                     |
| `-7`       | behaviour average (synthetic `Subject` named `behaviour1`/`behaviour2`, id `9999`)                                |
| `-8`       | second-semester behaviour, grafted in during the annual merge                                                     |
| `-9`       | absence-hours sum (synthetic subject `absence1`/`absence2`, id `8888`)                                            |
| `-10`      | second-semester absence                                                                                           |

**⚠︎ Synthetic `Subject` objects with hardcoded ids (`9999` behaviour, `8888` absence, `7777`
rating) are injected into the domain result maps and matched downstream by *name*.**

**⚠︎ The year predicate is `year == Y OR year == Y+1` for both semesters** (with a
`//TODO this is problematic` comment in the source), so adjacent academic years bleed together.
The two overloads of `findGradeBySemester` even use *different* month sets for the first semester
(`{9,11,12}` vs `{9,11,12,1}`).

### Annual aggregation — `getAnualGrades(classId, startYear, endYear)`

Merges the two semester maps per (student, subject) into:
`1` = semester 1, `2` = semester 2, `3` = `FINAL_EXAM`,
`4` = `calculateAverage(sem1, sem2, finalExam)` — the average of the non-zero semesters, then
averaged again with the final exam if one exists. Missing subjects are back-filled with zeros.

### Synthetic per-month rows

`fillMissingSubjects` / `fillWithEmptyGradeListOfGradeType` append three pseudo-subjects to the
monthly result:

* `behaviour` (id 9999) — `BEHAVIOUR_MONTHLY` for the month
* `absence` (id 8888) — total absence hours for the month, read from `ABSENCE_GRADES`
* `rating` (id 7777) — rounded average of all non-zero subject marks

### Trimester model (current)

Grades carry `identifier = trimester` and are keyed by
`(class, subject, student, identifier, gradeType)`. Columns:
`ONGOING_GRADE_1..7` → `INITIAL_KNOWLEDGE_GRADE` (საწყისი ცოდნის ტესტი) →
`PROGRESS_GRADE` (პროგრეს ტესტი) → `FINAL_EXAM_GRADE` (ფინალური ტესტი) →
`TRIMESTER_GRADE` (ტრიმესტრის შეფასება).

**⚠︎ There is no server-side calculation for trimester grades** — `TRIMESTER_GRADE` is typed in by
hand. `getTrimesterGradeBySubject` picks the newest `TRIMESTER_GRADE` per subject and pads missing
subjects with an empty `Grade`.

### Subject ordering

`SubjectOrderUtils.subjectPattern` (Sgs-server) and `ExcelUtils.subjectPattern` (core) are a
**duplicated** hardcoded list of ~36 Georgian subject names ending with `rating`, `behaviour`,
`absence`. Subjects sort by index in this list; unknown subjects sort last. The admin frontend
carries a third copy in `MonthlyGradePage/Helper.js`.

---

## 6. Closed periods — the publish mechanism

Students must not see grades before a teacher has finished a month. The mechanism:

1. A user with `MANAGE_CLOSED_PERIOD` calls `POST /close-period/create-closed-period` with a list
   of their classes.
2. For each class, `ClosedPeriodServiceImpl` upserts three rows — prefix `GENERAL`, `BEHAVIOUR`,
   `TRANSIT` — stamping `lastUpdateTime = now`.
3. It then emails **every student's `ownerMail`** in those classes:
   subject *"IB მთიები - ნიშნების განახლება"*, body pointing at the portal.
4. Every `/client/**` grade query resolves `closedPeriodService.getLatestClosedPeriodBy(classId)`
   and adds `grade.createTime < thatDate` to the query.

So the student console shows a snapshot frozen at the last close. Admin queries have overloads
with and without the cut-off; the admin console uses the uncut ones.

**⚠︎** `createClosedPeriod` returns `null` (its `//TODO change!!` is still there) and the initial
insert sets `id = 0L` explicitly before saving. `getClosedPeriodByClassId` requires a `gradeId` and
reports whether that grade predates the close — the admin console's use of it is partly commented
out.

---

## 7. Email

`EmailServiceImpl` (`spring-boot-starter-mail`), Gmail SMTP.
**⚠︎ Credentials committed in `application.yml`** (`ibmthiebi.student.rating@gmail.com` plus an app
password).

Two triggers, both to `Student.ownerMail`, both skipped when the address is null or shorter than
5 characters:

1. **Period closed** — "new grades are available".
2. **Change request approved** — sends the director's `description` as the body.

---

## 8. Admin console (`admin-console/src/front-ac`)

Single page, no router: `App.js` renders `LoginPage` or `MainContainer`.
`MainContainer` = `Sidebar` + `Content`, and `Content` is a **tab host** — pages open as closable
tabs (`TabNavigation`, `TabPanels`, `TabContextMenu`), state in `navigation-context`.
`useNavigationData.js` declares every page with its required permissions; a page shows if the user
holds **any** of them. Per-page filters are persisted via `utils/filters.js`.

Contexts: `user-context` (login + `hasPermission`), `navigation-context`, `sidebar-context`,
`notification-context`, `backdrop-context`, `react-query-context`, `table-ref-context`,
`initial-data-context`.

### Live pages

| Menu label (ka)                 | Page                                            | Required permissions                              | What it does                                                                                                                                                                                                                                                                                                                                                                 |
|---------------------------------|-------------------------------------------------|---------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| ტრიმესტრის ნიშნები              | `trimester/TrimesterDashBoard`                  | `ADD_GRADES` / `MANAGE_GRADES`                    | **The main grade-entry screen.** Filters: trimester (I/II/III), class, subject, student. Editable MUI DataGrid: 7 ongoing columns (grouped under "მიმდინარე შეფასება") plus initial-knowledge, progress, final and trimester grade. Cell edit → `POST /grade/insert-student-grade` with `identifier = trimester`. A change-request modal is wired but currently unreachable. |
| ეთიკური ნორმა                   | `behaviourPage/BehaviourDashBoard` (1790 lines) | `ADD_GRADES` / `MANAGE_GRADES`                    | Behaviour journal: 5 criteria × up to 6 weeks + monthly, per student per month. Checks `/close-period/get-period-by-class` before allowing an edit; if the period is closed it opens the change-request modal instead of saving. The toolbar can trigger `calculate-grade/behaviour-monthly`.                                                                                |
| გაცდენების ჟურნალი              | `absencePage/AbsenceDashBoard`                  | `ADD_GRADES` / `MANAGE_GRADES`                    | Absence per period (7 columns + "სულ"). Reads `absence/find-absence-grade`, writes `absence/add-absence-grade`; the hook hand-maps `AbsenceGradeType` → (year, month).                                                                                                                                                                                                       |
| მოთხოვნილი ცვლილებები           | `changeRequestPage/ChangeRequestDashBoard`      | `MANAGE_CHANGE_REQUESTS` / `VIEW_CHANGE_REQUESTS` | Approval queue: id, class, student, subject, old value, new value, grade type, explanation. Approve/reject with a director's comment → `PUT /change-request/change-request-status`. Polls `get-last-update-time`.                                                                                                                                                            |
| ტრიმესტრის შემაჯამებელი ნიშნები | `MonthlyGradePage/MonthlyGradeDashBoard`        | `ADD_GRADES` / `MANAGE_GRADES`                    | Read-only class × subject matrix for a month (`component=monthly`). **⚠︎ Every subject column is hardcoded in JSX** (ქართული ლიტ, მათემატიკა, ინგლისური, …) — adding a subject requires a code change.                                                                                                                                                                       |
| წლიური ნიშნები                  | `anualPage/AnualGradeDashBoard`                 | `ADD_GRADES` / `MANAGE_GRADES`                    | Annual matrix: semester 1 / semester 2 / final exam / annual per subject, plus rating, behaviour, absence hours and a remark column. Final-exam cells are editable (`useUpdateFinalExam`). Subject columns are hardcoded here too.                                                                                                                                           |
| ჯამური გაცდენები                | `totalAbsencePage/TotalAbsenceDashBoard`        | `MANAGE_TOTAL_ABSENCE`                            | CRUD over `TotalAbsence`: pick classes + month + total academic hours.                                                                                                                                                                                                                                                                                                       |
| სისტემური მომხმარებელი          | `systemUserPage/SystemUserDashBoard`            | `MANAGE_SYSTEM_USER`                              | CRUD over staff; assign permission groups and classes; toggle active.                                                                                                                                                                                                                                                                                                        |
| საგანები                        | `subjectPage/SubjectDashBoard`                  | `MANAGE_SUBJECT`                                  | CRUD over subjects (name + teacher).                                                                                                                                                                                                                                                                                                                                         |
| მოსწავლეები                     | `studentPage/StudentDashBoard`                  | `MANAGE_STUDENT`                                  | CRUD over students including username/password and guardian email.                                                                                                                                                                                                                                                                                                           |
| კლასები                         | `academyClassPage/AcademyClassDashBoard`        | `MANAGE_ACADEMY_CLASS`                            | CRUD over classes; attach students and subjects; `isTransit` flag.                                                                                                                                                                                                                                                                                                           |
| ნიშნების ჩაკეტვა                | `closePeriod/ClosePeriodDashBoard`              | `MANAGE_CLOSED_PERIOD`                            | Lists close events (class, type, date) and triggers a new close (which publishes and emails).                                                                                                                                                                                                                                                                                |
| უფლებათა ჯგუფები                | `systemUserGroup/SystemUserGroupDashBoard`      | `VIEW_SYSTEM_USER_GROUP`                          | CRUD over permission groups via a checkbox list built from `PERMISSION_OPTIONS`.                                                                                                                                                                                                                                                                                             |

### Dead admin code **⚠︎**

* `HomePage/DashBoard.js` (944 lines) — the old `GENERAL_*` monthly grade journal (summary 1/2/
  restoration, 8 schoolwork columns, percentages, absence, month mark, close-period check,
  change-request modal, `calculate-grade/grades-monthly` trigger). **Commented out of the menu.**
* `semesterPage/SemesterGradeDashBoard.js` (848 lines) + `useExportWord.js` — the semester view and
  all four Excel export hooks. **Commented out of the menu**, so the Excel exports are currently
  unreachable from the UI.
* `closePeriod/` duplicates four files verbatim from `changeRequestPage/`.
* `components/modal/` and `components/modals/` are two near-identical directories.
* `constants/endpoints.js` contains one garbage constant (`'ramerume'`).
* `utils/enums.js` is empty. `gradeTypeTranslator.js` returns `''` for most cases and ends with a
  `case` that has no body.

### Shared admin widgets

`main/components/grid/*` (DataGrid wrapper, paper, footer, toolbar, cell-expand, no-rows overlay),
`main/components/formik/*` (autocomplete, text field, textarea, date/datetime pickers),
`components/modal(s)/*`, `components/buttons/*`, `components/notifications/*`.
Hooks: `useAxios`, `useToggle`, `useUrlToggle`, `useTablePaging`, `useTabbedForm`,
`useMutationWithInvalidation`, `useMutationWithUpdate`, `useMutationWIthOptimisticUpdate` *(sic)*,
`useQueryWithoutCache`, `convertError`.

---

## 9. Client console (`client-console`)

`react-router-dom` v5 with `BrowserRouter`. `App.js` renders `LoginPage` until
`user-context.loggedIn`. Token and student object are stored via `react-secure-storage`. A fixed
`Header` provides "მთავარი" and a `UserBar` with logout.

### Landing — `/` (`afterLoginPage/AfterLoginPage`)

Five cards:

| Card                                                             | Route                  |
|------------------------------------------------------------------|------------------------|
| მოსწავლის ტრიმესტრული შეფასება აკადემიური დისციპლინების მიხედვით | `/grades/:subjectName` |
| მოსწავლის შემაჯამებელი ტრიმესტრული შეფასება                      | `/trimester`           |
| მოსწავლის შეფასება ეთიკური ნორმების მიხედვით                     | `/ethicalPage`         |
| მოსწავლის ტრიმესტრული და წლიური შეფასება                         | `/annual`              |
| მოსწავლის მიერ გაცდენილი საათები                                 | `/absence-page`        |

The first card deep-links to `subjectData[0].name` — **⚠︎ the subject *name* is the route
parameter**, so the page re-fetches all subjects and filters by name.

### `/grades/:id` — `Discipline/Discipline.js`

Per-subject trimester detail. Permanent sidebar listing the student's subjects; dropdowns for
trimester (I/II/III) and year. Renders `DisciplineBox` cards: ongoing grades 1–7, initial knowledge
test, progress test, final grade, trimester grade. A value of `-50` renders as **ჩთ**. Also carries
`Chart.js` / `Aside` / `FooterBox` / `SmallBox` pieces and a `useTransit` call.

### `/trimester` — `MonthlyGrade/index.js`

Summary table: one row per subject with its `TRIMESTER_GRADE` for the chosen trimester/year, plus a
`CustomShefasebaBar` chart. Filters out the synthetic `rating` / `behaviour` / `absence` rows.

### `/ethicalPage` — `ethicalPage/index.js`

Behaviour by month: for each of the five criteria, a box with per-week scores and the monthly score.
Re-derives the 4-vs-6-week shape from the selected month and re-sorts by parsing the trailing digit
out of the `gradeType` string. **⚠︎ enum-name string surgery in the UI.**

### `/annual` — `TsliuriShefaseba/index.js`

Per-subject annual grid (semester 1 / semester 2 / final exam / annual) with a bar chart. Contains a
leftover hardcoded demo dataset in `handleSearch`. **⚠︎**

### `/absence-page` — `AbsencePage/index.js`

Absence per period: a `BarChart`, a DataGrid of the 7 periods, a total, and the `TotalAbsence`
denominator for the class. Hand-maps month index → `AbsenceGradeType` in the UI (the third copy of
that mapping).

### Dead client code **⚠︎**

* `pages/semestruli-shefaseba/` (including a 736-line `SemesterGradeDashBoard` and the Word export
  hook hitting `/export/semester-word`) — **route commented out of `App.js`**, so the Word export is
  unreachable from the UI.
* `src/aaaa.css`.
* `context/` and `hooks/` both define `convertError`; `context/` also holds an unused
  `table-ref-context`, `sidebar-context` and `userDataContext`.
* `components/DataGridStyles.js` duplicates `components/datagrid/DataGridStyles.js`.
* `AbsencePage/index.js` contains a `useMemo` whose body is a malformed expression
  (`cond ? a : b [deps]`) that silently returns the wrong value.

---

## 10. Exports

### Word — `ExportWordServiceImpl`

Apache POI `XWPFDocument`, landscape A4, title
`სკოლა პანსიონ იბ მთიები - {years} - {პირველი|მეორე} სემესტრი`. Paginates at **4 subjects per
page**; per subject emits the semester's month columns plus `სემესტრული` and `შემოქმედობითობა`.
`isDecimal` shifts every value by `+3` (the 7-point ↔ 10-point scale switch). Assumes at least one
student exists and that every (student, subject, monthKey) is present. **⚠︎**

### Excel — `ExcelExportController`

Four `.xlsx` variants (monthly dashboard, semester, annual, "dashbord"). Header row = class +
month, one column per subject sorted by `subjectPattern`, one row per student
(`N. Lastname Firstname`), a trailing **"პედაგოგი"** row carrying each subject's teacher, thin
borders, auto-sized columns. `isDecimalSystem` applies the same `+3` shift.

---

## 11. Error handling & responses

`ControllerExceptionsHandler` (`@ControllerAdvice`) maps `SGSException` → `ErrorInfo`, with the
`SGSExceptionCode` mapped to an HTTP status; other exceptions become generic errors. Messages are
**Georgian strings hardcoded in `ExceptionKeys`** and rendered directly by the frontends via
`convertError` + `notification-context`.

**⚠︎** There are two exception types — `mthiebi.sgs.SGSException` (used) and
`mthiebi.sgs.exception.SgsException` (unused). Several controllers declare `throws Exception` and
several return raw untyped `ResponseEntity`. `SystemUserController.updateUser` and `delete` catch
everything and return `400` with the raw exception message as the body.

---

## 12. Configuration & operations

`core/src/main/resources/application.yml` is the only config file. No profiles, no externalisation,
no `.env`.

```
spring.datasource.url: jdbc:sqlserver://localhost:1433;databaseName=SGS
spring.datasource.username / password        # ⚠︎ committed (currently modified in the working tree)
spring.jpa.hibernate.ddl-auto: update        # ⚠︎ no migrations
spring.mail.*                                # ⚠︎ Gmail app password committed
jwt.secret: secretkey123                     # ⚠︎ committed
```

`HttpsConfiguration` (HTTP→HTTPS redirect connector, `@Profile("!develop")`) and
`CustomWebConfigurerAdapter` are **entirely commented out**. So is `AuthManager` — 282 lines of
commented RSA/JWT scaffolding copied from another project, still referencing `LcmsException`,
`Customer` and `CardType`.

Build: `admin-console/pom.xml` has `copy-mc` / `build-mc` profiles referencing `src/front-mc` —
**⚠︎ a directory that does not exist** (the real app lives in `src/front-ac`) — and they reference
undefined `${frontend.maven.plugin.version}`, `${node.version}`, `${npm.version}` properties.
`client-console/pom.xml` has no build configuration at all.

Swagger/OpenAPI is enabled via `springdoc-openapi-ui` 1.5.2 (`SwaggerConfiguration`) and
whitelisted in the security config.

Duplicate/conflicting dependencies in the poms: iText 5.5.13 **and** 5.5.13.3, POI 5.1.0 (core)
**and** 5.2.3 (root), both `java-jwt` (auth0) and `jjwt` while only `jjwt` is used, and
`spring-boot-devtools` shipped at runtime scope.

---

## 13. Rewrite checklist — behaviour that must be decided, not just ported

**Correctness bugs found while reading the code:**

1. `AcademyClassServiceImpl.attachSubjectsToAcademyClass` loads each subject, **never adds it to the
   list**, then saves an empty `subjectList` — the endpoint wipes a class's subjects.
2. `SystemUserServiceImpl.updateUser` mutates a managed entity but returns `findById(...)` and never
   explicitly saves; `changeActivity` does the same. Works only by accident of `@Transactional`
   dirty checking.
3. `ChangeRequestServiceImpl.createChangeRequest` does
   `gradeRepository.findById(changeRequest.getId())` — it looks the grade up by the **change
   request's** id (there is a `//TODO incorrect code` right there).
4. `ClientGradeController.getGradesForMonth` calls `monthlyGrades.get(0)` with no empty check.
5. `AbsenceController.getTotalAbsences` calls `activePeriod.equals("NaN")` on a parameter declared
   `required = false` → NPE when omitted. Same pattern in `ChangeRequestController`.
6. `AbsenceGradeType.monthNumber` values are wrong for MARCH/APRIL/MAY.
7. The `year == Y || year == Y+1` semester predicate mixes academic years together (flagged
   `//TODO this is problematic` in the source).
8. `Student.equals` and `Subject.equals` blind-cast, and compare `Long` ids with `==` in places.
9. `GradeServiceImpl` compares `student.getId() != studentId` (`long` vs `Long`) — unboxing NPE risk
   and identity-comparison bugs.
10. `calculateAverage` and several helpers use `BigDecimal.equals` (scale-sensitive) where
    `compareTo` is meant.

**Security work required:**

* Plaintext staff passwords (`NoOpPasswordEncoder`); MD5 student passwords.
* Committed DB credentials, SMTP app password and JWT secret.
* Password and full bearer token logged at INFO.
* `/client/**` is `permitAll`, with identity taken from the token subject and no per-student
  authorization check.
* Unsecured endpoints: all of `/calculate-grade`, `/export/semester-word`,
  `/close-period/get-period-by-class`, `/close-period/get-closed-period-ordered`,
  `/change-request/get-last-update-time`.
* Write operations on permission groups gated by a *view* permission.
* `CORS allowedOrigins("*")` on every path.
* `/authenticate-student` returns the student's password hash.

**Design decisions to make:**

* **Pick one grading model.** Ship the trimester model; decide whether the monthly/semester/annual/
  diagnostics machinery is history to preserve read-only, data to migrate, or code to delete.
* Replace the `GradeType` enum + `startsWith(prefix)` string matching with a real structure
  (assessment definition, component, weight, period) so a new assessment type does not require a
  code change and a redeploy.
* Kill the synthetic `Subject` rows (ids 7777/8888/9999) and the negative-integer map keys — model
  rating, behaviour and absence as first-class fields.
* Make subject ordering data (a sort index on `Subject`), not three copies of a hardcoded list.
* Make grid columns data-driven — the monthly/annual/semester screens hardcode Georgian subject
  names in JSX.
* Decide the absence story: `AbsenceGrade` and the `*_ABSENCE_*` `GradeType`s are two parallel
  stores for the same fact, and the code that kept them in sync (`addAbsenceGradeIfNecessary`) is
  commented out.
* Rethink "closed period" as an explicit publication/versioning concept rather than a
  `createTime < timestamp` filter smeared across every query.
* Introduce schema migrations (Flyway/Liquibase) before anything else.
* Add i18n instead of Georgian string literals in entities, exceptions and components.
* There are **zero tests** in the repository.
