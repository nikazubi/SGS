import React from "react";
import {useQuery} from "react-query";
import "./standing.css";

const WEEKDAYS = ["ორშაბათი", "სამშაბათი", "ოთხშაბათი", "ხუთშაბათი", "პარასკევი"];

/**
 * The class's weekly schedule or menu.
 *
 * One component for both, because they are the same document with one column
 * turned off — the schedule types a time against each row and the menu does not.
 *
 * **Deliberately plain.** This is a working layout so the data can be seen end
 * to end; the visual pass comes with the primary theme, and this whole file is
 * expected to be restyled then. Nothing here is load-bearing except the shape.
 */
const StandingDocPage = ({queryKey, title, fetcher, withTime}) => {

    const {data: doc, isLoading, isError} = useQuery(
        [queryKey], fetcher, {refetchOnWindowFocus: false});

    if (isLoading) {
        return <div className="ib__center column">
            <div className="pageName">…</div>
        </div>;
    }

    return (
        <div className="ib__center column">
            <div className="pageName">{title}</div>

            {/* Three states kept apart: a failed request must not look like a
                week the school has not filled in. */}
            {isError ? (
                <div className="std__empty">ვერ ჩაიტვირთა.</div>
            ) : !doc ? (
                <div className="std__empty">ჯერ არ არის შევსებული.</div>
            ) : (
                <div className="std__week">
                    {doc.days.map(day => (
                        <section key={day.weekday} className="std__day">
                            <h3 className="std__dayName">{WEEKDAYS[day.weekday - 1]}</h3>

                            {day.lines.length === 0 ? (
                                <div className="std__none">—</div>
                            ) : (
                                <ul className="std__lines">
                                    {day.lines.map((line, i) => (
                                        <li key={i} className="std__line">
                                            {withTime ? (
                                                <span className="std__time">
                                                    {line.timeText || ""}
                                                </span>
                                            ) : null}
                                            <span className="std__text">{line.text}</span>
                                        </li>
                                    ))}
                                </ul>
                            )}
                        </section>
                    ))}
                </div>
            )}
        </div>
    );
};

export default StandingDocPage;
