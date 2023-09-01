import {useState} from "react";
import DataGridPaper from "../../components/grid/DataGridPaper";
import DataGridSGS from "../../components/grid/DataGrid";
import useSubjects, {fetchSubjects} from "./useSubjects";
import SubjectTableToolbar from "./SubjectTableToolbar";
import DeleteTotalAbsenceGrades from "../totalAbsencePage/DeleteTotalAbsenceGrades";
import DeleteSubject from "./DeleteSubject";
import EditSubject from "./EditSubject";

const SubjectDashBoard = () => {
    const [filters, setFilters] = useState({});

    const {data, isLoading, isError, error} = useSubjects(filters);

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
            headerName: "საგანი",
            renderCell: ({row}) => {
                return row.name;
            },
            field: 'academyClass',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },
        {
            field: 'actions',
            type: 'actions',
            width: 2 * 42 + 10,
            getActions: ({row}) => [
                <EditSubject data={row}/>,
                <DeleteSubject data={row} />,
            ],
        },
    ]


    return (
        <div>
            <SubjectTableToolbar filters={filters} setFilters={setFilters}/>
            <div style={{height: `calc(100vh - ${130}px)`, width: '98%', marginTop:25,  marginLeft: 15, marginRight: 15}}>
                <DataGridPaper>
                    <DataGridSGS
                        sx={{
                            '& .MuiDataGrid-columnHeader, .MuiDataGrid-cell': {
                                borderRight: `3px solid ${
                                    '#f4f4f4'
                                }`,
                            },
                        }}
                        queryKey={"SUBJECTS"}
                        columns={columns}
                        rows={data ? data : []}
                        getRowId={(row) => {
                            return row.id;
                        }}
                        fetchData={fetchSubjects}
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

export default SubjectDashBoard;