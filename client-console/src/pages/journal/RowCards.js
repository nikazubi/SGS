import React from "react";

/**
 * A single row, drawn as cards.
 *
 * Used when there is exactly one thing to show — one subject's marks for a
 * trimester, one month's ethics mark. A table with one row is an awkward way to
 * say that, which is the whole reason this exists.
 *
 * Cards are grouped by the journal's own column grouping, so the seven ongoing
 * marks sit under their heading exactly as the teacher entered them. That is
 * the five-block shape the school has had for years, and it is worth keeping:
 * a parent already knows where to look.
 *
 * **A card is sized by what it holds.** A group of several short marks gets
 * small tiles; a group of one gets a wide one with room for its label. The
 * cards used to be a fixed 310 by 230 each, in a row that did not wrap, so
 * seven ongoing marks made the page 2242px wide and every phone scrolled
 * sideways.
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
        <div className="cards">
            {groups.map((group, i) => (
                <section key={i} className="cards__group">
                    {group.label ? (
                        <h3 className="cards__groupTitle">{group.label}</h3>
                    ) : null}

                    {/* A tile is small when its label is. The ongoing marks are
                        numbered I to VII and need almost no width; the tests are
                        named in full sentences and need all of it. Sizing by how
                        many are in the group instead put five long labels into
                        four narrow columns and broke every one of them across
                        three lines. */}
                    <div className={rowClass(group)}>
                        {group.columns.map(column => {
                            const value = row.values[column.code];
                            return (
                                <div key={column.code}
                                     className={value ? "markCard" : "markCard markCard--empty"}>
                                    <div className="markCard__label">{column.label}</div>
                                    {/* An unfilled column is shown rather than
                                        hidden: a trimester in progress should
                                        tell a parent what is still to come. */}
                                    <div className="markCard__value">{value || "—"}</div>
                                </div>
                            );
                        })}
                    </div>
                </section>
            ))}
        </div>
    );
};

/**
 * How a group of marks is laid out.
 *
 * Short labels tile small and wrap. A group of one does not stretch: the ethics
 * screen is a single month's mark, and a lone card told to fill the row is a
 * banner with a number in the middle of it.
 */
const rowClass = (group) => {
    if (group.columns.length === 1) {
        return "cards__row cards__row--single";
    }
    return group.columns.every(c => (c.label || "").length <= 4)
        ? "cards__row cards__row--compact"
        : "cards__row";
};

export default RowCards;
