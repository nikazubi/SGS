import React, {useEffect, useState} from "react";
import {useMutation, useQuery, useQueryClient} from "react-query";
import {
    Autocomplete, Button, Chip, Dialog, DialogActions, DialogContent,
    DialogTitle, IconButton, TextField, Typography
} from "@mui/material";
import {Add, Delete} from "@mui/icons-material";
import ReactQuill from "react-quill";
import "react-quill/dist/quill.snow.css";
import {archiveHomework, fetchHomeworkItem, publishHomework, saveHomework} from "./homeworkApi";
import {fetchStudents} from "../gradebook/gradebookApi";

/**
 * Writing one assignment.
 *
 * Save keeps it as a working document; Publish releases it. The school was
 * explicit that this is not the grade publish flow and needs nobody's sign-off —
 * but also that **any edit needs a re-publish**, so editing something already
 * published leaves parents on the old text until someone presses Publish again.
 * That is what the warning below exists to say.
 */

/** Exactly what the server's allowlist keeps. Offering more would be a lie. */
const TOOLBAR = [
    ["bold", "italic", "underline"],
    [{list: "ordered"}, {list: "bullet"}],
    [{header: [3, 4, false]}],
    ["blockquote", "link"],
    ["clean"]
];

const emptyDraft = (classGroupId, subjectId) => ({
    uuid: null,
    classGroupId,
    subjectId,
    eventDate: new Date().toISOString().slice(0, 10),
    title: "",
    bodyHtml: "",
    targetEnrollmentIds: [],
    links: []
});

const HomeworkEditor = ({
                            open, uuid, classGroupId, subjectId, subjectName,
                            onClose, onSaved, onError
                        }) => {

    const queryClient = useQueryClient();
    const [draft, setDraft] = useState(emptyDraft(classGroupId, subjectId));

    const {data: students} = useQuery(
        ["HOMEWORK_STUDENTS", classGroupId],
        () => fetchStudents(classGroupId),
        {enabled: open && Boolean(classGroupId), staleTime: 60000});

    const {data: existing} = useQuery(
        ["HOMEWORK_ITEM", uuid],
        () => fetchHomeworkItem(uuid),
        {enabled: open && Boolean(uuid)});

    // The dialog stays mounted, so initial state runs once. Without this an
    // edit would open showing whatever was last typed — the same fault the
    // journal settings dialog had, where it silently un-published journals.
    useEffect(() => {
        if (!open) return;
        if (uuid && existing) {
            setDraft({
                uuid: existing.uuid,
                classGroupId: existing.classGroupId,
                subjectId: existing.subjectId,
                eventDate: existing.eventDate || "",
                title: existing.title || "",
                bodyHtml: existing.bodyHtml || "",
                targetEnrollmentIds: existing.targetEnrollmentIds || [],
                links: existing.links || []
            });
        } else if (!uuid) {
            setDraft(emptyDraft(classGroupId, subjectId));
        }
    }, [open, uuid, existing, classGroupId, subjectId]);

    const set = (patch) => setDraft(d => ({...d, ...patch}));

    const invalidate = () => {
        queryClient.invalidateQueries("HOMEWORK_LIST");
        queryClient.invalidateQueries(["HOMEWORK_ITEM", uuid]);
    };

    const save = useMutation(saveHomework, {
        onSuccess: (saved) => {
            invalidate();
            onSaved(saved);
            onClose();
        },
        onError
    });

    /**
     * Save, then publish, in one action.
     *
     * Publishing what is on screen requires saving it first — otherwise the
     * button would release the previously saved text and the teacher would have
     * no way to tell.
     */
    const saveAndPublish = useMutation(
        async (d) => {
            const saved = await saveHomework(d);
            return publishHomework(saved.uuid);
        },
        {
            onSuccess: (published) => {
                invalidate();
                onSaved(published);
                onClose();
            },
            onError
        });

    const remove = useMutation(() => archiveHomework({uuid: draft.uuid}), {
        onSuccess: () => {
            invalidate();
            onSaved(null);
            onClose();
        },
        onError
    });

    const setLink = (index, patch) => setDraft(d => ({
        ...d, links: d.links.map((l, i) => i === index ? {...l, ...patch} : l)
    }));

    const busy = save.isLoading || saveAndPublish.isLoading || remove.isLoading;
    const published = existing && existing.status === "PUBLISHED";
    const selected = (students || []).filter(
        s => draft.targetEnrollmentIds.includes(s.enrollmentId));

    return (
        <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
            <DialogTitle>
                {uuid ? "დავალების რედაქტირება" : "ახალი დავალება"}
                {subjectName ? (
                    <Typography variant="body2" style={{color: "#666"}}>{subjectName}</Typography>
                ) : null}
            </DialogTitle>

            <DialogContent>
                {published ? (
                    <Typography variant="caption"
                                style={{display: "block", color: "#8a6d3b", marginBottom: 12}}>
                        ეს დავალება უკვე გამოქვეყნებულია. ცვლილება მშობლამდე
                        მხოლოდ ხელახალი გამოქვეყნების შემდეგ მიდის.
                    </Typography>
                ) : null}

                <div style={{display: "flex", gap: 12, marginBottom: 16}}>
                    <TextField
                        type="date" size="small" label="თარიღი"
                        InputLabelProps={{shrink: true}}
                        value={draft.eventDate || ""}
                        onChange={(e) => set({eventDate: e.target.value})}
                    />
                    <TextField
                        fullWidth size="small" label="სათაური"
                        value={draft.title}
                        onChange={(e) => set({title: e.target.value})}
                    />
                </div>

                {/* Empty means the whole class, so "all" is the absence of a
                    selection rather than a magic entry in the list. */}
                <Autocomplete
                    multiple size="small"
                    options={students || []}
                    value={selected}
                    getOptionLabel={(s) => s.name}
                    isOptionEqualToValue={(a, b) => a.enrollmentId === b.enrollmentId}
                    onChange={(e, value) =>
                        set({targetEnrollmentIds: value.map(v => v.enrollmentId)})}
                    renderTags={(value, getTagProps) =>
                        value.map((s, i) => (
                            <Chip size="small" label={s.name} {...getTagProps({index: i})}
                                  key={s.enrollmentId}/>
                        ))}
                    renderInput={(params) => (
                        <TextField {...params} label="მოსწავლეები"
                                   placeholder={draft.targetEnrollmentIds.length === 0
                                       ? "მთელი კლასი" : ""}/>
                    )}
                    style={{marginBottom: 6}}
                />
                <Typography variant="caption" style={{color: "#666"}}>
                    ცარიელი — მთელი კლასი.
                </Typography>

                <div style={{marginTop: 16, marginBottom: 44}}>
                    <ReactQuill
                        theme="snow"
                        value={draft.bodyHtml}
                        onChange={(html) => set({bodyHtml: html})}
                        modules={{toolbar: TOOLBAR}}
                        style={{height: 220}}
                    />
                </div>

                {/* Links rather than uploads: the school's server is short of
                    space, so this is what an attachment is here. */}
                <Typography variant="body2" style={{marginTop: 8, marginBottom: 4}}>
                    ბმულები
                </Typography>
                {draft.links.map((link, i) => (
                    <div key={i} style={{display: "flex", gap: 8, marginBottom: 8}}>
                        <TextField
                            size="small" label="ბმული" style={{flex: 2}}
                            value={link.url || ""}
                            onChange={(e) => setLink(i, {url: e.target.value})}
                        />
                        <TextField
                            size="small" label="დასახელება" style={{flex: 1}}
                            value={link.label || ""}
                            onChange={(e) => setLink(i, {label: e.target.value})}
                        />
                        <IconButton size="small" onClick={() => setDraft(d => ({
                            ...d, links: d.links.filter((_, j) => j !== i)
                        }))}>
                            <Delete fontSize="small"/>
                        </IconButton>
                    </div>
                ))}
                <Button size="small" startIcon={<Add/>}
                        onClick={() => setDraft(d => ({
                            ...d, links: [...d.links, {url: "", label: ""}]
                        }))}
                        style={{textTransform: "none"}}>
                    ბმულის დამატება
                </Button>
                <Typography variant="caption" style={{display: "block", color: "#666"}}>
                    მხოლოდ http და https. სხვა ბმული არ შეინახება.
                </Typography>
            </DialogContent>

            <DialogActions>
                {draft.uuid ? (
                    <Button color="error" disabled={busy} onClick={() => remove.mutate()}
                            style={{marginRight: "auto"}}>
                        წაშლა
                    </Button>
                ) : null}
                <Button onClick={onClose} disabled={busy}>გაუქმება</Button>
                <Button disabled={busy} onClick={() => save.mutate(draft)}>შენახვა</Button>
                <Button variant="contained" disabled={busy}
                        onClick={() => saveAndPublish.mutate(draft)}>
                    გამოქვეყნება
                </Button>
            </DialogActions>
        </Dialog>
    );
};

export default HomeworkEditor;
