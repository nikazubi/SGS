import React, {useCallback, useEffect, useState} from "react";
import {useQuery} from "react-query";
import {Formik} from "formik";
import {FormControlLabel, Switch} from "@mui/material";
import FlexBox from "../../../components/FlexBox";
import FormikAutocomplete from "../../components/formik/FormikAutocomplete";
import {setFiltersOfPage} from "../../../utils/filters";
import {fetchClasses, fetchPeriods, fetchSubjects} from "./gradebookApi";
import {fetchJournal} from "../journals/journalApi";
import SaveStatus from "./SaveStatus";
import PublishButton from "./PublishButton";
import ExportMenu from "./ExportMenu";

const PAGE_ID = "GRADEBOOK";

/**
 * Class, subject and period.
 *
 * All three come from the new model: a class knows its own period scheme, so
 * the periods on offer are always the ones that class is actually graded on.
 * The old toolbar offered a hardcoded I/II/III trimester list regardless.
 */
const GradebookToolbar = ({
                              filters, setFilters, status, pending, onFlush,
                              canPublish, canExport = true, onPublished, onError, columns,
                              convertible, converted, onConvertedChange,
                              pageId = PAGE_ID, journalUuid
                          }) => {

    const onClasses = useCallback(() => fetchClasses(), []);

    const onSubjects = useCallback(
        () => fetchSubjects(filters?.classGroup?.id), [filters?.classGroup?.id]);

    // The journal decides which filters mean anything. A class-wide journal
    // has no subject dimension at all, so offering a subject dropdown the grid
    // then ignores is worse than not offering one.
    const {data: journal} = useQuery(
        ["JOURNAL", journalUuid], () => fetchJournal(journalUuid),
        {enabled: Boolean(journalUuid), refetchOnWindowFocus: false});

    const showSubject = journal ? journal.subjectScoped : true;

    const [periodOptions, setPeriodOptions] = useState([]);

    const onPeriods = useCallback(
        async () => {
            const periods = await fetchPeriods(filters?.classGroup?.id, journalUuid);
            setPeriodOptions(periods);
            return periods;
        },
        [filters?.classGroup?.id, journalUuid]);

    // Decision 57: a once-a-year journal shows one grid and no period picker.
    const singlePeriod = periodOptions.length === 1;

    const commit = (values) => {
        setFiltersOfPage(pageId, values);
        setFilters(values);
    };

    // A dimension that collapses to one option is chosen, not merely hidden.
    // Hiding the control alone left a once-a-year journal with no period in the
    // filters and so no grid at all - the query will not run without one.
    useEffect(() => {
        if (singlePeriod && filters?.classGroup && !filters?.period) {
            commit({...filters, period: periodOptions[0]});
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [singlePeriod, periodOptions, filters?.classGroup, filters?.period]);

    // Likewise a class-wide journal: its grid is loaded with no subject, so a
    // stale one left in the filters would be sent and ignored.
    useEffect(() => {
        if (!showSubject && filters?.subject) {
            commit({...filters, subject: ""});
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [showSubject, filters?.subject]);

    return (
        <FlexBox justifyContent="space-between" alignItems="center">
            <Formik
                enableReinitialize
                initialValues={{
                    classGroup: filters?.classGroup || "",
                    subject: filters?.subject || "",
                    period: filters?.period || ""
                }}
                onSubmit={() => {
                }}
            >
                {({values, setFieldValue}) => (
                    <div style={{
                        display: "flex", flexDirection: "row",
                        marginTop: 25, marginBottom: 25, marginLeft: 15
                    }}>
                        <div style={{width: 220}}>
                            <FormikAutocomplete
                                name="classGroup"
                                multiple={false}
                                label="კლასი"
                                onFetch={onClasses}
                                getOptionLabel={(o) => `${o.name} — ${o.schoolName}`}
                                getOptionSelected={(o, v) => o.id === v.id}
                                onChange={(event, value) => {
                                    // Subject and period belong to the class, so
                                    // keeping them across a change would offer a
                                    // period from another scheme.
                                    setFieldValue("classGroup", value);
                                    setFieldValue("subject", "");
                                    setFieldValue("period", "");
                                    commit({classGroup: value, subject: "", period: ""});
                                }}
                            />
                        </div>

                        <div style={{
                            marginLeft: 20, width: 240,
                            display: showSubject ? undefined : "none"
                        }}>
                            <FormikAutocomplete
                                name="subject"
                                multiple={false}
                                label="საგანი"
                                disabled={!values.classGroup}
                                onFetch={onSubjects}
                                getOptionLabel={(o) => o.name}
                                getOptionSelected={(o, v) => o.id === v.id}
                                onChange={(event, value) => {
                                    setFieldValue("subject", value);
                                    commit({...values, subject: value});
                                }}
                            />
                        </div>

                        <div style={{
                            marginLeft: 20, width: 200,
                            display: singlePeriod ? "none" : undefined
                        }}>
                            <FormikAutocomplete
                                name="period"
                                multiple={false}
                                label="პერიოდი"
                                disabled={!values.classGroup}
                                onFetch={onPeriods}
                                getOptionLabel={(o) => o.label}
                                getOptionSelected={(o, v) => o.id === v.id}
                                onChange={(event, value) => {
                                    setFieldValue("period", value);
                                    commit({...values, period: value});
                                }}
                            />
                        </div>
                    </div>
                )}
            </Formik>

            <div style={{marginRight: 25, display: "flex", alignItems: "center", gap: 16}}>
                <SaveStatus status={status} pending={pending} onFlush={onFlush}/>

                {/* Only where a scale is actually configured. The label says
                    what the switch does rather than naming a scale, because a
                    grid can carry more than one. */}
                {convertible ? (
                    <FormControlLabel
                        control={
                            <Switch
                                size="small"
                                checked={Boolean(converted)}
                                onChange={(e) => onConvertedChange(e.target.checked)}
                            />
                        }
                        label={
                            <span style={{fontSize: 13, color: converted ? "#8a6d3b" : "#5b7c8d"}}>
                                {converted ? "გადაყვანილი შკალა (რედაქტირება გამორთულია)"
                                    : "გადაყვანილი შკალა"}
                            </span>
                        }
                        style={{marginRight: 0}}
                    />
                ) : null}

                {canExport ? <ExportMenu
                    classGroup={filters?.classGroup}
                    subject={filters?.subject}
                    period={filters?.period}
                    columns={columns}
                    journalUuid={journalUuid}
                    convertible={convertible}
                    // Exporting mid-flush would hand back a spreadsheet without
                    // the marks the teacher has just typed.
                    disabled={!filters?.classGroup || !filters?.period
                        || status === "dirty" || status === "saving"
                        || status === "error"}
                    onError={onError}
                /> : null}
                {canPublish ? (
                    <PublishButton
                        classGroup={filters?.classGroup}
                        period={filters?.period}
                        subject={filters?.subject}
                        journalUuid={journalUuid}
                        // Publishing mid-flush would release a value the server
                        // has not been told about yet.
                        // "error" too: a failed flush means the server never
                        // received marks the teacher can see on screen.
                        disabled={!filters?.classGroup || !filters?.period
                            || status === "dirty" || status === "saving"
                            || status === "error"}
                        onPublished={onPublished}
                        onError={onError}
                    />
                ) : null}
            </div>
        </FlexBox>
    );
};

export default GradebookToolbar;
