import React, {useState} from "react";
import {downloadHomeworkPdf} from "../journal/parentApi";

/**
 * The chosen day, one accordion section per subject.
 *
 * Everything closed, except when there is only one subject — then opening it is
 * the whole content of the page and making the parent click twice to read a
 * single assignment is the wrong default. With several, closed is right: the
 * list of which subjects set work is itself the answer to most visits.
 *
 * The body is the school's own HTML, sanitised on write against a fixed
 * allowlist (decision 84) rather than here — sanitising on read would leave the
 * unsafe original in the database for whatever renders it next.
 */
const DaySubjects = ({date, day}) => {

    const only = day && day.subjects.length === 1;
    const [open, setOpen] = useState(null);

    // Which download is in flight: "day", an assignment's uuid, or null. One
    // key rather than a boolean, so a slow day-sized PDF only disables its own
    // button and not every one on the page.
    const [busy, setBusy] = useState(null);

    const download = async (uuid) => {
        const key = uuid || "day";
        if (busy) return;
        setBusy(key);
        try {
            await downloadHomeworkPdf(date, uuid);
        } catch (e) {
            // The API's own wording where it reached us, which the download
            // helper digs back out of the blob body. It says what happened -
            // "no homework on this day" - where a download failure does not.
            alert(e?.serverMessage || "ფაილის ჩამოტვირთვა ვერ მოხერხდა.");
        } finally {
            setBusy(null);
        }
    };

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
            <div className="hw__panelHead">
                <div className="hw__panelDate">{formatDate(date)}</div>
                <button
                    type="button"
                    className="hw__pdf hw__pdf--day"
                    onClick={() => download(null)}
                    disabled={busy !== null}
                >
                    {busy === "day" ? "…" : "PDF"}
                </button>
            </div>

            {day.subjects.map((subject, index) => {
                // `open` is null until the parent touches one, so a lone
                // subject can start open without that decision sticking to
                // index 0 on a day that has several.
                const expanded = open === null ? only : open === index;
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
                                        <div className="hw__itemHead">
                                            {item.title ? (
                                                <h4 className="hw__itemTitle">{item.title}</h4>
                                            ) : <span/>}
                                            <button
                                                type="button"
                                                className="hw__pdf"
                                                title="ამ დავალების ჩამოტვირთვა"
                                                aria-label="ამ დავალების ჩამოტვირთვა PDF-ად"
                                                onClick={() => download(item.uuid)}
                                                disabled={busy !== null}
                                            >
                                                {busy === item.uuid ? "…" : "PDF"}
                                            </button>
                                        </div>
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
