import React, {useEffect, useMemo, useRef} from "react";
import {useHistory, useLocation} from "react-router-dom";
import {useQuery, useQueryClient} from "react-query";
import {markHomeworkSeen} from "../journal/parentApi";
import MonthCalendar from "./MonthCalendar";
import DaySubjects from "./DaySubjects";
import "./homework.css";

/**
 * A calendar, and the day you click on.
 *
 * Rendered twice: homework, and the child's description. They are the same
 * screen over the same shapes - a month of day counts, a day grouped by subject,
 * and a read mark per item - so they are one component with the differences
 * passed in. Two trees would drift, and every later fix would have to be made
 * in both.
 *
 * The file keeps the homework name for the same reason the homework_seen table
 * does: it is named for its first use rather than its only one.
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
 *
 * **The month and the day live in the address.** The browser's back button then
 * works the way a parent expects, a day can be returned to, and a reload does
 * not quietly move them to today.
 *
 * **Today is selected on arrival**, which is both the answer a parent opened the
 * page for and the reason the calendar needs no separate mark for today: the
 * selected border is already on it.
 */
const HomeworkPage = ({
                          title,
                          path,
                          queryKey,
                          fetchMonth,
                          fetchDay,
                      }) => {

    const history = useHistory();
    const {search} = useLocation();
    const params = new URLSearchParams(search);
    const queryClient = useQueryClient();

    const today = new Date().toISOString().slice(0, 10);
    // Today unless the address says otherwise, and the month follows the day
    // rather than being tracked apart from it - two pieces of state that must
    // agree are one piece of state.
    const selected = params.get("day") || today;
    const month = params.get("month") || selected.slice(0, 7);

    const go = (next) => history.replace(`${path}?${next.toString()}`);

    const setMonth = (m) => {
        const next = new URLSearchParams(search);
        next.set("month", m);
        // The day being read belonged to the month being left.
        next.delete("day");
        go(next);
    };

    const setSelected = (date) => {
        const next = new URLSearchParams(search);
        next.set("month", month);
        if (date) {
            next.set("day", date);
        } else {
            next.delete("day");
        }
        go(next);
    };

    const {data: calendar, isLoading} = useQuery(
        [queryKey, month],
        () => fetchMonth(month),
        {keepPreviousData: true, refetchOnWindowFocus: false}
    );

    // Today can be outside the academic year - every summer between two of
    // them, and for the whole of a year that has finished. Landing there strands
    // a parent on an empty month with both arrows disabled, so the first load
    // steps to the nearest month that is inside it. Only when the address does
    // not already name one, or this would fight the parent's own navigation.
    useEffect(() => {
        if (params.get("month") || !calendar) {
            return;
        }
        const from = calendar.yearStartsOn && calendar.yearStartsOn.slice(0, 7);
        const to = calendar.yearEndsOn && calendar.yearEndsOn.slice(0, 7);
        if (!from || !to) {
            return;
        }
        const nearest = month < from ? from : month > to ? to : null;
        if (nearest) {
            const next = new URLSearchParams(search);
            next.set("month", nearest);
            next.delete("day");
            history.replace(`${path}?${next.toString()}`);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [calendar, month]);

    // Only for a day inside the month on screen. Stepping to another month
    // clears the day, and asking for one that is not drawn would load a panel
    // for a date nobody can see.
    const showing = selected && selected.slice(0, 7) === month ? selected : null;

    const {data: day} = useQuery(
        [queryKey, "DAY", showing],
        () => fetchDay(showing),
        {enabled: Boolean(showing), refetchOnWindowFocus: false}
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
            queryClient.invalidateQueries([queryKey, month]);
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
        <div className="hw">
            <h1 className="hw__pageTitle">{title}</h1>

            <MonthCalendar
                month={month}
                byDate={byDate}
                selected={showing}
                loading={isLoading}
                bounds={{from: calendar?.yearStartsOn, to: calendar?.yearEndsOn}}
                onMonth={setMonth}
                onSelect={(date) => setSelected(date === showing ? null : date)}
            />

            {showing ? <DaySubjects date={showing} day={day}/> : null}
        </div>
    );
};

export default HomeworkPage;
