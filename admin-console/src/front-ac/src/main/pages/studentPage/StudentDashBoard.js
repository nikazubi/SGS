import {useState} from "react";
import DataGridPaper from "../../components/grid/DataGridPaper";
import DataGridSGS from "../../components/grid/DataGrid";
import useStudents, {fetchStudents} from "./useStudents";
import StudentTableToolbar from "./StudentTableToolbar";
import DeleteSubject from "./DeleteStudent";
import EditStudent from "./EditStudent";

const StudentDashBoard = () => {
    const [filters, setFilters] = useState({});

    const {data, isLoading, isError, error} = useStudents(filters);

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
                return row.firstName;
            },
            field: 'firstName',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },
        {
            headerName: "გვარი",
            renderCell: ({row}) => {
                return row.lastName;
            },
            field: 'lastName',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },
        {
            headerName: "პირადი ნომერი",
            renderCell: ({row}) => {
                return row.name;
            },
            field: 'personalNumber',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },
        {
            headerName: "მომხმარებლის სახელი",
            renderCell: ({row}) => {
                return row.username;
            },
            field: 'username',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },
        {
            field: 'actions',
            type: 'actions',
            width: 2 * 42 + 10,
            getActions: ({row}) => [
                <EditStudent data={row}/>,
                <DeleteSubject data={row}/>,
            ],
        },
    ]


    return (
        <div>
            <StudentTableToolbar filters={filters} setFilters={setFilters}/>
            <div style={{height: `calc(100vh - ${130}px)`, width: '98%', marginTop:25, marginLeft: 15, marginRight: 15}}>
                <DataGridPaper>
                    <DataGridSGS
                        sx={{
                            '& .MuiDataGrid-columnHeader, .MuiDataGrid-cell': {
                                borderRight: `3px solid ${
                                    '#f4f4f4'
                                }`,
                            },
                        }}
                        queryKey={"STUDENTS"}
                        columns={columns}
                        rows={data ? data : []}
                        getRowId={(row) => {
                            return row.id;
                        }}
                        fetchData={fetchStudents}
                        pagination={true}
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

export default StudentDashBoard;