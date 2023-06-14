import GradeTableToolbar from "./GradeTableToolbar";
import {useEffect, useState} from "react";
import DataGridSGS from "../../components/grid/DataGrid";
import useGrades from "./useGrades";
import DataGridPaper from "../../components/grid/DataGridPaper";

const DashBoard = () => {
    const [filters, setFilters] = useState({groupByClause: 'STUDENT'});
    const {data, isLoading, isError, error} = useGrades(filters);

    useEffect(() => {
        console.log(filters)
    }, [filters])

    useEffect(() => {
        console.log(data)
    }, [data])

    const gradeColumns = [
        {
            headerName: "აიდი",
            renderCell: ({row}) => {
                return row.student.id;
            },
            field: 'id',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },
        {
            headerName: "მოსწავლის გვარი, სახელი",
            renderCell: ({row}) => {
                return row.student.firstName + " " + row.student.lastName;
            },
            field: 'firstName',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },
        {
            headerName: "1",
            renderCell: ({row}) => {
                const summary1 = row.grades?.filter(grade => grade.gradeType === "GENERAL_SUMMARY_ASSIGMENT_1");
                if(summary1.length === 0){
                    return ""
                }
                return summary1.get(0).value;
            },
            field: 'GENERAL_SUMMARY_ASSIGMENT_1',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },
        {
            headerName: "2",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "GENERAL_SUMMARY_ASSIGMENT_2");
                if(summary2.length === 0){
                    return ""
                }
                return summary2.get(0).value;
            },
            field: 'GENERAL_SUMMARY_ASSIGMENT_2',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },
        {
            headerName: "აღდგენა",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "GENERAL_SUMMARY_ASSIGMENT_RESTORATION");
                if(summary2.length === 0){
                    return ""
                }
                return summary2.get(0).value;
            },
            field: 'GENERAL_SUMMARY_ASSIGMENT_RESTORATION',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },
        // {
        //     headerName: "მოსწავლის გვარი, სახელი",
        //     renderCell: ({row}) => {
        //         return row.student.firstName + " " + row.student.lastName;
        //     },
        //     field: 'firstName',
        //     sortable: false,
        //     align: 'center',
        //     headerAlign: 'center'
        // },
    ];

    if (isLoading) {
        return <div></div>
    }

    const columnGroupingModel = [
        {
            groupId: 'internal_data',
            headerName: 'Internal',
            description: '',
            // renderHeaderGroup: (params) => (
            //     <HeaderWithIcon {...params} icon={<BuildIcon fontSize="small" />} />
            // ),
            children: [{ field: 'id' }],
        },
        {
            groupId: 'character',
            description: 'Information about the character',
            headerName: 'Basic info',
            // renderHeaderGroup: (params) => (
            //     <HeaderWithIcon {...params} icon={<PersonIcon fontSize="small" />} />
            // ),
            children: [
                {
                    groupId: 'naming',
                    headerName: 'Names',
                    headerClassName: 'my-super-theme--naming-group',
                    children: [{ field: 'lastName' }, { field: 'firstName' }],
                },
                { field: 'age' },
            ],
        },
    ];



    return (
        <div>
            <GradeTableToolbar filters={filters} setFilters={setFilters}/>
            <div style={{height: 500, width: '100%'}}>
                <DataGridPaper>
                    <DataGridSGS
                        // queryKey={"GRADES"}
                        columns={gradeColumns}
                        rows={data ? data : []}
                        getRowId={(row) => {
                            return row.student.id;
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

export default DashBoard;