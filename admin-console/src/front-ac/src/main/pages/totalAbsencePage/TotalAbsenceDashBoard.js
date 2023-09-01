import {useEffect, useState} from "react";
import TotalAbsenceTableToolbar from "./TotalAbsenceTableToolbar";
import DataGridPaper from "../../components/grid/DataGridPaper";
import DataGridSGS from "../../components/grid/DataGrid";
import useFetchTotalAbsences from "./useTotalAbsenceGrades";
import moment from "moment";
import DeleteTotalAbsenceGrades from "./DeleteTotalAbsenceGrades";

const TotalAbsenceDashBoard = () => {
    const [filters, setFilters] = useState({});

    const {data, isLoading, isError, error} = useFetchTotalAbsences(filters);

    const columns = [
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
                return row.academyClass.className + '-' + row.academyClass.classLevel;
            },
            field: 'academyClass',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },
        {
            headerName: "აქტივობის დრო",
            renderCell: ({row}) => {
                return moment.utc(Date.parse(row.activePeriod)).local().format("MM-YYYY");
            },
            field: 'activePeriod',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },
        {
            headerName: "ჯამური სასწავლო საათი",
            renderCell: ({row}) => {
                return row.totalAcademyHour;
            },
            field: 'totalAcademyHour',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },
        {
            field: 'actions',
            type: 'actions',
            width: 42 + 10,
            getActions: ({row}) => [
                 <DeleteTotalAbsenceGrades data={row} />,
            ],
        },
    ]


    return (
        <div>
            <TotalAbsenceTableToolbar filters={filters} setFilters={setFilters}/>
            <div style={{height: `calc(100vh - ${130}px)`, width: '98%', marginLeft: 15, marginRight: 15, marginTop:25}}>
                <DataGridPaper>
                    <DataGridSGS
                        sx={{
                            '& .MuiDataGrid-columnHeader, .MuiDataGrid-cell': {
                                borderRight: `3px solid ${
                                    '#f4f4f4'
                                }`,
                            },
                        }}
                        // experimentalFeatures={{columnGrouping: true}}
                        // columnGroupingModel={columnGroupingModel}
                        queryKey={"TOTAL_ABSENCE"}
                        columns={columns}
                        rows={data ? data : []}
                        getRowId={(row) => {
                            return row.id;
                        }}
                        // processRowUpdate={processRowUpdate}
                        // onProcessRowUpdateError={handleProcessRowUpdateError}
                        getRowHeight={() => 'auto'}
                        disableColumnMenu
                        filters={filters}
                    />
                </DataGridPaper>
            </div>
            {/*<ConfirmationModal*/}
            {/*    open={openRequestModal}*/}
            {/*    title={"ნიშნის ცვლილება"}*/}
            {/*    onSubmit={*/}
            {/*        async (options) => {*/}
            {/*            await axios.post("/change-request/create-change-request", newRowToSave)*/}
            {/*        }}*/}
            {/*    onClose={() => (setOpenRequestModal(false))}*/}
            {/*/>*/}
        </div>

    )
}

export default TotalAbsenceDashBoard;