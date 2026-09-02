import React from "react";

const WEEKDAYS = ["ორშ", "სამ", "ოთხ", "ხუთ", "პარ", "შაბ", "კვი"];

const MONTHS = ["იანვარი", "თებერვალი", "მარტი", "აპრილი", "მაისი", "ივნისი",
    "ივლისი", "აგვისტო", "სექტემბერი", "ოქტომბერი", "ნოემბერი", "დეკემბერი"];

/**
 * One month, Monday first.
 *
 * Built from the month string alone — the server sends only the days that hold
 * something, because it has no reason to send thirty empty ones.
 *
 * A day carries up to three marks at once, which is why they are separate
 * classes rather than one state: a dot for work, a count for what is unread, and
 * the selected outline. A day can be all three.
 */
const MonthCalendar = ({month, byDate, selected, loading, onMonth, onSelect}) => {

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

    const shift = (delta) => {
        const next = new Date(Date.UTC(year, monthNo - 1 + delta, 1));
        onMonth(`${next.getUTCFullYear()}-${String(next.getUTCMonth() + 1).padStart(2, "0")}`);
    };

    return (
        <div className="hw__calendar">
            <div className="hw__monthBar">
                <button type="button" className="hw__nav" onClick={() => shift(-1)}>‹</button>
                <div className="hw__monthName">
                    {MONTHS[monthNo - 1]} {year}
                </div>
                <button type="button" className="hw__nav" onClick={() => shift(1)}>›</button>
            </div>

            <div className="hw__grid">
                {WEEKDAYS.map(name => (
                    <div key={name} className="hw__weekday">{name}</div>
                ))}

                {cells.map((date, index) => {
                    if (!date) {
                        return <div key={`pad-${index}`} className="hw__day hw__day--empty"/>;
                    }
                    const entry = byDate.get(date);
                    const has = Boolean(entry);
                    const unseen = entry ? entry.unseen : 0;

                    return (
                        <button
                            type="button"
                            key={date}
                            // Not disabled when empty: a parent tapping a quiet
                            // day should get the same "nothing set" answer as
                            // one who reads the calendar correctly.
                            className={[
                                "hw__day",
                                has ? "hw__day--has" : "",
                                unseen > 0 ? "hw__day--unseen" : "",
                                date === selected ? "hw__day--selected" : ""
                            ].filter(Boolean).join(" ")}
                            onClick={() => onSelect(date)}
                        >
                            <span className="hw__dayNumber">{Number(date.slice(8, 10))}</span>
                            {unseen > 0 ? <span className="hw__badge">{unseen}</span> : null}
                            {has && unseen === 0 ? <span className="hw__dot"/> : null}
                        </button>
                    );
                })}
            </div>

            {loading ? <div className="hw__loading">…</div> : null}
        </div>
    );
};

export default MonthCalendar;
