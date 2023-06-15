import GradeTableToolbar from "./GradeTableToolbar";
import {useEffect, useState} from "react";
import DataGridSGS from "../../components/grid/DataGrid";
import useGrades from "./useGrades";
import DataGridPaper from "../../components/grid/DataGridPaper";

const DashBoard = () => {
    const [filters, setFilters] = useState({groupByClause: 'STUDENT'});
    const {data, isLoading, isError, error} = useGrades(filters);

    const gradeColumns = [
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
                return summary1[0].value;
            },
            field: 'GENERAL_SUMMARY_ASSIGMENT_1',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable:true
        },
        {
            headerName: "2",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "GENERAL_SUMMARY_ASSIGMENT_2");
                if(summary2.length === 0){
                    return ""
                }
                return summary2[0].value;
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
                return summary2[0].value;
            },
            field: 'GENERAL_SUMMARY_ASSIGMENT_RESTORATION',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },
        {
            headerName: "თვის ნიშანი",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "GENERAL_SUMMARY_ASSIGMENT_MONTH");
                if(summary2.length === 0){
                    return ""
                }
                return summary2[0].value;
            },
            field: 'GENERAL_SUMMARY_ASSIGMENT_MONTH',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },
        {
            headerName: "%",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "GENERAL_SUMMARY_ASSIGMENT_PERCENT");
                if(summary2.length === 0){
                    return ""
                }
                return summary2[0].value;
            },
            field: 'GENERAL_SUMMARY_ASSIGMENT_PERCENT',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },
        {
            headerName: "წერილობითი დაავალება",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "GENERAL_HOMEWORK_WRITE_ASSIGMENT");
                if(summary2.length === 0){
                    return ""
                }
                return summary2[0].value;
            },
            field: 'GENERAL_HOMEWORK_WRITE_ASSIGMENT',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },
        {
            headerName: "შემოქმედებითი დავალება",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "GENERAL_HOMEWORK_CREATIVE_ASSIGMENT");
                if(summary2.length === 0){
                    return ""
                }
                return summary2[0].value;
            },
            field: 'GENERAL_HOMEWORK_CREATIVE_ASSIGMENT',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },
        {
            headerName: "1",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "GENERAL_HOMEWORK_SUM_1");
                if(summary2.length === 0){
                    return ""
                }
                return summary2[0].value;
            },
            field: 'GENERAL_HOMEWORK_SUM_1',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },
        {
            headerName: "2",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "GENERAL_HOMEWORK_SUM_2");
                if(summary2.length === 0){
                    return ""
                }
                return summary2[0].value;
            },
            field: 'GENERAL_HOMEWORK_SUM_2',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },
        {
            headerName: "1",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "GENERAL_SCHOOL_WORK_1");
                if(summary2.length === 0){
                    return ""
                }
                return summary2[0].value;
            },
            field: 'GENERAL_SCHOOL_WORK_1',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },
        {
            headerName: "თვის ნიშანი",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "GENERAL_SCHOOL_WORK_MONTH");
                if(summary2.length === 0){
                    return ""
                }
                return summary2[0].value;
            },
            field: 'GENERAL_SCHOOL_WORK_MONTH',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },
        {
            headerName: "%",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "GENERAL_SCHOOL_WORK_PERCENT");
                if(summary2.length === 0){
                    return ""
                }
                return summary2[0].value;
            },
            field: 'GENERAL_SCHOOL_WORK_PERCENT',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },{
            headerName: "I",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "GENERAL_COMPONENT_1");
                if(summary2.length === 0){
                    return ""
                }
                return summary2[0].value;
            },
            field: 'GENERAL_COMPONENT_1',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },{
            headerName: "II",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "GENERAL_COMPONENT_2");
                if(summary2.length === 0){
                    return ""
                }
                return summary2[0].value;
            },
            field: 'GENERAL_COMPONENT_2',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },{
            headerName: "III",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "GENERAL_COMPONENT_3");
                if(summary2.length === 0){
                    return ""
                }
                return summary2[0].value;
            },
            field: 'GENERAL_COMPONENT_3',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },
    ];

    if (isLoading) {
        return <div></div>
    }

    const columnGroupingModel = [
        {
            groupId: 'summary',
            headerName: 'შემაჯამებელი დავალება I',
            description: '',
            // renderHeaderGroup: (params) => (
            //     <HeaderWithIcon {...params} icon={<BuildIcon fontSize="small" />} />
            // ),
            children: [{ field: 'GENERAL_SUMMARY_ASSIGMENT_1' }, {field: 'GENERAL_SUMMARY_ASSIGMENT_2'},
                {field: 'GENERAL_SUMMARY_ASSIGMENT_RESTORATION'}, {field: 'GENERAL_SUMMARY_ASSIGMENT_MONTH'},
                {field: 'GENERAL_SUMMARY_ASSIGMENT_PERCENT'}],
            align: 'center',
            headerAlign: 'center'
        },{
            groupId: 'student',
            headerName: 'მოსწავლის გვარი, სახელი',
            description: '',
            // renderHeaderGroup: (params) => (
            //     <HeaderWithIcon {...params} icon={<BuildIcon fontSize="small" />} />
            // ),
            children: [{field: 'firstName'}],
            align: 'center',
            headerAlign: 'center'
        },
    ];


    const handleEdit = (params, changedRow) => {
        console.log(params)
        console.log(changedRow)
    };



    return (
        <div>
            <GradeTableToolbar filters={filters} setFilters={setFilters}/>
            <div style={{height: 500, width: '100%'}}>
                <DataGridPaper>
                    <DataGridSGS
                        experimentalFeatures={{ columnGrouping: true }}
                        columnGroupingModel={columnGroupingModel}
                        // queryKey={"GRADES"}
                        columns={gradeColumns}
                        rows={data ? data : []}
                        getRowId={(row) => {
                            return row.student.id;
                        }}
                        onCellEditCommit={handleEdit}
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