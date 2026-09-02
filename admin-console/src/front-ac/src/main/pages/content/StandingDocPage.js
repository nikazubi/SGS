import React, {useEffect, useState} from "react";
import {useQuery} from "react-query";
import {Button, Chip, IconButton, Paper, TextField, Tooltip, Typography} from "@mui/material";
import {Add, Delete} from "@mui/icons-material";
import {Formik} from "formik";
import FormikAutocomplete from "../../components/formik/FormikAutocomplete";
import {getFiltersOfPage, setFiltersOfPage} from "../../../utils/filters";
import {useNotification} from "../../../contexts/notification-context";
import {fetchClasses} from "../gradebook/gradebookApi";
import {fetchStanding, publishStanding, saveStanding} from "./contentApi";

/**
 * The daily schedule and the meal menu.
 *
 * One page for both — they are the same document with a different number of
 * columns per row, so the only difference is `withTime`.
 *
 * The school confirmed these are entered once for the year and adjusted
 * occasionally: no months, no trimesters, no weekly versions. So there is
 * exactly one document per class and no list to page through.
 *
 * Five weekday cards, three across then two, as the school described it.
 */

const WEEKDAYS = [
    {n: 1, label: "ორშაბათი"},
    {n: 2, label: "სამშაბათი"},
    {n: 3, label: "ოთხშაბათი"},
    {n: 4, label: "ხუთშაბათი"},
    {n: 5, label: "პარასკევი"}
];

const StandingDocPage = ({kind, title, withTime}) => {

    const pageId = `STANDING_${kind.toUpperCase()}`;
    const {setErrorMessage, setNotification} = useNotification();
    const [filters, setFilters] = useState({...getFiltersOfPage(pageId)});
    const [lines, setLines] = useState([]);
    const [uuid, setUuid] = useState(null);
    const [status, setStatus] = useState(null);
    const [pending, setPending] = useState(false);
    const [busy, setBusy] = useState(false);

    const classGroupId = filters?.classGroup?.id;

    const {data: document, refetch} = useQuery(
        [pageId, classGroupId],
        () => fetchStanding(kind, classGroupId),
        {enabled: Boolean(classGroupId), onError: setErrorMessage});

    // A class with no document yet returns null, which is a new one rather than
    // an error - so the page opens ready to type either way.
    useEffect(() => {
        setUuid(document?.uuid || null);
        setLines(document?.lines || []);
        setStatus(document?.status || null);
        setPending(Boolean(document?.hasUnpublishedChanges));
    }, [document]);

    const commit = (values) => {
        setFilters(values);
        setFiltersOfPage(pageId, values);
    };

    const linesOf = (weekday) => lines
        .filter(l => l.weekday === weekday)
        .sort((a, b) => a.ordinal - b.ordinal);

    const addLine = (weekday) => setLines(current => [
        ...current,
        {
            weekday, ordinal: current.filter(l => l.weekday === weekday).length,
            timeText: "", text: ""
        }
    ]);

    const setLine = (weekday, ordinal, patch) => setLines(current => current.map(
        l => l.weekday === weekday && l.ordinal === ordinal ? {...l, ...patch} : l));

    /** Renumbered on removal, so ordinals stay dense and the order is stable. */
    const removeLine = (weekday, ordinal) => setLines(current => {
        const kept = current.filter(l => !(l.weekday === weekday && l.ordinal === ordinal));
        return kept.map(l => l.weekday !== weekday ? l : {
            ...l,
            ordinal: kept.filter(o => o.weekday === weekday).indexOf(l)
        });
    });

    const run = async (action, message) => {
        setBusy(true);
        try {
            await action();
            await refetch();
            setNotification({message, severity: "success"});
        } catch (e) {
            setErrorMessage(e);
        } finally {
            setBusy(false);
        }
    };

    const save = () => run(
        () => saveStanding(kind, {uuid, classGroupId, lines}),
        "შენახულია");

    const publish = () => run(async () => {
        // Saved first: publishing without it would release the previously saved
        // text while the screen shows something else.
        const saved = await saveStanding(kind, {uuid, classGroupId, lines});
        await publishStanding(kind, saved.uuid);
    }, "გამოქვეყნებულია");

    return (
        <div style={{margin: "0 15px"}}>
            <div style={{
                display: "flex", justifyContent: "space-between",
                alignItems: "center", margin: "20px 0"
            }}>
                <div style={{display: "flex", gap: 16, alignItems: "center"}}>
                    <Typography variant="h6">{title}</Typography>
                    <Formik initialValues={{classGroup: filters?.classGroup || ""}} enableReinitialize onSubmit={() => {
                    }}>
                        {({values, setFieldValue}) => (
                            <FormikAutocomplete
                                name="classGroup"
                                label="კლასი"
                                style={{width: 220}}
                                onFetch={fetchClasses}
                                multiple={false}
                                getOptionSelected={(o, v) => o?.id === v?.id}
                                getOptionLabel={(o) => o?.name || ""}
                                onChange={(event, value) => {
                                    setFieldValue("classGroup", value);
                                    commit({...values, classGroup: value});
                                }}
                            />
                        )}
                    </Formik>
                    <StateChip status={status} pending={pending}/>
                </div>

                {classGroupId ? (
                    <div style={{display: "flex", gap: 12}}>
                        <Button disabled={busy} onClick={save}
                                style={{textTransform: "none"}}>შენახვა</Button>
                        <Button variant="contained" disabled={busy} onClick={publish}
                                style={{textTransform: "none"}}>გამოქვეყნება</Button>
                    </div>
                ) : null}
            </div>

            {!classGroupId ? (
                <Typography variant="body2" style={{color: "#888", padding: 24}}>
                    აირჩიეთ კლასი.
                </Typography>
            ) : (
                /* Three across then two, as the school described the layout. */
                <div style={{display: "grid", gridTemplateColumns: "repeat(3, 1fr)", gap: 16}}>
                    {WEEKDAYS.map(day => (
                        <Paper key={day.n} style={{padding: 12}}>
                            <Typography variant="subtitle2" style={{marginBottom: 8}}>
                                {day.label}
                            </Typography>

                            {linesOf(day.n).map(line => (
                                <div key={line.ordinal}
                                     style={{display: "flex", gap: 8, marginBottom: 8}}>
                                    {withTime ? (
                                        <TextField
                                            size="small" label="დრო" style={{width: 110}}
                                            value={line.timeText || ""}
                                            onChange={(e) => setLine(day.n, line.ordinal,
                                                {timeText: e.target.value})}
                                        />
                                    ) : null}
                                    <TextField
                                        size="small" fullWidth
                                        value={line.text || ""}
                                        onChange={(e) => setLine(day.n, line.ordinal,
                                            {text: e.target.value})}
                                    />
                                    <IconButton size="small"
                                                onClick={() => removeLine(day.n, line.ordinal)}>
                                        <Delete fontSize="small"/>
                                    </IconButton>
                                </div>
                            ))}

                            <Button size="small" startIcon={<Add/>}
                                    onClick={() => addLine(day.n)}
                                    style={{textTransform: "none"}}>
                                დამატება
                            </Button>
                        </Paper>
                    ))}
                </div>
            )}
        </div>
    );
};

/** The same three states as homework: the school publishes deliberately. */
const StateChip = ({status, pending}) => {
    if (!status) {
        return null;
    }
    if (status !== "PUBLISHED") {
        return <Chip size="small" label="შენახული" style={{backgroundColor: "#eceff1"}}/>;
    }
    if (pending) {
        return (
            <Tooltip title="შეიცვალა გამოქვეყნების შემდეგ — მშობელი ჯერ ძველ ვერსიას ხედავს">
                <Chip size="small" label="ცვლილება გამოსაქვეყნებელია"
                      style={{backgroundColor: "#fff3cd", color: "#8a6d3b"}}/>
            </Tooltip>
        );
    }
    return <Chip size="small" label="გამოქვეყნებული"
                 style={{backgroundColor: "#ddf1e5", color: "#2e6b4f"}}/>;
};

export default StandingDocPage;
