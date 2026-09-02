# SGS rewrite — project state

Written as a handoff. If you are picking this up cold (or after a conversation was compacted),
read this first: it says what the other documents contain, every decision taken and why, what has
actually been built, and how to build and run it.

Last updated after phase 10 (the two absence registers) was completed and verified.

---

## 1. The documents

| File                          | What it holds                                                                                                                                                                                                                                        |
|-------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `SYSTEM-FUNCTIONALITY.md`     | The **existing** system, exhaustively: every endpoint, screen, grade type, calculation, and the ~30 bugs and security problems found in it. Written before any rewrite work, so it is the reference for "what must the new thing do".                |
| `CLIENT-BRIEF-2026.md`        | Full English translation of the client's 2026 brief (`ელ. აკად. ჟურნალი_2026.docx`), including the Word-shape wireframes and merged table headers that a plain copy-paste loses. Ends with an assessment of what the brief does and does not settle. |
| `REWRITE-DATA-MODEL.md`       | The new data model: periods, templates, derivation, `grade_entry`, publication, authorization, plus what each thing replaces.                                                                                                                        |
| `REWRITE-RECOMPUTE-ENGINE.md` | The calculation engine: dependency graph, evaluation semantics, the write path, mid-year version policy, performance envelope, test list.                                                                                                            |
| `REWRITE-GRADE-ENTRY.md`      | **Phase 2 design.** The grid read endpoint, publication columns and why the legacy timestamp cut-off cannot be carried forward, write-path enforcement, and the grade entry screen.                                                                  |
| `REWRITE-PUBLICATION.md`      | **Phase 3 design.** Publishing to parents, the change-request flow, and why an approval republishes the dependency closure.                                                                                                                          |
| `REWRITE-EXPORTS.md`          | **Phase 4 design.** Why the legacy exports cannot be reproduced literally, the two shapes that replace them, and the subject-order and decimal defects found in the old ones.                                                                        |
| `REWRITE-ABSENCE.md`          | **Phase 10 design.** The two absence registers: why weeks became days, the transposed grid, and the notification window.                                                                                                                             |
| `REWRITE-CONTENT.md`          | **Phase 8 design.** One table for the brief's five content modules, why publication is frozen, and the sanitiser that makes rich text safe.                                                                                                          |
| `REWRITE-CONVERSION.md`       | **Phase 7 design.** The conversion formula: why the school needs a 10-point view at all, where it applies, why nothing is stored converted, and the one mapping shape it cannot express.                                                             |
| `REWRITE-JOURNALS.md`         | **Phase 5 design.** Journals as data — created, named and seen in the menu by the school. Supersedes the earlier template-editor plan and deletes `TemplateScope`.                                                                                   |
| `FOLLOW-UPS.md`               | Decisions taken that leave something outstanding, and work deferred on purpose. Not design questions and not bugs — things to circle back to.                                                                                                        |
| `CLIENT-QUESTIONS.md`         | Questions only the school can answer, with what we built in the meantime and the cost of a different answer. **Added to on request only.**                                                                                                           |
| `docs/client-brief-2026/`     | The original `.docx` and its five extracted images.                                                                                                                                                                                                  |
| `db/001_schema.sql`           | The schema, **generated from the entities** by a test. Do not hand-edit.                                                                                                                                                                             |
| `db/002_indexes.sql`          | The one index JPA cannot express. Hand-written.                                                                                                                                                                                                      |

---

## 2. What the project is

A grading portal for **სკოლა პანსიონ IB მთიები**, live at `ibmthiebistudentrating.edu.ge`.
Spring Boot 2.4.3 / Java 11 / MS SQL Server, plus two Create-React-App frontends
(`admin-console` for staff, `client-console` for parents).

We are rewriting the grading core first, then adding six new modules the client has asked for
(daily schedule, meals, homework, student characterization, news/posts, primary school).

---

## 3. Phase roadmap

Agreed in the phase 2 discussion. Each phase is discussed, documented, then built.

| Phase | Scope                                                                                                                | State                                                                                                                                                                                                                                                                                                                                                                     |
|-------|----------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1     | Backend foundation — model, engine, write path                                                                       | **done**, 44 tests green                                                                                                                                                                                                                                                                                                                                                  |
| 2     | Grade entry screen — grid read endpoint, publication columns, write-path lock, the UI                                | **done**, 53 tests green                                                                                                                                                                                                                                                                                                                                                  |
| 3     | Publish action and the change-request flow                                                                           | **done**, 67 tests green                                                                                                                                                                                                                                                                                                                                                  |
| 4     | Exports, regenerated from the template                                                                               | **done** (bulk button deferred — `CLIENT-QUESTIONS.md` §2), 76 tests green                                                                                                                                                                                                                                                                                                |
| 5     | Journals as data — the wizard, the column and formula editor, cross-journal references, migration                    | **done**, reviewed and repaired, 93 tests green                                                                                                                                                                                                                                                                                                                           |
| 6     | Parent console — journal-driven, one renderer                                                                        | **done**, 104 tests green                                                                                                                                                                                                                                                                                                                                                 |
| —     | **Ethics and absence journals** — no longer a phase. They became two rows and their columns once journals were data. |
| 7     | The conversion formula, bulk export, the brief §4 column gaps                                                        | **done**, 121 tests green                                                                                                                                                                                                                                                                                                                                                 |
| 8     | Content substrate + **homework**, staff side                                                                         | **done**, 145 tests green                                                                                                                                                                                                                                                                                                                                                 |
| 9     | Daily schedule, menu, student characterization, news — staff side                                                    | **done**, 160 tests green                                                                                                                                                                                                                                                                                                                                                 |
| 10    | Daily and monthly absence registers                                                                                  | **done**, 201 tests green. Daily absence was later extracted from `grade_entry` into its own table and the period reach given one owner - see below.                                                                                                                                                                                                                      |
| 11    | **Parent side** for every content module, as one piece                                                               | **functionality done**, 224 tests green. Homework, news, absence, daily schedule, menu and the child's description; primary gets the register but no gradebook. **Visuals outstanding** - the primary theme waits on the school choosing from what they were sent, and the console has still to be made to work on a phone. `PARENT-COMPONENTS.md` is the brief for both. |
| 12    | **Roster against the new model** — students, classes, subjects, teachers, year rollover                              | **done**, 253 tests green. The gap that made it urgent: the surviving admin pages wrote `dbo` while everything the rewrite built reads `sgs`, so a child added in the console was invisible to the gradebook. `REWRITE-ROSTER.md`.                                                                                                                                        |

The parent side of the content modules was pulled out into phase 11 during the phase 8
discussion: the school wants to make its UI decisions in one go rather than module by module.

Phases 7–10 were set in the phase 7 discussion, after the school sent a second brief.
The "seven new modules" in it are really **two groups**: homework, daily schedule, menu,
student characterization and news are one content model configured five ways — phase 8
builds it, phase 9 configures it four more times — while the two absence screens are
journals and reuse phase 5 wholesale.

**Unattributed:** real URLs replacing the tab-host shell (decision 20). Agreed in principle,
deferred out of phase 2, not yet assigned to a phase.

---

## 4. Decision log

Everything settled in discussion. This is the part most easily lost.

### Product and scope

| #  | Decision                                                                                                                                                                                        |
|----|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1  | **Rewrite first, new modules after.** Configurability lands before any new module.                                                                                                              |
| 2  | **The trimester model wins.** The legacy monthly/semester/diagnostics machinery is not mentioned once in the client brief and is not being carried forward.                                     |
| 3  | **Grids and formulas are configurable from the UI**, behind a permission. This is the central justification for the rewrite.                                                                    |
| 4  | **Derivation is structured, not a formula language.** Weighted sums over user-selected columns; no free-text expressions to evaluate.                                                           |
| 5  | **Only the calculations that exist today plus those in the brief** need supporting. Anything else is added when it appears.                                                                     |
| 6  | **Existing exports are kept** (4 Excel + 1 Word). No Excel *paste* needed.                                                                                                                      |
| 7  | **Two frontends stay** — staff and parent are separate apps.                                                                                                                                    |
| 8  | **Change requests are a must-keep**, not to be replaced by role-based locking.                                                                                                                  |
| 9  | **One login per child**, as today. `guardian_email` stays a contact field, not an account.                                                                                                      |
| 10 | **Annual data wipe stays this round.** Acknowledged as a workaround rather than a requirement, so `academic_year` and `enrollment` remain in the schema and the wipe becomes an explicit purge. |

### Architecture

| #  | Decision                                                                                                                                                                                                                                                                                                                                                                |
|----|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 11 | **One row per grade** (`grade_entry`), not a wide table and not a JSON document. Considered seriously — JSON is ~6× smaller and ~12× fewer rows — but rejected because at this scale (~950k rows/year, ~115 MB) those wins buy nothing, while the costs land on per-cell concurrency and on referential integrity for change requests. Revisit if volume grows 100×.    |
| 12 | **Derived values are materialised**, never computed on read. The old annual page issued ~2,400 queries because it recomputed everything each time.                                                                                                                                                                                                                      |
| 13 | **Permission groups, not roles.** Keep the existing model: freely-named groups, pick-and-choose permissions, plus a scope grant. The brief's "General Director" etc. become groups someone creates, not types in code. Two refinements: permissions become a join table rather than a CSV string, and scope gains `ALL_SCHOOLS` / `SCHOOL` / `CLASS` levels.            |
| 14 | **Last-writer audit for now.** A `grade_entry_history` table is additive and can come later; the change-request table already covers disputed changes.                                                                                                                                                                                                                  |
| 15 | **Optimistic concurrency, per cell.** Cells are independent, so a conflict only exists when two people edit the same one. Conflicts are returned per cell; the rest of the batch still lands.                                                                                                                                                                           |
| 16 | **Publication by snapshot**, replacing the `createTime < timestamp` filter. Not yet built.                                                                                                                                                                                                                                                                              |
| 17 | **No Flyway.** Numbered `.sql` scripts, applied by hand. `ddl-auto: update` stays for local development; production gets a generated, reviewed script.                                                                                                                                                                                                                  |
| 18 | **New tables live in a `sgs` schema**, alongside legacy `dbo`, because `subject`, `class_subject` and `change_request` would otherwise collide. Drop `dbo` at cutover.                                                                                                                                                                                                  |
| 19 | **Same stack, same versions.** Spring Boot 2.4.3, Java 11, React 17. Boot 2.7.18 and a CRA→Vite swap were offered as a cheap middle path and not taken up.                                                                                                                                                                                                              |
| 20 | **Real URLs in the staff console** (phase 2), replacing the tabbed SPA with no router.                                                                                                                                                                                                                                                                                  |
| 21 | ~~**Tests are narrow but real**: nothing on controllers or UI.~~ **Revised.** Two review passes found that every fault which stopped the product working lived in exactly that gap — security wiring, script order, React, transaction semantics — while the engine came back clean both times. `ApplicationWiringIT` now boots the real context and speaks HTTP to it. |

### Grading semantics

| #  | Decision                                                                                                                                                                                                                                                                 |
|----|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 22 | **Calculated columns stay editable by default.** `allowOverride` is per column and defaults to **true** — a formula is a convenience, not a cage. Locking a column is the deliberate exception.                                                                          |
| 23 | **An override is sticky**: held through recomputes, still feeding everything downstream, released only when explicitly cleared. Phase 2 needs a visible "revert to calculated" affordance.                                                                               |
| 24 | **Nothing recalculates on its own when config changes mid-year.** A period stays on the template version its marks were first entered under. Migrating an existing period is a deliberate action with a "recalculate?" prompt, landing in the template editor (phase 3). |
| 25 | **Rounding happens once, on the final value.** Intermediates carry full precision.                                                                                                                                                                                       |
| 26 | **Missing data is normal and yields null; bad configuration fails loudly.** Blanks are never silently zero unless a rule says `AS_ZERO`.                                                                                                                                 |
| 27 | **Weights renormalise by default** when a term is missing, so a student without the 30% final test is not quietly capped at 70% of scale.                                                                                                                                |
| 28 | **Template validation happens at save time**, so the evaluator has no runtime error path for bad config — a teacher entering marks can never be shown a configuration error.                                                                                             |
| 29 | **The explain view runs the same `evaluate()` call in trace mode.** Never a second implementation, which would drift and be believed anyway. Nothing is stored.                                                                                                          |

### Phase 2 (grade entry)

| #  | Decision                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
|----|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 30 | **Edit `admin-console` in place**, rather than starting a new app beside it.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| 31 | **MUI X DataGrid stays.** Two objections raised against it were wrong: Enter already moves down a column (`cellToFocusAfter = 'below'`), and `processRowUpdate` only fights batching if a network call is awaited inside it. The loss is column pinning (Pro-only), which matters on the wide annual matrix and not on a 12-column entry grid. Rebuilding pagination, grouping, virtualization and keyboard nav by hand is weeks whose best outcome is parity.                                                                                                                                                                                                                        |
| 32 | **Autosave stays**, because teachers are used to it — but leaving a cell stops being a transaction. Local `processRowUpdate`, debounced 800 ms, one batch for every dirty cell.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| 33 | **Publication becomes per cell and explicit** — `published_value`, `published_special_value`, `published_at` on `grade_entry` — replacing the legacy `createTime < cutoff` filter, which does not actually hold (see section 5).                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| 34 | **The publication lock blocks direct edits, never recomputation.** Recompute writes `value` only. A published derived cell may diverge from `published_value`; that divergence *is* the "changed since publication" state phase 3 resolves.                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| 35 | **Read and write share one template-version resolver.** If they disagreed, the UI would render columns the write path rejects.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| 36 | **`String` maps to `nvarchar`, not `varchar`.** The collation is `SQL_Latin1_General_CP1_CI_AS`, which has no code page for Georgian, so every label would have stored `??????????`. Set globally via `hibernate.use_nationalized_character_data`.                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| 37 | **Legacy subjects fold by name on migration, and the teacher moves to `class_subject.teacher_name` first.** `dbo.subject` is really "subject taught by teacher X" — 143 rows, 51 distinct names. Folding is right, but it destroys the teacher association, and migration is a one-way door. All 658 class/subject pairs have exactly one teacher, so nothing needed reconciling. It stays a *name*: only **3 of 98** teachers match a `system_user_table` row — most have no login, the accounts that exist are spelled in Latin, and some entries name two co-teachers. `teaching_assignment` (which needs a `system_user_id`) is the structured form, for when they have accounts. |
| 38 | **Bad legacy rows are repaired, not dropped.** Duplicate usernames get a suffix, classless students migrate unenrolled. A login nobody can use is recoverable; a deleted student is not.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |

### Phase 3 (publication and change requests)

| #  | Decision                                                                                                                                                                                                                                                                        |
|----|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 39 | **Publication scope is (class, period), subject optional.** Legacy had neither — period was implicit in a timestamp, subject was never a unit. Class-wide is the normal action; the subject filter is for releasing the rest when one teacher is late.                          |
| 40 | **`sgs.publication` is an audit log, not the mechanism.** What parents see is still decided per cell by `grade_entry.published_at`. Deriving the log by scanning for distinct timestamps would be lossy — a republish overwrites the previous stamp.                            |
| 41 | **Approving a change request republishes the whole dependency closure.** Releasing only the disputed cell would leave parents seeing marks of 7, 8, 9 beside an average that matches none of them. Downstream cells that were never published stay unpublished.                 |
| 42 | **Raising a request needs `ADD_GRADES`; deciding needs `MANAGE_CHANGE_REQUESTS`.** Legacy required the approval permission to *create* one, so only approvers could ask.                                                                                                        |
| 43 | **One open request per cell, via a filtered unique index.** A check-then-insert fails exactly when two teachers submit at once.                                                                                                                                                 |
| 44 | **No unpublish.** Retracting a grade a parent has seen is worse than correcting it forward, and a retraction has no story to tell them. A mistaken publish is fixed by a change request.                                                                                        |
| 45 | **Blank cells are never published.** Stamping `published_at` on a cell that never held a value would lock out the teacher who still has to fill it. Briefly reversed for the absence register - see decision 71 - and then restored when the reason for reversing it went away. |
| 46 | **Emails leave the publish transaction.** Legacy sent ~900 synchronous messages inside it, so a slow mail server made publishing slow and a failing one could fail it. A failed send is logged, never fatal.                                                                    |
| 47 | **The privileged write path is a method, not a flag.** `GradeWriteService.applyApproved` lifts the publication lock and is unreachable from any controller — a bypass carried in a request body is a bypass anyone can ask for.                                                 |

### Phase 4 (exports)

| #  | Decision                                                                                                                                                                                                                                                                                                                                       |
|----|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 48 | **Two shapes replace five exports.** *Matrix* (students × subjects, one component) and *detail* (students × components, one subject), parameterised by template component and period. The four Excel exports ran to 768 lines and were ~85% the same.                                                                                          |
| 49 | **The legacy exports cannot be reproduced column for column.** Three of the five are built on monthly and semester periods that decision 2 removed. Decision 6 ("keep the exports") therefore means keeping their shape, re-expressed against trimesters.                                                                                      |
| 50 | **Subject order moves to `class_subject.sort_index`.** It was a hardcoded 39-name Java list that **20 of the school's 51 subjects never matched** — five of them because the list spells them with a slash and no row in `dbo.subject` contains one. Matching is slash-insensitive, which recovered 6 more.                                    |
| 51 | **Exports read working values, not the published snapshot.** An export is a staff document produced from the journal on screen; a spreadsheet that disagrees with the screen is worse than one ahead of what parents see.                                                                                                                      |
| 52 | **Decimals are a template property, not an export concern.** Of 853 live grades only 13 are fractional and every one is a behaviour average or a percentage — no `TRIMESTER_*` value has ever had a decimal, and `7.00` accounts for 633. So grade components get `decimals = 0` and the engine rounds once; the export prints what is stored. |
| 53 | **The `+3` scale shift becomes `component.output_offset`.** An IB 7-point → Georgian 10-point conversion that lived as a magic number behind a boolean query parameter. Applied when printing; storage never moves.                                                                                                                            |

### Phase 5 (journals as data)

| #   | Decision                                                                                                                                                                                                                                                                                                                                                       |
|-----|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 54  | **A journal is a row, not an enum.** `TemplateScope` (ACADEMIC/ETHICS/ABSENCE) is deleted. The school has run many journals and changes them often, so it creates and names its own.                                                                                                                                                                           |
| 55  | **Identity is a UUID; the name is free to change.** The name is the menu label, so renaming is routine — if it were the identity, every formula and assignment referencing it would break. The `bigint` key stays for storage (sequences keep insert batching available).                                                                                      |
| 56  | **Periods survive, on one number.** Cutting them was tempting — rollups become ordinary formulas and the alignment problem vanishes — but the ethics journal flattens to **~279 columns**, and one journal per month means nine near-identical journals a year. A period is "the same columns again, for a different slice of time".                           |
| 57  | **The wizard asks "how often is this filled in?"**, never "choose a period scheme". `ONCE_A_YEAR` behaves like a plain table: one grid, no period dropdown anywhere.                                                                                                                                                                                           |
| 58  | **Cross-journal alignment is asked, never inferred.** An earlier draft matched periods by date overlap; that was machinery invented to solve a problem periods created. Same frequency → same occurrence. Different → the picker asks, via `PeriodRef.SPECIFIC`, which already existed.                                                                        |
| 59  | **The dependency graph spans journals.** Built per version, a cross-journal reference looks dangling and an A→B→A cycle is never seen. `componentsReachableFrom` follows references across versions — which also gives cross-journal recompute fan-out for free.                                                                                               |
| 60  | **A cell recomputed through a cross-journal formula is stamped with the *other* journal's version.** Stamping the writing version would make that period look like it holds two versions and jam the resolver.                                                                                                                                                 |
| 61  | **Columns are diffed by code, not replaced.** A renamed column keeps its identity, its data, and any cross-journal formula pointing at it.                                                                                                                                                                                                                     |
| 62  | **A version holding marks is never edited in place** — saving forks a draft. `LOCKED` (declared since phase 1, never set) is now set by publishing.                                                                                                                                                                                                            |
| 63  | **Migration always recalculates.** "Move but keep the old numbers" sounds cautious and is the dangerous option: values produced by the old rules under the new columns are data no rule explains. Answering no leaves the period where it is. Offered per (class, period) **and** for every stale period at once, preview first.                               |
| 64a | **Frequency maps to a depth in the period tree, and is enforced.** `PeriodKind` has three values against four frequencies — months and weeks are both `REPORTING`, so matching columns on kind would have shown a monthly journal's columns on all forty weeks. Columns match on depth; the period dropdown offers only the journal's own level plus the year. |
| 64  | **Scope is the menu entry; a template is the columns inside it.** Creating a journal adds a tab. Adding a column changes what an existing grid shows and adds nothing.                                                                                                                                                                                         |

### Phase 5 review — what an independent pass found

An agent was given the *goals* only, not the implementation, and told to check whether the
code met them. It found 15 real problems; all were verified by hand and fixed. Recorded
because the pattern behind them matters more than the individual bugs.

**The pattern.** Every failure sat in the layer where the tests build their own fixtures
instead of walking the path a user takes. `JournalServiceIT` hand-built a
`TemplateAssignment`, so it never noticed that **nothing in main code ever creates one** —
meaning every journal made in the wizard failed on grid open, permanently. The engine core,
which does have adversarial tests, came through clean.

| What was wrong                                                                                                                                                                                                                                                                | Now                                                                                                |
|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------|
| No `TemplateAssignment` was ever written, and `activate` never re-pointed one. A wizard journal could not be opened; activation never took effect.                                                                                                                            | `syncAssignments` on activation. Two tests walk the wizard→save→activate→open path.                |
| `componentsReachableFrom` followed only what a version *reads*, so a journal reading ours was never in the graph and its cells went stale. The javadoc claimed the opposite.                                                                                                  | Follows both directions, via `findComponentIdsReading`.                                            |
| `targetSubjects` keyed every source by the *dependent's* subject, so a per-subject column reading a class-wide one missed forever; the mirror case persisted subject-null rows for subject-scoped components.                                                                 | Resolved per source, from the source column's own shape.                                           |
| `@Version` was read before flush, so the second correction to a cell falsely conflicted — and the UI never applied the returned version, so a conflict could only be cleared by reloading.                                                                                    | Flush before reporting; the client adopts `currentVersion`.                                        |
| **No class scoping on any endpoint.** Any `ADD_GRADES` holder could write any class; any `MANAGE_CLOSED_PERIOD` holder could publish the school.                                                                                                                              | `ClassScopeGuard`, from the legacy `academyClassList`.                                             |
| Approval ignored conflicts (request marked APPROVED, guardian emailed, nothing changed), and stored the director's value without `override`, so the next recompute erased it.                                                                                                 | Refusal throws; the value is an override; the cell is released regardless.                         |
| Forking made new component rows, orphaning every cross-journal formula pointing at the journal.                                                                                                                                                                               | `repointExternalReferences` moves them by code.                                                    |
| `db/007_seed_template.sql` still inserted `scope`, which phase 5 dropped — a fresh install failed at step 007.                                                                                                                                                                | Rewritten for the journal columns.                                                                 |
| No value validation at all: 999 on a 0–10 column persisted. The console sent `ჩთ`.toUpperCase() — Mtavruli, not the code `CHT`.                                                                                                                                               | `OUT_OF_RANGE` / `INVALID_VALUE`; the client matches declared codes.                               |
| `SPECIFIC` periods outside the loaded neighbourhood computed from nothing and wrote the result.                                                                                                                                                                               | The working set includes every `SPECIFIC` period the graph names.                                  |
| `periodsOfKind` fanned a change to months *and* weeks; the export matched columns by kind while the grid matched by depth.                                                                                                                                                    | Both match on depth.                                                                               |
| A cleared published mark could never be republished.                                                                                                                                                                                                                          | `findPublishable` keeps published cells.                                                           |
| `ChangeRequestService` and `loadMatrix` resolved cells by code alone, across journals.                                                                                                                                                                                        | Both filter by journal.                                                                            |
| `save` persisted before validating and committed invalid structures; evicted one graph while others cached it; migration deleted **published** cells.                                                                                                                         | Validate before commit on a live version; `evictAll`; migration refuses to delete published marks. |
| Refetch wiped unsaved edits; a late flush patched old-period values into a new grid; failed flushes retried forever.                                                                                                                                                          | Scope-guarded responses, dirty preserved across refetch, exponential backoff.                      |
| `MANAGE_TEMPLATES` was absent from the permission catalogue, so **no group could be granted it** — the feature was unreachable. Archived journals could not be restored. Capabilities were hardcoded `true`. The change-request dialog showed the working value as published. | All fixed.                                                                                         |

### Phase 6 (parent console)

| #  | Decision                                                                                                                                                                                                                                                                                                                                                        |
|----|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 65 | **Cards versus table is a row count, not a setting.** One row renders as cards because a one-row table is an ugly way to show one thing; several rows render as a table. Every existing layout is what this rule already produces, so nothing needed configuring — and drilling into a subject is not a separate page, it is the same view filtered to one row. |
| 66 | **A journal decides its own rows.** Per-subject journals have a row per subject within a chosen period and get a period picker; class-wide journals have a row per period and need none.                                                                                                                                                                        |
| 67 | **`GradingTemplate.parentVisible`, off by default.** A journal is a staff working document until someone says otherwise; an internal grid appearing on the parent portal the moment it is created is the wrong way round. Checked when serving a journal, not merely when listing — a uuid is easy enough to guess at.                                          |
| 68 | **`GradeComponent.parentVisible` finally does something.** Modelled in phase 1, set by the editor, read by nothing — so a column marked staff-only was shown anyway. A journal can now be released while an internal working column inside it is not.                                                                                                           |
| 69 | **A chart is code; which journal draws it is data.** `journal.chartKey` names one and the console keeps a registry. Keyed by a stable name, never by uuid — uuids differ per environment, so a uuid-keyed registry needs different code in each. A journal naming no chart renders a complete page.                                                             |
| 70 | **Blank columns are shown, not hidden.** A trimester in progress should tell a parent what is still to come rather than conceal that anything is missing.                                                                                                                                                                                                       |
| 71 | **The student comes from the token, never a parameter.** A student id in the query string would let any logged-in parent read any child by changing a number. `/api/parent/**` is authenticated, unlike the legacy `/client/**`, which is `permitAll`.                                                                                                          |

### Second review — the wiring layer

A second independent pass, again given the goals and not the implementation. It
found 30 problems, four of them fatal, and all four sat in the layer decision 21
had chosen not to test. The engine came back clean a second time.

| What was wrong                                                                                                                                                                                                                                                                                                                                                                                                                | Now                                                                                                                                                                    |
|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **`/api/parent/authenticate` was not in the permitAll list** — a parent needed a token to get a token, and the console still called the legacy endpoint whose token carries a username. Every parent got a dead page.                                                                                                                                                                                                         | Login permitted, console repointed, token keyed by student id.                                                                                                         |
| **`SGSException` is checked and nothing declared `rollbackFor`** — Spring rolls back on unchecked only, so three "refuse and roll back" guards committed their partial work. It reinstated the very bug the previous review's repair had fixed.                                                                                                                                                                               | `rollbackFor = Exception.class` throughout, and the change-request decision applies *before* it writes a status, so correctness no longer rests on the rollback alone. |
| **A fresh install failed at step 4** (`007` omitted a NOT NULL column) and **`015` — the student identity rules — was not in the runbook at all.**                                                                                                                                                                                                                                                                            | Both fixed; the runbook now separates the fresh order from the upgrade-only scripts.                                                                                   |
| **Journals never appeared in the staff menu**: `useQuery(key, fetchJournals)` handed react-query's context object to the function, which serialised into the query string, and `onError: () => {}` swallowed the 400.                                                                                                                                                                                                         | Wrapped in a lambda.                                                                                                                                                   |
| The spanning graph admitted **two versions of one journal**, whose codes collide by construction, bricking the referenced journal permanently.                                                                                                                                                                                                                                                                                | One version per journal in a closure — the rule that was missing when the cross-journal repairs were made.                                                             |
| Repointing ran on every **draft** save, moving other journals onto components nobody writes yet and rewriting LOCKED versions.                                                                                                                                                                                                                                                                                                | Moved to activation; LOCKED versions excluded.                                                                                                                         |
| A YEAR rollup could be written under two versions mid-year, **jamming the resolver for that period forever**.                                                                                                                                                                                                                                                                                                                 | The version already present at a period wins.                                                                                                                          |
| Approval's closure release filtered to the disputed cell's journal, so **cross-journal dependents stayed stale for parents**.                                                                                                                                                                                                                                                                                                 | Filter removed; published rows preferred when a code is ambiguous.                                                                                                     |
| Class scoping **failed open** — a restricted user whose grants resolved to nothing got the whole school — and `explain` and `raise` had no check at all.                                                                                                                                                                                                                                                                      | Fails closed; both endpoints covered by enrollment and by cell.                                                                                                        |
| A concurrent insert **discarded the whole batch**: sequence ids defer the insert to a flush outside the guard.                                                                                                                                                                                                                                                                                                                | Flushed inside it, so it is a per-cell conflict as intended.                                                                                                           |
| The autosave scope guard read a ref reassigned during render, so the unmount flush **patched old-period values into the new grid**; a failed batch could replay into another period.                                                                                                                                                                                                                                          | Ref updated in an effect; requeue is scope-checked.                                                                                                                    |
| Plus: SPECIFIC fan-out targeting the wrong tier, exports resolving the wrong version and keying across journals, explain contradicting the value it explained, the validator rejecting legitimate monthly rollups, a settings dialog silently un-publishing journals, the Mtavruli special-value bug repeating in a second dialog, and a malformed token producing a 500 — **found by the new wiring test on its first run**. |

### Phase 7 (conversion scales, bulk export)

| #  | Decision                                                                                                                                                                                                                                                                                                                                                                                                              |
|----|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 72 | **One conversion formula, for the whole school.** The school grades German-style out of 7 and is **legally required to report to the government out of 10**; they are moving to a 9-point scale and have not settled the mapping, so the formula is configuration. `multiplier` and `offset` — today `× 1, + 3`. Both `isDecimalSystem` and phase 4's per-column `output_offset` are dropped.                         |
| 73 | **It is representation and nothing else.** No grade is stored converted, nothing recomputes through it, and the parent portal does not use it - a parent reads the mark the school gave, on the scale the school grades on. It applies in exactly two places: the grid when the toggle is on, and the Excel export when the box is ticked. Editing it can therefore never corrupt a grade or require a migration.     |
| 74 | **Converted output is never rounded.** Whatever the formula produces is displayed: `6.5` through `+3` shows as `9.5`. Rounding belongs to the engine, which already did it once when it calculated the grade; a printed conversion has no business deciding it again. Trailing zeros are stripped.                                                                                                                    |
| 75 | **A converted grid is read-only.** Entry is always on the real scale. Backed by more than the UI disabling itself: `convertedValue` is a separate field the write path never reads, and `scaleMin`/`scaleMax` validation rejects a converted value server-side.                                                                                                                                                       |
| 76 | **Not attached to journals or columns.** A first cut made the scale a named object settable per journal and per column, with banded as well as linear mappings. That was machinery the school had not asked for and it was cut back: there is one formula, and a journal that should not be converted is simply never viewed with the toggle on.                                                                      |
| 77 | **Bulk export is a zip, per trimester and year, for every export shape.** Scope is whatever classes the caller holds via `ClassScopeGuard`, so one button serves a coordinator and a director. There is no semester anywhere — the school corrected the wording, which is what decision 2 already assumed.                                                                                                            |
| 78 | **The zip streams synchronously, and gets a job only if measurement asks for one.** ~180 small workbooks is a few seconds of POI plus query time. A job queue is a table, a polling endpoint, a progress UI and a retention policy — a lot of machinery to build against an estimate rather than a measurement.                                                                                                       |
| 79 | **Absence emails coalesce on a 15-minute window and re-read the cell before sending.** Marking a cell autosaves in about a second, so sending immediately means a mis-click tells a parent their child was absent, with no unsend. Re-reading at send time means a correction always wins regardless of when it lands, rather than depending on beating a timer. Several absences in a day collapse into one message. |

### Phase 8 (content substrate and homework)

| #  | Decision                                                                                                                                                                                                                                                                                                                                                                              |
|----|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 80 | **One `post` table with a `kind`, not five tables.** Homework, the daily schedule, the menu, characterizations and news differ in about four fields and agree on everything structural. Five near-identical tables become five services that drift — which is exactly how the legacy system arrived at four copy-pasted export methods that each excluded a different set of columns. |
| 81 | **Only what homework uses is created.** `post_line` (schedule and menu) and news's picture and category are nullable additions phase 9 makes when it has something to put in them. A table built for a guess is worse than a migration.                                                                                                                                               |
| 82 | **Publication is frozen: any edit needs a re-publish.** The school's call, and the opposite of the obvious answer — live editing would let a typo fix reach parents at once. `published_payload` holds a JSON snapshot of what was released; the row holds the working copy. Same split as `grade_entry`, same reasoning as decision 16.                                              |
| 83 | **Three states, though the brief describes two.** Draft, published, and published-then-edited. Without the third a teacher edits, is satisfied, and the change never reaches anyone. `has_unpublished_changes` carries it.                                                                                                                                                            |
| 84 | **Rich text is sanitised on write, never on read.** OWASP `java-html-sanitizer` against a fixed allowlist. Sanitising only on read would leave the original payload in the database for whoever renders it next — an export, a mail template — none of which would know to be careful.                                                                                                |
| 85 | **Links, not file uploads.** The school's server is short of space, so an attachment here is a URL, restricted to http and https. A `javascript:` link is a script the reader's browser runs.                                                                                                                                                                                         |
| 86 | **Soft delete.** The brief's own wireframe says "deactivate", and something a parent has already read should leave a trace.                                                                                                                                                                                                                                                           |
| 87 | **A permission per module**, starting with `MANAGE_HOMEWORK`, rather than one `MANAGE_CONTENT` — so a subject teacher can set homework without also being able to publish school news.                                                                                                                                                                                                |
| 88 | **The parent side of all five modules is phase 11**, not spread across 8 and 9. The school wants to make those UI decisions in one go. Publish is still built now, and writes its snapshot, because retrofitting it into a term of accumulated data is the alternative.                                                                                                               |

### Phase 9 (schedule, menu, characterization, news)

| #  | Decision                                                                                                                                                                                                                                                                                                                    |
|----|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 89 | **Schedule and menu are one standing document per class.** The school enters them once for the year and adjusts them occasionally — no months, no trimesters, no weekly versions. So `post_line` hangs off a single post and there is no list to page through.                                                              |
| 90 | **News pictures are uploaded, not linked.** The exception to "links, not files": a news post without its picture is a worse page, where an assignment without an attached file is not. Paid for by shrinking rather than refusing — 2 MB cap, downscaled to 1600px, which puts a phone photo around 200 KB.                 |
| 91 | **Every stored image is re-encoded from decoded pixels.** That is the compression *and* the validation: a file that will not decode is not an image, and a payload smuggled into a comment segment does not survive being redrawn. The declared content type is never trusted. No new dependency — `ImageIO` is in the JDK. |
| 92 | **Six standing-document endpoints, not three taking a `kind`.** `@Secured({A, B})` is an *or*, so one shared endpoint would let anyone holding either permission edit both documents — defeating the point of having two.                                                                                                   |
| 93 | **Every uuid-addressed write verifies the post's kind.** The permission is per module, so the check has to be too: without it `MANAGE_MENU` publishes a schedule, and `MANAGE_HOMEWORK` publishes a news item, just by putting the right uuid in the wrong URL.                                                             |
| 94 | **News categories are a table behind an autocomplete**, matched case-insensitively and trimmed. Typing feels like free text; the unique name is what stops one category becoming two through a stray space.                                                                                                                 |

### Phase 10 (the absence registers)

| #   | Decision                                                                                                                                                                                                                                                                                                                                                                                                                                           |
|-----|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 95  | ~~**Weeks became dated days.**~~ **Undone by decision 103.** The day level was created for the daily register and deleted with it: absence is keyed on a date now, so the ~217 dated rows were unused again, exactly like the numbered weeks they replaced. The tree stops at months. `db/021` still creates them and `db/028` still removes them; on a fresh install that is churn, left in place because rewriting a shipped migration is worse. |
| 96  | **`GridMode` on the journal.** The transposed grid - students down, *periods* across - is a journal property rather than a hardcoded screen. It was added for two registers and now has one user, the monthly one; the daily register is not a journal any more. Kept because the alternative is a screen that knows a journal by name.                                                                                                            |
| 97  | ~~**1 for absent, blank for present**, drawn as a tick and a cross.~~ **Reversed by decision 103.** The tick and the cross stand; storing them as a number in `grade_entry` did not. A blank cell meant either "present" or "not yet marked" and nothing could tell them apart.                                                                                                                                                                    |
| 98  | **Daily and monthly stay independent.** Daily counts days, monthly counts academic hours typed by the coordinator; converting between them needs an hours-per-day figure nobody has.                                                                                                                                                                                                                                                               |
| 99  | ~~**Change requests on daily absence, deliberately.**~~ **Reversed by decision 104.** The stated reason - publication sends email, so the director should see it - did not hold: the email fires when a cell is *marked*, not when it is published, so approval sat on a path that never reached a parent. The school had not been firm about it.                                                                                                  |
| 100 | **Notices are queued, coalesced for 15 minutes, and the cell is re-read before sending.** A correction always wins whenever it lands, rather than depending on beating a timer. One notice per student per day. Its own table - what has been sent is absence's business, not the grade model's.                                                                                                                                                   |
| 101 | **`PeriodRef.DESCENDANTS`.** `CHILDREN` is one level, which was all the trimester journal needed. A total three levels above the days it counts needs to span the gap, in the evaluator and the recompute engine alike. Added rather than working around it, because both absence rollups and anything later at mixed frequencies need the same reach.                                                                                             |
| 102 | **Publication releases a period and everything beneath it.** A trimester's grades live on the trimester, so one period id sufficed until absence marks arrived on days: publishing the month they belong to released nothing and reported success.                                                                                                                                                                                                 |

| 103 | **Daily absence is its own table, not a journal.** A journal cell carries a value and a tick is not one, so "
absent" was the number 1 and "present" was a blank - and *every* serious defect in the register came from some part of
the system reading that blank differently from another. In `daily_absence` a row means absent and no row means present;
the third state does not exist. Gone with it: the value, the scale, the row version (marking is insert-or-delete and so
idempotent), the template version, the publication columns, and the period - a date, so "days absent in March" is a
range rather than a three-level descent. `uq(enrollment_id, absence_date)` makes double-marking impossible in the
database rather than guarded in a service. |
| 104 | **Publishing and freezing are two things.** `locks_on_publish` replaces `publishes_blanks`. Grades lock: parents
saw the mark, so changing it needs the director. The monthly register does not, because missed hours accumulate through
a month and the coordinator republishes as they do - that is the ordinary path, and an approval per top-up would sit on
it. Publication still means what it means: parents see the published figure and nothing newer. |
| 105 | **The blank question dissolved rather than being answered.** Freezing a register meant asserting its blanks,
which meant writing a row per student per day so the lock had something to fire on - `publishes_blanks`,
`materialiseBlanks`, and `db/027`, whose implementation inserted duplicates and broke the second publish of any period.
With no freeze there is nothing to assert: a day missing from the published set was, at publish time, not marked absent.
All three are deleted. |
| 106 | **One owner for period reach.** Six services each answered "which periods does this touch?" in their own inline
expression. They had to agree; nothing checked that they did, and they did not - only four learned about `DESCENDANTS`,
so a yearly total was computed from one month and stored over the year, and the explain trace reported as empty the
sources the write path had summed. `PeriodReach` owns all five questions, and `PeriodReachTest` asserts the property
that was violated: if evaluating X reads P, then changing P recomputes X. |
| 107 | **The daily register is staff-only; the monthly one is what parents see.** So daily has no publish button, no
publication and no approval - what reaches a parent is the email, the same day. Publication lives entirely on the
monthly hours register, which is the one the brief's green-to-red diagram is drawn from. |

### Phase 10 review — what an independent pass found

Given the goal and not the implementation, as the previous two were. It found that
**two of the requirements the school had confirmed were not actually delivered**, plus a
roll-up that could never compute. Every finding below was reproduced before being fixed.

| What was wrong                                                                                                                                                                                                                                                                                                                                                                                                             | Now                                                                                                                                                                                                                                                                                                                                                           |
|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **The director's approval never engaged on the daily register.** Three holes: no publish or change-request UI on the page at all; `findPublishable` matched one period id, so publishing a month released none of its days; and because **blank means present**, the lock only fired on rows that already existed — so *adding* an absence to a published day created a new row, bypassed approval and emailed the parent. | ~~Publish and change-request added to the register~~ — **superseded**. Daily absence left `grade_entry` for its own table (decision 103), so the blank has no representation and the hole it opened is gone rather than closed. There is no publish or change request on the daily register at all: it is staff-only, and what reaches a parent is the email. |
| **Parent emails were lost silently.** `EmailServiceImpl` reports failure in a returned string nobody reads, so the notifier stamped `sentAt` on mail that never went. The "next run retries" comment was dead code.                                                                                                                                                                                                        | `EmailService.sendOrThrow` added; a failure now leaves the notice pending.                                                                                                                                                                                                                                                                                    |
| **A cancelled notice permanently suppressed the next one.** `uq_absence_notice(enrollment, date)` plus a status-blind lookup meant a mis-click corrected in the morning left a dead row that a genuine afternoon absence silently reused — the parent was never told, and nothing recorded that.                                                                                                                           | The constraint is gone (`db/024`); the lookup matches only *pending* notices.                                                                                                                                                                                                                                                                                 |
| **One transaction wrapped a whole batch of sends**, so a failure part way through rolled back the rows marking already-delivered mail as sent, and the next run sent it all again.                                                                                                                                                                                                                                         | `AbsenceNoticeSender` sends each notice in its own transaction. Self-invocation would not have worked, hence a separate bean.                                                                                                                                                                                                                                 |
| **A student with no guardian address was recorded as told.**                                                                                                                                                                                                                                                                                                                                                               | Cancelled instead — "nobody was told" and "somebody was told" must not look alike.                                                                                                                                                                                                                                                                            |
| **Notices were queued from the raw request**, ignoring both class scope and whether the write was refused — so a rejected mark still told a parent, and a coordinator could queue (and, with the bug above, suppress) notices for any child in the school.                                                                                                                                                                 | Queued only for cells that landed, and only for enrollments in the request's own class.                                                                                                                                                                                                                                                                       |
| **The two settings fields wrote to different period levels** depending on which screen they were on — the month on one, the year on the other — so they never agreed. They also rendered for users the endpoint would 403, and a typo serialised to `NaN` and **deleted** the stored value.                                                                                                                                | Shown on the monthly register only, gated on the permissions the endpoint actually checks, and a non-numeric entry is ignored rather than destructive.                                                                                                                                                                                                        |
| **The register discarded every write rejection.** Conflicts return HTTP 200 and were never read, so a losing mark just never appeared.                                                                                                                                                                                                                                                                                     | Surfaced; a `PUBLISHED` rejection opens the change-request dialog.                                                                                                                                                                                                                                                                                            |
| **`db/021`'s weekend filter depended on session `DATEFIRST`** — a differently-configured login would have generated Saturday columns and dropped Mondays.                                                                                                                                                                                                                                                                  | `SET DATEFIRST 7` pinned in the script.                                                                                                                                                                                                                                                                                                                       |

### Phase 10 review — the roll-up that could never compute

Worth separating, because it was a fault in the **engine's reach**, not in phase 10's wiring.

`PeriodRef.CHILDREN` is exactly one level. That was all the trimester journal ever needed —
a year's annual mark reads the trimesters directly below it. The absence registers are
further apart: a yearly total over *days* is three levels, with trimesters and months in
between that hold nothing. Seeded as CHILDREN, both totals summed nothing, forever and
silently; worse, the recompute side resolved CHILDREN as one hop *up* from the changed cell
and persisted rows at the month and the trimester — levels those columns do not live at.

`PeriodRef.DESCENDANTS` now spans any distance, in both the evaluator and the recompute
engine. `db/023` repoints the two seeded rollups and deletes the wrong-level rows.
`DescendantRollupTest` covers it, including a test that pins the **old** behaviour so the
gap cannot reopen unnoticed.

### Phase 10 second review — what the first round of repairs broke

A second independent pass, again given only the goal. It found eight problems, and **the
two most serious were introduced by the previous round of fixes** rather than by the
original work.

| What was wrong                                                                                                                                                                                                                                                                                                                                                                                                                    | Now                                                                                                                                            |
|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------|
| **`@Transactional` came off `PublicationService.publish()`.** The sub-period helper was inserted *between* the annotation and the method, so it annotated a private method Spring ignores and publication ran with no transaction. It appeared to work only because open-session-in-view was masking it — turn OSIV off and publish reports success while writing the log row alone.                                              | Annotation back on `publish()`.                                                                                                                |
| **`DESCENDANTS` had a third leg nobody updated.** The evaluator and the recompute engine both learned the new reach; the *working set* did not. `relevantPeriods` loads a neighbourhood, so a yearly total summing every day evaluated against one month's worth and persisted it — a mark in October erased September from the year's count, silently. The same method already handled `SPECIFIC` terms for exactly this reason. | The working set loads the reach when the graph uses it. Covered by a test that marks three days in September and one in October and asserts 4. |
| **The two previous fixes cancelled each other out.** Published cells were disabled to look locked, *and* a published-cell click was made to open the change request. The first prevents the second, so the register was frozen after publication with no way to even ask the director — precisely what the fix claimed to remove.                                                                                                 | Published cells stay clickable; the click raises the request.                                                                                  |
| **Publication's new reach had no journal filter**, so releasing the monthly register's year would have released every unpublished cell in the class for the whole year and emailed the guardians.                                                                                                                                                                                                                                 | `publish` takes a journal; both consoles pass theirs.                                                                                          |
| **An approved change request never emailed the parent** — the one case that most needs it, since it corrects something parents were already told.                                                                                                                                                                                                                                                                                 | Queued on approval, through the same window.                                                                                                   |
| **The pending-notice guard was removed rather than reshaped.** `db/024` dropped the unique constraint to fix the cancelled-row bug and left nothing enforcing "one pending notice per student per day": two concurrent marks could double-send, and every later mark then threw from an `Optional` finding two rows — after the grade had already been written.                                                                   | A **filtered** unique index on pending rows only (`db/025`), and the lookup returns a list.                                                    |
| **Both yearly totals inherited the wrong scale** — `DAYS_ABSENT` got the daily column's 0..1, so a director correcting a yearly count was rejected as out of range.                                                                                                                                                                                                                                                               | `db/026`, and the seed fixed at source.                                                                                                        |
| A javadoc of mine claimed `pendingIds` ran in its own transaction; a self-call does not go through the proxy — the same trap the neighbouring class exists to avoid.                                                                                                                                                                                                                                                              | Comment says what actually happens.                                                                                                            |

**The pattern worth keeping.** Both severe findings were *repair damage*: one from inserting
code above a method without looking at what sat above it, one from extending a concept in
two of the three places that needed it. A fix is a change like any other, and the second
review earned its keep by treating it as one.

### First manual test - five faults that only appear when you run it

Everything up to here was verified by tests. Building a database to click around in, and
then clicking around in it, found five things in an afternoon - **none of which any test
could have caught**, and three of which stopped the product dead.

| What was wrong                                                                                                                                                                                                                                                                                                                                                                                                                                      | Now                                                                                                                                                                                                          |
|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **The configured dialect meant nothing could be written.** `SQLServerDialect` reports `supportsSequences() = false`, so Hibernate substituted a *table* generator for every `@SequenceGenerator` in the new model: it tried to `create table sgs.grade_entry_seq`, collided with the real sequence, and every insert failed with `Invalid object name`. The application still started, because `ddl-auto: update` logs schema failures as warnings. | `SQLServer2012Dialect`. Proved by writing marks over HTTP and reading the derived values back. `FOLLOW-UPS.md` §6 has the migration the school's own database still needs.                                   |
| **A war built without `clean` would not start.** `core/target/core-0.0.1-SNAPSHOT/` is an exploded war Maven does not tidy, so the package picked up a `gradebook/AbsenceController.class` left from before that class was renamed. Two controllers, one bean name, `ConflictingBeanDefinitionException`, dead on arrival.                                                                                                                          | Always `clean`. Nothing to fix in the code - the source was already right, which is exactly why no test saw it.                                                                                              |
| **The login screen reloaded forever.** `NavigationProvider` wraps the application *above* the point where `App` chooses between the login form and the console, so the journals query added in phase 5 ran with no token. It 401s; the axios interceptor answers any 401 by clearing the session and assigning `window.location.href`; the page reloads and mounts the hook again. Nobody could type a password.                                    | `enabled: !!loggedIn` on the query. Verified in a browser: zero network requests while logged out, one 200 after login.                                                                                      |
| **Deleting the legacy pages blanked the console.** `navigation-context` opened four tabs by hard-coded id and defaulted the landing page to `TRIMESTER`; with those pages gone the filter matched nothing and the app rendered an empty panel with no error. `CustomTab` and `TabNavigation` named the same four to keep them un-closable.                                                                                                          | The landing page is `pages[0]` - the first page this user may see, already permission-filtered, and impossible to make stale. No tab is pinned. String page ids are not something distant files should name. |
| **`e.preventDefault()` was commented out in `LoginForm.js`** - pre-existing. The submit button triggers the browser's own form submission, which fires the moment the handler returns at its first `await`: the page navigates to `/?` and the login POST is aborted mid-flight.                                                                                                                                                                    | Uncommented. It was a **race**, not a certainty - `setAuth` only had to land before the page finished tearing down - which is why it worked often enough to look fine.                                       |

**The pattern worth keeping.** Three of the four are invisible to the suite *by construction*:
the tests override the dialect, they read `target/classes` rather than the war, and they never
render the logged-out application. `ApplicationWiringIT` was added after an earlier review for
exactly this class of fault and it still missed all three - it boots the context, but it does not
write a row, package a war, or log out. Running the thing is a test technique, not a formality,
and the first run of anything is worth budgeting for.

### The period tree did not match the brief

Found by re-reading the brief after a challenge, not by a test - nothing could
have tested it, because the seed was internally consistent and simply described
a different calendar from the school's.

The brief states the shape twice, under the absence table and again under the
ethical-norms table, and states it in prose as well: **three trimesters over
seven reporting periods** — Sep-Oct, Nov to T1; Dec, Jan-Feb, Mar to T2; Apr, May
to T3; then Year. `db/013` seeded ten calendar months instead. Four differences:

|                   | Brief            | Seeded            |
|-------------------|------------------|-------------------|
| Reporting periods | 7                | 10                |
| Sept + Oct        | one period       | two               |
| Jan + Feb         | one period       | two               |
| March             | **Trimester II** | **Trimester III** |
| June              | absent           | present           |

Three are column counts. **March is not** — a March mark was rolling up into the
wrong trimester, and would have gone on doing so silently. `db/013` put months
under "the trimester their teaching falls in" rather than by date containment,
which was a reasonable rule and disagreed with the school.

`db/032` rebuilds the level rather than editing it: every row changes identity,
two pairs merge and one changes parent, so renaming rows to fit would silently
reassign whatever marks were attached. It refuses outright if anything points at
the old periods.

June is dropped rather than kept, on the asymmetry: adding a period back is one
INSERT that disturbs no computed value under the IGNORE null policy, while
removing one stops being safe the moment a mark, a permitted-hours setting or a
notice points at it. Cheap to add, expensive to remove — so it goes now and
returns if the school turns out to teach in June.

**Still open, and it affects the absence register equally:** the brief's tables
put Trimester I/II/III and Year in the same row as the reporting periods. A
component sits at its journal's own level or at the year — `PeriodKind` has no way
to say "the trimester" for a MONTH journal — so those columns cannot be expressed
today. Whatever fixes it fixes both journals, and it is a change to the grid
rather than to any journal.

### Three of the brief's four grids mix levels, and only one could be drawn

Found by reading the brief's tables against the code after the period tree was
corrected. The gap was not arithmetic:

| Brief table             | One row contains                | Before    |
|-------------------------|---------------------------------|-----------|
| Trimester assessment    | one trimester's columns         | drawn     |
| **Trimester and final** | 3 trimesters + 4 year columns   | not drawn |
| **Ethical norms**       | 7 periods + 3 trimesters + year | not drawn |
| **Hours missed**        | 7 periods + 3 trimesters + year | not drawn |

`GradeGridService.visibleComponents` read: *on the year show year columns, else
show the journal's own level*. One tier at a time, never two side by side. The
transposed register did the same from the other direction — one component, many
periods.

**The sums were never the problem.** A trimester total is a DERIVED column over
CHILDREN, which is the shape `ANNUAL` has used since phase 1. What was missing
was somewhere to put the column: `component.period_kind` was read as a binary,
so a monthly journal could have a column on its months and one on the year and
nothing in between.

`period_kind` now names the tier a column sits on — REPORTING, ROLLUP, YEAR " + D + "
which the enum already said and `Period.kind` already labelled the rows with.
About eight `== YEAR` checks across six files became one equality. One seeded row
was wrong under the new reading and is corrected in db/033: `HOURS_MISSED` was
labelled ROLLUP while being typed against reporting periods, which the old binary
made indistinguishable.

The transposed grid then walks the whole subtree in post-order — a trimester's
reporting periods, the trimester, the year last — which is the brief's column
order exactly.

The report card needed one thing more. Nothing in the model says which columns
belong in it: every column of the seeded journal is parent-visible, and
ONGOING_AVG and TRIMESTER_GRADE are both derived and both ungrouped. A report
card is an editorial selection, so `component.summary_column` states it (db/034).

**Not attempted:** putting those columns into the main gradebook grid. `GridCell`
is keyed by enrollment and component with the period implicit, so a column per
(component, period) changes the identity of every cell — and that ripples into
the write path, change requests and publication. The transposed grid already had
the right shape.

### Closed periods are superseded, and one thing has no replacement

Examined rather than deleted. `dbo.closed_period` is `(academy_class_id,
grade_prefix)`, and the legacy grade path passes a closing date into its queries
so that marks before it are frozen; the old behaviour screen checked it before
allowing an edit and opened a change request instead.

**Nothing in the rewrite reads it.** Its remaining consumers are the legacy
services behind `/client/**` and the legacy controllers, all of which die at
cutover. The function is covered by publication plus `locks_on_publish`: a
published cell freezes, and an edit becomes a change request. The page stays
until cutover because it still drives the legacy parent API; it is not worth
rebuilding.

**The one difference worth naming:** the new model freezes on *publication*, so a
trimester whose marks are never published stays editable indefinitely, and an
unpublished cell cannot even be disputed — a change request needs something
published to dispute. There is no "close this class's trimester without
publishing it". Whether the school wants that is a question nobody has asked.

### The first full pass through TEST-PLAN.md

Both consoles driven end to end for the first time. 78 of the 119 checks were
executed; 71 passed. Nine defects, seven fixed. Full account in
`TEST-RESULTS.md`; what matters here is the shape of them.

**Three could not have been caught by the suite, and one stopped it dead.**
`AbsenceRegisterPage.js` did not compile - `componentCode` was used in `write`
but never declared a parameter, left behind by the summary-grid change. The dev
server's error overlay covered the entire console, so before anything could be
tested the console could not be opened at all. The 255 tests were green
throughout: nothing compiles the front end.

**The two worst were both a query cache telling the truth about the past.**
The client keeps every answer fresh for a minute, which is right for a list of
schools and wrong for anything being typed into:

* a mark grid redrew from the copy taken *before* the marks were entered, so
  leaving a period and coming back showed an empty grid and made no request at
  all - the teacher's obvious response is to enter them again;
* activating a journal never reached the menu, because the index refetches
  `["JOURNALS","all"]` and the menu reads `["JOURNALS"]`.

The rule this leaves behind: **a query whose data someone is editing is never
fresh, and a mutation invalidates the prefix, not its own key.**

**One was already known and fixed in the wrong place.** The parent console's
`LoginForm` was still missing `e.preventDefault()` - the same fault repaired in
the staff console earlier in the rewrite. Parents could not log in. Finding a
fault in one console is a reason to grep the other.

**One had never been exercised.** The journal wizard could only produce a
working journal when the frequency happened to be TRIMESTER: `ColumnEditor`
hardcoded every new column to `ROLLUP` with no control to change it, so a
once-a-year journal got columns on a tier it has no periods of and its tab could
never draw a grid. `period_kind` naming the tier a column lives on (db/033) is
only half the story if the editor cannot set it.

**Two are not bugs and were left alone**: a journal's class assignment pins a
version rather than restricting who may open it, which is deliberate and
documented in `TemplateVersionResolver`, and the parent modules differ by school
rather than nesting. Both are in `TEST-RESULTS.md` §3 as questions for the
school.

### Corrections made along the way

Recorded because they are easy to regress into.

* **`IDENTITY` ids were wrong.** Hibernate disables JDBC insert batching for identity columns. Use
  per-table **sequences with `allocationSize = 50`**.
* **`MERGE` upsert was unnecessary.** Having loaded the working set we already know which cells
  exist, so the flush splits into a batched INSERT and a batched UPDATE.
* **`node_modules` is *not* committed.** An earlier claim in `SYSTEM-FUNCTIONALITY.md` was wrong;
  the repo tracks 509 files with no `target/` or `build/`.
* **The `DerivationRule.fallback` self-FK was unworkable** — a rule belongs to exactly one component
  under a unique constraint, so a linked fallback had nowhere to live. Replaced with `chain_order`.
* **Version resolution was retroactive.** The write path originally took the *currently assigned*
  template version, which would have silently recomputed October marks under February's rules.
* **Three "entirely new" columns in the brief were not all new.** `CLIENT-BRIEF-2026.md` listed
  final exam, overall academic assessment and academic project assessment as having no counterpart
  today. Checked on the school's challenge: **`GradeType.FINAL_EXAM` exists**, and overall academic
  assessment exists as a computation — `GradeServiceImpl:565` averages semester 1, semester 2 and
  the final exam. Only **academic project** is genuinely new. (`SHEMOKMEDEBITOBA` is creativity, a
  different column.) The right statement is that three columns are missing from the *seeded
  journal*, not from the system.
* **Asking the school for those formulas was the wrong question.** Formulas became configuration in
  phase 5; the school types them into the formula editor. The two readable from legacy code are
  seeded so the grid is not empty on day one, and can be changed from the UI.

---

## 5. Verified facts about the legacy system

Checked against the live database, not inferred.

* `dbo.grades`, `dbo.students`, `dbo.closed_period` are **heaps** — no clustered index and **no
  primary key index at all**. Every lookup is a full table scan, `findById` included. Only
  `dbo.absence_grades` has a PK.
* A `hibernate_sequence` **table** exists, confirming `GenerationType.AUTO` fell back to a single
  shared table generator across every entity.
* `application.yml` pins `org.hibernate.dialect.SQLServerDialect` — the SQL Server 2000-era dialect,
  which **has no sequence support**. This is why AUTO fell back, and it is why the new entities need
  `SQLServer2012Dialect` (proved by an integration test failing with
  `Invalid object name 'sgs.school_seq'` until it was changed).
* `grades.value` is `numeric(16,2)`, so decimals are preserved.
* **The legacy publication lock does not hold.** Parent queries filter
  `qGrade.createTime.before(latest)` (`GradeRepositoryCustomImpl:419, 453, 468, 483`), but
  `insertStudentGrade` updates rows **in place** (`GradeServiceImpl:82`), so `createTime` never
  moves. A mark published in October and edited in November still passes the filter and reaches
  parents at once — no publication, no change request, no director. Clearing a value **deletes the
  row** (`GradeServiceImpl:79`), so it vanishes for parents too. The only guard is a UI check on
  the *behaviour* page; on the trimester screen the change-request modal is wired but nothing
  opens it. This is why publication is rebuilt per cell rather than by timestamp.
* **The collation is `SQL_Latin1_General_CP1_CI_AS`, which has no code page for Georgian.**
  `SELECT CAST(N'მათემატიკა' AS varchar(64))` returns `??????????`. Legacy `dbo` tables use
  `nvarchar` and are fine; the generated `sgs` schema used `varchar` for all 37 string columns
  until this was found. Any future column holding Georgian must be `nvarchar`.
* **`dbo.academy_class.class_level` is the school (1/2/3), not the grade.** The grade is the
  leading integer of `class_name` (`5ა`, `7ტ1`, `10ჰ2`); all 47 rows parse.
* **`dbo.subject` is "subject taught by teacher X"** — 143 rows, **51 distinct names**,
  `ინგლისური ენა` alone appearing 16 times. Its free-text `teacher` column is filled on 142 of
  143 rows, formatted `პედაგოგი: <name>`, with 98 distinct names — **only 3 of which match a
  `system_user_table` row**. Displayed by the old trimester page in the grid header.
* **8 duplicate usernames and 7 students with no class** exist in the live data. Neither was
  constrained in `dbo`; both are in `sgs`.
* Live data volumes: 921 students, 853 grades, 143 subjects, 47 classes, 61 system users,
  273 change requests. **658 `class_subject` rows over 47 classes ≈ 14 subjects per class** — used
  for the row-count estimates above.

---

## 6. What has been built

All under `mthiebi.sgs.gradebook.*`. Nothing committed to git.

### Phase 1 — backend foundation

#### Sgs-model

* `gradebook/model/` — **30 files**: academic structure (School, AcademicYear, ClassGroup, Student,
  Enrollment, Subject, ClassSubject, TeachingAssignment), the period tree (PeriodScheme, Period,
  PeriodKind), templates (GradingTemplate, TemplateVersion, GradeComponent, DerivationRule,
  DerivationTerm, DerivationSource, TemplateAssignment), data (GradeEntry, ClassPeriodSetting),
  and the enums.
* `gradebook/repository/` — 8 Spring Data repositories, including the working-set queries.

#### Sgs-server

* `gradebook/engine/` — `TemplateGraph` (topological order, dependents), `Evaluator`,
  `RecomputeEngine`, `TemplateValidator`, `WorkingSet`, `PeriodTree`, trace types.
* `gradebook/service/` — `TemplateGraphLoader` and `PeriodTreeLoader` (cached),
  `GradeWriteService` (the batch write), `GradeExplainService`, `SpecialValueRegistry`, DTOs.

#### core

* `controllers/gradebook/GradebookController` — `POST /api/gradebook/grades/batch` and
  `GET /api/gradebook/grades/explain`.
* `SgsApplication` now scans `mthiebi.sgs.gradebook.model` and `mthiebi.sgs.gradebook.repository`
  alongside the legacy packages.

### Phase 2 — grade entry

**Backend**

* `GET /api/gradebook/grid` — the whole screen in one call: columns from the template, column
  groups, students, cells with `rowVersion`, publication state, capabilities.
* `GET /api/gradebook/classes`, `.../classes/{id}/subjects`, `.../classes/{id}/periods`.
* `grade_entry` gains `published_value`, `published_special_value`, `published_at`.
* `GradeWriteService` rejects edits to published cells per cell (`CellRejectionReason.PUBLISHED`);
  `NOT_EDITABLE` demoted from throwing to a per-cell rejection.
* `TemplateVersionResolver` shared by read, write and explain.
* `db/004_publication.sql`, `db/005_nvarchar.sql`, `db/006_migrate_from_dbo.sql`,
  `db/007_seed_template.sql`.

**Frontend** — `admin-console/src/front-ac/src/main/pages/gradebook/`

* `GradebookDashBoard` — the page; `GradebookToolbar` — class/subject/period from the new model.
* `useGradeSheet` — grid state and the debounced batch autosave.
* `gridColumns` — template columns to DataGrid columns; `gradebookStyles` — cell states.
* `ExplainPanel`, `SaveStatus`.
* `main/components/grid/DataGrid.js` — removed `rowBuffer={5000}` / `columnBuffer`, which had
  disabled virtualization outright; `sx` now merges with the base styles instead of replacing them.

### Phase 3 — publication and change requests

**Backend**

* `Publication` and `GradeChangeRequest` entities, with repositories.
* `PublicationService` — publish a (class, period[, subject]), release log, guardian email
  after commit.
* `ChangeRequestService` — raise, queue, approve/reject; approval republishes the dependency
  closure.
* `GradeWriteService.applyApproved` — the privileged write, not reachable from a controller.
* `PublicationController` — `POST /publish`, `GET /publications`, `POST|GET /change-requests`,
  `POST /change-requests/decide`. `ActorResolver` extracted and shared.
* `GridCell` gains `id` and `changeRequestPending`, so a locked cell can be disputed and a
  teacher is told before raising a second request.
* `db/009_publication.sql`; the filtered unique index in `db/002_indexes.sql`.

**Frontend**

* `PublishButton` — confirmation, class-wide by default, subject filter as an option.
* `ChangeRequestModal` — raised from the right-click menu on a published cell.
* `changeRequests/ChangeRequestQueue` — the director's queue with approve/reject and a drift
  warning when the cell moved after the request was raised.
* `changeRequests/PublicationLog` — the release history.

### Phase 4 — exports

**Backend**

* `GradeExportService` — `matrix(class, period, component, splitByChildPeriod)` and
  `detail(class, subject, period)`, both drawing columns from the template.
* `ExportController` — `GET /api/gradebook/export/matrix|detail`. No longer under `/test`;
  Georgian filenames RFC 5987 encoded.
* `GradeComponent.outputOffset`; `GradeEntryRepository.loadMatrix`.
* `db/010_subject_order.sql` (reseeds `sort_index`), `db/011_output_offset.sql`.

**Frontend**

* `ExportMenu` on the gradebook toolbar — this subject in full, all subjects summarised,
  or all subjects split by child period. Disabled while there are unsaved edits.

### Phase 5 — journals as data

**Backend**

* `TemplateScope` deleted. `GradingTemplate` is a journal: uuid, `JournalFrequency`,
  `subjectScoped`, `sortIndex`, `archived`. `TemplateAssignment` rekeyed to the journal.
* Journal threaded through the grid, write, explain and export paths;
  `TemplateVersionResolver` takes it instead of hardcoding `ACADEMIC`.
* `JournalService` — wizard create, rename, archive, reorder, whole-version save with
  validation, activate, and the cross-journal column picker.
* `MigrationService` — preview and apply, per (class, period) and for every stale period.
* `TemplateGraphLoader.componentsReachableFrom` / `versionIdByComponent` — the spanning graph.
* `GradeWriteService.recomputePeriod`; cross-journal working set and version stamping.
* `MANAGE_TEMPLATES`; `JournalController`. `db/012_journals.sql`.

**Frontend** — `admin-console/src/front-ac/src/main/pages/journals/`

* `JournalsDashBoard` — the index with the add button; `JournalWizard` — name, frequency,
  shape, in that order because the last two change what a column means.
* `ColumnEditor` — one row per column, with an advanced toggle; `FormulaEditor` — a row per
  term with weight and a picker spanning every journal.
* `JournalEditor` — validation banner, save, activate; `MigrationPrompt` — the recalculate
  prompt, naming the columns whose marks would be deleted.
* `useNavigationData` builds a menu entry per journal, keyed by uuid.

### Phase 6 — parent console

**Backend**

* `GradingTemplate.parentVisible` and `chartKey`; `db/014_parent_view.sql`.
* `ParentViewService` — one service for every journal, reading `published_value` only.
* `ParentController` at `/api/parent`, authenticated; `UserDetailsServiceImpl` now loads a
  student for that path as well as `/client/`.
* `GradeEntryRepository.loadPublishedForStudent` — `loadGrid` cannot serve it, because its
  subject clause returns **only subject-less cells** when passed null.

**Frontend** — `client-console/src/pages/journal/`

* `JournalPage` — the one renderer; `RowCards` and `RowTable`; `charts/` registry.
* `AfterLoginPage` rebuilt from `/api/parent/journals`, replacing five hardcoded boxes and
  the `/grades/<subject NAME>` route that matched subjects by string.
* Admin side: `JournalSettings` — the visibility tick and the chart picker.

### Phase 7 — conversion scales, bulk export, the brief's annual columns

* `ConversionFormula` — one row, `multiplier` and `offset`; `output_offset` dropped (`db/017`)
* `GradeConversionService` — conversion, unrounded rendering, save-time validation
* Applied in the grid (`GridCell.convertedValue`) and in the Excel exports behind a flag.
  **Not** in the parent view
* A toggle in the gradebook toolbar; the grid is read-only while it is on. A checkbox in the
  export menu
* `ConversionFormulaDialog` — edit the formula, with a live preview of what it does to a mark
* Bulk export: `/export/bulk/matrix` and `/export/bulk/detail`, streamed as a zip, scoped by
  `ClassScopeGuard`, foldered by class
* `db/018` — final exam, overall academic assessment and academic project, with
  overall = average(annual, final exam)

**Two faults this phase found in itself.** Naming a `@Service` class `ConversionService`
takes the bean name Spring Boot's property binding looks up for
`org.springframework.core.convert.ConversionService`, and **every application context fails
to start** — production included. Caught by `ApplicationWiringIT`, not by any unit test.
And an `IF EXISTS` guard around a statement naming a dropped column does not protect it:
SQL Server compiles a whole batch before running any of it, and deferred name resolution
covers missing tables but not missing columns. `db/017` would have broken on its second run;
the migration now goes through `EXEC`.

### Phase 8 — content substrate and homework

* `Post` / `PostTarget` / `PostLink` with a `PostKind` discriminator (`db/019`)
* `PostService` — create, edit, publish-with-snapshot, soft delete; shared by all five
  modules, used by homework
* `HtmlSanitizer` — OWASP allowlist applied on write, plus a URL check for the link list,
  which does not go through the HTML policy and would otherwise be the unguarded way in
* `HomeworkController`, every endpoint through `ClassScopeGuard`
* `MANAGE_HOMEWORK`, registered in both the server catalogue **and** the console's
  permission list — absent from the latter it cannot be granted, which is what made the
  journal editor unreachable in phase 5
* A class roster endpoint (`/classes/{id}/students`) for the target picker
* The homework screens: class and date filters, one accordion per subject, top-5 with
  "see more", and an editor with `react-quill`, a student picker whose empty state means
  the whole class, and a repeatable link list

**Two faults found while building it.** `published_payload` generated as `nvarchar(255)`
because the length was left to JPA's default — the first real publish would have
overflowed it. And the sanitiser policy called `allowStandardUrlProtocols()` after
restricting to http/https, quietly adding `mailto` back; an allowlist that widens itself
is not one.

**Two new dependencies**, against decision 19's "same stack": `react-quill` and
`owasp-java-html-sanitizer`. Neither is avoidable — the brief asks for text formatting,
and the sanitiser is what makes that safe.

### Phase 9 — schedule, menu, characterization, news

* `PostLine`, `PostCategory`, `PostImage`; `post` gains `category_id` and `image_id` (`db/020`)
* `ImageService` — cap, downscale, re-encode; `CategoryService` — find-or-create
* `StandingDocController` (schedule and menu), `CharacterizationController`,
  `NewsController` including upload and serving
* Four permissions, each registered in the server catalogue **and** the console's list
* Four screens: the five-weekday standing document shared by schedule and menu, the
  characterization accordion, and the news grid with upload and category autocomplete

**Three faults found while building it.** `@Secured` with two permissions is an *or*, so a
shared schedule/menu endpoint would have let either permission edit both. Uuid-addressed
writes never checked the post's *kind*, so a per-module permission could act on another
module's post through its own URL. And Spring's default multipart cap is **1 MB** — below
the application's own 2 MB limit — so a 1.5 MB photo would have failed with a framework
error before the check that has a readable message.

**One thing phase 8 got wrong, now fixed.** `PostService` required a class on every post
while `post.class_group_id` was deliberately nullable for news. The table was right and the
service contradicted it.

### Phase 10 — the absence registers

Two mechanisms, not one. The first version made both a journal; the revision below split
them, because a journal cell carries a value and a tick is not one.

**Daily — its own table.**

* `sgs.daily_absence(enrollment_id, absence_date, marked_at, marked_by)`, unique on
  `(enrollment_id, absence_date)`. A row means absent, no row means present, and the third
  state does not exist to be interpreted.
* No value, no scale, no row version, no publication, no period. `DailyAbsenceService` reads
  a month as a date range and `DailyAbsenceWriter` writes one cell per transaction, so a
  race lost on one child cannot roll back the column.
* `DailyRegisterPage` — staff only. No publish button and no change request.
* `db/028` creates the table, migrates the marks, retires the daily journal and deletes the
  day periods.

**Monthly — still a journal.**

* Typed academic hours with a yearly total, genuinely published to parents.
* `AbsenceGridService` — the transposed read, walking the period tree because the columns
  are the year's *grandchildren*. `AbsenceSettingsService` for the brief's two numbers,
  **per month**, on the `ClassPeriodSetting` table phase 1 already built for them.
* Published without freezing: `db/029` adds `locks_on_publish`, false here, so the
  coordinator can top the month up and republish without an approval on the ordinary path.

**Shared.** `AbsenceNotifier` — queue, coalesce, re-read, send, keyed on the date alone.
`AbsenceRegisterController` carries both, on separate endpoints.

**`PeriodReach`** — one owner for "which periods does this touch?", replacing six inline
expressions that had drifted apart. `PeriodReachTest` asserts the property none of them
could: if evaluating X reads P, then changing P recomputes X, and only X.

**One fault, and it was the same one as phase 7's.** A new `AbsenceController` collided with
the **legacy** `mthiebi.sgs.controllers.AbsenceController`: two `@RestController` classes
with the same simple name get the same bean name, and that does not clash quietly - it stops
the entire application context from starting. Caught by `ApplicationWiringIT` again, which
is now three times that test has found something no unit test could.

### Tests — 255, all passing

Counted from the build, not maintained by hand: 75 unit and 179 integration, plus the
schema export. The per-file list below is indicative and goes stale; `mvn verify` is the
authority.

* 13 `EvaluatorTest` — weighted sums, renormalisation, blanks, special values, cross-period,
  cross-subject, fallback chains, rounding, trace
* 7 `RecomputeEngineTest` — three-axis fan-out, ordering, overrides, idempotence
* 7 `TemplateGraphTest` — cycles, dangling refs, self-reference, duplicates, fallback edges
* 9 `TemplateValidatorTest` — errors vs warnings
* 1 `SchemaExportTest` — validates mappings and regenerates `db/001_schema.sql`
* 7 `GradeWriteServiceIT` — against real SQL Server: save + derived values returned, persistence,
  no-churn, per-cell conflicts, whole-class batch, explain output, **version pinning**
* 9 `GradeGridServiceIT` — columns from the template, Georgian round trip, column grouping,
  transitive dependents, calculated columns editable, row versions, the publication lock, one
  locked cell not sinking a batch, recompute-through-publication
* 2 `MigratedDataSmokeIT` — draws a real grid from the migrated school data and checks the
  teachers survived the subject fold; skips (does not fail) when the migration has not been run
* 13 `GradeExportServiceIT` — columns from the template, values printed as stored rather
  than truncated, the teacher row, the annual split by child period, unknown column
  refused, sheet named after the class, subject order, special values, and **the conversion
  formula**: it shifts only what is printed and storage is untouched, nothing converts
  unless the caller asks, converted output is unrounded, ჩთ is never converted, and
  nothing converts when no formula is configured
* 10 `AbsenceGridServiceIT` — a month of days as columns, the monthly register finding
  months two levels under the year, a period below the journal's level refused, marks
  landing on the right day, the two settings round-tripping and a null clearing one, and
  the notification window: nothing sent inside it, one notice per student per day, a
  withdrawn mark cancelled with **no mail sent**, and a standing mark actually sent
* 8 `ImageServiceTest` — large images scaled and small ones left alone, bytes re-encoded
  rather than stored as uploaded, a non-image refused, an oversize upload refused before it
  is decoded, transparency kept as PNG, and a 4000px photo actually ending up under a
  megabyte
* 11 `HtmlSanitizerTest` — formatting and Georgian surviving, and script tags, event
  handlers, `img onerror`, `javascript:` hrefs, style attributes and iframes not; plus the
  link list's own protocol check
* 20 `PostServiceIT` — the draft/publish state machine, **an edit to a published post not
  changing what parents see**, re-publishing catching up, targets replaced rather than
  appended, script stripped in the database rather than only in the response, a bad link
  dropped while a good one is kept, soft delete hiding without deleting, an archived post
  refusing to publish, newest-first with a limit, and Georgian round-tripping; plus phase 9's
  news saving with no class while every other kind still requires one, one standing schedule
  per class edited rather than added to, lines keeping their weekday order and being replaced
  rather than appended, empty lines dropped, and a repeated category name reused
* 7 `GradeConversionServiceTest` — the 7→10 conversion the school runs on, a proportional
  9→10, no rounding, trailing zeros, null multiplier and offset as identity, and "no
  formula" meaning print-as-stored rather than print-a-blank
* 11 `JournalServiceIT` — stable identity across a rename, the menu, archiving, columns and
  formulas round-tripping, a renamed column keeping its id, the picker, **a formula reading
  another journal**, **a cycle spanning two journals blocked**, forking on edit, activation
  leaving existing marks pinned
* 11 `ParentViewServiceIT` — unpublished marks invisible, published visible, **an edit
  after publication still shows what parents were shown**, subjects as rows with a period
  picker, one subject giving the single row that renders as cards, blank columns listed,
  unreleased journals refused by uuid, staff-only columns hidden, the chart key carried,
  one parent unable to read another child
* 13 `PublicationServiceIT` — publish copies values, blanks stay unpublished, republishing
  releases only what moved, the release is logged, a request needs publication and a reason,
  rejection changes nothing, **approval republishes the dependency closure**, an approval does
  not release cells that were never published, no deciding twice, the queue is flattened, the
  grid marks pending requests, one open request per cell

---

## 7. How to build and run

**Java.** The machine's default `java` is JDK 8; the project needs 11.

```bash
export JAVA_HOME="/c/Program Files/Java/jdk-11.0.10"
export PATH="$JAVA_HOME/bin:$PATH"
```

**Maven.** Use the wrapper from the repo root. When building a single module, `-am` is required or
it resolves a stale `Sgs-model` jar from the local repo (`C:/Users/AzRy/.m2`).

```bash
./mvnw -B test                       # unit tests only, no database needed
./mvnw -B verify                     # adds the *IT integration tests, needs the database
./mvnw -B -pl Sgs-server -am test    # one module
./mvnw -B -DskipTests clean install  # the runnable war - always with `clean`
```

**Always `clean` when building the war**, and stop any running instance first. `core/target/
core-0.0.1-SNAPSHOT/` is an exploded war Maven does not tidy between builds, so a rebuild without
it packages whatever was there before, renamed classes included - two controllers with one simple
name is a `ConflictingBeanDefinitionException` and the application will not start. No test catches
it, because tests read `target/classes`, which is correct; only the war is wrong. A running
`java -jar` also holds the war open, and `verify` then fails at `repackage` before any integration
test runs.

**Regenerating the schema** after changing an entity — then review the diff:

```bash
./mvnw -B -pl Sgs-model test -Dtest=SchemaExportTest   # rewrites db/001_schema.sql
```

**Database.** It lives in a Docker container from an old project. Docker Desktop's GUI does not
list containers on this machine, but the CLI works fine.

```bash
docker start sps-mssql-db            # port 1433
```

Credentials are the ones already in `core/src/main/resources/application.yml`. `sqlcmd` inside the
container is at `/opt/mssql-tools/bin/sqlcmd`.

**Two databases live in that container.** `SGS` holds the school's migrated data; `SGS_DEMO` is a
small seeded one to test against - two classes, two students each, three journals, an
administrator. Build or rebuild it from nothing with

```powershell
.\db\demo\reset.ps1                 # about half a minute, never touches SGS
```

From PowerShell use the `.ps1`, not `bash db/demo/reset.sh`: `bash` on PATH here is
`C:\Windows\system32\bash.exe`, the WSL launcher, which runs the script inside Ubuntu
against a different filesystem. The wrapper locates Git Bash and does nothing else.

`db/demo/README.md` says what is in it, what is deliberately left out, and the logins. The
application points at `SGS_DEMO`; changing `databaseName` in `application.yml` is the whole of the
switch back.

**Frontend.** **yarn**, not npm. The repo carried both lockfiles, out of sync since 2023;
`package-lock.json` has been removed and gitignored. yarn's own registry
(`registry.yarnpkg.com`) times out on this machine, so installs need the npm one:

```bash
yarn install --frozen-lockfile --registry https://registry.npmjs.org --network-timeout 120000
```

`react-scripts` 4 uses webpack 4, which will not run under Node 17+ without the
legacy OpenSSL provider:

```bash
cd admin-console/src/front-ac
NODE_OPTIONS=--openssl-legacy-provider npx react-scripts build     # or start
```

Do **not** set `CI=true`: it promotes warnings to errors, and the legacy pages carry hundreds of
pre-existing lint warnings. The build is clean of warnings from `pages/gradebook`.

**Database scripts**, in order. 001 and 002 build the schema; 004 and 005 only matter for a
database created before those changes; 006 and 007 bring the data in.

**A fresh database, in this order.** Every one of these is needed; leaving any
out gives a system that starts and then fails somewhere specific.

```
001_schema.sql            generated from the entities - do not hand-edit
002_indexes.sql           the covering index JPA cannot express
006_migrate_from_dbo.sql  students, classes, subjects, enrollments
007_seed_template.sql     the trimester journal. Weights are PROVISIONAL.
008_class_subject_teacher.sql  class_subject.teacher_name
010_subject_order.sql     class_subject.sort_index, the teaching order
013_period_levels.sql     months (10) and weeks (40), so MONTH and WEEK
                          journals have periods to hang off
015_student_identity.sql  merges duplicate student records and enforces
                          unique personal_number + unique (username, password)
016_change_request_version.sql  optimistic locking on a change request
019_posts.sql             post + post_target + post_link: staff-authored
                          content, homework in phase 8 and the other four
                          modules on the same table as they land
020_content_modules.sql   post_line (schedule and menu rows), post_category,
                          post_image, and the two news columns on post
021_absence.sql           replaces the unused week level with dated school days,
                          adds grading_template.grid_mode and absence_notice.
                          Refuses if anything has started using depth 3.
022_absence_journals.sql  the daily and monthly absence journals themselves
023_absence_rollup_fix.sql  repoints the two yearly totals from CHILDREN to
                          DESCENDANTS and deletes the wrong-level rows the
                          broken rules had persisted
024_absence_notice_constraint.sql  drops uq_absence_notice, which silently
                          suppressed a real absence after any cancelled one
025_absence_notice_pending_unique.sql  restores that guarantee as a *filtered*
                          unique index over pending notices only - 024 removed
                          the guard rather than reshaping it
026_absence_scales.sql    the yearly totals had inherited the daily column's
                          0..1 scale, so an override of one was out of range
028_daily_absence.sql     daily absence leaves grade_entry for its own table:
                          creates sgs.daily_absence, migrates the marks, retires
                          the daily journal and its rollup, deletes the depth-3
                          day periods and drops absence_notice.period_id.
                          Refuses if anything still hangs off a day period.
031_enrollment_placement.sql  where a child sat and when, beside the
                          enrollment rather than splitting it
033_trimester_columns.sql  period_kind stops meaning "the year or the
                          journal's own level" and starts naming the tier a
                          column lives on; the absence register gains its
                          trimester total
034_summary_columns.sql   component.summary_column: which columns make the
                          brief's report card
032_reporting_periods.sql  ten calendar months become the brief's seven
                          reporting periods. Must run after 028: it refuses
                          while anything sits below the months, and 013's weeks
                          only clear once the day periods are gone
029_publication_lock.sql  grading_template.locks_on_publish replaces
                          publishes_blanks: publishing and freezing become two
                          things, and the register does the first without the
                          second
018_annual_columns.sql    final exam, overall academic assessment and academic
                          project - the brief's section 4 columns the seed
                          lacked, plus the overall = average(annual, exam) rule
```

**Upgrade-only.** Each of these exists solely for a database created before the
change it makes; 001 already carries all of them.

```
004_publication.sql     published_value / published_special_value / published_at
005_nvarchar.sql        varchar -> nvarchar, rebuilding indexed columns' constraints
009_publication.sql     publication + grade_change_request tables
011_output_offset.sql   component.output_offset - SUPERSEDED by 017, which
                        drops the column
012_journals.sql        journals as data; rekeys template_assignment
014_parent_view.sql     grading_template.is_parent_visible + chart_key
017_conversion_formula.sql  the one conversion formula, seeded with the +3 the
                        school runs on today; drops output_offset. Fresh
                        installs get the table from 001 but still need this
                        script for the seed row.
```

All are idempotent, so running the upgrade set against a fresh database is
harmless - it simply finds everything already present.

> `007` was rewritten after phase 5 dropped `scope`; a fresh install now runs
> 001 → 002 → 006 → 007 → 013. Scripts 004, 005, 009, 011 and 012 only matter for a
> database created before the change each of them makes.

> A filtered index needs `QUOTED_IDENTIFIER ON`, which `sqlcmd` does not set — the scripts set it
> themselves. The JDBC driver sets it, so the application is unaffected.

**Applying schema changes:**

```bash
export MSYS_NO_PATHCONV=1            # needed for docker exec paths
docker cp db/001_schema.sql sps-mssql-db:/tmp/
docker exec sps-mssql-db /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P '<pw>' -d SGS -i /tmp/001_schema.sql
```

> **Gotcha:** `MSYS_NO_PATHCONV=1` breaks the Maven wrapper (it stops Git Bash rewriting
> `/c/...` into `C:\...` for `java.exe`). Set it only for the `docker` commands, never in the same
> shell as `./mvnw`.

Mounting the `sps-db-volume` from a throwaway container hangs; starting the container itself is
fine. That was the only real Docker fault found.

---

## 8. Carried forward

~~**No UI can add a student to the new system.**~~ **Closed by phase 12.**

The surviving admin pages write to `dbo`; everything the rewrite built reads `sgs`. The new API's
write endpoints are grades, absence, content, journals and change requests - there is no create or
update for a student, a class or a subject anywhere in it. So a child added through the console
does not exist as far as the gradebook is concerned, and the demo database needed a hand-written
roster script rather than four clicks.

`db/006` migrated the roster once, which is why it had not bitten yet — it would have
in September. The students, classes and subjects screens now write `sgs`, and the three legacy
pages that wrote `dbo` are deleted. `REWRITE-ROSTER.md` has the design and what is still open.

**Not yet done in the backend**

* ~~`application.yml` still pins `SQLServerDialect`.~~ **Changed to `SQLServer2012Dialect`, and it
  was a live fault rather than tidying.** The old dialect reports no sequence support, so Hibernate
  substituted a *table* generator for every sequence in the new model and no `sgs` row could be
  written at all - while the application still started, because `ddl-auto: update` logs schema
  failures as warnings. Invisible to the suite, which overrides the dialect. The legacy entities
  still owe one migration on the school's own database: `dbo.hibernate_sequence` is a table there
  and the new dialect wants a sequence. `FOLLOW-UPS.md` §6 has the script.
* ~~`TrimesterDashBoard` is still registered in the menu.~~ **Deleted**, along with every other
  legacy screen the rewrite replaced: 58 files, 8,305 lines. The comparison the pages were kept
  for had already been made. What is gone is `trimester`, `behaviourPage`, `absencePage`,
  `changeRequestPage`, `MonthlyGradePage`, `anualPage`, `semesterPage`, `HomePage` and
  `totalAbsencePage` - the last checked first, and superseded: the new absence register carries
  the same academic-hours figure per period plus the permitted one legacy never had.
* **What was *not* deleted, and why it matters.** `systemUserPage`, `systemUserGroup`,
  `studentPage`, `subjectPage`, `academyClassPage` and `closePeriod` are not old versions of
  anything - nothing replaced them. They are the only UI for staff accounts, permissions and the
  roster. See the gap below.
* `TeachingAssignment` is empty — it requires a `system_user_id` and only 3 of the 98 teacher
  names match an account. The names live on `class_subject.teacher_name` and are displayed; the
  structured form waits until teachers have logins.
* No production template seeder. **Deliberate**: the trimester weights in the test fixture are a
  stand-in, and the school has not said how the trimester assessment is actually calculated. It is
  configuration, so guessing it in a seeder would be worse than entering it once they answer.
* `node_modules/.yarn-integrity` is still tracked. Untrack with
  `git rm --cached node_modules/.yarn-integrity`.

**Open questions for the client** — only one is blocking anything, and it is not urgent

1. **Do any rules drop or ignore marks** — "discard the lowest of seven", or "use the final exam if
   it beats the trimester average"? Nothing in the legacy code does, and neither shape is currently
   expressible. Adding a `reduce` type is cheap; adding a conditional shape is about a day.
2. How is the trimester assessment actually calculated? (configuration, answerable late)
3. Is ethics one value per month, or the current 5-criteria weekly detail? (sizing only)
4. The exports' `isDecimal` **+3 shift** — a real stored-vs-printed scale conversion, or a hack?
5. The unlabelled 14th column in the brief's trimester table.

**Phases remaining** — see the roadmap in section 3.

The agreed rhythm is: **discuss a phase, document it, build it, then move on.**
