import React, {useCallback, useEffect, useState} from "react";
import {useQuery} from "react-query";
import {
    Button, Chip, Dialog, DialogActions, DialogContent, DialogTitle,
    FormControlLabel, IconButton, MenuItem, Switch, TextField, Tooltip, Typography
} from "@mui/material";
import {Add, Edit, History, Logout, SwapHoriz} from "@mui/icons-material";
import DataGridSGS from "../../components/grid/DataGrid";
import DataGridPaper from "../../components/grid/DataGridPaper";
import {useNotification} from "../../../contexts/notification-context";
import {useUserContext} from "../../../contexts/user-context";
import YearSelect, {currentYearId, useYears} from "./YearSelect";
import {
    fetchClassGroups, fetchPlacements, fetchStudents, leaveEnrollment, moveEnrollment, saveStudent
} from "./rosterApi";

const formatDate = (iso) => {
    if (!iso) return "";
    const [y, m, d] = iso.split("-");
    return `${d}.${m}.${y}`;
};

/**
 * The roster: who the children are, and which class each is in this year.
 *
 * This writes `sgs`, which is what the gradebook, the registers, homework and
 * the parent portal all read. The page it replaces wrote the legacy tables, so
 * a child added there was invisible to every one of them.
 */
const StudentsPage = () => {

    const {setErrorMessage, setNotification} = useNotification();
    const {hasPermission} = useUserContext();
    const canManage = hasPermission("MANAGE_STUDENT");

    const {data: years} = useYears();
    const [yearId, setYearId] = useState(null);
    const [classGroupId, setClassGroupId] = useState("");
    const [search, setSearch] = useState("");
    const [includeInactive, setIncludeInactive] = useState(false);

    const [editing, setEditing] = useState(null);
    const [moving, setMoving] = useState(null);
    const [leaving, setLeaving] = useState(null);
    const [historyFor, setHistoryFor] = useState(null);

    useEffect(() => {
        if (!yearId && years) setYearId(currentYearId(years));
    }, [years, yearId]);

    const {data: classes} = useQuery(["ROSTER_CLASSES", yearId],
        () => fetchClassGroups(yearId), {enabled: !!yearId, refetchOnWindowFocus: false});

    const {data: students, isLoading, isError, refetch} = useQuery(
        ["ROSTER_STUDENTS", yearId, classGroupId, search, includeInactive],
        () => fetchStudents({
            academicYearId: yearId,
            classGroupId: classGroupId === "" ? null : classGroupId,
            search,
            includeInactive
        }),
        {enabled: !!yearId, refetchOnWindowFocus: false, keepPreviousData: true});

    const columns = [
        {
            field: "name", headerName: "მოსწავლე", flex: 2, sortable: false,
            renderCell: ({row}) => (
                <div>
                    <div>{row.lastName} {row.firstName}</div>
                    <Typography variant="caption" color="text.secondary">
                        {row.personalNumber || "პირადი ნომრის გარეშე"}
                    </Typography>
                </div>
            )
        },
        {
            field: "className", headerName: "კლასი", flex: 1, sortable: false,
            renderCell: ({row}) => row.className
                ? <Chip size="small" label={row.className}/>
                : <Typography variant="caption" color="text.secondary">
                    ამ წელს ჩარიცხული არაა
                </Typography>
        },
        {
            field: "username", headerName: "მომხმარებელი", flex: 1, sortable: false,
            renderCell: ({row}) => row.username
        },
        {
            field: "guardianEmail", headerName: "მშობლის ელფოსტა", flex: 1.5, sortable: false,
            renderCell: ({row}) => row.guardianEmail || ""
        },
        {
            field: "status", headerName: "სტატუსი", flex: 1, sortable: false,
            renderCell: ({row}) => {
                if (!row.active) return <Chip size="small" label="გამორთული"/>;
                if (row.leftOn) {
                    return <Chip size="small" color="warning"
                                 label={`გავიდა ${formatDate(row.leftOn)}`}/>;
                }
                return <Chip size="small" color="success" label="აქტიური"/>;
            }
        },
        {
            field: "actions", type: "actions", width: 170,
            getActions: ({row}) => [
                <Tooltip title="კლასების ისტორია">
                    <IconButton size="small" onClick={() => setHistoryFor(row)}>
                        <History fontSize="small"/>
                    </IconButton>
                </Tooltip>,
                ...(canManage ? [
                    <Tooltip title="რედაქტირება">
                        <IconButton size="small" onClick={() => setEditing(row)}>
                            <Edit fontSize="small"/>
                        </IconButton>
                    </Tooltip>,
                    <Tooltip title={row.enrollmentId ? "სხვა კლასში გადაყვანა"
                        : "ჯერ ჩარიცხეთ კლასში"}>
                        <span>
                            <IconButton size="small" disabled={!row.enrollmentId || !!row.leftOn}
                                        onClick={() => setMoving(row)}>
                                <SwapHoriz fontSize="small"/>
                            </IconButton>
                        </span>
                    </Tooltip>,
                    <Tooltip title="სკოლიდან გასვლა">
                        <span>
                            <IconButton size="small" disabled={!row.enrollmentId || !!row.leftOn}
                                        onClick={() => setLeaving(row)}>
                                <Logout fontSize="small"/>
                            </IconButton>
                        </span>
                    </Tooltip>
                ] : [])
            ]
        }
    ];

    const afterChange = useCallback((message) => {
        setNotification({message, severity: "success"});
        refetch();
    }, [refetch, setNotification]);

    return (
        <div style={{padding: 16}}>
            <div style={{
                display: "flex", alignItems: "center", gap: 12, marginBottom: 16,
                flexWrap: "wrap"
            }}>
                <Typography variant="h6" sx={{flexGrow: 1}}>მოსწავლეები</Typography>

                <YearSelect value={yearId} years={years} onChange={setYearId}/>

                <TextField select size="small" label="კლასი" sx={{minWidth: 150}}
                           value={classGroupId}
                           onChange={(e) => setClassGroupId(e.target.value)}>
                    <MenuItem value="">ყველა</MenuItem>
                    {(classes || []).map(c => (
                        <MenuItem key={c.id} value={c.id}>{c.name}</MenuItem>
                    ))}
                </TextField>

                <TextField size="small" label="ძებნა" value={search}
                           placeholder="სახელი, გვარი, პირადი ნომერი"
                           onChange={(e) => setSearch(e.target.value)}
                           sx={{minWidth: 220}}/>

                <FormControlLabel
                    control={<Switch size="small" checked={includeInactive}
                                     onChange={(e) => setIncludeInactive(e.target.checked)}/>}
                    label="გამორთულებიც"
                />

                {canManage ? (
                    <Button variant="contained" startIcon={<Add/>}
                            onClick={() => setEditing({})}>
                        ახალი მოსწავლე
                    </Button>
                ) : null}
            </div>

            <DataGridPaper>
                <div style={{height: "calc(100vh - 240px)"}}>
                    <DataGridSGS
                        rows={students || []}
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
                <StudentDialog
                    student={editing}
                    yearId={yearId}
                    classes={classes}
                    onClose={() => setEditing(null)}
                    onSaved={() => {
                        setEditing(null);
                        afterChange("შენახულია");
                    }}
                />
            ) : null}

            {moving ? (
                <MoveDialog
                    student={moving}
                    classes={(classes || []).filter(c => c.id !== moving.classGroupId)}
                    onClose={() => setMoving(null)}
                    onDone={() => {
                        setMoving(null);
                        afterChange("მოსწავლე გადაყვანილია");
                    }}
                    onError={setErrorMessage}
                />
            ) : null}

            {leaving ? (
                <LeaveDialog
                    student={leaving}
                    onClose={() => setLeaving(null)}
                    onDone={() => {
                        setLeaving(null);
                        afterChange("მოსწავლე სკოლიდან გავიდა");
                    }}
                    onError={setErrorMessage}
                />
            ) : null}

            {historyFor ? (
                <HistoryDialog student={historyFor} yearId={yearId}
                               onClose={() => setHistoryFor(null)}/>
            ) : null}
        </div>
    );
};

// ---- the student form ------------------------------------------------------

/**
 * One field called "class", which is really the enrollment for the selected
 * year. Choosing one where there was none enrols; choosing a different one
 * moves, with the placement history to show for it.
 */
const StudentDialog = ({student, yearId, classes, onClose, onSaved}) => {
    const {setErrorMessage} = useNotification();
    const creating = !student.id;

    const [draft, setDraft] = useState({
        id: student.id,
        firstName: student.firstName || "",
        lastName: student.lastName || "",
        personalNumber: student.personalNumber || "",
        username: student.username || "",
        password: "",
        guardianEmail: student.guardianEmail || "",
        active: student.active !== false,
        classGroupId: student.classGroupId || "",
        joinedOn: ""
    });
    const [saving, setSaving] = useState(false);

    // Undo the browser's autofill.
    //
    // Chrome sees a text field followed by a password field, decides this is a
    // login form, and fills the signed-in administrator's own credentials into
    // a form that creates a *student*. It dispatches real input events doing
    // it, so React's state ends up holding them too and the Save button lights
    // up: press it without looking and the child's login is the admin's.
    //
    // autoComplete="off", autoComplete="new-password" and readonly-until-focus
    // were all tried and this Chrome ignored every one. What it cannot override
    // is state written after it has finished, so the fields are cleared a beat
    // after the dialog opens. Only on create - an edit legitimately shows the
    // saved username.
    useEffect(() => {
        if (!creating) return undefined;
        const timer = setTimeout(
            () => setDraft(d => ({...d, username: "", password: ""})), 400);
        return () => clearTimeout(timer);
    }, [creating]);

    const set = (field) => (e) => setDraft({...draft, [field]: e.target.value});

    const submit = async () => {
        setSaving(true);
        try {
            await saveStudent({
                academicYearId: yearId,
                draft: {
                    ...draft,
                    // Empty means "leave the password alone", which is what lets
                    // the edit form work without ever showing the current one.
                    password: draft.password.trim() === "" ? null : draft.password,
                    classGroupId: draft.classGroupId === "" ? null : Number(draft.classGroupId),
                    joinedOn: draft.joinedOn === "" ? null : draft.joinedOn
                }
            });
            onSaved();
        } catch (e) {
            setErrorMessage(e);
        } finally {
            setSaving(false);
        }
    };

    return (
        <Dialog open onClose={onClose} maxWidth="sm" fullWidth>
            <DialogTitle>{creating ? "ახალი მოსწავლე" : "მოსწავლის რედაქტირება"}</DialogTitle>
            <DialogContent>
                <div style={{display: "flex", gap: 8}}>
                    <TextField autoFocus fullWidth margin="dense" label="სახელი"
                               value={draft.firstName} onChange={set("firstName")}/>
                    <TextField fullWidth margin="dense" label="გვარი"
                               value={draft.lastName} onChange={set("lastName")}/>
                </div>

                <TextField fullWidth margin="dense" label="პირადი ნომერი"
                           helperText="უნიკალურია. ცარიელი დასაშვებია."
                           value={draft.personalNumber} onChange={set("personalNumber")}/>

                <div style={{display: "flex", gap: 8}}>
                    <NoAutofill fullWidth margin="dense" label="მომხმარებელი"
                                value={draft.username} onChange={set("username")}/>
                    <NoAutofill fullWidth margin="dense" label="პაროლი" type="password"
                                placeholder={creating ? "" : "უცვლელი"}
                                value={draft.password} onChange={set("password")}/>
                </div>
                {/* The rule, stated where somebody is about to break it. Two
                    siblings share a username by design; it is the pair that has
                    to differ. */}
                <Typography variant="caption" color="text.secondary">
                    მომხმარებელი შეიძლება დაემთხვეს სხვას (და-ძმებს ხშირად ერთი აქვთ) —
                    უნიკალური უნდა იყოს მომხმარებლისა და პაროლის წყვილი.
                </Typography>

                <TextField fullWidth margin="dense" label="მშობლის ელფოსტა"
                           value={draft.guardianEmail} onChange={set("guardianEmail")}/>

                <TextField select fullWidth margin="dense" label="კლასი"
                           helperText="ამ სასწავლო წლის ჩარიცხვა"
                           value={draft.classGroupId} onChange={set("classGroupId")}>
                    <MenuItem value="">კლასის გარეშე</MenuItem>
                    {(classes || []).map(c => (
                        <MenuItem key={c.id} value={c.id}>{c.name}</MenuItem>
                    ))}
                </TextField>

                {draft.classGroupId !== "" && draft.classGroupId !== student.classGroupId ? (
                    <TextField fullWidth margin="dense" type="date"
                               label={student.classGroupId ? "გადაყვანის თარიღი"
                                   : "ჩარიცხვის თარიღი"}
                               InputLabelProps={{shrink: true}}
                               value={draft.joinedOn} onChange={set("joinedOn")}
                               helperText="ცარიელი — წლის დასაწყისი"/>
                ) : null}

                <FormControlLabel
                    control={<Switch checked={draft.active}
                                     onChange={(e) => setDraft({...draft, active: e.target.checked})}/>}
                    label="აქტიური"
                />
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose}>გაუქმება</Button>
                <Button variant="contained" onClick={submit}
                        disabled={saving || !draft.firstName.trim() || !draft.lastName.trim()
                            || !draft.username.trim()
                            || (creating && !draft.password.trim())}>
                    შენახვა
                </Button>
            </DialogActions>
        </Dialog>
    );
};

/**
 * A field Chrome will not fill with the signed-in administrator's own login.
 *
 * This form has a text input followed by a password input, which is the shape
 * of a login form, so Chrome offers the credentials it has saved for this
 * origin - the admin's. Their username and password land in a form that creates
 * a *student*, and it holds them there against typing. Saved unnoticed, that
 * makes a child whose login is the administrator's.
 *
 * `autoComplete="off"` and `autoComplete="new-password"` were tried first and
 * Chrome ignored both, which is documented behaviour for login-shaped pairs.
 * What it does respect is readonly: it will not fill a field it cannot edit. So
 * the input starts readonly and unlocks on focus, which is invisible to anyone
 * typing into it and is the reason this component exists rather than a prop.
 */
const NoAutofill = ({onChange, ...rest}) => {
    const [unlocked, setUnlocked] = useState(false);
    return (
        <TextField
            {...rest}
            onChange={onChange}
            onFocus={() => setUnlocked(true)}
            InputProps={{readOnly: !unlocked}}
            inputProps={{autoComplete: "off"}}
        />
    );
};

// ---- dated events ----------------------------------------------------------

const MoveDialog = ({student, classes, onClose, onDone, onError}) => {
    const [classGroupId, setClassGroupId] = useState("");
    const [on, setOn] = useState("");
    const [saving, setSaving] = useState(false);

    const submit = async () => {
        setSaving(true);
        try {
            await moveEnrollment({
                enrollmentId: student.enrollmentId,
                classGroupId: Number(classGroupId),
                on: on === "" ? null : on
            });
            onDone();
        } catch (e) {
            onError(e);
        } finally {
            setSaving(false);
        }
    };

    return (
        <Dialog open onClose={onClose} maxWidth="xs" fullWidth>
            <DialogTitle>{student.lastName} {student.firstName} — გადაყვანა</DialogTitle>
            <DialogContent>
                <Typography variant="body2" color="text.secondary" sx={{mb: 2}}>
                    ნიშნები რჩება — ისინი ჩარიცხვას უკავშირდება და არა კლასს.
                    ძველი კლასის ჟურნალი გადაყვანამდე პერიოდზე უცვლელი დარჩება.
                </Typography>
                <TextField select fullWidth margin="dense" label="ახალი კლასი"
                           value={classGroupId}
                           onChange={(e) => setClassGroupId(e.target.value)}>
                    {classes.map(c => <MenuItem key={c.id} value={c.id}>{c.name}</MenuItem>)}
                </TextField>
                <TextField fullWidth margin="dense" type="date" label="თარიღი"
                           InputLabelProps={{shrink: true}}
                           helperText="ცარიელი — დღეს"
                           value={on} onChange={(e) => setOn(e.target.value)}/>
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose}>გაუქმება</Button>
                <Button variant="contained" onClick={submit} disabled={saving || !classGroupId}>
                    გადაყვანა
                </Button>
            </DialogActions>
        </Dialog>
    );
};

const LeaveDialog = ({student, onClose, onDone, onError}) => {
    const [on, setOn] = useState("");
    const [saving, setSaving] = useState(false);

    const submit = async () => {
        setSaving(true);
        try {
            await leaveEnrollment({enrollmentId: student.enrollmentId, on: on === "" ? null : on});
            onDone();
        } catch (e) {
            onError(e);
        } finally {
            setSaving(false);
        }
    };

    return (
        <Dialog open onClose={onClose} maxWidth="xs" fullWidth>
            <DialogTitle>{student.lastName} {student.firstName} — სკოლიდან გასვლა</DialogTitle>
            <DialogContent>
                <Typography variant="body2" color="text.secondary" sx={{mb: 2}}>
                    ჩანაწერი და ნიშნები რჩება — მოსწავლე კლასის სიიდან გადის.
                </Typography>
                <TextField fullWidth margin="dense" type="date" label="თარიღი"
                           InputLabelProps={{shrink: true}}
                           helperText="ცარიელი — დღეს"
                           value={on} onChange={(e) => setOn(e.target.value)}/>
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose}>გაუქმება</Button>
                <Button variant="contained" color="warning" onClick={submit} disabled={saving}>
                    დადასტურება
                </Button>
            </DialogActions>
        </Dialog>
    );
};

/** Where this child has sat, which is the whole reason the placement table exists. */
const HistoryDialog = ({student, yearId, onClose}) => {
    const {data: rows, isLoading} = useQuery(
        ["STUDENT_PLACEMENTS", student.id, yearId],
        () => fetchPlacements({studentId: student.id, academicYearId: yearId}),
        {refetchOnWindowFocus: false});

    return (
        <Dialog open onClose={onClose} maxWidth="xs" fullWidth>
            <DialogTitle>{student.lastName} {student.firstName} — კლასების ისტორია</DialogTitle>
            <DialogContent>
                {isLoading ? (
                    <Typography>…</Typography>
                ) : (rows || []).length === 0 ? (
                    <Typography color="text.secondary">ჩანაწერი არ არის.</Typography>
                ) : (
                    <ul style={{paddingLeft: 18, margin: 0}}>
                        {rows.map((row, i) => (
                            <li key={i} style={{marginBottom: 6}}>
                                <b>{row.className}</b>{" — "}
                                {formatDate(row.fromDate)}
                                {row.toDate ? ` – ${formatDate(row.toDate)}` : " – დღემდე"}
                            </li>
                        ))}
                    </ul>
                )}
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose}>დახურვა</Button>
            </DialogActions>
        </Dialog>
    );
};

export default StudentsPage;
