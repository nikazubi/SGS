# SGS — School Grade System: Architecture & Context

> Analysis snapshot — 2026-06-16. Internal grading platform for IB Mthiebi school
> (Georgian-language UI). Live in production at `api.ibmthiebistudents.edu.ge`.

## 1. What it does

SGS manages student assessment for a school:

- **Grades** — monthly grades, diagnostics, trimester/semester/annual grades per
  student, subject and academy class.
- **Absences** — per-lesson absence grades and total absence tracking.
- **Behaviour / discipline** — "ethical" and discipline scoring.
- **Reports / exports** — semester & annual report cards exported to **Word** and
  **Excel** (Georgian report templates).
- **Change requests** — teachers request grade changes, admins approve/reject.
- **Users & permissions** — system users grouped with per-feature permissions.

Domain vocabulary is transliterated Georgian:
`shefaseba` = assessment/grade, `tsliuri` = annual, `semestruli` = semester,
`trimester`, `behaviour/ethical` = conduct.

## 2. Repository layout (single backend module + 2 React apps)

> **2026-06-16:** the former 5-module Maven reactor (`Sgs-model` + `Sgs-server` +
> `core` + two stale frontend module poms) was **merged into one backend module**
> (`core`). All backend code now lives under `core/` as `mthiebi.sgs.*` packages.
> Build with `./mvnw -f core/pom.xml clean package` → `core-0.0.1-SNAPSHOT.war`
> (unchanged Tomcat deploy artifact).

```
SGS
├── core                         Single Spring Boot backend module (war)
│   └── src/main/java/mthiebi/sgs
│       ├── SgsApplication.java
│       ├── configuration/       security, web, swagger config
│       ├── controllers/         REST controllers (+ clientconsolecontrollers/)
│       ├── service/ + impl/     service interfaces and implementations
│       ├── repository/          Spring Data + custom QueryDSL repos
│       ├── models/              JPA entities + enums
│       ├── dto/                 DTOs + MapStruct mappers
│       ├── filter/ jwtmodels/   JWT filter + token models
│       ├── auth/ SMTP/          AuthManager, email
│       ├── handler/             centralized exception handling
│       ├── components/ db/      security UserDetails, QueryDSL factory
│       ├── exception/ + SGSException  exception types (see cleanup note below)
│       └── utils/               JWT/excel/util helpers (incl. SubjectOrderUtils)
├── admin-console  React 17 app (teachers/admins) under src/front-ac
└── client-console React 17 app (students/parents)
```

Layering is now expressed by **packages** (controller → service → repository),
not modules — the right altitude for a single deployable.

### Backend stack
- Spring Boot **2.4.3** (Spring Security, Data JPA, Data REST, Web)
- Java **11** declared in `pom.xml` (`HELP.md` notes a move to 17 — inconsistent)
- **MS SQL Server** via `mssql-jdbc`, Hibernate `ddl-auto: update`
- **QueryDSL** (APT processor) for custom repository queries
- **MapStruct** for entity↔DTO mapping
- JWT: **two** libraries on the classpath — `com.auth0:java-jwt` and the ancient
  `io.jsonwebtoken:jjwt:0.9.1`
- Exports: iText + PDFBox + Apache POI
- API docs: springdoc/Swagger UI

### Frontend stack (both consoles)
- React **17**, `react-scripts` **4** (Create React App)
- **Two MUI generations at once**: `@material-ui/*` v4 **and** `@mui/*` v5, plus
  `material-table`, `react-table`, `@mui/x-data-grid`, `react-virtuoso`
- `react-query` v3, `formik` + `yup`, `axios` 0.21.4, `moment`, `react-router-dom` v5
- `react-secure-storage` for JWT storage

## 3. How auth works

1. `POST /authenticate` (admin) / `/authenticate-student` (client) → `AuthController`
   → `AuthManager` validates credentials, `UtilsJwt` issues a JWT.
2. Frontend stores token in `react-secure-storage` and attaches
   `Authorization: Bearer <token>` via an axios interceptor.
3. `JwtFilter` (once per request) parses the token, loads `UserDetails`, sets the
   `SecurityContext`. Method-level security via `@EnableGlobalMethodSecurity`.
4. Stateless sessions; CSRF disabled.

## 4. Notable strengths (keep these)

- **Clean layering** — controller / service-interface / service-impl / repository
  separation is genuinely good for a first project.
- DTOs + MapStruct keep entities out of the API surface.
- Custom QueryDSL repositories isolate complex query logic.
- Centralised exception handling (`ControllerExceptionsHandler`, `SGSException`
  with codes).
- Feature-foldered React pages, shared contexts/hooks, react-query for server state.

## 5. Risk & debt inventory

### 🔴 Security (fix first — production system with real student data)
| # | Issue | Location |
|---|-------|----------|
| S1 | **Passwords stored in plaintext** — `NoOpPasswordEncoder`, BCrypt commented out | `SecurityConfiguration.java:64` |
| S2 | **Secrets committed to git** — DB user/pass, Gmail app password, `jwt.secret: secretkey123` | `core/.../application.yml` |
| S3 | **Entire client API is public** — `antMatchers("/client/**").permitAll()` | `SecurityConfiguration.java:96` |
| S4 | **JWT/Bearer token logged on every request** at INFO | `JwtFilter.java:36` |
| S5 | Weak JWT secret + two competing JWT libs (`jjwt 0.9.1` is years-old) | `pom.xml` |
| S6 | `baseURL` switched by commenting code; secrets toggled live (in current `git status`) | `*/axios.js`, `application.yml` |

### 🟠 Config / ops
- `ddl-auto: update` on a **production** DB — schema drift, no migration history,
  no rollback. No Flyway/Liquibase.
- No Spring profiles (dev/test/prod). Everything hardcoded.
- Frontend `REACT_APP_BACKEND_BASE_URL` exists but is commented out in favour of a
  hardcoded URL.

### 🟠 Dependencies (all EOL / aging)
- Spring Boot 2.4.3 (EOL since 2021), React 17 + CRA 4 (unmaintained), axios 0.21.4
  (known CVEs), `moment` (deprecated).
- **Dual MUI v4 + v5** = duplicated styling engines, large bundle, inconsistent UI,
  upgrade blocker.
- Two lockfiles (`yarn.lock` **and** `package-lock.json`) in client-console.

### 🟡 Code quality
- **Zero tests** anywhere (backend and frontend).
- God classes: `GradeServiceImpl` 858 LOC, `ExcelExportController` 768 LOC (export
  logic living in a controller), `BehaviourDashBoard.js` **1790 LOC**,
  `HomePage/DashBoard.js` 944, `SemesterGradeDashBoard.js` 848.
- **Field injection** (`@Autowired` on fields) everywhere instead of constructors.
- **Dead/commented code**: token-refresh flow fully commented in both `axios.js`;
  admin `logout()` is a no-op → a 401 never actually logs the user out.
- Magic business rules inline (month shifting Feb→Jan / Oct→Sep, hardcoded grade
  types) with no documentation or tests.
- Large copy-paste duplication between the two consoles (`FlexBox`, `avatar/*`,
  contexts, hooks, `DataGridStyles`, axios setup).
- Mixed Georgian/English + transliterated names; joke commit messages — no usable
  history for debugging.

### 🟡 Performance ("smooth and fast" goals)
- CRA build/dev is slow; dual-MUI bundle is heavy → slow first paint.
- 1000+ LOC dashboard components re-render large trees; little memoisation.
- Grade aggregation queries are hand-built; likely N+1 / unindexed scans worth
  profiling once `show-sql` is enabled in a dev profile.

## 6. Build & run (current)

- Backend: `./mvnw clean install` then deploy `core` WAR to Tomcat (or
  `spring-boot:run` on `core`). Needs SQL Server on `localhost:1433`, DB `SGS`.
- Frontends: `npm install && npm start` inside `admin-console/src/front-ac` and
  `client-console`.
