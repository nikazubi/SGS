import React, {useEffect, useState} from "react";
import {useMutation, useQuery, useQueryClient} from "react-query";
import {
    Accordion, AccordionDetails, AccordionSummary, Autocomplete, Button, Chip,
    Dialog, DialogActions, DialogContent, DialogTitle, IconButton, TextField,
    Tooltip, Typography
} from "@mui/material";
import {Add, Delete, Edit, ExpandMore} from "@mui/icons-material";
import {Formik} from "formik";
import ReactQuill from "react-quill";
import "react-quill/dist/quill.snow.css";
import FormikAutocomplete from "../../components/formik/FormikAutocomplete";
import {getFiltersOfPage, setFiltersOfPage} from "../../../utils/filters";
import {useNotification} from "../../../contexts/notification-context";
import {fetchClasses, fetchStudents, fetchSubjects} from "../gradebook/gradebookApi";
import {
    archiveCharacterization, fetchCharacterization, fetchCharacterizations,
    publishCharacterization, saveCharacterization
} from "./contentApi";

/**
 * Written accounts of a student, per subject.
 *
 * Homework's shape with fewer fields: a date, exactly one student, and a body.
 * No title — neither the brief's form nor the school's description has one.
 */

const PAGE_ID = "CHARACTERIZATION";
const TOP_N = 5;

const TOOLBAR = [
    ["bold", "italic", "underline"],
    [{list: "ordered"}, {list: "bullet"}],
    [{header: [3, 4, false]}],
    ["blockquote", "link"],
    ["clean"]
];

const CharacterizationPage = () => {

    const {setErrorMessage} = useNotification();
    const [filters, setFilters] = useState({...getFiltersOfPage(PAGE_ID)});
    const [editing, setEditing] = useState(null);

    const classGroupId = filters?.classGroup?.id;

    const commit = (values) => {
        setFilters(values);
        setFiltersOfPage(PAGE_ID, values);
    };

    const {data: subjects} = useQuery(
        ["CHAR_SUBJECTS", classGroupId],
        () => fetchSubjects(classGroupId),
        {enabled: Boolean(classGroupId)});

    return (
        <div style={{margin: "0 15px"}}>
            <div style={{display: "flex", gap: 16, alignItems: "center", margin: "20px 0"}}>
                <Typography variant="h6">მოსწავლის დახასიათება</Typography>
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
            </div>

            {!classGroupId ? (
                <Typography variant="body2" style={{color: "#888", padding: 24}}>
                    აირჩიეთ კლასი.
                </Typography>
            ) : (subjects || []).map(subject => (
                <Accordion key={subject.id} TransitionProps={{unmountOnExit: true}}>
                    <AccordionSummary expandIcon={<ExpandMore/>}>
                        <Typography>{subject.name}</Typography>
                    </AccordionSummary>
                    <AccordionDetails>
                        <Button size="small" startIcon={<Add/>}
                                onClick={() => setEditing({uuid: null, subject})}
                                style={{textTransform: "none", marginBottom: 8}}>
                            დამატება
                        </Button>
                        <ItemList
                            classGroupId={classGroupId}
                            subjectId={subject.id}
                            onEdit={(item) => setEditing({uuid: item.uuid, subject})}
                            onError={setErrorMessage}
                        />
                    </AccordionDetails>
                </Accordion>
            ))}

            <Editor
                open={Boolean(editing)}
                uuid={editing?.uuid}
                classGroupId={classGroupId}
                subject={editing?.subject}
                onClose={() => setEditing(null)}
                onError={setErrorMessage}
            />
        </div>
    );
};

const ItemList = ({classGroupId, subjectId, onEdit, onError}) => {

    const {data: items, refetch} = useQuery(
        ["CHAR_LIST", classGroupId, subjectId],
        () => fetchCharacterizations({classGroupId, subjectId, limit: TOP_N}),
        {enabled: Boolean(classGroupId), onError});

    const remove = async (item) => {
        try {
            await archiveCharacterization({uuid: item.uuid});
            refetch();
        } catch (e) {
            onError(e);
        }
    };

    if (!items || items.length === 0) {
        return (
            <Typography variant="body2" style={{color: "#888", padding: 8}}>
                ჯერ დახასიათება არ არის.
            </Typography>
        );
    }

    return (
        <div>
            {items.map(item => (
                <div key={item.uuid}
                     style={{
                         display: "flex", alignItems: "center", gap: 12,
                         padding: "6px 4px", borderBottom: "1px solid #f0f0f0"
                     }}>
                    <span style={{width: 96, color: "#5b7c8d"}}>{item.eventDate || "—"}</span>
                    <span style={{flex: 1}}>{(item.targetNames || [])[0] || "—"}</span>
                    <StateChip item={item}/>
                    <IconButton size="small" onClick={() => onEdit(item)}>
                        <Edit fontSize="small"/>
                    </IconButton>
                    <IconButton size="small" onClick={() => remove(item)}>
                        <Delete fontSize="small"/>
                    </IconButton>
                </div>
            ))}
        </div>
    );
};

const Editor = ({open, uuid, classGroupId, subject, onClose, onError}) => {

    const queryClient = useQueryClient();
    const [draft, setDraft] = useState({});

    const {data: students} = useQuery(
        ["CHAR_STUDENTS", classGroupId],
        () => fetchStudents(classGroupId),
        {enabled: open && Boolean(classGroupId), staleTime: 60000});

    const {data: existing} = useQuery(
        ["CHAR_ITEM", uuid], () => fetchCharacterization(uuid),
        {enabled: open && Boolean(uuid)});

    // The dialog stays mounted, so without this an edit opens showing whatever
    // was last typed.
    useEffect(() => {
        if (!open) return;
        if (uuid && existing) {
            setDraft({
                uuid: existing.uuid,
                classGroupId: existing.classGroupId,
                subjectId: existing.subjectId,
                eventDate: existing.eventDate || "",
                bodyHtml: existing.bodyHtml || "",
                targetEnrollmentIds: existing.targetEnrollmentIds || []
            });
        } else if (!uuid) {
            setDraft({
                uuid: null,
                classGroupId,
                subjectId: subject?.id,
                eventDate: new Date().toISOString().slice(0, 10),
                bodyHtml: "",
                targetEnrollmentIds: []
            });
        }
    }, [open, uuid, existing, classGroupId, subject]);

    const invalidate = () => {
        queryClient.invalidateQueries("CHAR_LIST");
        queryClient.invalidateQueries(["CHAR_ITEM", uuid]);
    };

    const save = useMutation(saveCharacterization,
        {
            onSuccess: () => {
                invalidate();
                onClose();
            }, onError
        });

    const saveAndPublish = useMutation(async (d) => {
        const saved = await saveCharacterization(d);
        return publishCharacterization(saved.uuid);
    }, {
        onSuccess: () => {
            invalidate();
            onClose();
        }, onError
    });

    const busy = save.isLoading || saveAndPublish.isLoading;
    // Exactly one student, which the server enforces too.
    const selected = (students || []).find(
        s => (draft.targetEnrollmentIds || [])[0] === s.enrollmentId) || null;
    const published = existing && existing.status === "PUBLISHED";

    return (
        <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
            <DialogTitle>
                {uuid ? "დახასიათების რედაქტირება" : "ახალი დახასიათება"}
                <Typography variant="body2" style={{color: "#666"}}>{subject?.name}</Typography>
            </DialogTitle>
            <DialogContent>
                {published ? (
                    <Typography variant="caption"
                                style={{display: "block", color: "#8a6d3b", marginBottom: 12}}>
                        უკვე გამოქვეყნებულია. ცვლილება მშობლამდე მხოლოდ ხელახალი
                        გამოქვეყნების შემდეგ მიდის.
                    </Typography>
                ) : null}

                <div style={{display: "flex", gap: 12, marginBottom: 16}}>
                    <TextField
                        type="date" size="small" label="თარიღი"
                        InputLabelProps={{shrink: true}}
                        value={draft.eventDate || ""}
                        onChange={(e) => setDraft(d => ({...d, eventDate: e.target.value}))}
                    />
                    <Autocomplete
                        size="small" style={{flex: 1}}
                        options={students || []}
                        value={selected}
                        getOptionLabel={(s) => s?.name || ""}
                        isOptionEqualToValue={(a, b) => a.enrollmentId === b.enrollmentId}
                        onChange={(e, value) => setDraft(d => ({
                            ...d, targetEnrollmentIds: value ? [value.enrollmentId] : []
                        }))}
                        renderInput={(params) => <TextField {...params} label="მოსწავლე"/>}
                    />
                </div>

                <div style={{marginBottom: 44}}>
                    <ReactQuill
                        theme="snow"
                        value={draft.bodyHtml || ""}
                        onChange={(html) => setDraft(d => ({...d, bodyHtml: html}))}
                        modules={{toolbar: TOOLBAR}}
                        style={{height: 240}}
                    />
                </div>
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose} disabled={busy}>გაუქმება</Button>
                <Button disabled={busy || !selected} onClick={() => save.mutate(draft)}>
                    შენახვა
                </Button>
                <Button variant="contained" disabled={busy || !selected}
                        onClick={() => saveAndPublish.mutate(draft)}>
                    გამოქვეყნება
                </Button>
            </DialogActions>
        </Dialog>
    );
};

const StateChip = ({item}) => {
    if (item.status !== "PUBLISHED") {
        return <Chip size="small" label="შენახული" style={{backgroundColor: "#eceff1"}}/>;
    }
    if (item.hasUnpublishedChanges) {
        return (
            <Tooltip title="შეიცვალა გამოქვეყნების შემდეგ">
                <Chip size="small" label="ცვლილება გამოსაქვეყნებელია"
                      style={{backgroundColor: "#fff3cd", color: "#8a6d3b"}}/>
            </Tooltip>
        );
    }
    return <Chip size="small" label="გამოქვეყნებული"
                 style={{backgroundColor: "#ddf1e5", color: "#2e6b4f"}}/>;
};

export default CharacterizationPage;
