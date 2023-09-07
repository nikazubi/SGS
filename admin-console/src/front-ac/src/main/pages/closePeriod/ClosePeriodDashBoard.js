import React, {useEffect, useState} from "react";
import ClosePeriodTableToolbar from "./ClosePeriodTableToolbar";
import DataGridPaper from "../../components/grid/DataGridPaper";
import DataGridSGS from "../../components/grid/DataGrid";
import useFetchClosePeriod from "./useChangeRequests";
import {getFiltersOfPage} from "../../../utils/filters";
import {useNotification} from "../../../contexts/notification-context";
import moment from "moment/moment";

const ClosePeriodDashBoard = () => {
    const [filters, setFilters] = useState({...getFiltersOfPage("CLOSE_PERIOD")});
    const {setErrorMessage} = useNotification();
    const {data, isLoading, isError, error} = useFetchClosePeriod(filters);

    useEffect(() => {
        if (isError && error) {
            setErrorMessage(error);
        }
    }, [isError, error])

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
                return row.academyClass.className;
            },
            field: 'className',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },
        {
            headerName: "ტიპი",
            renderCell: ({row}) => {
                return row.gradePrefix ? row.gradePrefix === "GENERAL" ? "ნიშნების ჟურნალი" : "ეთიკური ნორმა" : '';
            },
            field: 'gradePrefix',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },
        {
            headerName: "შექმნის თარიღი",
            renderCell: ({row}) => {
                return moment.utc(Date.parse(row.createTime)).local().format("DD-MM-YYYY");
            },
            field: 'createTime',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },
    ];

    return (
        <div>
            <ClosePeriodTableToolbar filters={filters} setFilters={setFilters}/>
            <div style={{
                height: `calc(100vh - ${130}px)`,
                width: '98%',
                marginLeft: 15,
                marginRight: 15,
                marginTop: 25
            }}>
                <DataGridPaper>
                    <DataGridSGS
                        sx={{
                            '& .MuiDataGrid-columnHeader, .MuiDataGrid-cell': {
                                border: `1px solid ${
                                    '#cce1ea'
                                }`,
                            }
                        }}
                        queryKey={"CLOSE_PERIOD"}
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

export default ClosePeriodDashBoard;