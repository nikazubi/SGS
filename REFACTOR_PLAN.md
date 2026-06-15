# SGS — Refactoring Plan

Goal: make SGS **safer, smoother and faster** without a risky big-bang rewrite.
The layering is sound, so this is incremental modernisation, not a rebuild.

Ordering principle: **stop the bleeding (security) → make change safe (tests +
migrations) → modernise → optimise.** Each phase ships independently and leaves the
app working.

---

## Phase 0 — Stop the bleeding (security & secrets) · ~1–2 days · do first

These are live-production data risks. Highest impact, lowest effort.

1. **Hash passwords.** Swap `NoOpPasswordEncoder` → `BCryptPasswordEncoder`. Write a
   one-off migration that BCrypts existing plaintext passwords (or force-reset).
   *(`SecurityConfiguration.java`)*
2. **Get secrets out of git.** Move DB creds, mail password, `jwt.secret` to
   environment variables / Spring config import. Rotate every leaked secret
   (DB password, Gmail app password, JWT signing key) — they are in history.
   Add `application.yml` secret keys to `.gitignore`; commit an
   `application-example.yml`.
3. **Lock down `/client/**`.** Replace `permitAll()` with real authentication for
   student endpoints; keep only `/authenticate*` and Swagger public.
   *(`SecurityConfiguration.java:96`)*
4. **Stop logging tokens.** Remove the `Authorization`-header log line; drop log
   level to DEBUG for the filter. *(`JwtFilter.java:36`)*
5. **Strong JWT secret**, loaded from env, ≥256-bit. Pick **one** JWT library
   (keep `java-jwt`, drop `jjwt 0.9.1`).

**Exit:** no plaintext passwords, no secrets in source, client API authenticated,
secrets rotated.

---

## Phase 1 — Make change safe · ~3–5 days

You cannot refactor confidently with zero tests and Hibernate-managed schema.

6. **Introduce DB migrations (Flyway).** Set `ddl-auto: validate`. Baseline the
   current schema as `V1__baseline.sql`; every future change is a versioned script.
   Removes silent prod schema drift.
7. **Spring profiles.** Split `application.yml` into `-dev` / `-prod`; enable
   `show-sql` + SQL formatting in dev only. Frontend: use
   `REACT_APP_BACKEND_BASE_URL` from `.env.{development,production}` and **delete the
   commented baseURL toggling** in `axios.js`.
8. **Backend test harness.** Add JUnit 5 + Testcontainers (SQL Server) or H2.
   Cover the highest-risk logic **first**: grade insert/aggregation, the month-shift
   rules, semester/annual calculation, change-request flow. These are the rules
   nobody remembers — pin them before touching them.
9. **Frontend test harness.** Vitest/RTL smoke tests on auth flow + one dashboard.
10. **CI.** A GitHub Actions pipeline: `mvn verify` + frontend build/test on every
    push. Stops "works on my machine" regressions.

**Exit:** schema under version control, profiles split, core business rules pinned
by tests, CI green.

---

## Phase 2 — Backend cleanup · ~1 week

11. **Break up the god classes.**
    - `ExcelExportController` (768 LOC): move all export logic into a service
      (`ExcelExportService`); controller only wires HTTP. Mirror the existing
      `ExportWordService` pattern.
    - `GradeServiceImpl` (858 LOC): split by concern — grade CRUD, grade
      *calculation*, absence-as-grade — extract the magic month/diagnostic rules
      into a documented, tested `GradePeriodResolver`.
12. **Constructor injection** everywhere (`@RequiredArgsConstructor` + `final`
    fields). Removes field `@Autowired`, makes dependencies explicit and testable.
13. **Delete dead code** — commented blocks, the no-op `addAbsenceGradeIfNecessary`,
    unused `utils/` module if empty.
14. **Profile the slow queries** (now that dev `show-sql` is on). Add indexes for
    grade lookups by (class, subject, student, type, month); fix any N+1 in
    aggregation with fetch joins or projections.

**Exit:** no class > ~300 LOC, constructor injection, indexed hot queries.

---

## Phase 3 — Frontend modernisation · ~1.5–2 weeks

Biggest lever for "smooth and fast" (bundle size + build speed).

15. **Kill dual MUI.** Pick **MUI v5** and migrate the remaining `@material-ui/*` v4
    usage off it. Drop `material-table` + `react-table` in favour of the single
    `@mui/x-data-grid` already in use. This alone cuts bundle weight and styling
    bugs dramatically.
16. **CRA → Vite.** Replace `react-scripts` 4 with Vite. Dramatically faster dev
    server and builds; removes the `react-error-overlay`/`SKIP_PREFLIGHT_CHECK`
    hacks.
17. **Replace `moment` with `date-fns`** (already a dependency) — smaller bundle.
   Bump `axios` to a current version.
18. **De-duplicate the two consoles.** Extract shared `FlexBox`, `avatar/*`,
    contexts, hooks, axios setup, `DataGridStyles` into a shared
    `packages/ui-common` (or a small internal package). Today they are copy-pasted.
19. **Split the 1790-LOC `BehaviourDashBoard` / 944-LOC `DashBoard`.** Break into
    sub-components, lazy-load route bundles (`React.lazy`), memoise heavy grids.
20. **Fix the auth UX.** Implement real logout (admin `logout()` is a no-op) and
    either finish or remove the commented token-refresh flow — pick one and make it
    real.

**Exit:** single UI library, Vite builds, shared code factored out, route-level code
splitting, working logout.

---

## Phase 4 — Platform upgrade (optional, after the above) · ~1 week

21. **Spring Boot 2.4 → 3.x** (Java 17, `jakarta.*` namespace). The Phase-1 tests
    make this tractable. Note `WebSecurityConfigurerAdapter` is removed in Spring
    Security 6 → migrate to the `SecurityFilterChain` bean style.
22. **React 17 → 18**, react-query v3 → TanStack Query v5, react-router v5 → v6.
23. Containerise (Dockerfile + compose for app + SQL Server) for reproducible
    deploys.

---

## Quick wins (do any time, < 1 hour each)
- Delete the stray tracked file under `node_modules/`; remove the second lockfile
  (`yarn.lock` **or** `package-lock.json`, not both).
- Add a real `.gitignore` entry set; stop committing `build/`.
- Write meaningful commit messages going forward.
- Replace `System.out`/header logging with structured logging.

## Suggested sequencing
```
Phase 0  █ security            (this week — non-negotiable)
Phase 1  ██ safety net         (before any refactor)
Phase 2  ███ backend cleanup   ─┐ can run in parallel
Phase 3  ████ frontend         ─┘ by different focus areas
Phase 4  ██ platform upgrade   (last, gated by Phase 1 tests)
```

Don't start Phase 2/3 refactors before Phase 1's tests exist — that's how the
"filters disappear once search is clicked… it is a mystery" bugs happen.
