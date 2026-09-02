import React, {useEffect, useMemo, useState} from "react";
import {useQuery} from "react-query";
import {CircularProgress, TextField, Typography} from "@mui/material";
import {Formik} from "formik";
import FormikAutocomplete from "../../components/formik/FormikAutocomplete";
import {getFiltersOfPage, setFiltersOfPage} from "../../../utils/filters";
import {useNotification} from "../../../contexts/notification-context";
import {useUserContext} from "../../../contexts/user-context";
import {fetchClasses, fetchPeriodsAtDepth} from "../gradebook/gradebookApi";
import {fetchAbsenceGrid, markAbsence, saveAbsenceSettings} from "./absenceApi";
import PublishButton from "../gradebook/PublishButton";

/**
 * The monthly absence register: students down, months across.
 *
 * Academic hours missed, typed by the coordinator. This is the parent-facing
 * half of absence - it is published, and the permitted figure is what turns the
 * parent's diagram from green to red.
 *
 * **Published, not frozen.** Hours accumulate through a month and the
 * coordinator republishes as they do, so a published cell stays editable and
 * there is no change request in the way. That is a decision, not an omission:
 * an approval per top-up would sit on the normal path rather than on an
 * exception to it, and the school was never firm about wanting one here. The
 * flag that would restore it is GradingTemplate.locksOnPublish.
 *
 * The daily register is a separate page and a separate table - a tick is not a
 * number, and pretending otherwise is what made the register hard.
 */
const AbsenceRegisterPage = ({journalUuid, journalName}) => {

    const pageId = `ABSENCE_${journalUuid}`;
    const {setErrorMessage} = useNotification();
    const {hasPermission} = useUserContext();
    const [filters, setFilters] = useState({...getFiltersOfPage(pageId)});
    const [saving, setSaving] = useState(null);

    const classGroupId = filters?.classGroup?.id;
    const parentPeriodId = filters?.period?.id;

    const commit = (values) => {
        setFilters(values);
        setFiltersOfPage(pageId, values);
    };

    const {data: grid, isLoading, isError, refetch} = useQuery(
        [pageId, classGroupId, parentPeriodId],
        () => fetchAbsenceGrid({classGroupId, parentPeriodId, journalUuid}),
        {
            enabled: Boolean(classGroupId && parentPeriodId), onError: setErrorMessage,
            // A grid is live, shared, per-cell-versioned data, and the
            // save path updates this page's state without refetching. Anything
            // the cache still holds from before those edits is wrong, so it is
            // never fresh: leaving and returning reloads it.
            staleTime: 0,
        });

    // The year. Filled in a year at a time, so the picker offers the level
    // above the columns rather than the columns themselves.
    const onPeriods = useMemo(
        () => () => fetchPeriodsAtDepth(classGroupId, 0), [classGroupId]);

    const cellsByKey = useMemo(() => {
        const map = new Map();
        (grid?.cells || []).forEach(
            c => map.set(`${c.enrollmentId}:${c.periodId}:${c.componentCode}`, c));
        return map;
    }, [grid]);

    const canEdit = grid?.canEdit && hasPermission("ADD_GRADES");

    // The settings endpoint is secured on these two, not on ADD_GRADES. Gating
    // the fields on the wrong permission meant they rendered for someone whose
    // every blur then came back 403.
    const canEditSettings = hasPermission("MANAGE_TOTAL_ABSENCE")
        || hasPermission("MANAGE_GRADES");

    // MANAGE_CLOSED_PERIOD alone, which is all POST /publish requires. It was
    // also gated on grid.canEdit - the server's ADD_GRADES answer - so the one
    // person most likely to publish, a director who may release but not enter
    // marks, never saw the button.
    const canPublish = hasPermission("MANAGE_CLOSED_PERIOD");

    const write = async (enrollmentId, periodId, componentCode, value, expectedVersion) => {
        setSaving(`${enrollmentId}:${periodId}:${componentCode}`);
        try {
            const result = await markAbsence({
                journalUuid,
                classGroupId,
                periodId,
                entries: [{
                    enrollmentId,
                    // The column's own component, not the grid's input one:
                    // the columns are three different components now, on three
                    // kinds of period.
                    componentCode,
                    value,
                    expectedVersion
                }]
            });

            // A refused cell comes back HTTP 200 inside `conflicts`. Ignoring it
            // meant a value simply never appeared and nobody was told why. The
            // only reason left here is a version conflict - publication does not
            // refuse this journal - but silence would still be the worst answer.
            const refused = (result?.conflicts || [])[0];
            if (refused) {
                setErrorMessage(conflictMessage(refused), false, false);
            }
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
                <Typography variant="h6">{journalName}</Typography>
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
                                label="წელი"
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

                {/* Publishing the year releases the months beneath it - the
                    release walks the period tree, because the hours live on
                    months while the register is chosen a year at a time. */}
                {grid && canPublish ? (
                    <PublishButton
                        classGroup={filters?.classGroup}
                        period={filters?.period}
                        subject={null}
                        journalUuid={journalUuid}
                        // Published, not frozen. The dialog says so.
                        locksOnPublish={false}
                        disabled={Boolean(saving)}
                        onPublished={() => refetch()}
                        onError={setErrorMessage}
                    />
                ) : null}

            </div>

            <Body
                grid={grid}
                isLoading={isLoading}
                isError={isError}
                chosen={Boolean(classGroupId && parentPeriodId)}
                cellsByKey={cellsByKey}
                canEdit={canEdit}
                canEditSettings={canEditSettings}
                classGroupId={classGroupId}
                saving={saving}
                onCommit={write}
                onSettingsSaved={refetch}
                onError={setErrorMessage}
            />
        </div>
    );
};

/**
 * The grid, or why there isn't one.
 *
 * Three states, kept apart deliberately. Collapsing them into "no grid means
 * pick a class" is what this page used to do, so a request that failed looked
 * exactly like a page nobody had touched yet.
 */
const Body = ({
                  grid, isLoading, isError, chosen, cellsByKey, canEdit, canEditSettings,
                  classGroupId, saving, onCommit, onSettingsSaved, onError
              }) => {

    if (!chosen) {
        return <Placeholder>აირჩიეთ კლასი და წელი.</Placeholder>;
    }
    if (isLoading) {
        return <div style={{padding: 24}}><CircularProgress size={24}/></div>;
    }
    if (isError || !grid) {
        return <Placeholder>ჟურნალი ვერ ჩაიტვირთა.</Placeholder>;
    }

    return (
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
                    {/* A trimester or year column is a total of the ones to
                            its left, so it is set apart rather than reading as
                            another reporting period. */}
                    {grid.columns.map(column => (
                        <th key={`${column.periodId}:${column.componentCode}`}
                            style={{
                                ...headerCell, minWidth: 70,
                                background: column.editable ? undefined : "#eef2f6",
                                fontWeight: column.editable ? undefined : 700
                            }}>
                            {column.label}
                        </th>
                    ))}
                    <th style={{...headerCell, minWidth: 60}}>ჯამი</th>
                </tr>
                </thead>
                <tbody>
                {canEditSettings ? (
                    <SettingsRows
                        grid={grid}
                        classGroupId={classGroupId}
                        onSaved={onSettingsSaved}
                        onError={onError}
                    />
                ) : null}
                {grid.students.map(student => (
                    <tr key={student.enrollmentId}>
                        <td style={{
                            ...bodyCell, position: "sticky", left: 0,
                            background: "white", textAlign: "left"
                        }}>
                            {`${student.index}. ${student.lastName} ${student.firstName}`}
                        </td>
                        {grid.columns.map(column => {
                            const key = `${student.enrollmentId}:${column.periodId}`
                                + `:${column.componentCode}`;
                            return (
                                <td key={`${column.periodId}:${column.componentCode}`}
                                    style={{
                                        ...bodyCell,
                                        background: column.editable
                                            ? undefined : "#f6f8fa"
                                    }}>
                                    <HoursCell
                                        cell={cellsByKey.get(key)}
                                        // A computed column is shown, never
                                        // typed into: it is the sum of the
                                        // columns to its left.
                                        canEdit={canEdit && column.editable}
                                        busy={saving === key}
                                        onCommit={(value, version) => onCommit(
                                            student.enrollmentId, column.periodId,
                                            column.componentCode, value, version)}
                                    />
                                </td>
                            );
                        })}
                        <td style={{...bodyCell, fontWeight: 600}}>
                            {rowTotal(grid, cellsByKey, student.enrollmentId)}
                        </td>
                    </tr>
                ))}
                </tbody>
            </table>
        </div>
    );
};

/**
 * Academic hours, typed and committed on blur.
 *
 * A published cell stays editable: hours accumulate and the month is
 * republished, so freezing it would put an approval on the ordinary path.
 */
const HoursCell = ({cell, canEdit, busy, onCommit}) => {

    const [text, setText] = useState(cell?.value == null ? "" : String(cell.value));

    useEffect(() => {
        setText(cell?.value == null ? "" : String(cell.value));
    }, [cell?.value]);

    const commit = () => {
        const trimmed = text.trim();
        // A half-typed number is not an instruction to clear the stored one.
        // Number("12a") is NaN, which serialised to null and deleted the value.
        if (trimmed !== "" && Number.isNaN(Number(trimmed))) {
            setText(cell?.value == null ? "" : String(cell.value));
            return;
        }
        const next = trimmed === "" ? null : Number(trimmed);
        const current = cell?.value == null ? null : Number(cell.value);
        if (next !== current) {
            onCommit(next, cell?.rowVersion);
        }
    };

    return (
        <TextField
            variant="standard"
            disabled={!canEdit || busy}
            value={text}
            onChange={(e) => setText(e.target.value)}
            onBlur={commit}
            // A published value that has since been edited is worth showing:
            // it is what parents are *not* yet seeing.
            style={cell?.changedSincePublication ? {background: "#fff8e1"} : undefined}
            inputProps={{style: {textAlign: "center", width: 48, fontSize: 13}}}
        />
    );
};

/**
 * Each month's academic hours and the permitted absence, as two rows.
 *
 * Per column, not one pair above the grid. They used to be a single pair
 * written against the period the user had chosen - the *year* - so setting
 * September's 120 hours set every month's, and the permitted figure that turns
 * a parent's chart red had the wrong granularity to do it. The brief has always
 * said per class and per month.
 */
const SettingsRows = ({grid, classGroupId, onSaved, onError}) => {

    const byPeriod = new Map();
    (grid.settings || []).forEach(s => byPeriod.set(s.periodId, s));

    const save = async (periodId, field, raw) => {
        const setting = byPeriod.get(periodId) || {};
        const trimmed = (raw ?? "").trim();
        // A half-typed number is not an instruction to delete the stored one.
        // Number("12a") is NaN, which serialised to null and cleared the value.
        if (trimmed !== "" && Number.isNaN(Number(trimmed))) {
            return;
        }
        const next = trimmed === "" ? undefined : Number(trimmed);
        try {
            await saveAbsenceSettings({
                classGroupId,
                periodId,
                // Both keys travel together: the endpoint writes the pair, and
                // omitting one would clear it.
                totalAcademicHours: field === "total" ? next : setting.totalAcademicHours,
                permittedMissedHours: field === "permitted" ? next : setting.permittedMissedHours
            });
            onSaved();
        } catch (e) {
            onError(e);
        }
    };

    const row = (label, field, read, tint) => (
        <tr style={{background: tint}}>
            <td style={{
                ...bodyCell, position: "sticky", left: 0, background: tint,
                textAlign: "left", fontStyle: "italic"
            }}>
                {label}
            </td>
            {/* Only where hours are entered. A trimester's academic hours are
                the sum of its reporting periods', and a second place to type
                them is a second answer to the same question. */}
            {grid.columns.map(column => (
                <td key={`${column.periodId}:${column.componentCode}`}
                    style={{...bodyCell, background: column.editable ? undefined : "#eef2f6"}}>
                    {column.editable ? (
                        <SettingCell
                            value={read(byPeriod.get(column.periodId) || {})}
                            onCommit={(raw) => save(column.periodId, field, raw)}
                        />
                    ) : null}
                </td>
            ))}
            <td style={bodyCell}/>
        </tr>
    );

    return (
        <>
            {row("აკადემიური საათები", "total", s => s.totalAcademicHours, "#f6f8fa")}
            {row("დასაშვები გაცდენა", "permitted", s => s.permittedMissedHours, "#f6f8fa")}
        </>
    );
};

/** One month's one number, committed on blur. */
const SettingCell = ({value, onCommit}) => {

    const [text, setText] = useState(value == null ? "" : String(value));

    useEffect(() => {
        setText(value == null ? "" : String(value));
    }, [value]);

    return (
        <TextField
            variant="standard"
            value={text}
            onChange={(e) => setText(e.target.value)}
            onBlur={() => {
                const current = value == null ? "" : String(value);
                if (text.trim() !== current.trim()) {
                    onCommit(text);
                }
            }}
            inputProps={{style: {textAlign: "center", width: 48, fontSize: 12}}}
        />
    );
};

const Placeholder = ({children}) => (
    <Typography variant="body2" style={{color: "#888", padding: 24}}>{children}</Typography>
);

const conflictMessage = (conflict) => {
    switch (conflict.reason) {
        case "NOT_EDITABLE":
            return "სვეტი არ ექვემდებარება შესწორებას";
        case "OUT_OF_RANGE":
            return "მნიშვნელობა დიაპაზონს სცდება";
        default:
            return `სხვამ შეცვალა: ${conflict.current ?? "—"}`;
    }
};

/**
 * The row's own total, computed here rather than fetched.
 *
 * The stored roll-up lives at the year and is what parents will be shown; this
 * is the running total for the year on screen, which is what the person filling
 * it in is looking for.
 */
const rowTotal = (grid, cellsByKey, enrollmentId) => {
    let total = 0;
    grid.columns.forEach(column => {
        // Only the columns that are typed into. The trimester and year columns
        // are totals of those, so counting them too reported a student with
        // twenty hours as having sixty.
        if (!column.editable) {
            return;
        }
        const cell = cellsByKey.get(
            `${enrollmentId}:${column.periodId}:${column.componentCode}`);
        if (cell?.value != null) {
            total += Number(cell.value);
        }
    });
    return total === 0 ? "" : total;
};

const headerCell = {
    border: "1px solid #e0e0e0", padding: "6px 4px", background: "#fafafa",
    fontWeight: 600, textAlign: "center", whiteSpace: "nowrap"
};

const bodyCell = {
    border: "1px solid #eee", padding: "2px 4px", textAlign: "center", whiteSpace: "nowrap"
};

export default AbsenceRegisterPage;
