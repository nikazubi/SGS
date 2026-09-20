import React from "react";
import {useQuery} from "react-query";
import PatternBackground from "../../components/PatternBackground";
import {pattern, weekdays as weekdayColours} from "../../theme/primaryTheme";
import "./standing.css";

const WEEKDAYS = ["ორშაბათი", "სამშაბათი", "ოთხშაბათი", "ხუთშაბათი", "პარასკევი"];

/**
 * The class's weekly schedule or menu.
 *
 * One component for both, because they are the same document with one column
 * turned off — the schedule types a time against each row and the menu does not.
 *
 * Five cards rather than five stacked strips: a week is a shape a parent reads
 * at a glance, and stacking it made Friday a scroll away from Monday. Three
 * across and two below, the lower pair sitting over the gaps in the row above so
 * the block reads as one thing rather than a row and a remainder.
 *
 * **Both pages are primary-only** — `ParentContentService.modulesFor` gives
 * SCHEDULE and MENU to that school alone — so the primary palette is safe here
 * and nobody else's screen changes.
 */
const StandingDocPage = ({queryKey, title, fetcher, withTime}) => {

    const {data: doc, isLoading, isError} = useQuery(
        [queryKey], fetcher, {refetchOnWindowFocus: false});

    return (
        <PatternBackground colors={pattern} className="std">
            <h1 className="std__title">{title}</h1>

            {/* Three states kept apart: a failed request must not read as a week
                the school has not filled in yet. */}
            {isLoading ? (
                <div className="std__state">იტვირთება…</div>
            ) : isError ? (
                <div className="std__state std__state--error">ვერ ჩაიტვირთა.</div>
            ) : !doc ? (
                <div className="std__state">ჯერ არ არის შევსებული.</div>
            ) : (
                <div className="std__week">
                    {doc.days.map((day, i) => {
                        const colour = weekdayColours[i % weekdayColours.length];
                        return (
                            <section key={day.weekday} className="std__day"
                                     style={{background: colour.tile, color: colour.label}}>
                                <h2 className="std__dayName">{WEEKDAYS[day.weekday - 1]}</h2>

                                {day.lines.length === 0 ? (
                                    // A quiet day, not a missing one. All five are
                                    // always drawn - a week with Wednesday absent
                                    // reads as a fault.
                                    <p className="std__none">—</p>
                                ) : (
                                    <ul className="std__lines">
                                        {day.lines.map((line, n) => (
                                            <li key={n} className="std__line"
                                                style={{borderTopColor: colour.accent}}>
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
                        );
                    })}
                </div>
            )}
        </PatternBackground>
    );
};

export default StandingDocPage;
