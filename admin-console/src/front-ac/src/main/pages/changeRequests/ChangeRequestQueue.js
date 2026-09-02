import React, {useCallback, useMemo, useState} from "react";
import {useQuery} from "react-query";
import {
    Button, Chip, Dialog, DialogActions, DialogContent, DialogTitle,
    Tab, Tabs, TextField, Typography
} from "@mui/material";
import DataGridSGS from "../../components/grid/DataGrid";
import DataGridPaper from "../../components/grid/DataGridPaper";
import {useNotification} from "../../../contexts/notification-context";
import {useUserContext} from "../../../contexts/user-context";
import {decideChangeRequest, fetchChangeRequests} from "../gradebook/gradebookApi";

/**
 * The director's queue.
 *
 * Every published grade that someone wants changed, with the teacher's
 * explanation. Approving writes the value and releases it together with
 * everything it moved, so parents never see a corrected mark beside an average
 * computed from the old one.
 */
const ChangeRequestQueue = () => {

    const {setErrorMessage, setNotification} = useNotification();
    const {hasPermission} = useUserContext();
    const canDecide = hasPermission("MANAGE_CHANGE_REQUESTS");

    const [status, setStatus] = useState("PENDING");
    const [decision, setDecision] = useState(null);
    const [comment, setComment] = useState("");
    const [busy, setBusy] = useState(false);

    // onError, so a failed fetch says so. Without it a 403 or a dropped
    // connection rendered as an empty queue - indistinguishable from "nothing
    // to approve", which is the one reading a director must not be given.
    const {data, isLoading, refetch} = useQuery(
        ["CHANGE_REQUESTS", status],
        () => fetchChangeRequests({status}),
        {refetchOnWindowFocus: false, onError: setErrorMessage}
    );

    const decide = useCallback(async (approve) => {
        setBusy(true);
        try {
            await decideChangeRequest({
                changeRequestId: decision.id,
                approve,
                comment: comment.trim()
            });
            setNotification({
                message: approve ? "ცვლილება დამტკიცდა" : "მოთხოვნა უარყოფილია",
                severity: "success"
            });
            setDecision(null);
            setComment("");
            refetch();
        } catch (e) {
            setErrorMessage(e);
        } finally {
            setBusy(false);
        }
    }, [decision, comment, refetch, setErrorMessage, setNotification]);

    const columns = useMemo(() => [
        {field: "className", headerName: "კლასი", width: 90, sortable: false},
        {field: "studentName", headerName: "მოსწავლე", width: 200, sortable: false},
        {field: "subjectName", headerName: "საგანი", width: 180, sortable: false},
        {field: "periodLabel", headerName: "პერიოდი", width: 130, sortable: false},
        {field: "componentLabel", headerName: "სვეტი", width: 180, sortable: false},
        {
            field: "previousValue", headerName: "იყო", width: 80, sortable: false,
            align: "center", headerAlign: "center",
            renderCell: ({row}) => row.previousValue ?? row.previousSpecialValue ?? "—"
        },
        {
            field: "requestedValue", headerName: "გახდება", width: 90, sortable: false,
            align: "center", headerAlign: "center",
            renderCell: ({row}) => (
                <strong>{row.requestedValue ?? row.requestedSpecialValue ?? "—"}</strong>
            )
        },
        {
            field: "drift", headerName: "", width: 44, sortable: false,
            // The cell moved after the request was raised. The director is
            // about to overwrite a value they were not shown, so say so.
            renderCell: ({row}) => {
                const shown = row.previousValue ?? row.previousSpecialValue;
                const now = row.currentPublishedValue;
                if (shown == null || now == null) return null;
                return String(shown) === String(now)
                    ? null
                    : <Chip size="small" color="warning" label="!"
                            title={`ამჟამად: ${now}`}/>;
            }
        },
        {field: "reason", headerName: "ახსნა-განმარტება", flex: 1, minWidth: 240, sortable: false},
        {
            field: "actions", headerName: "", width: 130, sortable: false,
            renderCell: ({row}) => (
                row.status === "PENDING" && canDecide ? (
                    <Button size="small" onClick={() => setDecision(row)}
                            style={{textTransform: "none"}}>
                        განხილვა
                    </Button>
                ) : (
                    <Typography variant="caption" style={{color: "#888"}}>
                        {row.decisionComment || ""}
                    </Typography>
                )
            )
        }
    ], [canDecide]);

    return (
        <div>
            <Tabs value={status} onChange={(e, v) => setStatus(v)} style={{marginLeft: 15}}>
                <Tab value="PENDING" label="განსახილველი"/>
                <Tab value="APPROVED" label="დამტკიცებული"/>
                <Tab value="REJECTED" label="უარყოფილი"/>
            </Tabs>

            <div style={{
                height: "calc(100vh - 150px)", width: "98%",
                marginLeft: 15, marginRight: 15
            }}>
                <DataGridPaper>
                    <DataGridSGS
                        queryKey="CHANGE_REQUESTS"
                        rows={data || []}
                        columns={columns}
                        loading={isLoading}
                        getRowHeight={() => "auto"}
                        disableColumnMenu
                        disableSelectionOnClick
                        fullyHideFooter
                    />
                </DataGridPaper>
            </div>

            <Dialog open={Boolean(decision)} onClose={() => setDecision(null)}
                    maxWidth="sm" fullWidth>
                <DialogTitle>ნიშნის ცვლილების განხილვა</DialogTitle>
                <DialogContent>
                    {decision ? (
                        <>
                            <Typography variant="body2" style={{color: "#666"}}>
                                {`${decision.studentName} — ${decision.subjectName || ""}`}
                            </Typography>
                            <Typography variant="body1" style={{margin: "12px 0"}}>
                                {`${decision.componentLabel}: `}
                                {decision.previousValue ?? decision.previousSpecialValue ?? "—"}
                                {" → "}
                                <strong>
                                    {decision.requestedValue
                                        ?? decision.requestedSpecialValue ?? "—"}
                                </strong>
                            </Typography>

                            <Typography variant="body2" style={{marginBottom: 16}}>
                                <em>{decision.reason}</em>
                            </Typography>

                            <Typography variant="caption"
                                        style={{
                                            display: "block", color: "#8a6d3b",
                                            marginBottom: 12
                                        }}>
                                დამტკიცების შემთხვევაში ხელახლა გამოქვეყნდება ყველა
                                გამოთვლილი შეფასება, რომელზეც ეს ცვლილება მოქმედებს.
                            </Typography>
                        </>
                    ) : null}

                    <TextField
                        fullWidth
                        multiline
                        minRows={2}
                        label="დირექციის კომენტარი"
                        placeholder="დამტკიცების შემთხვევაში ეს ტექსტი ეგზავნება მშობელს"
                        value={comment}
                        onChange={(e) => setComment(e.target.value)}
                    />
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setDecision(null)}>დახურვა</Button>
                    <Button color="error" disabled={busy} onClick={() => decide(false)}>
                        უარყოფა
                    </Button>
                    <Button variant="contained" disabled={busy} onClick={() => decide(true)}>
                        დამტკიცება
                    </Button>
                </DialogActions>
            </Dialog>
        </div>
    );
};

export default ChangeRequestQueue;
