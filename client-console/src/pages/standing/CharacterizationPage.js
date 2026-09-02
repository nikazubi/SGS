import React from "react";
import {useQuery} from "react-query";
import {fetchCharacterizations} from "../journal/parentApi";
import "./standing.css";

/**
 * What the school has written about this child, newest first.
 *
 * One card per characterization, with the subject it was written for. The body
 * is the school's own HTML, sanitised on write against a fixed allowlist
 * (decision 84) rather than here — sanitising on read would leave the unsafe
 * original in the database for whatever renders it next.
 *
 * **Deliberately plain**, like the schedule and the menu: a working layout so
 * the data can be read end to end, to be restyled with the primary theme.
 */
const CharacterizationPage = () => {

    const {data: items, isLoading, isError} = useQuery(
        ["PARENT_CHARACTERIZATIONS"], fetchCharacterizations,
        {refetchOnWindowFocus: false});

    if (isLoading) {
        return <div className="ib__center column">
            <div className="pageName">…</div>
        </div>;
    }

    return (
        <div className="ib__center column">
            <div className="pageName">მოსწავლის დახასიათება</div>

            {isError ? (
                <div className="std__empty">ვერ ჩაიტვირთა.</div>
            ) : (items || []).length === 0 ? (
                <div className="std__empty">ჯერ არ არის დაწერილი.</div>
            ) : (
                <div className="chr__list">
                    {items.map(item => (
                        <article key={item.uuid} className="chr__item">
                            <div className="chr__head">
                                <h3 className="chr__title">{item.title}</h3>
                                {item.subjectName ? (
                                    <span className="chr__subject">{item.subjectName}</span>
                                ) : null}
                                {item.date ? (
                                    <span className="chr__date">{formatDate(item.date)}</span>
                                ) : null}
                            </div>

                            <div className="chr__body"
                                 dangerouslySetInnerHTML={{__html: item.bodyHtml}}/>

                            {item.links.length > 0 ? (
                                <ul className="chr__links">
                                    {item.links.map((link, i) => (
                                        <li key={i}>
                                            <a href={link.url} target="_blank"
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
            )}
        </div>
    );
};

/** 2026-03-12 -> 12.03.2026 */
const formatDate = (iso) => {
    const [y, m, d] = iso.split("-");
    return `${d}.${m}.${y}`;
};

export default CharacterizationPage;
