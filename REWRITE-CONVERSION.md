# Phase 7 — the conversion formula

How a stored mark becomes the number the government is shown.

---

## 1. Why it exists

IB Mtiebi grades **German-style, out of 7**, but is **legally required to report to the
government out of 10**. So the converted view is a compliance output, not a convenience —
and it is the *only* reason the conversion exists.

They are now moving to a **9-point scale**, and the 9-to-10 mapping is not decided. That
is what makes it configuration rather than a constant.

## 2. What is there today

`ExcelExportController` at lines 626 and 675, duplicated:

```java
return isDecimalSystem ? String.valueOf(value.add(new BigDecimal(3)).longValue())
                       : String.valueOf(value.longValue());
```

`+3`, truncated, behind a checkbox on three of the four Excel exports. Two things about it
are worth carrying forward as warnings rather than as design:

**It truncates.** 6.5 + 3 = 9.5 prints as 9.

**It is inconsistent between exports.** The monthly one (`adjustGradeValue:597`) excludes
rating, behaviour and absence by name. The semester and annual ones pull those same rows
out by name and then add 3 to them anyway — so today, ticking that box **adds 3 to a
student's missed hours**.

Phase 4's `component.output_offset` was an intermediate attempt at the same thing, per
column. Both are replaced here and dropped.

## 3. The shape

**One formula, for the whole school.** `multiplier` and `offset`; the current conversion is
`× 1, + 3`, and a proportional 9-to-10 would be `× 1.1111`.

It is **not** attached to journals or columns. A first cut did that, and it was machinery
the school had not asked for: there is one formula, and a journal that should not be
converted is simply never viewed with the toggle on.

### Where it applies — and nowhere else

| Place        | When                                 |
|--------------|--------------------------------------|
| The grid     | The toolbar toggle is on             |
| Excel export | The box in the export menu is ticked |

**Not** the parent portal. A parent reads the mark the school actually gave, on the scale
the school actually grades on; the 10-point scale is for the ministry.

**Nothing is ever stored converted**, nothing recomputes through it, and no grade carries
any meaning from it. Editing the formula therefore cannot corrupt anything and is never a
migration — the next render simply reads differently.

### Output is not rounded

Settled with the school: whatever the formula produces is what is displayed. `6.5` through
`+3` shows as **9.5**. Rounding is the engine's job and it already did it once, when it
calculated the grade. Trailing zeros are stripped, so `7 → 10.0000` prints as `10`.

### What is never converted

`null` and special values (`ჩთ`) pass through untouched. A formula maps numbers; "not
attested" is not a number, and multiplying it is how it would silently become a mark.

### Editing is off while conversion is showing

Entry is always on the real scale. Two things enforce that beyond the UI disabling itself:
`convertedValue` is a separate field the write path never reads, and the column's
`scaleMin`/`scaleMax` check rejects an out-of-range value server-side.

## 4. Data model

```
conversion_formula   id, name, multiplier, offset_value, + audit    -- one row
component            - output_offset                                -- dropped
```

## 5. The known limit

`multiplier` and `offset` describe a **straight line**, which covers every scale-to-scale
conversion that is proportional — including any mapping fixed by two points.

If the 9-to-10 mapping the school settles on turns out to be a **lookup table** (say 9→10,
8→9, 7→7, 6→5) rather than a line, this cannot express it, and it would become a table of
ranges instead. That is a contained change — one entity and one branch in
`GradeConversionService` — but it is worth knowing before they commit to a mapping, so
**ask for the actual table once they have decided it.**

## 6. Tests

* The current 7→10 conversion, and a proportional 9→10.
* Unrounded output, and trailing zeros stripped rather than exponentiated.
* Null multiplier and offset reading as identity.
* Null in, null out; and no formula meaning "print as stored" rather than "print a blank".
* The export printing stored values unless the flag is set, with storage untouched after.
* `ჩთ` never converted.
* Nothing converting when no formula is configured.
