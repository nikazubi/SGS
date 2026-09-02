import {useCallback, useEffect, useMemo, useRef, useState} from "react";
import {useQuery} from "react-query";
import {fetchGrid, saveGradeBatch} from "./gradebookApi";

export const cellKey = (enrollmentId, componentCode) => `${enrollmentId}:${componentCode}`;

const FLUSH_DELAY_MS = 800;

/**
 * The grid's state and its autosave.
 *
 * Teachers are used to a mark sticking when they leave the cell, and that does
 * not change. What changes is that leaving a cell stops being a transaction:
 * edits collect and go out as one batch, and the recomputed values ride home on
 * the response.
 *
 * The old page posted one request per cell and then invalidated the query,
 * refetching the entire grid - so entering marks for a class of 25 cost 25
 * posts and 25 full reloads.
 */
/** A typed code or label, resolved to the code the template declares. */
const matchSpecial = (text, specialValues) => {
    const needle = text.trim().toLocaleLowerCase();
    const hit = (specialValues || []).find(sv =>
        sv.code.toLocaleLowerCase() === needle
        || (sv.label || "").toLocaleLowerCase() === needle);
    return hit ? hit.code : text.trim();
};

const useGradeSheet = ({classGroupId, subjectId, periodId, journalUuid}) => {

    const enabled = Boolean(classGroupId && periodId);

    const {data: grid, isLoading, isError, error, refetch} = useQuery(
        ["GRADEBOOK_GRID", journalUuid, classGroupId, subjectId, periodId],
        () => fetchGrid({classGroupId, subjectId, periodId, journalUuid}),
        {
            enabled, keepPreviousData: false, refetchOnWindowFocus: false,
            // A grid is live, shared, per-cell-versioned data, and the
            // save path updates this page's state without refetching. Anything
            // the cache still holds from before those edits is wrong, so it is
            // never fresh: leaving and returning reloads it.
            staleTime: 0,
        }
    );

    /** cellKey -> {value, specialValue, source, override, rowVersion, published, changedSincePublication} */
    const [cells, setCells] = useState(() => new Map());
    /** cellKey -> conflict returned by the server */
    const [conflicts, setConflicts] = useState(() => new Map());
    /** cells whose calculated value is being recomputed server-side */
    const [stale, setStale] = useState(() => new Set());

    const [status, setStatus] = useState("idle");   // idle | dirty | saving | saved | error
    const [pending, setPending] = useState(0);

    const dirty = useRef(new Map());
    const inFlight = useRef(false);
    const timer = useRef(null);

    // Which grid the state belongs to. A flush that lands after the filters
    // changed would otherwise patch its old values into the newly loaded grid,
    // because a cell key is only (student, column).
    const scope = `${journalUuid || ""}:${classGroupId}:${subjectId}:${periodId}`;
    // Updated in an effect, not during render. Assigning it inline meant the
    // unmount flush - the one that saves work when the filters change - read a
    // ref that had *already* moved to the new scope, so its guard passed and it
    // patched old-period values into the new grid.
    const scopeRef = useRef(scope);
    useEffect(() => {
        scopeRef.current = scope;
    }, [scope]);
    const failures = useRef(0);

    // Dependents are static per template version, so which calculated cells a
    // given input moves is known without evaluating anything in the browser.
    const dependentsByCode = useMemo(() => {
        const map = new Map();
        (grid?.columns || []).forEach(c => map.set(c.code, c.dependents || []));
        return map;
    }, [grid]);

    const specialValues = useMemo(() => grid?.specialValues || [], [grid]);

    const columnsByCode = useMemo(() => {
        const map = new Map();
        (grid?.columns || []).forEach(c => map.set(c.code, c));
        return map;
    }, [grid]);

    const loadedScope = useRef(null);

    useEffect(() => {
        if (!grid) return;
        const next = new Map();
        grid.cells.forEach(c => next.set(cellKey(c.enrollmentId, c.componentCode), c));
        setCells(next);
        setConflicts(new Map());
        setStale(new Set());

        // Only a genuine scope change discards pending edits. A refetch of the
        // same grid - after publishing, or after raising a change request -
        // used to wipe whatever the teacher had typed since.
        if (loadedScope.current !== scope) {
            dirty.current = new Map();
            setPending(0);
            setStatus("idle");
            loadedScope.current = scope;
        } else if (dirty.current.size > 0) {
            setStatus("dirty");
        }
    }, [grid, scope]);

    const applyServerCells = useCallback((list) => {
        if (!list?.length) return;
        setCells(prev => {
            const next = new Map(prev);
            list.forEach(c => {
                const key = cellKey(c.enrollmentId, c.componentCode);
                const before = next.get(key);
                next.set(key, {
                    ...before,
                    enrollmentId: c.enrollmentId,
                    componentCode: c.componentCode,
                    value: c.value,
                    specialValue: c.specialValue,
                    rowVersion: c.rowVersion,
                    // The server does not resend publication state on a write,
                    // so it is carried over rather than dropped to false.
                    published: before?.published ?? false,
                    changedSincePublication: before?.published
                        ? true
                        : (before?.changedSincePublication ?? false),
                    source: before?.source,
                    override: before?.override
                });
            });
            return next;
        });
    }, []);

    const flush = useCallback(async () => {
        if (inFlight.current || dirty.current.size === 0) return;

        const batch = Array.from(dirty.current.values());
        const sentFor = scopeRef.current;
        // Edits made while this request is on the wire belong to the next
        // batch; they are never merged into one already in flight.
        dirty.current = new Map();
        inFlight.current = true;
        setStatus("saving");

        try {
            const result = await saveGradeBatch({
                journalUuid, classGroupId, subjectId, periodId,
                entries: batch.map(e => ({
                    enrollmentId: e.enrollmentId,
                    componentCode: e.componentCode,
                    value: e.value,
                    specialValue: e.specialValue,
                    expectedVersion: e.expectedVersion,
                    override: e.override
                }))
            });

            if (sentFor !== scopeRef.current) {
                // The grid moved on while this was in flight. Its values belong
                // to a period nobody is looking at any more.
                return;
            }

            applyServerCells(result.applied);
            applyServerCells(result.derived);

            setConflicts(prev => {
                const next = new Map(prev);
                (result.conflicts || []).forEach(c =>
                    next.set(cellKey(c.enrollmentId, c.componentCode), c));
                return next;
            });

            // Adopt the server's row version for every refused cell, so the
            // next attempt is judged against what is actually stored. Without
            // it a conflict could only be cleared by reloading the page.
            if (result.conflicts?.length) {
                setCells(prev => {
                    const next = new Map(prev);
                    result.conflicts.forEach(c => {
                        if (c.currentVersion === null || c.currentVersion === undefined) {
                            return;
                        }
                        const k = cellKey(c.enrollmentId, c.componentCode);
                        next.set(k, {
                            ...next.get(k),
                            enrollmentId: c.enrollmentId,
                            componentCode: c.componentCode,
                            value: c.current,
                            rowVersion: c.currentVersion
                        });
                    });
                    return next;
                });
            }

            failures.current = 0;
            setStale(new Set());
            setPending(dirty.current.size);
            setStatus(dirty.current.size > 0 ? "dirty" : (result.conflicts?.length ? "error" : "saved"));
        } catch (e) {
            // Put the batch back so nothing is silently lost, newer edits winning.
            // Requeued only if the grid has not moved on. Otherwise these
            // cells belong to a period nobody is looking at, and replaying them
            // would write one trimester's marks into another.
            if (sentFor === scopeRef.current) {
                batch.forEach(entry => {
                    const key = cellKey(entry.enrollmentId, entry.componentCode);
                    if (!dirty.current.has(key)) dirty.current.set(key, entry);
                });
            }
            failures.current += 1;
            setPending(dirty.current.size);
            setStale(new Set());
            setStatus("error");
        } finally {
            inFlight.current = false;
            if (dirty.current.size > 0 && sentFor === scopeRef.current) {
                // Backed off after a failure: a server that is down should not
                // be hit three times a second for as long as the tab is open.
                const delay = failures.current > 0
                    ? Math.min(FLUSH_DELAY_MS * (2 ** failures.current), 30000)
                    : FLUSH_DELAY_MS;
                timer.current = setTimeout(flush, delay);
            }
        }
    }, [journalUuid, classGroupId, subjectId, periodId, applyServerCells]);

    /**
     * Record an edit locally and schedule the flush. Synchronous on purpose:
     * awaiting the network here is what made the old grid feel slow.
     */
    const editCell = useCallback((enrollmentId, componentCode, rawValue) => {
        const column = columnsByCode.get(componentCode);
        if (!column) return;

        const key = cellKey(enrollmentId, componentCode);
        const current = cells.get(key);

        const text = rawValue === null || rawValue === undefined ? "" : String(rawValue).trim();
        // Matched against the codes the template declares rather than
        // upper-cased blindly: a teacher types ჩთ, whose uppercase is Mtavruli
        // Georgian, not the code CHT. Anything unrecognised is refused by the
        // server rather than persisted as a special value nobody defined.
        const special = column.allowSpecialValues && text !== "" && Number.isNaN(Number(text))
            ? matchSpecial(text, specialValues)
            : null;
        const value = text === "" || special !== null ? null : Number(text);

        const isDerived = column.kind === "DERIVED";

        setCells(prev => {
            const next = new Map(prev);
            next.set(key, {
                ...current,
                enrollmentId, componentCode,
                value, specialValue: special,
                // Typing into a calculated column is an override, and it is
                // sticky: held through recomputes until explicitly cleared.
                override: isDerived ? true : (current?.override ?? false),
                source: isDerived ? "MANUAL" : (current?.source ?? "MANUAL")
            });
            return next;
        });

        setConflicts(prev => {
            if (!prev.has(key)) return prev;
            const next = new Map(prev);
            next.delete(key);
            return next;
        });

        // Dim exactly what this edit will move, rather than a whole row.
        const affected = dependentsByCode.get(componentCode) || [];
        if (affected.length) {
            setStale(prev => {
                const next = new Set(prev);
                affected.forEach(code => next.add(cellKey(enrollmentId, code)));
                return next;
            });
        }

        dirty.current.set(key, {
            enrollmentId, componentCode, value, specialValue: special,
            // Null while a save is in flight: the version this edit was based
            // on is about to be superseded by the response, and sending the old
            // one would make the teacher's own next flush conflict with itself.
            expectedVersion: inFlight.current ? null : (current?.rowVersion ?? null),
            override: isDerived ? true : undefined
        });
        setPending(dirty.current.size);
        setStatus("dirty");

        clearTimeout(timer.current);
        timer.current = setTimeout(flush, FLUSH_DELAY_MS);
    }, [cells, columnsByCode, dependentsByCode, specialValues, flush]);

    /** Hand a calculated cell back to the engine. */
    const revertToCalculated = useCallback((enrollmentId, componentCode) => {
        const key = cellKey(enrollmentId, componentCode);
        const current = cells.get(key);
        dirty.current.set(key, {
            enrollmentId, componentCode,
            value: current?.value ?? null,
            specialValue: current?.specialValue ?? null,
            expectedVersion: current?.rowVersion ?? null,
            override: false
        });
        setStale(prev => new Set(prev).add(key));
        setPending(dirty.current.size);
        setStatus("dirty");
        clearTimeout(timer.current);
        timer.current = setTimeout(flush, 0);
    }, [cells, flush]);

    const flushNow = useCallback(() => {
        clearTimeout(timer.current);
        return flush();
    }, [flush]);

    /** Nothing typed should be lost to a closed tab or a changed filter. */
    useEffect(() => {
        const warn = (e) => {
            if (dirty.current.size === 0) return;
            flushNow();
            e.preventDefault();
            e.returnValue = "";
        };
        window.addEventListener("beforeunload", warn);
        return () => {
            window.removeEventListener("beforeunload", warn);
            clearTimeout(timer.current);
            if (dirty.current.size > 0) flush();
        };
    }, [flush, flushNow]);

    return {
        grid, isLoading, isError, error, refetch, specialValues,
        cells, conflicts, stale,
        status, pending,
        editCell, revertToCalculated, flushNow
    };
};

export default useGradeSheet;
