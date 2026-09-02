import React, {useMemo, useState} from "react";
import {useQuery} from "react-query";
import {CircularProgress, IconButton, Typography} from "@mui/material";
import {Check, Close} from "@mui/icons-material";
import {Formik} from "formik";
import FormikAutocomplete from "../../components/formik/FormikAutocomplete";
import {getFiltersOfPage, setFiltersOfPage} from "../../../utils/filters";
import {useNotification} from "../../../contexts/notification-context";
import {useUserContext} from "../../../contexts/user-context";
import {fetchClasses, fetchPeriodsAtDepth} from "../gradebook/gradebookApi";
import {fetchDailyGrid, markDailyAbsence} from "./absenceApi";

/**
 * The daily register: students down, school days across.
 *
 * A cross means absent, a tick means present, and there is no third state to
 * draw - which is the whole reason this page is short. It used to render a
 * journal grid where a cell might be marked, or blank-because-nobody-had-got-to-
 * it-yet, or blank-because-the-child-was-here, and no amount of UI could tell
 * the last two apart because the data could not either.
 *
 * There is no publish button and no change request. The daily register is a
 * staff document; what reaches a parent is an email the same day. Publication
 * lives on the monthly hours register, which is a different page.
 *
 * Marking saves immediately rather than on a debounce: a register is read across
 * a room and a half-saved one is worse than a slow one. A mis-click costs
 * nothing as long as it is noticed inside the quarter of an hour before the
 * notification goes out.
 */
const DailyRegisterPage = () => {

    const pageId = "DAILY_ABSENCE";
    const {setErrorMessage} = useNotification();
    const {hasPermission} = useUserContext();
    const [filters, setFilters] = useState({...getFiltersOfPage(pageId)});
    const [saving, setSaving] = useState(null);

    const classGroupId = filters?.classGroup?.id;
    const monthPeriodId = filters?.period?.id;

    const commit = (values) => {
        setFilters(values);
        setFiltersOfPage(pageId, values);
    };

    const {data: grid, isLoading, isError, refetch} = useQuery(
        [pageId, classGroupId, monthPeriodId],
        () => fetchDailyGrid({classGroupId, monthPeriodId}),
        {
            enabled: Boolean(classGroupId && monthPeriodId), onError: setErrorMessage,
            // A grid is live, shared, per-cell-versioned data, and the
            // save path updates this page's state without refetching. Anything
            // the cache still holds from before those edits is wrong, so it is
            // never fresh: leaving and returning reloads it.
            staleTime: 0,
        });

    // Months. The register is filled in one month at a time, so the picker
    // offers the level above the columns rather than the columns themselves.
    const onPeriods = useMemo(
        () => () => fetchPeriodsAtDepth(classGroupId, 2), [classGroupId]);

    /**
     * The marks, as a set of "enrollment:date".
     *
     * A Set rather than a Map because there is no value to hold: membership is
     * the entire state of a cell.
     */
    const marked = useMemo(() => {
        const set = new Set();
        (grid?.marks || []).forEach(m => set.add(`${m.enrollmentId}:${m.date}`));
        return set;
    }, [grid]);

    const totals = useMemo(() => {
        const map = new Map();
        (grid?.totals || []).forEach(t => map.set(t.enrollmentId, t.daysAbsent));
        return map;
    }, [grid]);

    const canEdit = grid?.canEdit && hasPermission("ADD_GRADES");

    const toggle = async (enrollmentId, date, absent) => {
        const key = `${enrollmentId}:${date}`;
        setSaving(key);
        try {
            await markDailyAbsence({
                classGroupId,
                date,
                marks: [{enrollmentId, absent}]
            });
            await refetch();
        } catch (e) {
            setErrorMessage(e);
        } finally {
            setSaving(null);
        }
    };

    return (
        <div style={{margin: "0 15px"}}>
            <div style={{display: "flex", gap: 16, alignItems: "center", margin: "20px 0"}}>
                <Typography variant="h6">გაცდენები (დღიური)</Typography>
                <Formik initialValues={{classGroup: filters?.classGroup || "", period: filters?.period || ""}}
                        enableReinitialize onSubmit={() => {
                }}>
                    {({values, setFieldValue}) => (
                        <div style={{display: "flex", gap: 16}}>
                            <FormikAutocomplete
                                name="classGroup"
                                label="კლასი"
                                style={{width: 200}}
                                onFetch={fetchClasses}
                                multiple={false}
                                getOptionSelected={(o, v) => o?.id === v?.id}
                                getOptionLabel={(o) => o?.name || ""}
                                onChange={(event, value) => {
                                    setFieldValue("classGroup", value);
                                    // The old period belongs to the old class's
                                    // scheme, so it cannot carry over.
                                    setFieldValue("period", null);
                                    commit({...values, classGroup: value, period: null});
                                }}
                            />
                            <FormikAutocomplete
                                name="period"
                                label="თვე"
                                style={{width: 200}}
                                disabled={!classGroupId}
                                onFetch={onPeriods}
                                multiple={false}
                                getOptionSelected={(o, v) => o?.id === v?.id}
                                getOptionLabel={(o) => o?.label || ""}
                                onChange={(event, value) => {
                                    setFieldValue("period", value);
                                    commit({...values, period: value});
                                }}
                            />
                        </div>
                    )}
                </Formik>
            </div>

            <Body
                grid={grid}
                isLoading={isLoading}
                isError={isError}
                chosen={Boolean(classGroupId && monthPeriodId)}
                marked={marked}
                totals={totals}
                canEdit={canEdit}
                saving={saving}
                onToggle={toggle}
            />
        </div>
    );
};

/**
 * The grid, or why there isn't one.
 *
 * Three states, kept apart deliberately. Collapsing them into "no grid means
 * pick a class" is what the earlier version did, so a request that failed
 * looked exactly like a page nobody had touched yet.
 */
const Body = ({grid, isLoading, isError, chosen, marked, totals, canEdit, saving, onToggle}) => {

    // ISO strings compare lexicographically, and the server's dates are ISO, so
    // no Date is constructed here - which is also what keeps the register free
    // of the timezone off-by-one that a local Date would introduce.
    const today = new Date().toISOString().slice(0, 10);

    if (!chosen) {
        return <Placeholder>აირჩიეთ კლასი და თვე.</Placeholder>;
    }
    if (isLoading) {
        return <div style={{padding: 24}}><CircularProgress size={24}/></div>;
    }
    if (isError || !grid) {
        return <Placeholder>ჟურნალი ვერ ჩაიტვირთა.</Placeholder>;
    }

    return (
        /* Hand-rolled rather than a DataGrid: the columns are data, a cell is one
           click, and a register wants to be readable across a room more than it
           wants sorting and pagination. */
        <div style={{overflowX: "auto", background: "white", paddingBottom: 12}}>
            <table style={{borderCollapse: "collapse", fontSize: 13}}>
                <thead>
                <tr>
                    <th style={{
                        ...headerCell, position: "sticky", left: 0,
                        background: "#fafafa", minWidth: 220, textAlign: "left"
                    }}>
                        მოსწავლე
                    </th>
                    {grid.columns.map(column => (
                        <th key={column.date}
                            style={{
                                ...headerCell, minWidth: 40,
                                // A rule after Friday, so a week reads
                                // as a week rather than 22 equal boxes.
                                color: column.date > today ? "#bbb" : undefined,
                                borderRight: column.dayOfWeek === "FRIDAY"
                                    ? "2px solid #bdbdbd" : headerCell.border
                            }}>
                            {column.dayOfMonth}
                        </th>
                    ))}
                    <th style={{...headerCell, minWidth: 60}}>ჯამი</th>
                </tr>
                </thead>
                <tbody>
                {grid.students.map(student => (
                    <tr key={student.enrollmentId}>
                        <td style={{
                            ...bodyCell, position: "sticky", left: 0,
                            background: "white", textAlign: "left"
                        }}>
                            {`${student.index}. ${student.lastName} ${student.firstName}`}
                        </td>
                        {grid.columns.map(column => {
                            const key = `${student.enrollmentId}:${column.date}`;
                            const absent = marked.has(key);
                            return (
                                <td key={column.date}
                                    style={{
                                        ...bodyCell,
                                        borderRight: column.dayOfWeek === "FRIDAY"
                                            ? "2px solid #bdbdbd" : bodyCell.border
                                    }}>
                                    <IconButton
                                        size="small"
                                        // A future day is refused by the
                                        // server, so offering it is a
                                        // guaranteed 400. The columns are
                                        // the whole month, including days
                                        // that have not happened.
                                        disabled={!canEdit || saving === key
                                            || column.date > today}
                                        onClick={() => onToggle(
                                            student.enrollmentId, column.date, !absent)}
                                        style={{padding: 2}}
                                    >
                                        {absent
                                            ? <Close fontSize="small"
                                                     style={{color: "#c62828"}}/>
                                            : <Check fontSize="small"
                                                     style={{color: "#cfd8dc"}}/>}
                                    </IconButton>
                                </td>
                            );
                        })}
                        <td style={{...bodyCell, fontWeight: 600}}>
                            {totals.get(student.enrollmentId) || ""}
                        </td>
                    </tr>
                ))}
                </tbody>
            </table>
        </div>
    );
};

const Placeholder = ({children}) => (
    <Typography variant="body2" style={{color: "#888", padding: 24}}>{children}</Typography>
);

const headerCell = {
    border: "1px solid #e0e0e0", padding: "6px 4px", background: "#fafafa",
    fontWeight: 600, textAlign: "center", whiteSpace: "nowrap"
};

const bodyCell = {
    border: "1px solid #eee", padding: "2px 4px", textAlign: "center", whiteSpace: "nowrap"
};

export default DailyRegisterPage;
