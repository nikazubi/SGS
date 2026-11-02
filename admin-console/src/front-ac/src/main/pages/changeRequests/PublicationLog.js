import React, {useMemo, useState} from "react";
import {useQuery} from "react-query";
import {Button, Chip} from "@mui/material";
import {Send} from "@mui/icons-material";
import DataGridSGS from "../../components/grid/DataGrid";
import DataGridPaper from "../../components/grid/DataGridPaper";
import {fetchPublications} from "../gradebook/gradebookApi";
import {useNotification} from "../../../contexts/notification-context";
import {useUserContext} from "../../../contexts/user-context";
import BulkPublishDialog from "./BulkPublishDialog";

/**
 * When grades were released, and by whom - and where several classes and
 * journals are released at once.
 *
 * The audit trail the close-period screen shows today. Publication itself is
 * per cell — this is the log of the events, which is why a republish that
 * changed nothing still appears with a count of zero.
 *
 * The release button lives here rather than on a page of its own: publishing
 * and its record sit together, and the history refreshes with what was just
 * released. Shown only to those who may publish; the page itself is also open to
 * MANAGE_GRADES, for reading.
 */
const PublicationLog = () => {

    const {setErrorMessage} = useNotification();
    const {hasPermission} = useUserContext();
    const [publishing, setPublishing] = useState(false);

    // onError, so a failed fetch is not shown as "nothing has been published".
    const {data, isLoading, refetch} = useQuery(
        ["PUBLICATIONS"],
        () => fetchPublications(null),
        {refetchOnWindowFocus: false, onError: setErrorMessage}
    );

    const columns = useMemo(() => [
        {field: "className", headerName: "კლასი", width: 110, sortable: false},
        {
            field: "journalName", headerName: "ჟურნალი", width: 240, sortable: false,
            // Releases logged before the log recorded the journal.
            renderCell: ({row}) => row.journalName || "—"
        },
        {field: "periodLabel", headerName: "პერიოდი", width: 160, sortable: false},
        {
            field: "subjectName", headerName: "მოცულობა", width: 220, sortable: false,
            renderCell: ({row}) => row.subjectName
                ? <Chip size="small" label={row.subjectName}/>
                : <Chip size="small" variant="outlined" label="ყველა საგანი"/>
        },
        {
            field: "cellCount", headerName: "შეფასება", width: 110, sortable: false,
            align: "center", headerAlign: "center"
        },
        {
            field: "publishedAt", headerName: "თარიღი", width: 200, sortable: false,
            renderCell: ({row}) => row.publishedAt
                ? new Date(row.publishedAt).toLocaleString("ka-GE")
                : ""
        }
    ], []);

    return (
        <div style={{
            height: "calc(100vh - 150px)", width: "98%",
            marginLeft: 15, marginRight: 15, marginTop: 15
        }}>
            {hasPermission("MANAGE_CLOSED_PERIOD") ? (
                // Clear of the user avatar, which sits over the top-right corner
                // of every page.
                <div style={{display: "flex", justifyContent: "flex-end", marginBottom: 10, marginRight: 56}}>
                    <Button variant="contained" size="small" startIcon={<Send/>}
                            style={{textTransform: "none"}}
                            onClick={() => setPublishing(true)}>
                        გამოქვეყნება
                    </Button>
                </div>
            ) : null}
            <BulkPublishDialog
                open={publishing}
                onClose={() => setPublishing(false)}
                onPublished={() => refetch()}
                onError={setErrorMessage}
            />
            <DataGridPaper>
                <DataGridSGS
                    queryKey="PUBLICATIONS"
                    rows={data || []}
                    columns={columns}
                    loading={isLoading}
                    disableColumnMenu
                    disableSelectionOnClick
                    fullyHideFooter
                />
            </DataGridPaper>
        </div>
    );
};

export default PublicationLog;
