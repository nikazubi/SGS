import React, {useMemo, useState} from "react";
import {useParams} from "react-router-dom";
import {useQuery} from "react-query";
import {fetchParentView} from "./parentApi";
import {chartFor} from "./charts";
import RowCards from "./RowCards";
import RowTable from "./RowTable";

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
 */
const JournalPage = () => {

    const {uuid} = useParams();
    const [periodId, setPeriodId] = useState(null);
    const [subjectId, setSubjectId] = useState(null);

    const {data: view, isLoading} = useQuery(
        ["PARENT_VIEW", uuid, periodId, subjectId],
        () => fetchParentView({uuid, periodId, subjectId}),
        {keepPreviousData: true, refetchOnWindowFocus: false}
    );

    const Chart = useMemo(() => chartFor(view?.chartKey), [view]);

    if (isLoading || !view) {
        return <div className="ib__center column">
            <div className="pageName">…</div>
        </div>;
    }

    const single = view.rows.length === 1;

    return (
        <div className="ib__center column">
            <div className="pageName">{view.journalName}</div>

            {/* Only when rows are subjects. A journal whose rows are its own
                periods has nothing to pick — they are all on screen. */}
            {view.periods.length > 1 ? (
                <div className="yearDropwdown">
                    <select
                        value={view.selectedPeriodId || ""}
                        onChange={(e) => setPeriodId(Number(e.target.value))}
                    >
                        {view.periods.map(p =>
                            <option key={p.id} value={p.id}>{p.label}</option>)}
                    </select>
                </div>
            ) : null}

            {subjectId ? (
                <button className="backLink" onClick={() => setSubjectId(null)}>
                    ← ყველა საგანი
                </button>
            ) : null}

            {view.rows.length === 0 ? (
                <div className="emptyState">
                    ამ პერიოდისთვის შეფასებები ჯერ არ არის გამოქვეყნებული.
                </div>
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

            {Chart ? <Chart view={view} row={single ? view.rows[0] : null}/> : null}
        </div>
    );
};

export default JournalPage;
