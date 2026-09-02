import React from "react";

/**
 * Several rows, drawn as a table.
 *
 * Rows are whatever the journal says they are — subjects for a per-subject
 * journal, its own periods for a class-wide one — and the columns are the
 * journal's columns. Nothing is named here.
 */
const RowTable = ({view, onOpenRow}) => (
    <div className="journalTable">
        <table>
            <thead>
            {view.columns.some(c => c.groupLabel) ? (
                <tr>
                    <th/>
                    {groupSpans(view.columns).map((group, i) => (
                        <th key={i} colSpan={group.span}>{group.label}</th>
                    ))}
                </tr>
            ) : null}
            <tr>
                <th>{view.subjectScoped ? "საგანი" : "პერიოდი"}</th>
                {view.columns.map(c => <th key={c.code}>{c.label}</th>)}
            </tr>
            </thead>
            <tbody>
            {view.rows.map((row, i) => (
                <tr key={i}
                    className={onOpenRow ? "clickable" : undefined}
                    onClick={onOpenRow ? () => onOpenRow(row) : undefined}>
                    <td className="rowLabel">{row.label}</td>
                    {view.columns.map(c => (
                        // Blank cells stay blank rather than disappearing,
                        // so a parent can see what has yet to be filled in.
                        <td key={c.code}>{row.values[c.code] || ""}</td>
                    ))}
                </tr>
            ))}
            </tbody>
        </table>
    </div>
);

/** Consecutive columns sharing a group label are merged into one header cell. */
const groupSpans = (columns) => {
    const groups = [];
    columns.forEach(column => {
        const label = column.groupLabel || "";
        const last = groups[groups.length - 1];
        if (last && last.label === label) {
            last.span += 1;
        } else {
            groups.push({label, span: 1});
        }
    });
    return groups;
};

export default RowTable;
