# Phase 8 — the content substrate, and homework

The five things the client brief asks for beyond grades — homework, the daily schedule, the
menu, student characterizations and news — are one model configured five ways. This builds
that model, and builds **homework** on it.

**Staff side only.** The parent side of all five modules is deferred to phase 11 as one
piece, because it carries UI decisions the school has not made yet.

---

## 1. Why one table

|                      | Homework | Schedule | Menu | Characterization | News        |
|----------------------|----------|----------|------|------------------|-------------|
| Dated                | ✔        |          |      | ✔                | ✔           |
| Belongs to a subject | ✔        |          |      | ✔                |             |
| Belongs to a class   | ✔        | ✔        | ✔    | ✔                | school-wide |
| Targets students     | some     |          |      | exactly one      |             |
| Rich-text body       | ✔        |          |      | ✔                | ✔           |
| Draft → published    | ✔        | ✔        | ✔    | ✔                | ✔           |

They differ in about four fields and agree on everything structural: authored by staff,
scoped, dated, drafted then published, edited, archived.

So: **one `post` table with a `kind`**, and the differences as nullable columns. The cost is
those nulls. The win is one service, one publish flow, one HTML sanitiser, one scope check
and one audit trail, rather than five that drift apart — which is precisely how the legacy
system arrived at four copy-pasted export methods that each excluded a different set of
columns.

### What phase 8 actually creates

Only what homework uses. `post_line` (schedule and menu), `image_id` and `category_id`
(news) are nullable additions phase 9 makes when it has a use for them. Building an unused
table now would be designing against a guess.

```
post         id, uuid, kind, class_group_id, subject_id, event_date,
             title, body_html, status, published_at, published_payload,
             has_unpublished_changes, archived, + audit
post_target  post_id → enrollment_id      empty means the whole class
post_link    post_id, url, label, ordinal
```

---

## 2. Publication: frozen, not live

**Settled with the school: any edit needs a re-publish.** A published item keeps showing
parents what was published until someone publishes again.

This was worth asking, because it is the opposite of the obvious answer. Live editing means
a typo fix reaches parents at once; frozen means nothing reaches them that a person has not
deliberately released. The school chose the second, consistent with how they already work
on grades.

### Three states, not two

The brief asks that saved and sent assignments be visually distinguishable. Frozen
publication adds a third state that the brief does not mention but the school cannot work
without:

| State                 | Meaning                                                        |
|-----------------------|----------------------------------------------------------------|
| **Draft**             | Never published. Parents have never seen it.                   |
| **Published**         | Published, and the working copy matches what was published.    |
| **Published, edited** | Published, then edited. Parents are still seeing the old text. |

Without the third, a teacher edits, walks away satisfied, and parents never see the change.
`has_unpublished_changes` is set by any edit to a published item and cleared on publish.

### The snapshot

Publishing writes `published_payload` — a JSON snapshot of exactly what a parent should
see, including targets and links. Parents read the snapshot; the working copy is staff-only.

One column rather than mirrored `published_*` columns, because the content spans child
tables and mirroring them all is how this would turn into a versioning system. It is also
what decision 16 already chose for grades: publication by snapshot.

Structural fields stay as real columns — `kind`, `class_group_id`, `subject_id`,
`event_date`, `status`, `published_at` — so phase 11 can query "this class's published
homework in March" without parsing JSON.

**Nothing reads the snapshot until phase 11.** It is written now because the alternative is
retrofitting it into data that has been accumulating for a term.

---

## 3. Rich text, and the hole it would open

Nothing in either console has an editor, and nothing on the server sanitises HTML. Storing
what a WYSIWYG editor produces and later rendering it to parents is a **stored XSS
vulnerability** unless the server strips it.

* **`react-quill`** in the console. Works under webpack 4, which `react-scripts` 4 pins us
  to. Bold, italic, underline, lists, links, headings.
* **OWASP `java-html-sanitizer`** on the server, applied **on write**, so nothing dangerous
  is ever stored. Sanitising only on read leaves a loaded gun in the database for the next
  person who renders it somewhere else.

Allowlist: `p br strong b em i u ol ul li a[href] h3 h4 blockquote`. Everything else is
dropped, including `style`, `script`, `on*` and any `href` that is not http/https.

Two new dependencies, against decision 19's "same stack" — noted deliberately. Neither is
avoidable: the brief asks for text formatting, and the sanitiser is what makes that safe.

---

## 4. Scope and permissions

**`MANAGE_HOMEWORK`**, new. Per module rather than one `MANAGE_CONTENT`, so a subject
teacher can be allowed to set homework without also being allowed to publish school news.
The other four permissions arrive with their modules.

Every endpoint goes through **`ClassScopeGuard`**. Confirmed with the school: opening
homework shows the whole class's subjects, so class scoping is the right axis and no
subject-level narrowing is wanted. That is as well — narrowing by subject needs
`TeachingAssignment`, which is empty, because only 3 of 98 teacher names match an account.

---

## 5. The screens

**Homework list.** Filters for class and date range. One accordion per subject the class
takes; opening one shows the newest N items, each with its date, title, state marker, and
edit and delete actions. First row is *Add*, last is *See more*, which opens the full list
in a dialog. N is a named constant, tuned by eye.

**Editor.** Date picker; student picker with an **All** option, where all is the default and
an empty selection means the whole class; title; rich-text body; a repeatable list of links
(the file-attachment stand-in — the server is short of space, so links rather than
uploads); Save, Publish, Delete.

**Delete is soft.** `archived`, not a row deletion. The brief's own wireframe says
"deactivate", and a published item that a parent has already read should leave a trace.

---

## 6. Tests

* A draft is created, edited and published; the snapshot matches what was published.
* Editing a published item sets `has_unpublished_changes` and **does not** change the
  snapshot — the whole point of the school's answer.
* Publishing again clears the flag and rewrites the snapshot.
* An empty target list means the whole class; a non-empty one is stored and snapshotted.
* Script tags, `onerror` and `javascript:` hrefs do not survive a save.
* Georgian round-trips through the sanitiser unharmed.
* A user scoped to one class cannot read, write or publish another class's homework.
* Soft delete hides an item from the list without removing the row.
* `MANAGE_HOMEWORK` is in the catalogue, so a group can actually be granted it — the
  failure that made the journal editor unreachable in phase 5.

---

# Phase 9 — the other four modules

Schedule, menu, characterization and news, all staff side. Same table, same publish flow,
same sanitiser. Three of them are configuration of what phase 8 built; news is the only one
that needs new machinery.

## 7. What each one is

**Daily schedule** (`SCHEDULE`) and **menu** (`MENU`) are the same screen twice. One
document per class, five weekday cards, each holding an ordered list of lines. The schedule
line has a time and a text; the menu line has only a text.

Confirmed with the school: **entered once for the year and adjusted occasionally**. No
months, no trimesters, no weekly versions — one page per class, edited in place. That
settles the shape: `post_line` hangs off a single standing post, and there is exactly one
SCHEDULE post and one MENU post per class.

**Characterization** (`CHARACTERIZATION`) is homework with fewer fields: accordion by
subject, and each item is a date, **one** student and a rich-text body. No title.

**News** (`NEWS`) is school-wide — no class at all — and is the only one that brings
something new: a picture and a category.

## 8. What the substrate has to grow

```
post          + category_id, + image_id          nullable, news only
post_line     post_id, weekday, ordinal, time_text, text
post_category id, uuid, name, is_archived
post_image    id, uuid, content_type, byte_size, width, height, bytes
```

**And one thing phase 8 got wrong.** `PostService.apply()` rejects a post with no class,
while `post.class_group_id` was deliberately made nullable for exactly this case. The table
was right and the service contradicted it. The requirement becomes per kind: every kind
needs a class except `NEWS`.

### Images

Uploaded and stored, not linked — the school's answer, having already accepted that
homework attachments are links. News is different because a post without its picture is a
worse page, where an assignment without an attached file is not.

* **Capped at 2 MB** on the way in, and **downscaled to 1600px** on the long edge. A phone
  photo arrives at 4 MB and lands at roughly 200 KB, so a year of news is around 10 MB.
  Storing what was uploaded, unresized, is what makes a small server a problem.
* **Re-encoded, always.** The upload is decoded with `ImageIO` and written back out from
  the decoded pixels. That is also the security check: a file that will not decode is not an
  image, and anything embedded in the original — a polyglot, a payload in a comment
  segment — does not survive being redrawn. The declared content type is not trusted.
* JPEG out, unless the source has an alpha channel, in which case PNG so transparency is
  not flattened onto black.
* **Its own table**, so listing news does not drag image bytes through every query, and
  served by its own endpoint rather than base64 in JSON.

No new dependency: `ImageIO` is in the JDK.

### Categories

A `post_category` table behind an autocomplete. The school said it was irrelevant which
way; a table means `საბავშვო ბაღი` cannot become two categories through a stray space,
while still feeling like free text to whoever types it.

## 9. Permissions

`MANAGE_SCHEDULE`, `MANAGE_MENU`, `MANAGE_CHARACTERIZATION`, `MANAGE_NEWS` — one per
module, as `MANAGE_HOMEWORK` is. News is school-wide, so it is the one module that does
**not** go through `ClassScopeGuard`; every other endpoint does.

Each must be registered in the console's permission catalogue as well as the server's, or
it cannot be granted and the module is unreachable — the failure that hid the journal
editor in phase 5.

## 10. Tests

* A class has exactly one schedule and one menu; saving again edits rather than adds.
* Lines keep their order, and re-saving replaces them rather than appending.
* News saves with no class, and every other kind still refuses to.
* An upload that is not an image is refused; a large one is downscaled and shrinks.
* An image is re-encoded rather than stored as uploaded.
* A characterization takes exactly one student.
* Categories are reused rather than duplicated on a repeated name.
