# Frontend ↔ Backend contract notes (for the upcoming frontend refactor)

These are coupling points discovered while refactoring the backend. They are **intentional**
("features that look like bugs"). When we refactor the frontends (admin-console / client-console)
we must either keep these exact values **or** change both sides together.

## 1. Synthetic "pseudo-subject" rows (sentinel subject ids)
The grade dashboards receive fabricated subject rows that do not exist in the DB. The frontend keys
off these **exact ids**. Centralised on the backend in `utils/GradeViewConstants.java`.

| Column     | Sentinel subject id | Name string             |
|------------|---------------------|-------------------------|
| Behaviour  | `9999`              | `behaviour` / `behaviour1` / `behaviour2` |
| Absence    | `8888`              | `absence` / `absence1` / `absence2`       |
| Rating     | `7777`              | `rating`                |

Also synthetic **grade ids** `1` (rating), `2` (absence), `3` (behaviour) are set on the monthly view.

➡️ **Frontend TODO:** find where these ids/names are matched (look for `9999`, `8888`, `7777`,
`"behaviour"`, `"absence"`, `"rating"`) and replace magic numbers with shared named constants on the
frontend too. Keep values identical unless both sides change together.

## 2. Magic integer map-keys (grade positioning)
Semester/annual endpoints return `Map<Subject, Map<Integer, BigDecimal>>` where the **integer key
encodes the cell position** the frontend renders. Documented in `GradePeriods` / service javadoc.

Observed keys: `-1` (final/computed average), `-3/-4` (diagnostics 1/2), `-5/-6` (diagnostics 3/4),
`-7/-8` (behaviour semester 1/2), `-9/-10` (absence semester 1/2), and positive `1..12`
(month columns + computed cells `1,2,3,4` for annual: sem1, sem2, final exam, average).

Per-semester fill key-sets differ:
- first semester: `[-9,-7,-4,-3,-2,-1, 9,11,12]`
- second semester: `[-9,-7,-6,-5,-2,-1, 1,3,4,5,6]`

➡️ **Frontend TODO:** the components reading these maps (`SemesterGradeDashBoard`,
`AnualGradeDashBoard`, monthly views) hardcode the same indices. Keep a single shared map of
"key → meaning" on the frontend mirroring the backend.

## 3. Reporting-calendar quirks (now centralised on backend in `GradePeriods`)
- February is reported under January; October under September.
- Diagnostics roll to December (1/2) or June (3/4).
- Default year fallback is `2023` in several endpoints when no `yearRange` is supplied.

➡️ **Frontend TODO:** any month pickers / labels must account for Feb→Jan and Oct→Sep merging so the
UI matches what the backend stores/returns.

## 4. Auth / axios issues to fix on the frontend (already noted in REFACTOR_PLAN)
- `client-console` and `admin-console` `axios.js` have the entire **token-refresh flow commented out**;
  admin `logout()` is a **no-op** → a 401 never logs the user out.
- `baseURL` is hardcoded instead of using `REACT_APP_BACKEND_BASE_URL` (the env var is commented out).
- Backend currently leaves `/client/**` public — when we secure it (Phase 0), the client-console must
  send the JWT on those calls.

## 5. Confirmed frontend usage locations (verified 2026-06-16)
Sentinel ids are matched here — keep values identical when refactoring:
- `client-console/src/pages/TsliuriShefaseba/useGradeAnual.js:11` — filters `subject.id !== 8888 && subject.id !== 9999`.

Magic name-keys (`behaviour1/2`, `absence1/2`, `rating`) are consumed by these dashboards:
- `admin-console/.../anualPage/AnualGradeDashBoard.js`
- `admin-console/.../MonthlyGradePage/MonthlyGradeDashBoard.js`
- `admin-console/.../semesterPage/SemesterGradeDashBoard.js`
- `client-console/src/pages/MonthlyGrade/index.js`
- `client-console/src/pages/semestruli-shefaseba/SemesterGradeDashBoard.js`

Suggested first frontend step: create a single `gradeContract.js` (per app, or shared) exporting
`BEHAVIOUR_SUBJECT_ID=9999`, `ABSENCE_SUBJECT_ID=8888`, `RATING_SUBJECT_ID=7777` and the map-key meanings,
then replace the magic literals in the files above — mirroring backend `GradeViewConstants` / `GradePeriods`.

_Add to this file as more coupling points surface during the backend work._
