import React, {useMemo, useState} from "react";
import {useQuery} from "react-query";
import {Formik} from "formik";
import {Button, CircularProgress, FormControlLabel, Switch, Typography} from "@mui/material";
import {FileDownload} from "@mui/icons-material";
import FormikAutocomplete from "../../components/formik/FormikAutocomplete";
import {getFiltersOfPage, setFiltersOfPage} from "../../../utils/filters";
import {useNotification} from "../../../contexts/notification-context";
import {useUserContext} from "../../../contexts/user-context";
import {fetchJournals} from "../journals/journalApi";
import {exportClassSummary, fetchClasses, fetchClassSummary, fetchPeriods} from "./gradebookApi";

const PAGE_ID = "CLASS_SUMMARY";

/**
 * Every subject's mark for one class and trimester, side by side.
 *
 * The old monthly page, which the school asked to have back: students down, a
 * column per subject, then the rating, the ethics mark and the hours missed.
 * Per trimester rather than per month, because subject marks no longer exist
 * per month. Read-only - marks are entered in each subject's own journal.
 *
 * Nothing here knows which columns those are. The server picks each journal's
 * column for the period and says what it is; this page only draws them.
 */
const ClassSummaryPage = () => {

    const {setErrorMessage} = useNotification();
    const {hasPermission} = useUserContext();
    const [filters, setFilters] = useState({...getFiltersOfPage(PAGE_ID)});
    const [converted, setConverted] = useState(false);
    const [exporting, setExporting] = useState(false);

    const commit = (values) => {
        setFilters(values);
        setFiltersOfPage(PAGE_ID, values);
    };

    // The same query the menu runs, so this is normally already cached.
    const {data: journals} = useQuery(["JOURNALS"], () => fetchJournals(),
        {
            refetchOnWindowFocus: false, staleTime: 5 * 60 * 1000, onError: () => {
            }
        });

    // The subject journals - on production there is one, so the picker only
    // appears if the school ever adds a second.
    const subjectJournals = useMemo(() => (journals || []).filter(j =>
        j.subjectScoped && !j.archived
        && (j.currentVersionStatus === "ACTIVE" || j.currentVersionStatus === "LOCKED")
    ), [journals]);
    const journal = subjectJournals.find(j => j.uuid === filters?.journalUuid)
        || subjectJournals[0];
    const journalUuid = journal?.uuid;

    const classGroupId = filters?.classGroup?.id;
    const periodId = filters?.period?.id;
    const chosen = Boolean(classGroupId && periodId && journalUuid);

    const {data: summary, isLoading, isError} = useQuery(
        [PAGE_ID, classGroupId, periodId, journalUuid],
        () => fetchClassSummary({classGroupId, periodId, journalUuid}),
        {
            enabled: chosen,
            onError: setErrorMessage,
            // Marks change in other screens; coming back should show them.
            staleTime: 0,
            refetchOnWindowFocus: false
        });

    // The trimesters, then the year - the order of the school's own tables.
    const onPeriods = useMemo(() => async () => {
        const periods = await fetchPeriods(classGroupId, journalUuid);
        return [...periods].sort((a, b) =>
            (a.depth === 0) - (b.depth === 0) || a.ordinal - b.ordinal);
    }, [classGroupId, journalUuid]);

    const onExport = async () => {
        setExporting(true);
        try {
            await exportClassSummary({
                classGroupId, periodId, journalUuid,
                className: filters?.classGroup?.name,
                converted: converted && Boolean(summary?.convertible)
            });
        } catch (e) {
            setErrorMessage(e);
        } finally {
            setExporting(false);
        }
    };

    return (
        <div style={{margin: "0 15px"}}>
            <div style={{
                display: "flex", gap: 16, alignItems: "center", margin: "20px 0",
                flexWrap: "wrap"
            }}>
                <Formik
                    enableReinitialize
                    initialValues={{
                        classGroup: filters?.classGroup || "",
                        period: filters?.period || "",
                        journal: journal || ""
                    }}
                    onSubmit={() => {
                    }}
                >
                    {({values, setFieldValue}) => (
                        <div style={{display: "flex", gap: 16}}>
                            <div style={{width: 220}}>
                                <FormikAutocomplete
                                    name="classGroup"
                                    label="კლასი"
                                    multiple={false}
                                    onFetch={fetchClasses}
                                    getOptionLabel={(o) => o?.name
                                        ? `${o.name}${o.schoolName ? ` — ${o.schoolName}` : ""}` : ""}
                                    getOptionSelected={(o, v) => o?.id === v?.id}
                                    onChange={(event, value) => {
                                        // A period belongs to its class's scheme,
                                        // so it cannot carry over to another class.
                                        setFieldValue("classGroup", value);
                                        setFieldValue("period", "");
                                        commit({...filters, classGroup: value, period: ""});
                                    }}
                                />
                            </div>

                            {subjectJournals.length > 1 ? (
                                <div style={{width: 240}}>
                                    <FormikAutocomplete
                                        name="journal"
                                        label="ჟურნალი"
                                        multiple={false}
                                        onFetch={async () => subjectJournals}
                                        getOptionLabel={(o) => o?.name || ""}
                                        getOptionSelected={(o, v) => o?.uuid === v?.uuid}
                                        onChange={(event, value) => {
                                            setFieldValue("journal", value);
                                            setFieldValue("period", "");
                                            commit({...filters, journalUuid: value?.uuid, period: ""});
                                        }}
                                    />
                                </div>
                            ) : null}

                            <div style={{width: 200}}>
                                <FormikAutocomplete
                                    name="period"
                                    label="პერიოდი"
                                    multiple={false}
                                    disabled={!values.classGroup || !journalUuid}
                                    onFetch={onPeriods}
                                    getOptionLabel={(o) => o?.label || ""}
                                    getOptionSelected={(o, v) => o?.id === v?.id}
                                    onChange={(event, value) => {
                                        setFieldValue("period", value);
                                        commit({...filters, period: value});
                                    }}
                                />
                            </div>
                        </div>
                    )}
                </Formik>

                <div style={{marginLeft: "auto", display: "flex", alignItems: "center", gap: 16}}>
                    {summary?.convertible ? (
                        <FormControlLabel
                            control={
                                <Switch size="small" checked={converted}
                                        onChange={(e) => setConverted(e.target.checked)}/>
                            }
                            label={
                                <span style={{fontSize: 13, color: converted ? "#8a6d3b" : "#5b7c8d"}}>
                                    გადაყვანილი შკალა
                                </span>
                            }
                            style={{marginRight: 0}}
                        />
                    ) : null}

                    {/* The export endpoints are all MANAGE_GRADES; offering the
                        button to anyone else is a button that always 403s. */}
                    {hasPermission("MANAGE_GRADES") ? (
                        <Button size="small" startIcon={<FileDownload/>}
                                disabled={!summary || exporting}
                                onClick={onExport}
                                style={{textTransform: "none"}}>
                            ექსპორტი
                        </Button>
                    ) : null}
                </div>
            </div>

            <Body summary={summary} chosen={chosen} isLoading={isLoading} isError={isError}
                  converted={converted && Boolean(summary?.convertible)}/>
        </div>
    );
};

/**
 * The table, or why there isn't one - kept apart so a failed request never
 * looks like a page nobody has touched yet.
 */
const Body = ({summary, chosen, isLoading, isError, converted}) => {

    const specialLabels = useMemo(() => {
        const map = {};
        (summary?.specialValues || []).forEach(sv => {
            map[sv.code] = sv.label;
        });
        return map;
    }, [summary]);

    if (!chosen) {
        return <Placeholder>აირჩიეთ კლასი და პერიოდი.</Placeholder>;
    }
    if (isLoading) {
        return <div style={{padding: 24}}><CircularProgress size={24}/></div>;
    }
    if (isError || !summary) {
        return <Placeholder>შეფასებები ვერ ჩაიტვირთა.</Placeholder>;
    }

    const text = (cell, column) => {
        if (!cell) return "";
        if (cell.specialValue) return specialLabels[cell.specialValue] || cell.specialValue;
        if (cell.value === null || cell.value === undefined) return "";
        // Converted values arrive unrounded, as the school asked: 6.5 + 3 is 9.5.
        if (converted && column.converts && cell.convertedValue != null) {
            return String(Number(cell.convertedValue));
        }
        return Number(cell.value).toFixed(column.decimals);
    };

    return (
        <div>
            <div style={{textAlign: "center", marginBottom: 10}}>
                <div style={{fontSize: 20, fontWeight: "bold"}}>
                    {[summary.className, summary.periodLabel].filter(Boolean).join(" — ")}
                </div>
                {/* Said outright: a staff sheet reads the working marks, so it
                    can be ahead of what parents have been shown. */}
                <div style={{fontSize: 13, color: "#5b7c8d", marginTop: 2}}>
                    მიმდინარე ნიშნები, მათ შორის ჯერ გამოუქვეყნებელიც
                </div>
            </div>

            <div style={{
                overflow: "auto", background: "white",
                maxHeight: "calc(100vh - 210px)", border: "1px solid #cce1ea"
            }}>
                <table style={{borderCollapse: "separate", borderSpacing: 0, fontSize: 13}}>
                    <thead>
                    <tr>
                        <th style={{...headerCell, ...stickyLeft, zIndex: 3, minWidth: 240, textAlign: "left"}}>
                            მოსწავლის გვარი, სახელი
                        </th>
                        {summary.columns.map(column => (
                            <th key={column.key}
                                title={[column.detail, column.teacherName].filter(Boolean).join(" — ")}
                                style={{
                                    ...headerCell,
                                    ...(column.kind === "SUBJECT" ? {} : extraHeader)
                                }}>
                                {column.label}
                            </th>
                        ))}
                    </tr>
                    </thead>
                    <tbody>
                    {summary.rows.map(row => (
                        <tr key={row.enrollmentId}>
                            <td style={{...bodyCell, ...stickyLeft, textAlign: "left", background: "white"}}>
                                {`${row.index}. ${row.lastName} ${row.firstName}`}
                            </td>
                            {summary.columns.map(column => {
                                const cell = row.cells[column.key];
                                const shownConverted = converted && column.converts
                                    && cell?.convertedValue != null && !cell?.specialValue;
                                return (
                                    <td key={column.key} style={{
                                        ...bodyCell,
                                        ...(column.kind === "SUBJECT" ? {} : extraCell),
                                        ...(shownConverted ? convertedCell : {})
                                    }}>
                                        {text(cell, column)}
                                    </td>
                                );
                            })}
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
};

const Placeholder = ({children}) => (
    <Typography color="textSecondary" style={{padding: 24}}>{children}</Typography>
);

const headerCell = {
    position: "sticky", top: 0, zIndex: 2,
    background: "#fafafa",
    borderRight: "1px solid #cce1ea", borderBottom: "1px solid #cce1ea",
    padding: "6px 8px", fontWeight: 600, textAlign: "center",
    // Subject names run long ("ქართული ენა და ლიტერატურა"); wrapping keeps the
    // columns narrow enough for a whole class to fit across the screen.
    minWidth: 90, maxWidth: 130, whiteSpace: "normal", verticalAlign: "bottom"
};

const bodyCell = {
    borderRight: "1px solid #e3eef3", borderBottom: "1px solid #e3eef3",
    padding: "6px 8px", textAlign: "center", whiteSpace: "nowrap", height: 30
};

const stickyLeft = {position: "sticky", left: 0, zIndex: 1, borderRight: "1px solid #cce1ea"};

// The rating, ethics and hours: figures about the student rather than a
// subject's mark, so set apart from the subjects to their left.
const extraHeader = {background: "#eef2f6", fontWeight: 700};
const extraCell = {background: "#f6f9fb", fontWeight: 500};

// Marked because a 10 and a 7 look equally like a real mark: nothing but the
// styling says which scale is on screen.
const convertedCell = {color: "#8a6d3b", fontWeight: 600};

export default ClassSummaryPage;
