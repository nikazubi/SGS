import React from "react";

/**
 * Several rows, drawn as a table — or as a list when the table would be two
 * columns wide.
 *
 * Rows are whatever the journal says they are: subjects for a per-subject
 * journal, its own periods for a class-wide one. Nothing is named here.
 *
 * **A grid cannot reflow, so it scrolls inside its own box** with the first
 * column pinned — the rule `PARENT-COMPONENTS.md` sets for the pieces that
 * cannot stack, and the same one the rich-text block already follows for
 * pasted tables. The alternative is a page that scrolls sideways, which is the
 * one thing the parent console must not do.
 *
 * One value column is not a grid, though. Subject and mark, or month and mark,
 * is a list, and on a phone it is drawn as one: a table with two columns wastes
 * half a narrow screen on rules and padding.
 */
const RowTable = ({view, onOpenRow}) => {
    const single = view.columns.length === 1;

    return (
        <div className={single ? "rowList" : "rowGrid"}>
            {single ? (
                <ul className="rowList__items">
                    <li className="rowList__head">
                        <span>{view.subjectScoped ? "საგანი" : "პერიოდი"}</span>
                        <span>{view.columns[0].label}</span>
                    </li>
                    {view.rows.map((row, i) => (
                        <li key={i}
                            className={onOpenRow ? "rowList__item clickable" : "rowList__item"}
                            onClick={onOpenRow ? () => onOpenRow(row) : undefined}>
                            <span className="rowList__label">{row.label}</span>
                            <span className="rowList__value">
                                {row.values[view.columns[0].code] || "—"}
                            </span>
                        </li>
                    ))}
                </ul>
            ) : (
                <div className="rowGrid__scroll">
                    <table>
                        <thead>
                        {view.columns.some(c => c.groupLabel) ? (
                            <tr>
                                <th className="rowGrid__corner"/>
                                {groupSpans(view.columns).map((group, i) => (
                                    <th key={i} colSpan={group.span}>{group.label}</th>
                                ))}
                            </tr>
                        ) : null}
                        <tr>
                            <th className="rowGrid__corner">
                                {view.subjectScoped ? "საგანი" : "პერიოდი"}
                            </th>
                            {view.columns.map(c => <th key={c.code}>{c.label}</th>)}
                        </tr>
                        </thead>
                        <tbody>
                        {view.rows.map((row, i) => (
                            <tr key={i}
                                className={onOpenRow ? "clickable" : undefined}
                                onClick={onOpenRow ? () => onOpenRow(row) : undefined}>
                                <td className="rowGrid__label">{row.label}</td>
                                {view.columns.map(c => (
                                    // Blank cells stay blank rather than
                                    // disappearing, so a parent can see what
                                    // has yet to be filled in.
                                    <td key={c.code}>{row.values[c.code] || ""}</td>
                                ))}
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>
            )}
        </div>
    );
};

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
