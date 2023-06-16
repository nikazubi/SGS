import React, {useEffect, useState} from "react";
import ChangeRequestTableToolbar from "./ChangeRequestTableToolbar";
import DataGridPaper from "../../components/grid/DataGridPaper";
import DataGridSGS from "../../components/grid/DataGrid";
import useChangeRequests from "./useChangeRequests";
import ChangeRequestStatusChangeAction from "./ChangeRequestStatusChangeAction";
import {Check, Close} from "@material-ui/icons";

const ChangeRequestDashBoard = () => {
    const [filters, setFilters] = useState({});

    const {data, isLoading, isError, error} = useChangeRequests(filters);


    const gradeColumns = [
        {
            headerName: "იდენტიფიკატორი",
            renderCell: ({row}) => {
                return row.id;
            },
            field: 'id',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },
        {
            headerName: "კლასი",
            renderCell: ({row}) => {
                return row.prevGrade.academyClass.className;
            },
            field: 'className',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },
        {
            headerName: "მოსწავლე",
            renderCell: ({row}) => {
                return row.prevGrade.student.firstName + " " + row.prevGrade.student.lastName;
            },
            field: 'name',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },
        {
            headerName: "საგანი",
            renderCell: ({row}) => {
                return row.prevGrade.subject.name;
            },
            field: 'subjectName',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },
        {
            headerName: "ძველი ქულა",
            renderCell: ({row}) => {
                return row["prevValue"];
            },
            field: 'prevGrade',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },
        {
            headerName: "ახალი ქულა",
            renderCell: ({row}) => {
                return row.newValue;
            },
            field: 'newGrade',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },
        {
            headerName: "სტატუსი",
            renderCell: ({row}) => {
                if (row.status === "PENDING") return "მოთხოვნა მოლოდინშია"
                if (row.status === "REJECTED") return "უარყოფილია"
                if (row.status === "APPROVED") return "დადასტურებულია"
            },
            field: 'status',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },
        {
            field: 'actions',
            type: 'actions',
            actionCount: 2,
            getActions: ({row}) => [
                <ChangeRequestStatusChangeAction row={row} status={"APPROVED"} tooltip={"დადასტურება"} icon={<Check/>}/>,
                <ChangeRequestStatusChangeAction row={row} status={"REJECTED"} tooltip={"უარყოფა"} icon={<Close/>}/>,
            ],
        }
    ];

    return (
        <div>
            <ChangeRequestTableToolbar filters={filters} setFilters={setFilters}/>
            <div style={{height: 500, width: '100%'}}>
                <DataGridPaper>
                    <DataGridSGS
                        // queryKey={"GRADES"}
                        columns={gradeColumns}
                        rows={data ? data : []}
                        getRowId={(row) => {
                            return row.id;
                        }}
                        getRowHeight={() => 'auto'}
                        disableColumnMenu
                        filters={filters}
                    />
                </DataGridPaper>
            </div>
        </div>
    )
}

export default ChangeRequestDashBoard;