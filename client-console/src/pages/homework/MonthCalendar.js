import React, {useState} from "react";

const WEEKDAYS = ["ორშ", "სამ", "ოთხ", "ხუთ", "პარ", "შაბ", "კვი"];

const MONTHS = ["იანვარი", "თებერვალი", "მარტი", "აპრილი", "მაისი", "ივნისი",
    "ივლისი", "აგვისტო", "სექტემბერი", "ოქტომბერი", "ნოემბერი", "დეკემბერი"];

/**
 * One month, Monday first.
 *
 * A cell says three things, on three properties that cannot be confused with
 * each other:
 *
 *     fill    dark when something on that day is unread, light when it is all
 *             read, nothing at all when the school set nothing
 *     border  the day being read below
 *     —       there is no separate mark for today, because the page opens with
 *             today already selected, so the border is already on it
 *
 * Lightness of one colour rather than two colours: two hues read as two
 * categories, two weights read as more and less, which is what this is. It also
 * survives a phone in sunlight and the commoner kinds of colour blindness, where
 * hue does not. And it is deliberately not the green-to-red of the absence
 * chart, which in this console means over or under a limit — homework is
 * neither good nor bad.
 *
 * **A partly-read day is dark.** "Is there anything new here" is what a parent
 * scans a month for; how many is answered one tap below, where each subject
 * carries its own count.
 */
const MonthCalendar = ({month, byDate, selected, loading, bounds, onMonth, onSelect}) => {

    const [picking, setPicking] = useState(false);

    const [year, monthNo] = month.split("-").map(Number);
    const first = new Date(Date.UTC(year, monthNo - 1, 1));
    const daysInMonth = new Date(Date.UTC(year, monthNo, 0)).getUTCDate();

    // getUTCDay is Sunday-based; the school's week starts on Monday.
    const leading = (first.getUTCDay() + 6) % 7;

    const cells = [];
    for (let i = 0; i < leading; i++) {
        cells.push(null);
    }
    for (let d = 1; d <= daysInMonth; d++) {
        cells.push(`${month}-${String(d).padStart(2, "0")}`);
    }

    const monthOf = (delta) => {
        const next = new Date(Date.UTC(year, monthNo - 1 + delta, 1));
        return `${next.getUTCFullYear()}-${String(next.getUTCMonth() + 1).padStart(2, "0")}`;
    };

    // Inside the academic year, or anywhere when the year has no dates. Stepping
    // past either end reaches months that cannot hold homework, which is a dead
    // end that looks like navigation.
    const within = (m) => {
        if (!bounds || !bounds.from || !bounds.to) {
            return true;
        }
        return m >= bounds.from.slice(0, 7) && m <= bounds.to.slice(0, 7);
    };

    const months = monthChoices(bounds, month);

    return (
        <div className="hw__calendar">
            <div className="hw__monthBar">
                <button type="button" className="hw__nav" aria-label="წინა თვე"
                        disabled={!within(monthOf(-1))}
                        onClick={() => onMonth(monthOf(-1))}>‹
                </button>

                {/* The month name is the picker. A dropdown of its own would be
                    more chrome on a screen whose whole job is "what is new",
                    and this is what every calendar already does. */}
                <button type="button" className="hw__monthName"
                        onClick={() => setPicking(p => !p)}>
                    {MONTHS[monthNo - 1]} {year} <Caret/>
                </button>

                <button type="button" className="hw__nav" aria-label="შემდეგი თვე"
                        disabled={!within(monthOf(1))}
                        onClick={() => onMonth(monthOf(1))}>›
                </button>
            </div>

            {picking ? (
                <div className="hw__picker">
                    {months.map(m => (
                        <button type="button" key={m}
                                className={`hw__pick${m === month ? " hw__pick--on" : ""}`}
                                onClick={() => {
                                    setPicking(false);
                                    onMonth(m);
                                }}>
                            {MONTHS[Number(m.slice(5, 7)) - 1]} {m.slice(0, 4)}
                        </button>
                    ))}
                </div>
            ) : null}

            <div className="hw__grid">
                {WEEKDAYS.map(name => (
                    <div key={name} className="hw__weekday">{name}</div>
                ))}

                {cells.map((date, index) => {
                    if (!date) {
                        return <div key={`pad-${index}`} className="hw__day hw__day--pad"/>;
                    }
                    const entry = byDate.get(date);
                    const unseen = entry ? entry.unseen : 0;

                    return (
                        <button
                            type="button"
                            key={date}
                            // Never disabled. A parent tapping a quiet day should
                            // be told nothing was set, not met with a dead cell.
                            className={[
                                "hw__day",
                                entry ? (unseen > 0 ? "hw__day--unread" : "hw__day--read") : "",
                                date === selected ? "hw__day--selected" : ""
                            ].filter(Boolean).join(" ")}
                            onClick={() => onSelect(date)}
                        >
                            {Number(date.slice(8, 10))}
                        </button>
                    );
                })}
            </div>

            {loading ? <div className="hw__loading">…</div> : null}
        </div>
    );
};

/**
 * Every month of the academic year, for the picker.
 *
 * Falls back to a year either side of the month on screen when the year has no
 * dates, so the control still works rather than showing one option.
 */
const monthChoices = (bounds, current) => {
    const out = [];
    const step = (from, count) => {
        const [y, m] = from.split("-").map(Number);
        for (let i = 0; i < count; i++) {
            const d = new Date(Date.UTC(y, m - 1 + i, 1));
            out.push(`${d.getUTCFullYear()}-${String(d.getUTCMonth() + 1).padStart(2, "0")}`);
        }
    };

    if (bounds && bounds.from && bounds.to) {
        const from = bounds.from.slice(0, 7);
        const to = bounds.to.slice(0, 7);
        const [fy, fm] = from.split("-").map(Number);
        const [ty, tm] = to.split("-").map(Number);
        step(from, (ty - fy) * 12 + (tm - fm) + 1);
        return out;
    }

    const [cy, cm] = current.split("-").map(Number);
    const start = new Date(Date.UTC(cy, cm - 7, 1));
    step(`${start.getUTCFullYear()}-${String(start.getUTCMonth() + 1).padStart(2, "0")}`, 13);
    return out;
};

/* Drawn rather than a glyph: the arrow characters are not in every Georgian
   font and fall back to a dash, which reads as a hyphen after the year. */
const Caret = () => (
    <svg className="hw__caret" width="10" height="10" viewBox="0 0 24 24"
         fill="none" stroke="currentColor" strokeWidth="3" aria-hidden="true">
        <polyline points="6 9 12 16 18 9"/>
    </svg>
);

export default MonthCalendar;
