import {useState} from "react";
import DataGridPaper from "../../components/grid/DataGridPaper";
import DataGridSGS from "../../components/grid/DataGrid";
import useSystemUserGroup from "./useSystemUserGroup";
import SystemUserGroupTableToolbar from "./SystemUserGroupTableToolbar";
import DeleteSubject from "./DeleteSystemUserGroup";
import EditSystemUserGroup from "./EditSystemUserGroup";
import {Tooltip} from "@mui/material";
import {PERMISSIONS} from "./permissions";

const SystemUserGroupDashBoard = () => {
    const [filters, setFilters] = useState({});

    const {data, isLoading, isError, error} = useSystemUserGroup(filters);
    const getTranslatedString = (permissions) => {
        return permissions.toString()
            .split(",")
            .map(permission => PERMISSIONS[permission]).join(", ")
    }


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
            field: 'name',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },
        {
            headerName: "უფლებები",
            renderCell: ({row}) => {
                return (
                    <Tooltip title={getTranslatedString(row.permissions)}>

                        <div style={{whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis'}}>
                            {getTranslatedString(row.permissions)}
                        </div>
                    </Tooltip>
                );

            },
            field: 'permissions',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },

        {
            field: 'actions',
            type: 'actions',
            width: 2 * 42 + 10,
            getActions: ({row}) => [
                <EditSystemUserGroup data={row}/>,
                <DeleteSubject data={row}/>,
            ],
        },
    ]


    return (
        <div>
            <SystemUserGroupTableToolbar filters={filters} setFilters={setFilters}/>
            <div style={{
                height: `calc(100vh - ${130}px)`,
                width: '98%',
                marginTop: 25,
                marginLeft: 15,
                marginRight: 15
            }}>
                <DataGridPaper>
                    <DataGridSGS
                        sx={{
                            '& .MuiDataGrid-columnHeader, .MuiDataGrid-cell': {
                                borderRight: `3px solid ${
                                    '#f4f4f4'
                                }`,
                            },
                        }}
                        queryKey={"SYSTEM_USER_GROUP"}
                        columns={columns}
                        rows={data ? data : []}
                        getRowId={(row) => {
                            return row.id;
                        }}
                        pagination={true}
                        getRowHeight={() => 'auto'}
                        disableColumnMenu
                        filters={filters}
                    />
                </DataGridPaper>
            </div>
        </div>

    )
}

export default SystemUserGroupDashBoard;