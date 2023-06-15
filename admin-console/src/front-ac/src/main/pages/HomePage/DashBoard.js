import GradeTableToolbar from "./GradeTableToolbar";
import {useCallback, useEffect, useState} from "react";
import DataGridSGS from "../../components/grid/DataGrid";
import useGrades from "./useGrades";
import DataGridPaper from "../../components/grid/DataGridPaper";
import useUpdateGrade from "./useUpdateGrade";

const DashBoard = () => {
    const [filters, setFilters] = useState({groupByClause: 'STUDENT'});
    const {data, isLoading, isError, error, isSuccess} = useGrades(filters);
    const {mutateAsync: mutateRow} = useUpdateGrade();

    const gradeColumns = [
        {
            headerName: "მოსწავლის გვარი, სახელი",
            renderCell: ({row}) => {
                return <div style={{height:50, justifyContent:'center', alignItems: 'center', display: 'flex'}}>
                    {row.student.lastName + " " + row.student.firstName}</div>
            },
            field: 'firstName',
            sortable: false,
            headerAlign: 'center',
            align: 'center',
            width: 200,
            maxWidth: 200,
        },
        {
            headerName: "1",
            renderCell: ({row}) => {
                const summary1 = row.grades?.filter(grade => grade.gradeType === "GENERAL_SUMMARY_ASSIGMENT_1");
                if (summary1.length === 0) {
                    return ""
                }
                return summary1[0].value;
            },
            field: 'GENERAL_SUMMARY_ASSIGMENT_1',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 80,
            maxWidth: 80,
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
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 80,
            maxWidth: 80,
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
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 80,
            maxWidth: 80,
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
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 80,
            maxWidth: 80,
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
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 80,
            maxWidth: 80,
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
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 80,
            maxWidth: 80,
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
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 80,
            maxWidth: 80,
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
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 80,
            maxWidth: 80,
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
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 80,
            maxWidth: 80,
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
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 80,
            maxWidth: 80,
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
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 80,
            maxWidth: 80,
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
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 80,
            maxWidth: 80,
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
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 80,
            maxWidth: 80,
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
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 80,
            maxWidth: 80,
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
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 80,
            maxWidth: 80,
        },{
            headerName: "თვის ქულა",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "GENERAL_COMPLETE_MONTHLY");
                if(summary2.length === 0){
                    return ""
                }
                return summary2[0].value;
            },
            field: 'GENERAL_COMPLETE_MONTHLY',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 80,
            maxWidth: 80,
        },
    ];

    const columnGroupingModel = [
        {
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
        },
        {
            groupId: 'homework',
            headerName: 'საშინაო დავალება II',
            description: '',
            // renderHeaderGroup: (params) => (
            //     <HeaderWithIcon {...params} icon={<BuildIcon fontSize="small" />} />
            // ),
            children: [{ field: 'GENERAL_HOMEWORK_WRITE_ASSIGMENT' }, {field: 'GENERAL_HOMEWORK_CREATIVE_ASSIGMENT'},
                {field: 'GENERAL_HOMEWORK_SUM_1'}, {field: 'GENERAL_HOMEWORK_SUM_2'}],
            align: 'center',
            headerAlign: 'center'
        },
        {
            groupId: 'classwork',
            headerName: 'საკლასო დავალება III',
            description: '',
            // renderHeaderGroup: (params) => (
            //     <HeaderWithIcon {...params} icon={<BuildIcon fontSize="small" />} />
            // ),
            children: [{ field: 'GENERAL_SCHOOL_WORK_1' }, {field: 'GENERAL_SCHOOL_WORK_MONTH'},
                {field: 'GENERAL_SCHOOL_WORK_PERCENT'}],
            align: 'center',
            headerAlign: 'center'
        },
        {
            groupId: 'components',
            headerName: 'კომპონენტები',
            description: '',
            // renderHeaderGroup: (params) => (
            //     <HeaderWithIcon {...params} icon={<BuildIcon fontSize="small" />} />
            // ),
            children: [{ field: 'GENERAL_COMPONENT_1' }, {field: 'GENERAL_COMPONENT_2'},
                {field: 'GENERAL_COMPONENT_3'}],
            align: 'center',
            headerAlign: 'center'
        },
    ];


    const processRowUpdate = useCallback(
        async (newRow) => {
            newRow.subject = filters.subject
            return await mutateRow(newRow);
        },
        [mutateRow],
    );

    if (isLoading) {
        return <div></div>
    }

    return (
        <div>
            <GradeTableToolbar filters={filters} setFilters={setFilters}/>
            <div style={{height: 500, width: '100%'}}>
                <DataGridPaper>
                    <DataGridSGS
                        experimentalFeatures={{ columnGrouping: true }}
                        columnGroupingModel={columnGroupingModel}
                        queryKey={"GRADES"}
                        columns={gradeColumns}
                        rows={data ? data : []}
                        getRowId={(row) => {
                            return row.student.id;
                        }}
                        processRowUpdate={processRowUpdate}
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

export default DashBoard;