import React, {useEffect, useMemo, useRef, useState} from "react";
import {useQuery, useQueryClient} from "react-query";
import {fetchHomeworkDay, fetchHomeworkMonth, markHomeworkSeen} from "../journal/parentApi";
import MonthCalendar from "./MonthCalendar";
import DaySubjects from "./DaySubjects";
import "./homework.css";

/**
 * Homework, as the brief asks for it: a calendar, and the day you click on.
 *
 * Three things a day can be, and they are independent — a day can be all three
 * at once:
 *
 *     holds work  — the school set something for that date
 *     unopened    — some of it this parent has not read
 *     selected    — the day currently expanded below
 *
 * Opening a day marks its assignments read, but not on the click. The marks are
 * collected and sent a couple of seconds later, so flicking through a week is
 * one request rather than seven. That is safe here only because the write is
 * idempotent: a re-send, a double tap or a retry after a dropped response all
 * land on the same state. It would not be safe against a stored list.
 */
const HomeworkPage = () => {

    const [month, setMonth] = useState(() => new Date().toISOString().slice(0, 7));
    const [selected, setSelected] = useState(null);
    const queryClient = useQueryClient();

    const {data: calendar, isLoading} = useQuery(
        ["PARENT_HOMEWORK", month],
        () => fetchHomeworkMonth(month),
        {keepPreviousData: true, refetchOnWindowFocus: false}
    );

    const {data: day} = useQuery(
        ["PARENT_HOMEWORK_DAY", selected],
        () => fetchHomeworkDay(selected),
        {enabled: Boolean(selected), refetchOnWindowFocus: false}
    );

    const byDate = useMemo(() => {
        const map = new Map();
        (calendar?.days || []).forEach(d => map.set(d.date, d));
        return map;
    }, [calendar]);

    // ---- the debounced "seen" batch ----------------------------------------
    //
    // A ref, not state: adding to it must not re-render, and the timer has to
    // read the latest set rather than the one captured when it was scheduled.
    const pending = useRef(new Set());
    const timer = useRef(null);

    const flush = React.useCallback(async () => {
        const batch = Array.from(pending.current);
        if (batch.length === 0) {
            return;
        }
        pending.current = new Set();
        try {
            await markHomeworkSeen(batch);
            // The calendar's unread counts are now stale by exactly this batch.
            queryClient.invalidateQueries(["PARENT_HOMEWORK", month]);
        } catch (e) {
            // Put them back. Nothing is lost by trying again, and the parent has
            // genuinely read them - dropping the batch would leave the day
            // flagged unread forever.
            batch.forEach(uuid => pending.current.add(uuid));
        }
    }, [queryClient, month]);

    useEffect(() => {
        const unseen = (day?.subjects || [])
            .flatMap(s => s.items)
            .filter(item => !item.seen)
            .map(item => item.uuid);

        if (unseen.length === 0) {
            return;
        }
        unseen.forEach(uuid => pending.current.add(uuid));

        clearTimeout(timer.current);
        timer.current = setTimeout(flush, 2000);
        return () => clearTimeout(timer.current);
    }, [day, flush]);

    // Leaving the page must not lose what was read. The timer dies with the
    // component, so the batch is sent on the way out.
    useEffect(() => () => {
        flush();
    }, [flush]);

    return (
        <div className="ib__center column">
            <div className="pageName">საშინაო დავალებები</div>

            <MonthCalendar
                month={month}
                byDate={byDate}
                selected={selected}
                loading={isLoading}
                onMonth={(next) => {
                    setMonth(next);
                    // The selected day belonged to the month being left.
                    setSelected(null);
                }}
                onSelect={(date) => setSelected(date === selected ? null : date)}
            />

            {selected ? <DaySubjects date={selected} day={day}/> : null}
        </div>
    );
};

export default HomeworkPage;
