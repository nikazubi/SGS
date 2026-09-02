import React, {useState} from "react";

/**
 * The chosen day, one accordion section per subject.
 *
 * Opens on the first subject, because a day usually holds one or two and making
 * the parent click twice to read a single assignment is the wrong default.
 *
 * The body is the school's own HTML, sanitised on write against a fixed
 * allowlist (decision 84) rather than here — sanitising on read would leave the
 * unsafe original in the database for whatever renders it next.
 */
const DaySubjects = ({date, day}) => {

    const [open, setOpen] = useState(0);

    if (!day) {
        return <div className="hw__panel hw__loading">…</div>;
    }
    if (day.subjects.length === 0) {
        return (
            <div className="hw__panel hw__empty">
                {formatDate(date)} — დავალება არ არის.
            </div>
        );
    }

    return (
        <div className="hw__panel">
            <div className="hw__panelDate">{formatDate(date)}</div>

            {day.subjects.map((subject, index) => {
                const expanded = open === index;
                const unseen = subject.items.filter(i => !i.seen).length;

                return (
                    <div key={subject.subjectId ?? index}
                         className={`hw__subject${expanded ? " hw__subject--open" : ""}`}>
                        <button
                            type="button"
                            className="hw__subjectHead"
                            onClick={() => setOpen(expanded ? -1 : index)}
                        >
                            <span className="hw__subjectName">{subject.subjectName}</span>
                            {unseen > 0 ? <span className="hw__badge">{unseen}</span> : null}
                            <span className="hw__chevron">{expanded ? "▾" : "▸"}</span>
                        </button>

                        {expanded ? (
                            <div className="hw__items">
                                {subject.items.map(item => (
                                    <article key={item.uuid} className="hw__item">
                                        {item.title ? (
                                            <h4 className="hw__itemTitle">{item.title}</h4>
                                        ) : null}
                                        <div
                                            className="hw__itemBody"
                                            dangerouslySetInnerHTML={{__html: item.bodyHtml}}
                                        />
                                        {item.links.length > 0 ? (
                                            <ul className="hw__links">
                                                {item.links.map((link, i) => (
                                                    <li key={i}>
                                                        {/* noreferrer as well as
                                                            noopener: the target
                                                            is a link the school
                                                            typed, not one we
                                                            control. */}
                                                        <a href={link.url}
                                                           target="_blank"
                                                           rel="noopener noreferrer">
                                                            {link.label || link.url}
                                                        </a>
                                                    </li>
                                                ))}
                                            </ul>
                                        ) : null}
                                    </article>
                                ))}
                            </div>
                        ) : null}
                    </div>
                );
            })}
        </div>
    );
};

/** 2026-03-12 -> 12.03.2026, which is how the school writes a date. */
const formatDate = (iso) => {
    const [y, m, d] = iso.split("-");
    return `${d}.${m}.${y}`;
};

export default DaySubjects;
