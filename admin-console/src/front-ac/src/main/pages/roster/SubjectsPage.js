import React, {useCallback, useState} from "react";
import {useQuery} from "react-query";
import {
    Button, Chip, Dialog, DialogActions, DialogContent, DialogTitle,
    FormControlLabel, IconButton, Switch, TextField, Tooltip, Typography
} from "@mui/material";
import {Add, Delete, Edit} from "@mui/icons-material";
import DataGridSGS from "../../components/grid/DataGrid";
import DataGridPaper from "../../components/grid/DataGridPaper";
import {useNotification} from "../../../contexts/notification-context";
import {useUserContext} from "../../../contexts/user-context";
import {deleteSubject, fetchSubjects, saveSubject} from "./rosterApi";

/**
 * The subjects the school teaches.
 *
 * A flat global list — the model makes the name unique on its own. Who teaches
 * a subject, and to which class, is not here: that lives on the class, because
 * the same subject is taught to different classes by different people.
 */
const SubjectsPage = () => {

    const {setErrorMessage, setNotification} = useNotification();
    const {hasPermission} = useUserContext();
    const canManage = hasPermission("MANAGE_SUBJECT");

    const [includeInactive, setIncludeInactive] = useState(false);
    const [editing, setEditing] = useState(null);

    const {data: subjects, isLoading, isError, refetch} = useQuery(
        ["ROSTER_SUBJECTS", includeInactive],
        () => fetchSubjects(includeInactive),
        {refetchOnWindowFocus: false});

    const remove = useCallback(async (row) => {
        try {
            await deleteSubject(row.id);
            setNotification({message: "საგანი წაიშალა", severity: "success"});
            refetch();
        } catch (e) {
            // The server refuses when a class still takes it, and says so. That
            // is the whole reason this page cannot orphan a class_subject row
            // the way the old one did.
            setErrorMessage(e);
        }
    }, [refetch, setErrorMessage, setNotification]);

    const columns = [
        {
            field: "name", headerName: "საგანი", flex: 2, sortable: false,
            renderCell: ({row}) => (
                <div>
                    <div>{row.name}</div>
                    {row.shortName ? (
                        <Typography variant="caption" color="text.secondary">
                            {row.shortName}
                        </Typography>
                    ) : null}
                </div>
            )
        },
        {
            field: "classCount", headerName: "კლასები", flex: 1, sortable: false,
            renderCell: ({row}) => row.classCount
        },
        {
            field: "active", headerName: "სტატუსი", flex: 1, sortable: false,
            renderCell: ({row}) => row.active
                ? <Chip size="small" color="success" label="აქტიური"/>
                : <Chip size="small" label="გამორთული"/>
        },
        {
            field: "actions", type: "actions", width: 110,
            getActions: ({row}) => canManage ? [
                <Tooltip title="რედაქტირება">
                    <IconButton size="small" onClick={() => setEditing(row)}>
                        <Edit fontSize="small"/>
                    </IconButton>
                </Tooltip>,
                <Tooltip title={row.classCount > 0
                    ? "საგანს კლასები სწავლობენ - წაშლა შეუძლებელია"
                    : "წაშლა"}>
                    <span>
                        <IconButton size="small" disabled={row.classCount > 0}
                                    onClick={() => remove(row)}>
                            <Delete fontSize="small"/>
                        </IconButton>
                    </span>
                </Tooltip>
            ] : []
        }
    ];

    return (
        <div style={{padding: 16}}>
            <div style={{display: "flex", alignItems: "center", gap: 16, marginBottom: 16}}>
                <Typography variant="h6" sx={{flexGrow: 1}}>საგნები</Typography>
                <FormControlLabel
                    control={<Switch size="small" checked={includeInactive}
                                     onChange={(e) => setIncludeInactive(e.target.checked)}/>}
                    label="გამორთულებიც"
                />
                {canManage ? (
                    <Button variant="contained" startIcon={<Add/>}
                            onClick={() => setEditing({})}>
                        ახალი საგანი
                    </Button>
                ) : null}
            </div>

            <DataGridPaper>
                <div style={{height: "calc(100vh - 220px)"}}>
                    <DataGridSGS
                        rows={subjects || []}
                        columns={columns}
                        loading={isLoading}
                        getRowId={(row) => row.id}
                        disableSelectionOnClick
                    />
                </div>
            </DataGridPaper>

            {isError ? (
                <Typography color="error" sx={{mt: 2}}>სია ვერ ჩაიტვირთა.</Typography>
            ) : null}

            {editing ? (
                <SubjectDialog
                    subject={editing}
                    onClose={() => setEditing(null)}
                    onSaved={() => {
                        setEditing(null);
                        refetch();
                    }}
                />
            ) : null}
        </div>
    );
};

const SubjectDialog = ({subject, onClose, onSaved}) => {
    const {setErrorMessage, setNotification} = useNotification();
    const [draft, setDraft] = useState({
        id: subject.id,
        name: subject.name || "",
        shortName: subject.shortName || "",
        active: subject.active !== false
    });
    const [saving, setSaving] = useState(false);

    const submit = async () => {
        setSaving(true);
        try {
            await saveSubject(draft);
            setNotification({message: "შენახულია", severity: "success"});
            onSaved();
        } catch (e) {
            setErrorMessage(e);
        } finally {
            setSaving(false);
        }
    };

    const set = (field) => (e) => setDraft({...draft, [field]: e.target.value});

    return (
        <Dialog open onClose={onClose} maxWidth="sm" fullWidth>
            <DialogTitle>{subject.id ? "საგნის რედაქტირება" : "ახალი საგანი"}</DialogTitle>
            <DialogContent>
                <TextField autoFocus fullWidth margin="dense" label="დასახელება"
                           value={draft.name} onChange={set("name")}/>
                <TextField fullWidth margin="dense" label="შემოკლებით"
                           helperText="სვეტის სათაურისთვის, სადაც სრული სახელი არ ეტევა"
                           value={draft.shortName} onChange={set("shortName")}/>
                <FormControlLabel
                    control={<Switch checked={draft.active}
                                     onChange={(e) => setDraft({...draft, active: e.target.checked})}/>}
                    label="აქტიური"
                />
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose}>გაუქმება</Button>
                <Button variant="contained" onClick={submit} disabled={saving || !draft.name.trim()}>
                    შენახვა
                </Button>
            </DialogActions>
        </Dialog>
    );
};

export default SubjectsPage;
