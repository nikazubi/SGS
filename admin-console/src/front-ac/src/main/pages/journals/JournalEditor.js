import React, {useCallback, useEffect, useMemo, useState} from "react";
import {useQuery} from "react-query";
import {
    AppBar,
    Button,
    Chip,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    IconButton,
    Toolbar,
    Typography
} from "@mui/material";
import {Close, PublishedWithChanges, Save} from "@mui/icons-material";
import ColumnEditor from "./ColumnEditor";
import MigrationPrompt from "./MigrationPrompt";
import {activateVersion, fetchPickableColumns, fetchStructure, saveStructure} from "./journalApi";
import {fetchClasses, fetchPeriods} from "../gradebook/gradebookApi";

/**
 * Editing a journal's columns.
 *
 * Saving a version that already holds marks forks a draft rather than editing
 * in place — changing a live version would silently re-render marks entered
 * under it, which is the failure the whole versioning scheme exists to prevent.
 * The word "version" only surfaces when that happens, or when a period is
 * pinned and someone is being asked about recalculating.
 *
 * Once the journal holds marks this school year, only how the columns look can
 * change - names, group headings, order, the summary tick - and the server
 * applies that in place to every class. Everything else is locked here and
 * refused there. Mid-term restructuring split the school's classes across five
 * versions in November 2026.
 */
const JournalEditor = ({journal, onClose, onSaved, onError, onNotify}) => {

    const uuid = journal?.uuid;

    const {data: structure, refetch} = useQuery(
        ["JOURNAL_STRUCTURE", uuid], () => fetchStructure(uuid),
        {enabled: Boolean(uuid), refetchOnWindowFocus: false});

    const {data: pickable, refetch: refetchPickable} = useQuery(
        ["JOURNAL_COLUMNS", uuid], () => fetchPickableColumns(uuid),
        {enabled: Boolean(uuid), refetchOnWindowFocus: false});

    const {data: periods} = useQuery(["EDITOR_PERIODS"], async () => {
        const classes = await fetchClasses();
        return classes.length ? fetchPeriods(classes[0].id) : [];
    }, {refetchOnWindowFocus: false});

    const [columns, setColumns] = useState([]);
    const [issues, setIssues] = useState([]);
    const [dirty, setDirty] = useState(false);
    const [busy, setBusy] = useState(false);
    const [versionId, setVersionId] = useState(null);
    const [activatable, setActivatable] = useState(false);
    const [migrationOpen, setMigrationOpen] = useState(false);
    // What the server last said: which columns hold marks, under which names.
    const [saved, setSaved] = useState(null);
    const [confirmRename, setConfirmRename] = useState(false);

    useEffect(() => {
        if (!structure) return;
        setColumns(structure.components || []);
        setVersionId(structure.versionId);
        setSaved(structure);
        setIssues([]);
        setDirty(false);
    }, [structure]);

    const locked = Boolean(saved?.holdsMarks);
    const marksByCode = useMemo(() => saved?.marksByCode || {}, [saved]);
    const savedLabels = useMemo(() => {
        const map = {};
        (saved?.components || []).forEach(c => {
            map[c.code] = c.label || "";
        });
        return map;
    }, [saved]);

    // Renaming keeps a column's marks, under the new name - which is how a
    // column can quietly come to mean something else. Allowed, after a warning.
    const renamed = useMemo(() => columns
        .filter(c => c.code && (marksByCode[c.code] || 0) > 0
            && savedLabels[c.code] !== undefined
            && (c.label || "") !== savedLabels[c.code])
        .map(c => ({
            code: c.code, from: savedLabels[c.code], to: c.label || "",
            marks: marksByCode[c.code]
        })), [columns, marksByCode, savedLabels]);

    const issuesByCode = useMemo(() => {
        const map = {};
        issues.forEach(issue => {
            const key = issue.componentCode || "";
            (map[key] = map[key] || []).push(issue);
        });
        return map;
    }, [issues]);

    const errors = issues.filter(i => i.severity === "ERROR");
    const warnings = issues.filter(i => i.severity === "WARNING");

    const doSave = useCallback(async () => {
        setConfirmRename(false);
        setBusy(true);
        try {
            const result = await saveStructure({uuid, versionId, components: columns});
            setIssues(result.issues || []);
            setActivatable(result.activatable);
            setVersionId(result.version.versionId);
            setColumns(result.version.components || []);
            setSaved(result.version);
            setDirty(false);
            // Columns added in this sitting have to become referenceable now,
            // not after the editor is closed and reopened.
            refetchPickable();

            onNotify({
                message: result.forked
                    ? `შენახულია ახალ ვერსიად (${result.version.versionNo}) — `
                    + "არსებული შეფასებები ხელუხლებელი დარჩა"
                    : result.version.holdsMarks
                        ? "შენახულია — ცვლილება ყველა კლასში გამოჩნდა"
                        : "შენახულია",
                severity: "success"
            });
            onSaved();
        } catch (e) {
            onError(e);
        } finally {
            setBusy(false);
        }
    }, [uuid, versionId, columns, onNotify, onSaved, onError, refetchPickable]);

    const save = useCallback(() => {
        if (renamed.length) {
            setConfirmRename(true);
        } else {
            doSave();
        }
    }, [renamed, doSave]);

    const activate = useCallback(async () => {
        setBusy(true);
        try {
            await activateVersion({uuid, versionId});
            onNotify({
                message: "გააქტიურდა — ახალი პერიოდები ამ ვერსიით შეივსება",
                severity: "success"
            });
            onSaved();
            refetch();
            // Existing periods stay where they were; moving one is deliberate.
            setMigrationOpen(true);
        } catch (e) {
            onError(e);
        } finally {
            setBusy(false);
        }
    }, [uuid, versionId, onNotify, onSaved, onError, refetch]);

    return (
        <Dialog fullScreen open={Boolean(journal)} onClose={onClose}>
            <AppBar position="sticky" color="default" elevation={1}>
                <Toolbar>
                    <IconButton edge="start" onClick={onClose}><Close/></IconButton>

                    <div style={{flex: 1, marginLeft: 12}}>
                        <Typography variant="h6">{journal?.name}</Typography>
                        <Typography variant="caption" style={{color: "#666"}}>
                            {structure
                                ? `ვერსია ${structure.versionNo} · ${structure.status}`
                                : ""}
                            {structure && !structure.editableInPlace && !locked
                                ? " · შენახვა შექმნის ახალ ვერსიას"
                                : ""}
                        </Typography>
                    </div>

                    {errors.length
                        ? <Chip size="small" color="error"
                                label={`${errors.length} შეცდომა`} style={{marginRight: 8}}/>
                        : null}
                    {warnings.length
                        ? <Chip size="small" color="warning"
                                label={`${warnings.length} გაფრთხილება`}
                                style={{marginRight: 8}}/>
                        : null}

                    <Button startIcon={<Save/>} disabled={busy || !dirty} onClick={save}
                            style={{textTransform: "none", marginRight: 8}}>
                        შენახვა
                    </Button>
                    <Button
                        variant="contained" startIcon={<PublishedWithChanges/>}
                        disabled={busy || dirty || !activatable || locked}
                        onClick={activate}
                        style={{textTransform: "none"}}
                    >
                        გააქტიურება
                    </Button>
                </Toolbar>
            </AppBar>

            <div style={{padding: 24, maxWidth: 1100, margin: "0 auto", width: "100%"}}>
                {locked ? (
                    <div style={{
                        background: "#eef6fb", border: "1px solid #9cc7e0",
                        borderRadius: 4, padding: 12, marginBottom: 16
                    }}>
                        <Typography variant="body2" style={{color: "#31708f"}}>
                            ამ სასწავლო წელს ჟურნალში უკვე შეყვანილია ნიშნები. სვეტების
                            დამატება, წაშლა და ტიპის, შკალის ან ფორმულის შეცვლა აღარ
                            შეიძლება. შეგიძლიათ შეცვალოთ დასახელება, ჯგუფი, რიგი და
                            შემაჯამებელი სვეტი — ცვლილება ყველა კლასში მაშინვე გამოჩნდება.
                        </Typography>
                    </div>
                ) : null}

                {errors.length ? (
                    <div style={{
                        background: "#fdf2f2", border: "1px solid #d9534f",
                        borderRadius: 4, padding: 12, marginBottom: 16
                    }}>
                        {errors.map((issue, i) => (
                            <Typography key={i} variant="body2" style={{color: "#a94442"}}>
                                {issue.componentCode ? `${issue.componentCode}: ` : ""}
                                {issue.message}
                            </Typography>
                        ))}
                    </div>
                ) : null}

                {warnings.length ? (
                    <div style={{
                        background: "#fff8e6", border: "1px solid #e0c27a",
                        borderRadius: 4, padding: 12, marginBottom: 16
                    }}>
                        {warnings.map((issue, i) => (
                            <Typography key={i} variant="body2" style={{color: "#8a6d3b"}}>
                                {issue.componentCode ? `${issue.componentCode}: ` : ""}
                                {issue.message}
                            </Typography>
                        ))}
                    </div>
                ) : null}

                {journal ? (
                    <ColumnEditor
                        columns={columns}
                        onChange={(next) => {
                            setColumns(next);
                            setDirty(true);
                        }}
                        journal={journal}
                        pickable={pickable}
                        periods={periods}
                        issuesByCode={issuesByCode}
                        locked={locked}
                        marksByCode={marksByCode}
                        savedLabels={savedLabels}
                    />
                ) : null}
            </div>

            <Dialog open={confirmRename} onClose={() => setConfirmRename(false)}
                    maxWidth="sm" fullWidth>
                <DialogTitle>სვეტის სახელის შეცვლა</DialogTitle>
                <DialogContent>
                    <Typography variant="body2" style={{marginBottom: 12}}>
                        ამ სვეტებში უკვე შეყვანილი ნიშნები დარჩება და ახალი სახელით
                        გამოჩნდება, ყველა კლასში:
                    </Typography>
                    {renamed.map(r => (
                        <Typography key={r.code} variant="body2" style={{marginBottom: 4}}>
                            „{r.from}“ → „{r.to}“ — {r.marks} შეფასება
                        </Typography>
                    ))}
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setConfirmRename(false)}>გაუქმება</Button>
                    <Button variant="contained" onClick={doSave}>შენახვა</Button>
                </DialogActions>
            </Dialog>

            <MigrationPrompt
                journal={migrationOpen ? journal : null}
                onClose={() => setMigrationOpen(false)}
                onError={onError}
                onNotify={onNotify}
            />
        </Dialog>
    );
};

export default JournalEditor;
