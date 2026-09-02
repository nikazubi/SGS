import React, {useCallback, useEffect, useMemo, useState} from "react";
import {useQuery} from "react-query";
import {AppBar, Button, Chip, Dialog, IconButton, Toolbar, Typography} from "@mui/material";
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

    useEffect(() => {
        if (!structure) return;
        setColumns(structure.components || []);
        setVersionId(structure.versionId);
        setIssues([]);
        setDirty(false);
    }, [structure]);

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

    const save = useCallback(async () => {
        setBusy(true);
        try {
            const result = await saveStructure({uuid, versionId, components: columns});
            setIssues(result.issues || []);
            setActivatable(result.activatable);
            setVersionId(result.version.versionId);
            setColumns(result.version.components || []);
            setDirty(false);
            // Columns added in this sitting have to become referenceable now,
            // not after the editor is closed and reopened.
            refetchPickable();

            onNotify({
                message: result.forked
                    ? `შენახულია ახალ ვერსიად (${result.version.versionNo}) — `
                    + "არსებული შეფასებები ხელუხლებელი დარჩა"
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
                            {structure && !structure.editableInPlace
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
                        disabled={busy || dirty || !activatable}
                        onClick={activate}
                        style={{textTransform: "none"}}
                    >
                        გააქტიურება
                    </Button>
                </Toolbar>
            </AppBar>

            <div style={{padding: 24, maxWidth: 1100, margin: "0 auto", width: "100%"}}>
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
                    />
                ) : null}
            </div>

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
