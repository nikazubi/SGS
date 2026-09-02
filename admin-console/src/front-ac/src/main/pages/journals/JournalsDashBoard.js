import React, {useCallback, useMemo, useState} from "react";
import {useQuery, useQueryClient} from "react-query";
import {Button, Chip, IconButton, Tooltip, Typography} from "@mui/material";
import {Add, Archive, Edit, Settings, Straighten, Unarchive, Visibility}
    from "@mui/icons-material";
import DataGridSGS from "../../components/grid/DataGrid";
import DataGridPaper from "../../components/grid/DataGridPaper";
import {useNotification} from "../../../contexts/notification-context";
import {useUserContext} from "../../../contexts/user-context";
import {archiveJournal, fetchJournals} from "./journalApi";
import {fetchClasses, fetchPeriods} from "../gradebook/gradebookApi";
import JournalWizard from "./JournalWizard";
import JournalEditor from "./JournalEditor";
import JournalSettings from "./JournalSettings";
import ConversionFormulaDialog from "./ConversionFormulaDialog";

// Every JournalFrequency value, so nothing renders as a raw enum name. WEEK was
// here and is not a value; DAY was a value and was not here.
const FREQUENCY_LABELS = {
    ONCE_A_YEAR: "წელიწადში ერთხელ",
    TRIMESTER: "ტრიმესტრში ერთხელ",
    MONTH: "თვეში ერთხელ",
    DAY: "ყოველდღე"
};

/**
 * The journals index.
 *
 * Every grid the school has, and the button that makes another. Each one also
 * gets its own entry in the left menu — a journal is what a tab is, whereas
 * adding a column to one changes what an existing grid shows and adds nothing
 * to the menu.
 */
const JournalsDashBoard = () => {

    const {setErrorMessage, setNotification} = useNotification();
    const {hasPermission} = useUserContext();
    const canManage = hasPermission("MANAGE_TEMPLATES");

    const [wizardOpen, setWizardOpen] = useState(false);
    // The editor opens over the list rather than as its own tab: it is a modal
    // task, and the tab shell has no route to hand it.
    const [editing, setEditing] = useState(null);
    const [settingsFor, setSettingsFor] = useState(null);
    const [scalesOpen, setScalesOpen] = useState(false);

    // The index shows archived ones too, or the restore button below could
    // never be reached.
    const {data: journals, isLoading} = useQuery(
        ["JOURNALS", "all"], () => fetchJournals(true), {refetchOnWindowFocus: false});

    // Not this query's own refetch. The left menu is a second query - ["JOURNALS"],
    // without the archived ones - and a journal that has just been activated
    // belongs in it. Invalidating the prefix marks both stale, so the tab
    // appears when the journal does rather than a minute later.
    const queryClient = useQueryClient();
    const refetch = useCallback(
        () => queryClient.invalidateQueries("JOURNALS"), [queryClient]);

    // A new journal needs a period scheme; every class in a year shares one, so
    // the first class's is the school's.
    const {data: schemeId} = useQuery(["DEFAULT_SCHEME"], async () => {
        const classes = await fetchClasses();
        if (!classes.length) return null;
        await fetchPeriods(classes[0].id);
        return classes[0].periodSchemeId;
    }, {refetchOnWindowFocus: false});

    const toggleArchive = useCallback(async (row) => {
        try {
            await archiveJournal({uuid: row.uuid, archived: !row.archived});
            setNotification({
                message: row.archived ? "ჟურნალი დაბრუნდა" : "ჟურნალი დაარქივდა",
                severity: "success"
            });
            refetch();
        } catch (e) {
            setErrorMessage(e);
        }
    }, [refetch, setErrorMessage, setNotification]);

    const columns = useMemo(() => [
        {
            field: "name", headerName: "ჟურნალი", flex: 1, minWidth: 220, sortable: false,
            renderCell: ({row}) => (
                <div style={{opacity: row.archived ? 0.5 : 1}}>
                    <div style={{fontWeight: 500}}>
                        {row.name}
                        {row.archived ? " (დაარქივებული)" : ""}
                    </div>
                    {row.description
                        ? <div style={{fontSize: 12, color: "#888"}}>{row.description}</div>
                        : null}
                </div>
            )
        },
        {
            field: "frequency", headerName: "სიხშირე", width: 190, sortable: false,
            renderCell: ({row}) => FREQUENCY_LABELS[row.frequency] || row.frequency
        },
        {
            field: "subjectScoped", headerName: "დაყოფა", width: 190, sortable: false,
            renderCell: ({row}) => (
                <Chip size="small" variant="outlined"
                      label={row.subjectScoped ? "საგნების მიხედვით" : "მთელ კლასზე"}/>
            )
        },
        {
            field: "columnCount", headerName: "სვეტები", width: 100, sortable: false,
            align: "center", headerAlign: "center"
        },
        {
            field: "parentVisible", headerName: "მშობლები", width: 110, sortable: false,
            align: "center", headerAlign: "center",
            renderCell: ({row}) => row.parentVisible
                ? <Tooltip title="მშობლები ხედავენ"><Visibility fontSize="small"
                                                                style={{color: "#3c763d"}}/></Tooltip>
                : null
        },
        {
            field: "currentVersionNo", headerName: "ვერსია", width: 90, sortable: false,
            align: "center", headerAlign: "center",
            renderCell: ({row}) => row.currentVersionNo || "—"
        },
        {
            field: "actions", headerName: "", width: 150, sortable: false,
            renderCell: ({row}) => canManage ? (
                <>
                    <Tooltip title="სვეტების რედაქტირება">
                        <IconButton size="small" onClick={() => setEditing(row)}>
                            <Edit fontSize="small"/>
                        </IconButton>
                    </Tooltip>
                    <Tooltip title="მშობლების ხილვადობა და დიაგრამა">
                        <IconButton size="small" onClick={() => setSettingsFor(row)}>
                            <Settings fontSize="small"/>
                        </IconButton>
                    </Tooltip>
                    <Tooltip title={row.archived ? "დაბრუნება" : "დაარქივება"}>
                        <IconButton size="small" onClick={() => toggleArchive(row)}>
                            {row.archived
                                ? <Unarchive fontSize="small"/>
                                : <Archive fontSize="small"/>}
                        </IconButton>
                    </Tooltip>
                </>
            ) : null
        }
    ], [canManage, toggleArchive]);

    return (
        <div>
            <div style={{
                display: "flex", justifyContent: "space-between",
                alignItems: "center", margin: "20px 15px"
            }}>
                <Typography variant="h6">ჟურნალები</Typography>
                {canManage ? (
                    <div style={{display: "flex", gap: 12}}>
                        {/* One formula for the whole school, so it lives here
                            rather than inside any one journal. */}
                        <Button startIcon={<Straighten/>}
                                onClick={() => setScalesOpen(true)}
                                style={{textTransform: "none"}}>
                            ბეჭდვის შკალა
                        </Button>
                        <Button variant="contained" startIcon={<Add/>}
                                disabled={!schemeId}
                                onClick={() => setWizardOpen(true)}
                                style={{textTransform: "none"}}>
                            ახალი ჟურნალი
                        </Button>
                    </div>
                ) : null}
            </div>

            <div style={{
                height: "calc(100vh - 190px)", width: "98%",
                marginLeft: 15, marginRight: 15
            }}>
                <DataGridPaper>
                    <DataGridSGS
                        queryKey="JOURNALS"
                        rows={journals || []}
                        columns={columns}
                        loading={isLoading}
                        rowIdField="uuid"
                        getRowHeight={() => "auto"}
                        disableColumnMenu
                        disableSelectionOnClick
                        fullyHideFooter
                    />
                </DataGridPaper>
            </div>

            <JournalWizard
                open={wizardOpen}
                periodSchemeId={schemeId}
                onClose={() => setWizardOpen(false)}
                onCreated={(created) => {
                    setNotification({
                        message: `„${created.name}" შეიქმნა — დაამატეთ სვეტები`,
                        severity: "success"
                    });
                    refetch();
                    setEditing(created);
                }}
                onError={setErrorMessage}
            />

            <JournalSettings
                journal={settingsFor}
                onClose={() => setSettingsFor(null)}
                onSaved={refetch}
                onError={setErrorMessage}
            />

            <ConversionFormulaDialog
                open={scalesOpen}
                onClose={() => setScalesOpen(false)}
                onError={setErrorMessage}
            />

            <JournalEditor
                journal={editing}
                onClose={() => setEditing(null)}
                onSaved={refetch}
                onError={setErrorMessage}
                onNotify={setNotification}
            />
        </div>
    );
};

export default JournalsDashBoard;
