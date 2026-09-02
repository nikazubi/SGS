import React, {useCallback, useEffect, useMemo, useState} from "react";
import {useQuery} from "react-query";
import {
    Button, Dialog, DialogActions, DialogContent, DialogTitle, IconButton,
    MenuItem, TextField, Tooltip, Typography
} from "@mui/material";
import {Add, ArrowDownward, ArrowUpward, Delete, Edit, MenuBook} from "@mui/icons-material";
import DataGridSGS from "../../components/grid/DataGrid";
import DataGridPaper from "../../components/grid/DataGridPaper";
import {useNotification} from "../../../contexts/notification-context";
import {useUserContext} from "../../../contexts/user-context";
import YearSelect, {currentYearId, useYears} from "./YearSelect";
import {
    addClassSubject, deleteClassGroup, fetchClassGroups, fetchClassSubjects, fetchSchools,
    fetchSubjects, removeClassSubject, reorderClassSubjects, saveClassGroup, updateClassSubject
} from "./rosterApi";

/**
 * Classes, and what each of them is taught.
 *
 * The second half has never had a screen at all: class_subject was written once
 * by the migration and reordered once by a script, so adding a subject to a
 * class has been a SQL statement — and the teacher names the whole console
 * displays have been unmaintainable since the day they were imported.
 */
const ClassesPage = () => {

    const {setErrorMessage, setNotification} = useNotification();
    const {hasPermission} = useUserContext();
    const canManage = hasPermission("MANAGE_ACADEMY_CLASS");

    const {data: years} = useYears();
    const [yearId, setYearId] = useState(null);
    const [editing, setEditing] = useState(null);
    const [subjectsFor, setSubjectsFor] = useState(null);

    useEffect(() => {
        if (!yearId && years) setYearId(currentYearId(years));
    }, [years, yearId]);

    const {data: classes, isLoading, isError, refetch} = useQuery(
        ["ROSTER_CLASSES", yearId],
        () => fetchClassGroups(yearId),
        {enabled: !!yearId, refetchOnWindowFocus: false});

    const remove = useCallback(async (row) => {
        try {
            await deleteClassGroup(row.id);
            setNotification({message: "კლასი წაიშალა", severity: "success"});
            refetch();
        } catch (e) {
            // Refused while children are in it — every mark, absence and
            // homework target hangs off their enrollments.
            setErrorMessage(e);
        }
    }, [refetch, setErrorMessage, setNotification]);

    const columns = [
        {
            field: "name", headerName: "კლასი", flex: 1, sortable: false,
            renderCell: ({row}) => row.name
        },
        {
            field: "schoolName", headerName: "სკოლა", flex: 1.5, sortable: false,
            renderCell: ({row}) => row.schoolName
        },
        {
            field: "level", headerName: "საფეხური", flex: 1, sortable: false,
            renderCell: ({row}) => row.level
        },
        {
            field: "studentCount", headerName: "მოსწავლეები", flex: 1, sortable: false,
            renderCell: ({row}) => row.studentCount
        },
        {
            field: "actions", type: "actions", width: 150,
            getActions: ({row}) => [
                <Tooltip title="საგნები და პედაგოგები">
                    <IconButton size="small" onClick={() => setSubjectsFor(row)}>
                        <MenuBook fontSize="small"/>
                    </IconButton>
                </Tooltip>,
                ...(canManage ? [
                    <Tooltip title="რედაქტირება">
                        <IconButton size="small" onClick={() => setEditing(row)}>
                            <Edit fontSize="small"/>
                        </IconButton>
                    </Tooltip>,
                    <Tooltip title={row.studentCount > 0
                        ? "კლასში მოსწავლეებია - ჯერ გადაიყვანეთ ისინი"
                        : "წაშლა"}>
                        <span>
                            <IconButton size="small" disabled={row.studentCount > 0}
                                        onClick={() => remove(row)}>
                                <Delete fontSize="small"/>
                            </IconButton>
                        </span>
                    </Tooltip>
                ] : [])
            ]
        }
    ];

    return (
        <div style={{padding: 16}}>
            <div style={{display: "flex", alignItems: "center", gap: 16, marginBottom: 16}}>
                <Typography variant="h6" sx={{flexGrow: 1}}>კლასები</Typography>
                <YearSelect value={yearId} years={years} onChange={setYearId}/>
                {canManage ? (
                    <Button variant="contained" startIcon={<Add/>}
                            onClick={() => setEditing({academicYearId: yearId})}>
                        ახალი კლასი
                    </Button>
                ) : null}
            </div>

            <DataGridPaper>
                <div style={{height: "calc(100vh - 220px)"}}>
                    <DataGridSGS
                        rows={classes || []}
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
                <ClassDialog
                    classGroup={editing}
                    yearId={yearId}
                    onClose={() => setEditing(null)}
                    onSaved={() => {
                        setEditing(null);
                        refetch();
                    }}
                />
            ) : null}

            {subjectsFor ? (
                <ClassSubjectsDialog
                    classGroup={subjectsFor}
                    canManage={canManage}
                    onClose={() => setSubjectsFor(null)}
                />
            ) : null}
        </div>
    );
};

// ---- the class itself ------------------------------------------------------

const ClassDialog = ({classGroup, yearId, onClose, onSaved}) => {
    const {setErrorMessage, setNotification} = useNotification();
    const {data: schools} = useQuery(["ROSTER_SCHOOLS"], fetchSchools,
        {refetchOnWindowFocus: false, staleTime: 5 * 60 * 1000});

    const [draft, setDraft] = useState({
        id: classGroup.id,
        name: classGroup.name || "",
        level: classGroup.level != null ? classGroup.level : "",
        schoolId: classGroup.schoolId || "",
        academicYearId: classGroup.academicYearId || yearId
    });
    const [saving, setSaving] = useState(false);

    const submit = async () => {
        setSaving(true);
        try {
            await saveClassGroup({
                ...draft,
                level: draft.level === "" ? null : Number(draft.level),
                schoolId: draft.schoolId === "" ? null : Number(draft.schoolId)
            });
            setNotification({message: "შენახულია", severity: "success"});
            onSaved();
        } catch (e) {
            setErrorMessage(e);
        } finally {
            setSaving(false);
        }
    };

    return (
        <Dialog open onClose={onClose} maxWidth="sm" fullWidth>
            <DialogTitle>{classGroup.id ? "კლასის რედაქტირება" : "ახალი კლასი"}</DialogTitle>
            <DialogContent>
                <TextField autoFocus fullWidth margin="dense" label="დასახელება"
                           helperText="მაგალითად 5ა — უნიკალური სკოლისა და წლის ფარგლებში"
                           value={draft.name}
                           onChange={(e) => setDraft({...draft, name: e.target.value})}/>
                <TextField select fullWidth margin="dense" label="სკოლა"
                           value={draft.schoolId}
                           onChange={(e) => setDraft({...draft, schoolId: e.target.value})}>
                    {(schools || []).map(s => (
                        <MenuItem key={s.id} value={s.id}>{s.name}</MenuItem>
                    ))}
                </TextField>
                <TextField fullWidth margin="dense" label="საფეხური" type="number"
                           helperText="კლასის ნომერი — 5ა-სთვის 5"
                           value={draft.level}
                           onChange={(e) => setDraft({...draft, level: e.target.value})}/>
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose}>გაუქმება</Button>
                <Button variant="contained" onClick={submit}
                        disabled={saving || !draft.name.trim() || !draft.schoolId}>
                    შენახვა
                </Button>
            </DialogActions>
        </Dialog>
    );
};

// ---- what the class is taught ----------------------------------------------

/**
 * The subject list, in teaching order, with a teacher against each.
 *
 * Teacher is two things and both are shown: a name, which is what 98 of the
 * school's teachers have, and optionally an account, which is what 3 of them
 * have. Requiring the account would make the screen unusable; dropping the name
 * would lose 95 teachers.
 */
const ClassSubjectsDialog = ({classGroup, canManage, onClose}) => {
    const {setErrorMessage, setNotification} = useNotification();
    const [adding, setAdding] = useState(false);

    const {data: rows, isLoading, refetch} = useQuery(
        ["CLASS_SUBJECTS", classGroup.id],
        () => fetchClassSubjects(classGroup.id),
        {refetchOnWindowFocus: false});

    const {data: allSubjects} = useQuery(["ROSTER_SUBJECTS", false],
        () => fetchSubjects(false), {refetchOnWindowFocus: false});

    const available = useMemo(() => {
        const taken = new Set((rows || []).map(r => r.subjectId));
        return (allSubjects || []).filter(s => !taken.has(s.id));
    }, [rows, allSubjects]);

    const move = async (index, delta) => {
        const ids = (rows || []).map(r => r.id);
        const target = index + delta;
        if (target < 0 || target >= ids.length) return;
        [ids[index], ids[target]] = [ids[target], ids[index]];
        try {
            await reorderClassSubjects({classGroupId: classGroup.id, classSubjectIds: ids});
            refetch();
        } catch (e) {
            setErrorMessage(e);
        }
    };

    const remove = async (row) => {
        try {
            await removeClassSubject(row.id);
            setNotification({message: "საგანი მოიხსნა", severity: "success"});
            refetch();
        } catch (e) {
            setErrorMessage(e);
        }
    };

    const setTeacher = async (row, teacherName) => {
        try {
            await updateClassSubject({
                classSubjectId: row.id,
                draft: {teacherName, teacherUserId: row.teacherUserId}
            });
            refetch();
        } catch (e) {
            setErrorMessage(e);
        }
    };

    return (
        <Dialog open onClose={onClose} maxWidth="md" fullWidth>
            <DialogTitle>{classGroup.name} — საგნები</DialogTitle>
            <DialogContent>
                {isLoading ? (
                    <Typography sx={{p: 2}}>…</Typography>
                ) : (rows || []).length === 0 ? (
                    <Typography sx={{p: 2}} color="text.secondary">
                        კლასს ჯერ საგნები არ აქვს.
                    </Typography>
                ) : (
                    <table style={{width: "100%", borderCollapse: "collapse"}}>
                        <thead>
                        <tr>
                            <th style={{textAlign: "left", padding: 8, width: 90}}>რიგი</th>
                            <th style={{textAlign: "left", padding: 8}}>საგანი</th>
                            <th style={{textAlign: "left", padding: 8}}>პედაგოგი</th>
                            <th style={{width: 48}}/>
                        </tr>
                        </thead>
                        <tbody>
                        {rows.map((row, index) => (
                            <tr key={row.id} style={{borderTop: "1px solid #eee"}}>
                                <td style={{padding: 8, whiteSpace: "nowrap"}}>
                                    <IconButton size="small" disabled={!canManage || index === 0}
                                                onClick={() => move(index, -1)}>
                                        <ArrowUpward fontSize="inherit"/>
                                    </IconButton>
                                    <IconButton size="small"
                                                disabled={!canManage || index === rows.length - 1}
                                                onClick={() => move(index, 1)}>
                                        <ArrowDownward fontSize="inherit"/>
                                    </IconButton>
                                </td>
                                <td style={{padding: 8}}>{row.subjectName}</td>
                                <td style={{padding: 8}}>
                                    <TeacherField row={row} canManage={canManage}
                                                  onCommit={setTeacher}/>
                                </td>
                                <td style={{padding: 8}}>
                                    {canManage ? (
                                        <IconButton size="small" onClick={() => remove(row)}>
                                            <Delete fontSize="small"/>
                                        </IconButton>
                                    ) : null}
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                )}

                {canManage && adding ? (
                    <AddSubjectRow
                        available={available}
                        onCancel={() => setAdding(false)}
                        onAdd={async (draft) => {
                            try {
                                await addClassSubject({classGroupId: classGroup.id, draft});
                                setAdding(false);
                                refetch();
                            } catch (e) {
                                setErrorMessage(e);
                            }
                        }}
                    />
                ) : null}
            </DialogContent>
            <DialogActions>
                {canManage && !adding ? (
                    <Button startIcon={<Add/>} onClick={() => setAdding(true)}
                            disabled={!available.length}>
                        საგნის დამატება
                    </Button>
                ) : null}
                <div style={{flexGrow: 1}}/>
                <Button onClick={onClose}>დახურვა</Button>
            </DialogActions>
        </Dialog>
    );
};

/** Committed on blur rather than per keystroke, so one edit is one request. */
const TeacherField = ({row, canManage, onCommit}) => {
    const [value, setValue] = useState(row.teacherName || "");
    useEffect(() => setValue(row.teacherName || ""), [row.teacherName]);

    return (
        <TextField
            size="small" variant="standard" fullWidth
            placeholder="სახელი გვარი"
            disabled={!canManage}
            value={value}
            onChange={(e) => setValue(e.target.value)}
            onBlur={() => {
                if ((row.teacherName || "") !== value) onCommit(row, value);
            }}
        />
    );
};

const AddSubjectRow = ({available, onAdd, onCancel}) => {
    const [subjectId, setSubjectId] = useState("");
    const [teacherName, setTeacherName] = useState("");

    return (
        <div style={{display: "flex", gap: 8, alignItems: "center", marginTop: 16}}>
            <TextField select size="small" label="საგანი" sx={{minWidth: 220}}
                       value={subjectId} onChange={(e) => setSubjectId(e.target.value)}>
                {available.map(s => <MenuItem key={s.id} value={s.id}>{s.name}</MenuItem>)}
            </TextField>
            <TextField size="small" label="პედაგოგი" value={teacherName}
                       onChange={(e) => setTeacherName(e.target.value)}/>
            <Button variant="contained" disabled={!subjectId}
                    onClick={() => onAdd({subjectId: Number(subjectId), teacherName})}>
                დამატება
            </Button>
            <Button onClick={onCancel}>გაუქმება</Button>
        </div>
    );
};

export default ClassesPage;
