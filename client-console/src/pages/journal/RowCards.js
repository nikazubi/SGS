import React from "react";

/**
 * A single row, drawn as cards.
 *
 * Used when there is exactly one thing to show — one subject's marks for a
 * trimester, one month's ethics criteria. A table with one row is an awkward
 * way to say that, which is the whole reason this exists.
 *
 * Cards are grouped by the journal's own column grouping, so the seven ongoing
 * marks sit under their heading exactly as the teacher entered them.
 */
const RowCards = ({view, row}) => {

    const groups = [];
    view.columns.forEach(column => {
        const label = column.groupLabel || "";
        const last = groups[groups.length - 1];
        if (last && last.label === label) {
            last.columns.push(column);
        } else {
            groups.push({label, columns: [column]});
        }
    });

    return (
        <div className="cardsWrap">
            <div className="cardsWrap__title">{row.label}</div>

            {groups.map((group, i) => (
                <div key={i} className="cardsGroup">
                    {group.label ? (
                        <div className="cardsGroup__title">{group.label}</div>
                    ) : null}

                    <div className="cardsGroup__row">
                        {group.columns.map(column => {
                            const value = row.values[column.code];
                            return (
                                <div key={column.code}
                                     className={value ? "DisciplineBox" : "DisciplineBox empty"}>
                                    <div className="DisciplineBox__title">{column.label}</div>
                                    {/* Empty columns are shown rather than
                                        hidden: a trimester in progress should
                                        tell a parent what is still to come. */}
                                    <div className="DisciplineBox__value">
                                        {value || "—"}
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                </div>
            ))}
        </div>
    );
};

export default RowCards;
