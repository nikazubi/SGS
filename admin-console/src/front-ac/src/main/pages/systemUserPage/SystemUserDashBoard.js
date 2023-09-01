import {useEffect, useState} from "react";
import DataGridPaper from "../../components/grid/DataGridPaper";
import DataGridSGS from "../../components/grid/DataGrid";
import SystemUserTableToolbar from "./SystemUserTableToolbar";
import useFetchSystemuser from "./useSystemuserGrades";
import {getFiltersOfPage} from "../../../utils/filters";
import DeleteTotalSystemUser from "./DeleteSystemUser";

const SystemUserDashBoard = () => {
    const [filters, setFilters] = useState({...getFiltersOfPage("SYSTEM_USER")});

    const {data, isLoading, isError, error} = useFetchSystemuser(filters);

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
            headerName: "სახელი",
            renderCell: ({row}) => {
                return row.name;
            },
            field: 'academyClass',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },
        {
            headerName: "მომხმარებლის სახელი",
            renderCell: ({row}) => {
                return row.username;
            },
            field: 'activePeriod',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },
        {
            headerName: "სტატუსი",
            renderCell: ({row}) => {
                return row.active ? 'აქტიური' : 'არააქტიური';
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
                 <DeleteTotalSystemUser data={row} />,
            ],
        },
    ]


    return (
        <div>
            <SystemUserTableToolbar filters={filters} setFilters={setFilters}/>
            <div style={{height: `calc(100vh - ${130}px)`, width: '98%', marginLeft: 15,
                marginRight: 15, marginTop: 25}}>
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
                        queryKey={"SYSTEM_USER"}
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
        </div>

    )
}

export default SystemUserDashBoard;