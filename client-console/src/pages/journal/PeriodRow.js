import React from "react";

/**
 * A register's year, read across: one column per period, the year last.
 *
 * The shape the school has had for years and the one the brief draws — Sept-Oct,
 * Nov, Dec, Jan-Feb, Mar, Apr, May, then the total. Months across rather than
 * down because a year is a thing you scan left to right, and because the figure
 * a parent actually wants — how much has been missed altogether — belongs at the
 * end of the figures it totals rather than at the top of them.
 *
 * Used instead of the list when the rows are periods and there is one value per
 * period, which is what a register is. Nothing configures that: it follows from
 * the journal being class-wide with a single column.
 *
 * The year is the last cell and gets no bar of its own in the chart below — a
 * year bar is several times the tallest month and flattens all of them.
 */
const PeriodRow = ({view}) => {

    const months = view.rows.filter(r => r.periodKind !== "YEAR");
    const year = view.rows.find(r => r.periodKind === "YEAR");
    const code = view.columns[0] && view.columns[0].code;

    // The school's own total where it has published one, otherwise the months
    // added up. A rollup is a SUM over the same periods, so the two agree when
    // both exist; the fallback means the total is there before anyone publishes
    // the year, which is most of the year.
    const published = year && year.values[code];
    const total = published !== "" && published != null
        ? published
        : months.reduce((sum, r) => sum + (Number(r.values[code]) || 0), 0);

    return (
        <div className="periodRow">
            <div className="periodRow__scroll">
                <table>
                    <thead>
                    <tr>
                        {months.map(r => <th key={r.periodId}>{r.label}</th>)}
                        {year ? <th className="periodRow__total">{year.label}</th> : null}
                    </tr>
                    </thead>
                    <tbody>
                    <tr>
                        {months.map(r => (
                            // A period nobody has filled in yet stays blank
                            // rather than reading as nought hours missed.
                            <td key={r.periodId}>{r.values[code] || "—"}</td>
                        ))}
                        {year ? <td className="periodRow__total">{total}</td> : null}
                    </tr>
                    </tbody>
                </table>
            </div>
        </div>
    );
};

export default PeriodRow;
