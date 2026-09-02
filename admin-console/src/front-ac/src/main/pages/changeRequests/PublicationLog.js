import React, {useMemo} from "react";
import {useQuery} from "react-query";
import {Chip} from "@mui/material";
import DataGridSGS from "../../components/grid/DataGrid";
import DataGridPaper from "../../components/grid/DataGridPaper";
import {fetchPublications} from "../gradebook/gradebookApi";
import {useNotification} from "../../../contexts/notification-context";

/**
 * When grades were released, and by whom.
 *
 * The audit trail the close-period screen shows today. Publication itself is
 * per cell — this is the log of the events, which is why a republish that
 * changed nothing still appears with a count of zero.
 */
const PublicationLog = () => {

    const {setErrorMessage} = useNotification();

    // onError, so a failed fetch is not shown as "nothing has been published".
    const {data, isLoading} = useQuery(
        ["PUBLICATIONS"],
        () => fetchPublications(null),
        {refetchOnWindowFocus: false, onError: setErrorMessage}
    );

    const columns = useMemo(() => [
        {field: "className", headerName: "კლასი", width: 110, sortable: false},
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
            height: "calc(100vh - 110px)", width: "98%",
            marginLeft: 15, marginRight: 15, marginTop: 15
        }}>
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
