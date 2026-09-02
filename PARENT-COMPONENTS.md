# The parent console, component by component

A design brief for theming the **primary school** parent side. It describes what
exists today, what each piece is for, what states it has, and what constraints it
has to keep. Primary is to look different — "more playful" — but structurally the
same, so this is written as *what the theme has to cover*, not as a layout.

Basic and secondary keep the current look. Everything here is shared code with a
theme on top, not a second console: two component trees drift, and every later
fix has to be made twice.

---

## What a primary parent sees

Eight screens.

| Screen            | Route               | Built from                                  |
|-------------------|---------------------|---------------------------------------------|
| Login             | `/` when logged out | `pages/loginPage/LoginForm`                 |
| Landing           | `/`                 | `pages/afterLoginPage`                      |
| Homework          | `/homework`         | `pages/homework`                            |
| News              | `/news`             | `pages/news`                                |
| Daily schedule    | `/schedule`         | `pages/standing/StandingDocPage`            |
| Menu              | `/menu`             | the same component, times off               |
| Child description | `/description`      | `pages/standing/CharacterizationPage`       |
| Absence           | `/journal/<uuid>`   | `pages/journal` + `charts/AbsenceBarsChart` |

**Not on the primary side, so not worth designing:** grade tables (`RowTable`),
grade cards (`RowCards`), the subject and period pickers, and the grade-trend
chart. Those are basic and secondary only.

Which boxes appear is decided by the server, from the child's school —
`GET /api/parent/modules` plus the journal list. The console maps a name to a
route and does not decide.

---

## The recurring pieces

Screens are mostly arrangements of these. Theming these seven moves everything.

### 1. Card shell

The single most load-bearing visual. Today: white, `border-radius: 0 20px 20px
20px`, a blue drop shadow.

Used by the calendar, the homework day panel, each weekday card, each news card,
and each description card. **Restyle this one thing and five screens move
together** — which is the main reason not to give any screen its own container.

### 2. Rich-text block

School-authored HTML, rendered as-is. Appears in homework items, news bodies and
child descriptions.

Sanitised **on write** against a fixed allowlist, never on read — so the theme
does not have to defend against it, but does have to survive it. Whatever the
school pastes lands here: headings, lists, tables, images.

Two constraints that must not be lost:

* images capped at container width,
* tables scrolling inside their own box rather than widening the page.

Both are set today. A theme that replaces these rules has to reinstate them, or
one pasted screenshot breaks the layout on a phone.

### 3. Chip

A small rounded label. Three uses already, and they are not all the same thing:

* **filter chip** — news categories, one selected at a time, clickable;
* **category tag** — on a news card, not clickable;
* **subject tag** — on a description card, not clickable.

Worth deciding whether the theme distinguishes "you can press this" from "this
is a label", because the current styling barely does.

### 4. Badge

A count on a coloured pill. Only ever means **unread**: on a calendar day, and on
a homework subject header.

This is the one element that should be loud. It is the whole answer to "is there
anything new for my child", and a parent scanning the calendar is looking for
exactly this and nothing else.

### 5. Date line

A date, sometimes with a clock icon. On news cards, in the news dialog, and on
description cards. Formatted `DD.MM.YYYY`, which is how the school writes it.

### 6. The three states

Every screen distinguishes:

* **loading** — a request is in flight,
* **empty** — the school has not written one yet,
* **failed** — the request did not come back.

These must stay visually distinct. Collapsing "nothing here yet" into "it broke"
has been a real defect in this project twice, and on the parent side it is worse
than on the staff side: a parent who is told "no homework" when the request
actually failed will not check again.

### 7. Accordion row

Only homework subjects, so far. A header that expands, with the subject name, an
unread badge, and a chevron.

---

## Screen by screen

### Login

`pages/loginPage/LoginForm`. Username and password, both typed. Two children in
one family may share a username with different passwords, so the pair is the
identity — the form must not imply the username alone identifies anyone.

The first thing a parent sees, and the only screen with no header.

### Landing

A grid of boxes, one per module the school shows. Currently rendered by
`Box` inside `boxWrap`.

For primary that is six: homework, news, daily schedule, menu, child
description, and the absence register. Order comes from the server.

This is the screen that carries the most of the "playful" brief — it is the only
one that is purely navigation, so it can afford illustration in a way the content
screens cannot.

### Homework

Two components stacked.

**`MonthCalendar`** — a month, Monday first, with a previous/next month bar.
Each day carries up to **three independent marks**, and a day can have all three
at once:

| mark         | meaning                  | today                   |
|--------------|--------------------------|-------------------------|
| *holds work* | the school set something | tinted background, bold |
| *unopened*   | some of it is unread     | orange count badge      |
| *selected*   | currently expanded below | outlined                |

They are separate classes rather than one state deliberately. A theme that
collapses them loses real information — "there is homework" and "there is
homework you have not read" are the distinction the page exists for.

Days with nothing are still clickable. A parent tapping a quiet day should get
"nothing set" rather than a dead cell.

**`DaySubjects`** — the chosen day, one accordion section per subject, first one
open. Each item is a title, a rich-text body, and an optional list of links.

Opening a day marks its assignments read, batched and sent two seconds later —
so the badge on the calendar clears shortly after, not instantly. Worth knowing
for the theme: there is a brief window where a day is open and still badged.

### News

**List** — newest first, ten per page. Each card is: picture on the left, title,
date with a clock icon top-right, category tag, a plain-text excerpt, and a
"ვრცლად" link. The whole card is clickable.

The excerpt is stripped to plain text on purpose — rendering the school's HTML
into a card would let one item's heading resize every other card.

Pictures are fetched with the auth token and drawn from a blob, so they arrive a
moment after the text. The space is reserved either way, including for items with
no picture, so the column does not jump.

**Dialog** — backdrop, hero image, title, date, category, the full rich-text
body, and links. Closes on Escape and on the backdrop. The page behind is locked
from scrolling while it is open.

**Filter** — category chips above the list, "all" plus one per category. The
categories are whatever the school has invented.

**Pager** — previous/next with `n / m`, shown only when there is more than one
page.

### Daily schedule and menu

One component, `StandingDocPage`, rendered twice. The only difference is a
column: the schedule shows a hand-typed time against each row, the menu does not.

Five day cards, Monday to Friday, **always all five** even when a day is empty. A
week with Wednesday missing reads as a fault; an empty Wednesday reads as a quiet
day.

One document per class for the whole year — no weeks, no months, no versions,
which the school was explicit about. A class that has not written one shows "not
filled in yet", which is deliberately different from an empty week.

Currently the plainest screens in the console; they were built as working layouts
to get the data visible, and are the ones most expected to change.

### Child description

A list of cards, newest first. Each has a title, the subject it was written for,
a date, a rich-text body, and optional links.

Written per subject, about one named child. Nothing here is aggregated — a parent
reads them as separate notes, most recent first.

### Absence

The journal page (`pages/journal`) rendering the monthly register, with
`AbsenceBarsChart` beneath it.

This is the one screen with a **hard visual rule from the brief**: a bar per
month, **green under the permitted number of missed hours, red over it**. The
allowance is set per month, so the comparison is per bar — a child can be inside
September's ceiling and past October's, and the colours must be able to say so.

A dashed reference line is drawn only when every month shares the same allowance;
one line at one month's ceiling would misread as the rule for all of them.

A month with no allowance set gets neither colour — it stays neutral rather than
guessing.

---

## Constraints worth keeping

* **The header is on every screen but login.**
* **Nothing is editable.** The parent console is read-only apart from one
  invisible write: marking homework opened.
* **Everything shown is the published snapshot**, never a teacher's working copy.
  There is no "draft" state to represent.
* **Dates are ISO on the wire, `DD.MM.YYYY` on screen.** No `Date` is constructed
  from a server string anywhere in the parent console, deliberately — it is what
  keeps the calendar free of a timezone off-by-one.
* **Georgian throughout**, and it is not narrow: allow for labels wider than the
  English equivalent, and there is no uppercase form to fall back on.
* **It has to work on a phone.** A parent opens this on the way home, not at a
  desk, so the phone is the primary target and the desktop is the wide case -
  not the other way round. Nothing may scroll sideways, every tap target has to
  be reachable with a thumb, and the pieces that are laid out horizontally today
  each need a stacked form:

  | Piece | On a narrow screen |
    |---|---|
  | Landing grid of boxes | one or two columns, not a row |
  | News card (image left, text right) | image above, text below |
  | Month calendar | seven columns is the floor - it shrinks, it does not reflow |
  | Weekday cards (schedule, menu) | five across becomes five down |
  | Absence register (students down, days across) | scrolls inside its own box, with the name column pinned |
  | News dialog | full screen rather than a centred panel |
  | Header | the module name may not push the logout off the edge |

  The register and the calendar are the two that cannot simply stack: a grid is
  a grid. They scroll inside their own container instead, which is the rule the
  rich-text block already follows for tables.

  **This is not deferred to the primary theme.** The current basic/secondary
  look needs it too, so it belongs in the shared components rather than in the
  playful skin on top of them.

---

## Open

The primary theme itself. Everything above is the current, shared look; what
"more playful" means concretely is the next conversation, one screen at a time.

Responsiveness is **not** on this list. It is a requirement of the parent side
as it stands, not something the theme brings with it.
