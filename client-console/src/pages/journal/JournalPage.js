import React, {useEffect, useMemo, useState} from "react";
import {useLocation, useParams} from "react-router-dom";
import {useQuery} from "react-query";
import {fetchParentView} from "./parentApi";
// context/, not hooks/: the two files are identical, but each creates its own
// context, and only this one has a provider above the app.
import {useConvertError} from "../../context/convertError";
import {chartFor} from "./charts";
import RowCards from "./RowCards";
import RowTable from "./RowTable";
import PeriodRow from "./PeriodRow";
import "./journal.css";

/**
 * One journal, as a parent sees it.
 *
 * There is one of these for every journal the school has, including ones that
 * do not exist yet. Nothing here knows what a trimester is, how many marks a
 * subject has, or what the ethics criteria are called — all of it comes from
 * the journal.
 *
 * How it is drawn follows from the data rather than from a setting:
 *
 *     one row    -> cards, because a one-row table is an ugly way to show
 *                   one thing
 *     many rows  -> a table
 *
 * Which is why drilling into a single subject is not a different page. It is
 * this page with one row, and it turns into cards on its own.
 *
 * Loading, empty and failed are three different things on screen. A failure
 * used to read as loading forever, so a journal the server refused - one this
 * child's class is not shown - looked like a slow connection.
 */
const JournalPage = () => {

    const {uuid} = useParams();
    const [periodId, setPeriodId] = useState(null);
    const [subjectId, setSubjectId] = useState(null);

    // Where the landing page's button asked this to open. A starting position,
    // not an identity: the uuid addresses the journal, and everything here is
    // overridden the moment the parent touches a picker.
    const {search} = useLocation();
    const opened = useMemo(() => {
        const q = new URLSearchParams(search);
        return {
            periodKind: q.get("periodKind"),
            oneSubject: q.get("subjects") === "ONE",
            chartKey: q.get("chart"),
            chartColumn: q.get("chartColumn")
        };
    }, [search]);

    // Summary while looking at every subject, and not once drilled into one -
    // the whole point of opening a subject is to see the columns the summary
    // left out.
    const summary = !opened.oneSubject && subjectId === null;

    const {data: view, isLoading, isError, error} = useQuery(
        ["PARENT_VIEW", uuid, periodId, subjectId, opened.periodKind, summary],
        () => fetchParentView({
            uuid, periodId, subjectId,
            periodKind: opened.periodKind, summary
        }),
        {keepPreviousData: true, refetchOnWindowFocus: false, retry: retryUnlessRefused}
    );

    // The server's own sentence where it sent one - "ჟურნალი ხელმისაწვდომი არ
    // არის" says more than a generic failure - without the status code in front.
    const {convert} = useConvertError();
    const failure = isError
        ? (convert(error, false)?.message || "ჟურნალი ვერ ჩაიტვირთა.")
        : null;

    // "One subject at a time" means the first one, with the picker left free -
    // the parent changes it from here and the back link takes them to all of
    // them. Which subject is first is the class's teaching order, so this does
    // not need to name one.
    useEffect(() => {
        if (opened.oneSubject && subjectId === null
            && view?.subjectScoped && view.rows.length > 1) {
            setSubjectId(view.rows[0].subjectId);
        }
    }, [opened.oneSubject, subjectId, view]);

    // The button's chart beats the journal's. Three of the school's buttons are
    // one journal and want three different pictures - a trend for one subject,
    // bars across subjects, and nothing at all on the annual table - so it
    // cannot be a property of the journal alone.
    const Chart = useMemo(
        () => chartFor(opened.chartKey || view?.chartKey), [opened.chartKey, view]);

    if (failure && !view) {
        return <div className="journalPage">
            <div className="emptyState emptyState--error">{failure}</div>
        </div>;
    }

    if (isLoading || !view) {
        return <div className="journalPage">
            <div className="emptyState">იტვირთება…</div>
        </div>;
    }

    const single = view.rows.length === 1;
    const subject = single && view.subjectScoped ? view.rows[0].label : null;

    // A register: rows are periods and each carries one figure. Drawn across,
    // the year last, which is the shape the school has and the brief draws.
    // Nothing configures this - it follows from what the journal is.
    const register = !view.subjectScoped && view.rows.length > 1
        && view.columns.length === 1;

    return (
        <div className="journalPage">
            <h1 className="journalPage__title">{view.title || view.journalName}</h1>

            <div className="journalPage__controls">
                {/* Only when there is a choice. A journal whose rows are its own
                    periods has nothing to pick - they are all on screen. */}
                {view.periods.length > 1 ? (
                    <select
                        aria-label="პერიოდი"
                        value={view.selectedPeriodId || ""}
                        onChange={(e) => setPeriodId(Number(e.target.value))}
                    >
                        {view.periods.map(p =>
                            <option key={p.id} value={p.id}>{p.label}</option>)}
                    </select>
                ) : null}

                {subjectId ? (
                    <button className="backLink" onClick={() => setSubjectId(null)}>
                        ← ყველა საგანი
                    </button>
                ) : null}
            </div>

            {/* The subject heads the marks rather than sitting inside them: on
                one row the cards are the content and the name is the title. */}
            {subject ? <h2 className="journalPage__subject">{subject}</h2> : null}

            {/* A failed period or subject switch keeps the pickers - drawn from
                the last answer - so the parent can step back to one that works,
                but never shows the last answer's marks as if they were this one's. */}
            {failure ? (
                <div className="emptyState emptyState--error">{failure}</div>
            ) : view.rows.length === 0 ? (
                <div className="emptyState">
                    ამ პერიოდისთვის შეფასებები ჯერ არ არის გამოქვეყნებული.
                </div>
            ) : register ? (
                <PeriodRow view={view}/>
            ) : single ? (
                <RowCards view={view} row={view.rows[0]}/>
            ) : (
                <RowTable
                    view={view}
                    // Only a journal whose rows are subjects can be drilled
                    // into; period rows are already the finest grain there is.
                    onOpenRow={view.subjectScoped
                        ? (row) => setSubjectId(row.subjectId)
                        : null}
                />
            )}

            {Chart && !failure ? <Chart view={view} row={single ? view.rows[0] : null}
                                        column={opened.chartColumn}/> : null}
        </div>
    );
};

/**
 * Retry what might work a second time - the network, a server error - but not
 * a refusal. A 4xx is the server's answer, and asking three more times only
 * delays telling the parent by several seconds.
 */
const retryUnlessRefused = (failureCount, error) => {
    const status = error?.response?.status;
    return (!status || status >= 500) && failureCount < 3;
};

export default JournalPage;
