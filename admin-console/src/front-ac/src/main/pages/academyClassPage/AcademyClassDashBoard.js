import {useState} from "react";
import DataGridPaper from "../../components/grid/DataGridPaper";
import DataGridSGS from "../../components/grid/DataGrid";
import useSubjects, {fetchAcademyClass} from "./useAcademyClass";
import AcademyClassTableToolbar from "./AcademyClassTableToolbar";
import DeleteTotalAbsenceGrades from "../totalAbsencePage/DeleteTotalAbsenceGrades";
import DeleteSubject from "./DeleteAcademyClass";
import EditAcademyClass from "./EditAcademyClass";

const AcademyClassDashBoard = () => {
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
            headerName: "კლასის საფეხური",
            renderCell: ({row}) => {
                return row.classLevel;
            },
            field: 'classLevel',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },
        {
            headerName: "კლასის სახელი",
            renderCell: ({row}) => {
                return row.className;
            },
            field: 'className',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },
        {
            field: 'actions',
            type: 'actions',
            width: 2 * 42 + 10,
            getActions: ({row}) => [
                <EditAcademyClass data={row}/>,
                <DeleteSubject data={row} />,
            ],
        },
    ]


    return (
        <div>
            <AcademyClassTableToolbar filters={filters} setFilters={setFilters}/>
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
                        queryKey={"ACADEMY_CLASS"}
                        columns={columns}
                        rows={data ? data : []}
                        getRowId={(row) => {
                            return row.id;
                        }}
                        fetchData={fetchAcademyClass}
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

export default AcademyClassDashBoard;