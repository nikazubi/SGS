# Manual test plan

Every page, every CRUD operation, every filter. Written to be executed by
somebody with no memory of building it, so it states the setup, the fixture and
the expected result of each check rather than assuming any of them.

**How to use it:** work down a section, and for each numbered check record
`PASS`, `FAIL — what happened`, or `SKIP — why`. A check that cannot be
performed (control missing, page will not open) is a **FAIL**, not a skip.

Section 9 lists things that are deliberately unfinished. **Do not report those
as bugs.**

---

## 1. Setup

```powershell
.\db\demo\reset.ps1                       # ~30s, clean slate every time
```

```bash
export JAVA_HOME="/c/Program Files/Java/jdk-11.0.10"
./mvnw -B -DskipTests clean install       # always with clean; stop any running app first
java -jar core/target/core-0.0.1-SNAPSHOT.war
```

```powershell
cd admin-console\src\front-ac
$env:NODE_OPTIONS = "--openssl-legacy-provider"   # required on Node 20; not on 16
yarn start                                        # first compile ~6 min, port stays silent meanwhile
```

The parent console is the same, from `client-console`, on another port.

Both consoles proxy to `localhost:8080`. **Clear site data for the console's
origin before starting** — filters, the open-tab list and the auth token are all
in `localStorage`, and stale ones from an earlier database look like bugs.

### Logins

| Console | Username     | Password     |
|---------|--------------|--------------|
| Staff   | `admin`      | `admin`      |
| Parent  | `beridze`    | `nino2025`   |
| Parent  | `maisuradze` | `giorgi2025` |
| Parent  | `beridze`    | `mariam2025` |
| Parent  | `chkheidze`  | `luka2025`   |

The two `beridze` logins are deliberate: sisters share a username with different
passwords. Each must show a **different child**.

---

## 2. The fixture

Reset gives exactly this. Anything else means the reset did not run.

**Year** 2025-26, current. It is in the past, deliberately — daily absence
refuses future dates, so a finished year is the only one where every school day
can be marked.

**Seven reporting periods**, three trimesters, one year:

| Trimester | Reporting periods                   |
|-----------|-------------------------------------|
| I         | სექტემბერი-ოქტომბერი, ნოემბერი      |
| II        | დეკემბერი, იანვარი-თებერვალი, მარტი |
| III       | აპრილი, მაისი                       |

No June. September pairs with October and January with February because the
brief's tables do.

**Two classes**

| Class | School              | Students                      | Subjects |
|-------|---------------------|-------------------------------|----------|
| 3ა    | დაწყებითი (primary) | ნინო ბერიძე, გიორგი მაისურაძე | 5        |
| 8ბ    | საბაზო (basic)      | მარიამ ბერიძე, ლუკა ჩხეიძე    | 5        |

**Three journals**

| Journal                            | Frequency | Grid    | Assigned to  |
|------------------------------------|-----------|---------|--------------|
| ტრიმესტრული შეფასება               | TRIMESTER | columns | both classes |
| გაცდენილი საათები                  | MONTH     | periods | both classes |
| შეფასება ეთიკური ნორმების მიხედვით | MONTH     | periods | 8ბ only      |

**Empty on purpose:** no marks, no absences, no homework, no news. Entering them
is what is being tested.

**Absence allowances** are set for all 7 periods of both classes, with მაისი
lower than the rest.

---

## 3. Staff console — the journal tabs

### 3.1 ტრიმესტრული შეფასება (columns across, one period)

1. Open the tab. **Expect** class, subject and period pickers, and an empty grid
   until all are chosen.
2. Choose 8ბ, any subject, I ტრიმესტრი. **Expect** two student rows and these
   columns: I–VII under a "მიმდინარე შეფასება" group, მიმდინარე საშუალო,
   საწყისი ცოდნის…, პროგრეს ტესტი, ფინალური ტესტი, ტრიმესტრის შეფასება.
3. Type `8` into I for the first student and blur. **Expect** it saves without a
   page reload, and მიმდინარე საშუალო becomes 8.
4. Type `9` into II. **Expect** the average becomes 8.50.
5. Enter საწყისი 7 and ფინალური 10. **Expect** ტრიმესტრის შეფასება computes
   (0.5·avg + 0.2·initial + 0.3·final, rounded to a whole number).
6. Switch the period picker to წელი. **Expect** a *different* column set —
   წლიური, რეიტინგი, ფინალური გამოცდა, საბოლოო, აკადემიური პროექტი — and that
   წლიური has a value derived from the trimester you filled.
7. Switch back to I ტრიმესტრი. **Expect** your marks are still there.
8. Try to type into a calculated column (მიმდინარე საშუალო). **Expect** it is
   refused or accepted only as an explicit override, never silently discarded.
9. Change the subject picker. **Expect** the grid empties — marks are per
   subject — and returns when you switch back.
10. Change the class to 3ა. **Expect** its own two students, no marks.
11. Reload the page. **Expect** the same class/subject/period still selected
    (filters persist) and the marks still shown.

### 3.2 გაცდენილი საათები (periods across)

12. Open the tab, choose 8ბ and წელი. **Expect exactly eleven columns in this
    order**: სექტემბერი-ოქტომბერი, ნოემბერი, **I ტრიმესტრი**, დეკემბერი,
    იანვარი-თებერვალი, მარტი, **II ტრიმესტრი**, აპრილი, მაისი,
    **III ტრიმესტრი**, **წელი**.
13. **Expect** the three trimester columns and წელი are visibly set apart
    (shaded, bolder) and **cannot be typed into**.
14. Enter 12 in სექტემბერი-ოქტომბერი and 8 in ნოემბერი for one student.
    **Expect I ტრიმესტრი shows 20 and წელი shows 20** — hours are summed.
15. Change the 12 to 4. **Expect** I ტრიმესტრი becomes 12 without a reload.
16. **Expect** two extra rows above the students for აკადემიური საათები and
    დასაშვები გაცდენა, with an input on each reporting period and **no input**
    on the trimester or year columns.
17. Change დასაშვები გაცდენა for ნოემბერი. **Expect** it saves and survives a
    reload.
18. Publish the journal for a period. **Expect** the publish action reports what
    it released, and the log records it (check 8.2).

### 3.3 შეფასება ეთიკური ნორმების მიხედვით

19. Open the tab. **Expect** it offers **8ბ only** — it is not assigned to 3ა.
20. Choose 8ბ and წელი. **Expect** the same eleven-column shape as 3.2.
21. Enter 9 in სექტემბერი-ოქტომბერი and 6 in ნოემბერი.
    **Expect I ტრიმესტრი shows 7.50** — marks are averaged, not summed. This is
    the check that distinguishes the two journals; if it shows 15, the wrong
    rule is configured.
22. **Expect** წელი is the average of the reporting periods entered so far.

---

## 4. Staff console — journals and structure

### 4.1 ჟურნალები (the journal index)

23. Open. **Expect** three journals listed with frequency, grid mode, column
    count, parent visibility and version.
24. Create a journal with the wizard. **Expect** it appears in the list **and as
    a new tab in the left menu** without a reload.
25. Edit its columns: add an input column, and a derived one with a formula.
    **Expect** the formula editor refuses a cycle and refuses a column that
    totals its own children.
26. Activate it, then open its tab. **Expect** the grid draws the columns you
    defined.
27. Archive it. **Expect** it leaves the menu and the default list, and can be
    restored.
28. Rename an existing journal. **Expect** the menu label changes and the tab
    still works — tabs are keyed by uuid, not by name.
29. Open ბეჭდვის შკალა (the conversion formula). **Expect** it loads, saves and
    survives a reload.

---

## 5. Staff console — roster

These write `sgs`, which is what every other screen reads. A student created
here **must** appear in the gradebook.

### 5.1 მოსწავლეები

30. Open. **Expect** four students, a year picker defaulting to 2025-26, a class
    filter, a search box and a "გამორთულებიც" switch.
31. Filter by class 3ა. **Expect** two students.
32. Search `ბერიძე`. **Expect** both sisters, in different classes.
33. Search a personal number. **Expect** the matching student only.
34. **Create** a student: press ახალი მოსწავლე. **Expect the username and
    password fields are empty** — not pre-filled with `admin`. If they are
    filled, that is a FAIL (browser autofill; there is code specifically to
    prevent it).
35. Fill name, surname, personal number, username, password, choose class 3ა,
    save. **Expect** the list shows them in 3ა.
36. **Verify it reached the new model:** open ტრიმესტრული შეფასება for 3ა.
    **Expect the new student has a row.** This is the whole point of the roster
    rewrite; if they are missing, the write went to the wrong tables.
37. **Duplicate personal number:** create another student with the same personal
    number. **Expect a refusal naming the personal number.**
38. **Duplicate login pair:** create a student with the same username *and*
    password as an existing one. **Expect a refusal that says the pair must
    differ**, not "username taken".
39. **Same username, different password.** **Expect it is accepted** — siblings
    share usernames by design.
40. **Edit** a student without touching the password. **Expect** it saves and
    the old password still works on the parent console.
41. **Move:** press the move action, choose another class and a date inside the
    year. **Expect** it succeeds and the class chip changes.
42. Open the history action. **Expect two rows** — the old class ending the day
    *before* the move, the new one starting on the move date and open.
43. **Verify marks survived the move:** any marks entered for that student
    before the move must still be there.
44. **Move backwards:** try a move dated before the current placement started.
    **Expect a refusal.**
45. **Leave:** press the leave action with a date. **Expect** the student shows
    as having left and drops out of class lists, but their record and marks
    remain.
46. Try to move a student who has left. **Expect a refusal.**
47. Switch the year picker to another year if one exists. **Expect** students
    show as not enrolled that year rather than disappearing.

### 5.2 კლასები

48. Open. **Expect** both classes with school, level and a student count.
49. **Create** a class in 3ა's school with a new name. **Expect** it appears and
    can be chosen in the students screen's class filter.
50. **Duplicate name** in the same school and year. **Expect a refusal.**
51. **Delete** the empty class you created. **Expect** it goes.
52. **Delete a populated class** (3ა). **Expect a refusal naming the student
    count** — deleting it would orphan every mark hanging off those enrollments.
53. Open the subjects action on 8ბ. **Expect** its five subjects in teaching
    order, each with a teacher name.
54. **Add** a subject. **Expect** it appears at the end of the order.
55. **Reorder** with the arrows. **Expect** the new order survives a reload.
56. **Edit a teacher name** and blur. **Expect** it saves; check it shows on the
    gradebook's subject header.
57. **Remove** a subject from the class. **Expect** it goes from the list, and
    any marks already entered for it are *not* destroyed.
58. **Add the same subject twice.** **Expect a refusal.**

### 5.3 საგნები

59. Open. **Expect** six subjects with a count of how many classes take each.
60. **Create** a subject. **Expect** it appears and can be added to a class.
61. **Duplicate name.** **Expect a refusal.**
62. **Delete** the unused subject you created. **Expect** it goes.
63. **Delete a subject a class takes.** **Expect a refusal** — the legacy page
    deleted it and left the class pointing at nothing.
64. Toggle "გამორთულებიც". **Expect** inactive subjects appear.

---

## 6. Staff console — content

### 6.1 საშინაო დავალებები

65. Open. **Expect** class and date filters and a list.
66. **Create** homework for 8ბ on a date inside the year: title, rich text, a
    link, a subject. **Expect** it saves as a draft.
67. **Expect a draft is not visible to parents** (verify in 7.3).
68. **Publish** it. **Expect** the list marks it published.
69. **Edit** the published item. **Expect** the edit is allowed and reaches
    parents at the next publish.
70. **Target** homework at particular students rather than the whole class.
    **Expect** only those students' parents see it.
71. **Archive** it. **Expect** it leaves the parent side.
72. Paste an image and a table into the rich text. **Expect** they are kept, and
    that the parent side does not let them break the layout.

### 6.2 დღის რეჟიმი and კვება

73. Open დღის რეჟიმი, choose 8ბ. **Expect** an editor for five weekdays.
74. Add rows with times for two days, leave others empty, save, publish.
75. **Expect** the parent side shows **all five days**, with the empty ones
    reading as empty rather than missing.
76. Repeat for კვება. **Expect** the same editor **without the time column**.
77. **Expect** one document per class for the year — no week or month picker.

### 6.3 მოსწავლის დახასიათება

78. Create a characterization for one named student, with a subject and date.
79. Publish it. **Expect** only that student's parent sees it (7.5).
80. Create one **without naming a student**. **Expect** it reaches nobody — this
    is deliberately unlike homework, where no target means the whole class.

### 6.4 სიახლეები

81. Open. **Expect** a list and a category filter.
82. **Create** a news item with a title, body, an image and a category. Publish.
83. **Expect** the parent side shows it newest-first with the image, an excerpt
    and a working "ვრცლად".
84. **Create a category**, assign it, and check the parent side's filter chips.
85. **Edit** and **archive** an item. **Expect** archiving removes it from the
    parent side.
86. Upload an image over 2 MB. **Expect** a clear message about size, not a
    stack trace.

---

## 7. Parent console

Log in as `beridze` / `nino2025` (ნინო, 3ა, **primary**) unless told otherwise.

87. **Login:** wrong password. **Expect** a refusal, and **the page must not
    reload in a loop**.
88. Correct password. **Expect** the landing page.
89. **Expect the landing page shows six boxes for a primary child**: homework,
    news, schedule, menu, description, and the absence register — **and no
    gradebook**.
90. Log in as `chkheidze` / `luka2025` (8ბ, **basic**). **Expect** the same
    modules **plus** the grade journals.
91. Log in as `beridze` / `mariam2025`. **Expect მარიამ's** data, not ნინო's.
    Same username, different child.
92. **Homework:** the month calendar marks days that hold work, days with unread
    work, and the selected day as three separate things. Click a day with work:
    subjects appear as accordions below.
93. Open a day. **Expect** the unread badge clears a couple of seconds later,
    not instantly.
94. Click a day with nothing. **Expect** "nothing set", not a dead cell.
95. **News:** newest first, ten per page, image left, date top-right, category
    chip, excerpt as plain text. The dialog opens, closes on Escape and on the
    backdrop.
96. **Absence:** the register and a bar chart. Enter hours on the staff side
    above a period's permitted figure and **expect that bar to be red** and
    others green.
97. **Expect no dashed reference line** while the periods have different
    allowances (მაისი is lower in the fixture).
98. **Schedule and menu:** five weekday cards, empty ones still present.
99. **Description:** the characterization from 6.3, newest first, with subject
    and date.
100. **Expect every screen distinguishes loading, empty and failed.** Stop the
     backend and reload: it must say something failed, not "no homework".

---

## 8. Cross-cutting flows

### 8.1 Publication reaches parents

101. Enter marks in a trimester journal, do **not** publish. **Expect** the
     parent sees nothing.
102. Publish. **Expect** the parent sees exactly those marks.
103. Change a mark after publishing. **Expect** the parent still sees the
     **published** value until the next publish.

### 8.2 Change requests

104. As a user with `ADD_GRADES` but not `MANAGE_GRADES`, edit a published cell.
     **Expect** it opens a change request rather than saving.
105. **Expect** one open request per cell — a second attempt does not create a
     duplicate.
106. Open ცვლილების მოთხოვნები. **Expect** the request, with the old and new
     value and the reason.
107. **Reject** it. **Expect** nothing changes and it cannot be decided twice.
108. Raise another and **approve** it. **Expect** the value changes, everything
     derived from it recomputes, and the parent sees it after republication.
109. Open გამოქვეყნების ისტორია. **Expect** each publication and decision
     recorded.

### 8.3 Daily absence

110. Open გაცდენები (დღიური), choose 3ა and a reporting period. **Expect**
     columns for the weekdays in that period and no weekend columns.
111. Mark a student absent on a past weekday. **Expect** it saves and the total
     for that student increases.
112. Unmark them. **Expect** it clears.
113. Try a **future** date. **Expect** a refusal.
114. **Expect** a paired period (სექტემბერი-ოქტომბერი) shows both months'
     weekdays — that is the consequence of the school reporting them together.

---

## 9. Permissions

115. Create a permission group with only `VIEW_STUDENT`, and a user in it.
116. Log in as that user. **Expect** the students page opens **read-only** — no
     create, edit, move or leave controls.
117. **Expect** pages the group has no permission for are absent from the menu,
     not present-and-broken.
118. Give a user a class in their academy-class list. **Expect** they see only
     that class in every class picker, and are refused on others.
119. **Expect a user whose granted class no longer exists sees nothing**, rather
     than everything — the scope check fails closed.

---

## 10. Known gaps — do not report these as bugs

* **The parent console is not responsive.** It is desktop-only for now.
  `PARENT-COMPONENTS.md` has the plan.
* **The primary school has no distinct theme.** Both schools look the same.
* **Legacy pages show nothing.** `მომხმარებლები`, `უფლებათა ჯგუფები` and
  `პერიოდის დახურვა` read the old `dbo` tables, which the demo database leaves
  empty apart from the `admin` login. Creating records there works.
* **`პერიოდის დახურვა` does nothing to the new journals.** Freezing is done by
  publication now; the page survives until cutover for the legacy parent API.
* **The brief's trimester table has an unlabelled 14th column.** Seeded as
  მიმდინარე საშუალო. Unconfirmed — `CLIENT-QUESTIONS.md`.
* **The ethics journal exists only in the demo database.** Its shape is
  unconfirmed — `CLIENT-QUESTIONS.md` §3.
* **No teacher-account picker.** The class subject screen takes a teacher
  *name*; only 3 of 98 teachers have logins.
* **Emails go to `@example.invalid`.** Absence notices will not arrive. Change a
  guardian address in the database to test the mail path.
* **Rollover creates no enrollments.** By design — the school places children
  itself.

---

## 11. Recording results

| §                | Checks  | Pass | Fail | Notes |
|------------------|---------|------|------|-------|
| 3 Journals       | 1–22    |      |      |       |
| 4 Journal editor | 23–29   |      |      |       |
| 5 Roster         | 30–64   |      |      |       |
| 6 Content        | 65–86   |      |      |       |
| 7 Parent         | 87–100  |      |      |       |
| 8 Flows          | 101–114 |      |      |       |
| 9 Permissions    | 115–119 |      |      |       |

For each FAIL record: the check number, what you did, what happened, and what
you expected. A screenshot helps for anything visual.
