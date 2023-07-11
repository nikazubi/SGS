import {useCallback, useEffect, useState} from "react";
import BehaviourTableToolbar from "./BehaviourTableToolbar";
import useGrades from "../HomePage/useGrades";
import useUpdateGrade from "../HomePage/useUpdateGrade";
import DataGridPaper from "../../components/grid/DataGridPaper";
import DataGridSGS from "../../components/grid/DataGrid";
import ConfirmationModal from "../../../components/modals/ConfirmationModal";
import axios from "../../../utils/axios";
import useFetchBehaviour from "./useBehaviour";
import useUpdateBehaviourGrade from "./useUpdateBehaviourGrade";
import {getFiltersOfPage} from "../../../utils/filters";
import {useNotification} from "../../../contexts/notification-context";

const BehaviourDashBoard = () => {
    const [filters, setFilters] = useState({groupByClause: 'STUDENT', ...getFiltersOfPage("BEHAVIOUR")});
    const {data, isLoading, isError, error, isSuccess} = useFetchBehaviour(filters);
    const {setErrorMessage} = useNotification();
    const [openRequestModal, setOpenRequestModal] = useState(false);
    const [newRowToSave, setNewRowToSave] = useState({});
    const {mutateAsync: mutateRow} = useUpdateBehaviourGrade();

    useEffect(() =>{
        if(isError && error){
            setErrorMessage(error);
        }
    }, [isError, error])

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
            headerName: "I კვირა",
            renderCell: ({row}) => {
                const summary1 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_APPEARING_IN_UNIFORM_1");
                if (summary1.length === 0) {
                    return ""
                }
                return summary1[0].value;
            },
            field: 'BEHAVIOUR_APPEARING_IN_UNIFORM_1',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
             width: 70,
            maxWidth: 70,
        },
        {
            headerName: "II კვირა",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_APPEARING_IN_UNIFORM_2");
                if(summary2.length === 0){
                    return ""
                }
                return summary2[0].value;
            },
            field: 'BEHAVIOUR_APPEARING_IN_UNIFORM_2',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
             width: 70,
            maxWidth: 70,
        },
        {
            headerName: "III კვირა",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_APPEARING_IN_UNIFORM_3");
                if(summary2.length === 0){
                    return ""
                }
                return summary2[0].value;
            },
            field: 'BEHAVIOUR_APPEARING_IN_UNIFORM_3',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
             width: 70,
            maxWidth: 70,
        },
        {
            headerName: "IV კვირა",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_APPEARING_IN_UNIFORM_4");
                if(summary2.length === 0){
                    return ""
                }
                return summary2[0].value;
            },
            field: 'BEHAVIOUR_APPEARING_IN_UNIFORM_4',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
             width: 70,
            maxWidth: 70,
        },
        // {
        //     headerName: "5",
        //     renderCell: ({row}) => {
        //         const summary2 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_APPEARING_IN_UNIFORM_5");
        //         if(summary2.length === 0){
        //             return ""
        //         }
        //         return summary2[0].value;
        //     },
        //     field: 'BEHAVIOUR_APPEARING_IN_UNIFORM_5',
        //     sortable: false,
        //     align: 'center',
        //     headerAlign: 'center',
        //     editable: true,
        //     type: "number",
        //     width: 20,
        //     maxWidth: 20,
        // },
        {
            headerName: "თვე",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_APPEARING_IN_UNIFORM_MONTHLY");
                if(summary2.length === 0){
                    return ""
                }
                return summary2[0].value;
            },
            field: 'BEHAVIOUR_APPEARING_IN_UNIFORM_MONTHLY',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 50,
            maxWidth: 50,
        },

        {
            headerName: "I კვირა",
            renderCell: ({row}) => {
                const summary1 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_STUDENT_DELAYS_1");
                if (summary1.length === 0) {
                    return ""
                }
                return summary1[0].value;
            },
            field: 'BEHAVIOUR_STUDENT_DELAYS_1',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
             width: 70,
            maxWidth: 70,
        },
        {
            headerName: "II კვირა",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_STUDENT_DELAYS_2");
                if(summary2.length === 0){
                    return ""
                }
                return summary2[0].value;
            },
            field: 'BEHAVIOUR_STUDENT_DELAYS_2',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
             width: 70,
            maxWidth: 70,
        },
        {
            headerName: "III კვირა",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_STUDENT_DELAYS_3");
                if(summary2.length === 0){
                    return ""
                }
                return summary2[0].value;
            },
            field: 'BEHAVIOUR_STUDENT_DELAYS_3',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
             width: 70,
            maxWidth: 70,
        },
        {
            headerName: "IV კვირა",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_STUDENT_DELAYS_4");
                if(summary2.length === 0){
                    return ""
                }
                return summary2[0].value;
            },
            field: 'BEHAVIOUR_STUDENT_DELAYS_4',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
             width: 70,
            maxWidth: 70,
        },
        {
            headerName: "თვე",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_STUDENT_DELAYS_MONTHLY");
                if(summary2.length === 0){
                    return ""
                }
                return summary2[0].value;
            },
            field: 'BEHAVIOUR_STUDENT_DELAYS_MONTHLY',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 70,
            maxWidth: 70,
        },
        // {
        //     headerName: "5",
        //     renderCell: ({row}) => {
        //         const summary2 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_STUDENT_DELAYS_5");
        //         if(summary2.length === 0){
        //             return ""
        //         }
        //         return summary2[0].value;
        //     },
        //     field: 'BEHAVIOUR_STUDENT_DELAYS_5',
        //     sortable: false,
        //     align: 'center',
        //     headerAlign: 'center',
        //     editable: true,
        //     type: "number",
        //     width: 20,
        //     maxWidth: 20,
        // },
        // {
        //     headerName: "ქულა",
        //     renderCell: ({row}) => {
        //         const summary2 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_APPEARING_IN_UNIFORM_COMPLETE");
        //         if(summary2.length === 0){
        //             return ""
        //         }
        //         return summary2[0].value;
        //     },
        //     field: 'BEHAVIOUR_STUDENT_DELAYS_GRADE',
        //     sortable: false,
        //     align: 'center',
        //     headerAlign: 'center',
        //     editable: true,
        //     type: "number",
        //     width: 50,
        //     maxWidth: 50,
        // },

        {
            headerName: "I კვირა",
            renderCell: ({row}) => {
                const summary1 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_CLASSROOM_INVENTORY_1");
                if (summary1.length === 0) {
                    return ""
                }
                return summary1[0].value;
            },
            field: 'BEHAVIOUR_CLASSROOM_INVENTORY_1',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
             width: 70,
            maxWidth: 70,
        },
        {
            headerName: "II კვირა",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_CLASSROOM_INVENTORY_2");
                if(summary2.length === 0){
                    return ""
                }
                return summary2[0].value;
            },
            field: 'BEHAVIOUR_CLASSROOM_INVENTORY_2',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
             width: 70,
            maxWidth: 70,
        },
        {
            headerName: "III კვირა",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_CLASSROOM_INVENTORY_3");
                if(summary2.length === 0){
                    return ""
                }
                return summary2[0].value;
            },
            field: 'BEHAVIOUR_CLASSROOM_INVENTORY_3',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
             width: 70,
            maxWidth: 70,
        },
        {
            headerName: "IV კვირა",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_CLASSROOM_INVENTORY_4");
                if(summary2.length === 0){
                    return ""
                }
                return summary2[0].value;
            },
            field: 'BEHAVIOUR_CLASSROOM_INVENTORY_4',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
             width: 70,
            maxWidth: 70,
        },
        // {
        //     headerName: "5",
        //     renderCell: ({row}) => {
        //         const summary2 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_CLASSROOM_INVENTORY_5");
        //         if(summary2.length === 0){
        //             return ""
        //         }
        //         return summary2[0].value;
        //     },
        //     field: 'BEHAVIOUR_CLASSROOM_INVENTORY_5',
        //     sortable: false,
        //     align: 'center',
        //     headerAlign: 'center',
        //     editable: true,
        //     type: "number",
        //     width: 20,
        //     maxWidth: 20,
        // },
        {
            headerName: "თვე",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_CLASSROOM_INVENTORY_MONTHLY");
                if(summary2.length === 0){
                    return ""
                }
                return summary2[0].value;
            },
            field: 'BEHAVIOUR_CLASSROOM_INVENTORY_MONTHLY',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 50,
            maxWidth: 50,
        },


        {
            headerName: "I კვირა",
            renderCell: ({row}) => {
                const summary1 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_STUDENT_HYGIENE_1");
                if (summary1.length === 0) {
                    return ""
                }
                return summary1[0].value;
            },
            field: 'BEHAVIOUR_STUDENT_HYGIENE_1',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
             width: 70,
            maxWidth: 70,
        },
        {
            headerName: "II კვირა",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_STUDENT_HYGIENE_2");
                if(summary2.length === 0){
                    return ""
                }
                return summary2[0].value;
            },
            field: 'BEHAVIOUR_STUDENT_HYGIENE_2',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
             width: 70,
            maxWidth: 70,
        },
        {
            headerName: "III კვირა",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_STUDENT_HYGIENE_3");
                if(summary2.length === 0){
                    return ""
                }
                return summary2[0].value;
            },
            field: 'BEHAVIOUR_STUDENT_HYGIENE_3',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
             width: 70,
            maxWidth: 70,
        },
        {
            headerName: "IV კვირა",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_STUDENT_HYGIENE_4");
                if(summary2.length === 0){
                    return ""
                }
                return summary2[0].value;
            },
            field: 'BEHAVIOUR_STUDENT_HYGIENE_4',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
             width: 70,
            maxWidth: 70,
        },
        // {
        //     headerName: "5",
        //     renderCell: ({row}) => {
        //         const summary2 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_STUDENT_HYGIENE_5");
        //         if(summary2.length === 0){
        //             return ""
        //         }
        //         return summary2[0].value;
        //     },
        //     field: 'BEHAVIOUR_STUDENT_HYGIENE_5',
        //     sortable: false,
        //     align: 'center',
        //     headerAlign: 'center',
        //     editable: true,
        //     type: "number",
        //     width: 20,
        //     maxWidth: 20,
        // },
        {
            headerName: "თვე",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_STUDENT_HYGIENE_MONTHLY");
                if(summary2.length === 0){
                    return ""
                }
                return summary2[0].value;
            },
            field: 'BEHAVIOUR_STUDENT_HYGIENE_MONTHLY',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 50,
            maxWidth: 50,
        },


        {
            headerName: "I კვირა",
            renderCell: ({row}) => {
                const summary1 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_STUDENT_BEHAVIOR_1");
                if (summary1.length === 0) {
                    return ""
                }
                return summary1[0].value;
            },
            field: 'BEHAVIOUR_STUDENT_BEHAVIOR_1',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
             width: 70,
            maxWidth: 70,
        },
        {
            headerName: "II კვირა",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_STUDENT_BEHAVIOR_2");
                if(summary2.length === 0){
                    return ""
                }
                return summary2[0].value;
            },
            field: 'BEHAVIOUR_STUDENT_BEHAVIOR_2',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
             width: 70,
            maxWidth: 70,
        },
        {
            headerName: "III კვირა",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_STUDENT_BEHAVIOR_3");
                if(summary2.length === 0){
                    return ""
                }
                return summary2[0].value;
            },
            field: 'BEHAVIOUR_STUDENT_BEHAVIOR_3',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
             width: 70,
            maxWidth: 70,
        },
        {
            headerName: "IV კვირა",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_STUDENT_BEHAVIOR_4");
                if(summary2.length === 0){
                    return ""
                }
                return summary2[0].value;
            },
            field: 'BEHAVIOUR_STUDENT_BEHAVIOR_4',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
             width: 70,
            maxWidth: 70,
        },
        // {
        //     headerName: "5",
        //     renderCell: ({row}) => {
        //         const summary2 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_STUDENT_BEHAVIOR_5");
        //         if(summary2.length === 0){
        //             return ""
        //         }
        //         return summary2[0].value;
        //     },
        //     field: 'BEHAVIOUR_STUDENT_BEHAVIOR_5',
        //     sortable: false,
        //     align: 'center',
        //     headerAlign: 'center',
        //     editable: true,
        //     type: "number",
        //     width: 20,
        //     maxWidth: 20,
        // },
        {
            headerName: "ქულა",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_STUDENT_BEHAVIOR_MONTHLY");
                if(summary2.length === 0){
                    return ""
                }
                return summary2[0].value;
            },
            field: 'BEHAVIOUR_STUDENT_BEHAVIOR_MONTHLY',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 50,
            maxWidth: 50,
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
            groupId: 'uniform',
            headerName: 'მოსწავლის ფორმით გამოცხადება',
            description: '',
            // renderHeaderGroup: (params) => (
            //     <HeaderWithIcon {...params} icon={<BuildIcon fontSize="small" />} />
            // ),
            children: [{ field: 'BEHAVIOUR_APPEARING_IN_UNIFORM_1' }, {field: 'BEHAVIOUR_APPEARING_IN_UNIFORM_2'},
                {field: 'BEHAVIOUR_APPEARING_IN_UNIFORM_3'}, {field: 'BEHAVIOUR_APPEARING_IN_UNIFORM_4'},
                {field: 'BEHAVIOUR_APPEARING_IN_UNIFORM_5'},{field: 'BEHAVIOUR_APPEARING_IN_UNIFORM_MONTHLY'}],
            align: 'center',
            headerAlign: 'center'
        },
        {
            groupId: 'delay',
            headerName: 'მოსწავლის დაგვიანება',
            description: '',
            // renderHeaderGroup: (params) => (
            //     <HeaderWithIcon {...params} icon={<BuildIcon fontSize="small" />} />
            // ),
            children: [{ field: 'BEHAVIOUR_STUDENT_DELAYS_1' }, {field: 'BEHAVIOUR_STUDENT_DELAYS_2'},
                {field: 'BEHAVIOUR_STUDENT_DELAYS_3'}, {field: 'BEHAVIOUR_STUDENT_DELAYS_4'},
                {field: 'BEHAVIOUR_STUDENT_DELAYS_5'},{field: 'BEHAVIOUR_STUDENT_DELAYS_GRADE'}],
            align: 'center',
            headerAlign: 'center'
        },
        {
            groupId: 'inventory',
            headerName: 'საკლასო ინვენტარის მოვლა',
            description: '',
            // renderHeaderGroup: (params) => (
            //     <HeaderWithIcon {...params} icon={<BuildIcon fontSize="small" />} />
            // ),
            children: [{ field: 'BEHAVIOUR_CLASSROOM_INVENTORY_1' }, {field: 'BEHAVIOUR_CLASSROOM_INVENTORY_2'},
                {field: 'BEHAVIOUR_CLASSROOM_INVENTORY_3'}, {field: 'BEHAVIOUR_CLASSROOM_INVENTORY_4'},
                {field: 'BEHAVIOUR_CLASSROOM_INVENTORY_5'},{field: 'BEHAVIOUR_CLASSROOM_INVENTORY_GRADE'}],
            align: 'center',
            headerAlign: 'center'
        },
        {
            groupId: 'hygiene',
            headerName: 'მოსწავლის მიერ ჰიგიენური ნორმების დაცვა',
            description: '',
            // renderHeaderGroup: (params) => (
            //     <HeaderWithIcon {...params} icon={<BuildIcon fontSize="small" />} />
            // ),
            children: [{ field: 'BEHAVIOUR_STUDENT_HYGIENE_1' }, {field: 'BEHAVIOUR_STUDENT_HYGIENE_2'},
                {field: 'BEHAVIOUR_STUDENT_HYGIENE_3'}, {field: 'BEHAVIOUR_STUDENT_HYGIENE_4'},
                {field: 'BEHAVIOUR_STUDENT_HYGIENE_5'},{field: 'BEHAVIOUR_STUDENT_HYGIENE_GRADE'}],
            align: 'center',
            headerAlign: 'center'
        },
        {
            groupId: 'behavior',
            headerName: 'მოსწავლის ყოფაქცევა',
            description: '',
            // renderHeaderGroup: (params) => (
            //     <HeaderWithIcon {...params} icon={<BuildIcon fontSize="small" />} />
            // ),
            children: [{ field: 'BEHAVIOUR_STUDENT_BEHAVIOR_1' }, {field: 'BEHAVIOUR_STUDENT_BEHAVIOR_2'},
                {field: 'BEHAVIOUR_STUDENT_BEHAVIOR_3'}, {field: 'BEHAVIOUR_STUDENT_BEHAVIOR_4'},
                {field: 'BEHAVIOUR_STUDENT_BEHAVIOR_5'},{field: 'BEHAVIOUR_STUDENT_BEHAVIOR_GRADE'}],
            align: 'center',
            headerAlign: 'center'
        },

    ];

    const processRowUpdate = useCallback(
        async (newRow) => {
            const gradeType = Object.keys(newRow).filter(field => field !== "student" && field !== "grades")[0]
            const gradesOfType = newRow.grades?.filter(g => g.gradeType === gradeType)
            if (gradesOfType > 0) {
                setNewRowToSave({newValue: newRow[gradeType], gradeId: gradesOfType[0].id})
                setOpenRequestModal(true);
            } else {
                newRow.subject = filters.subject
                return await mutateRow(newRow);
            }
        },
        [mutateRow],
    );

    if (isLoading) {
        return <div></div>
    }

    return (
        <div>
            <BehaviourTableToolbar filters={filters} setFilters={setFilters}/>
            <div style={{height: `calc(100vh - ${130}px)`, width: '98%', marginLeft:15, marginRight:15}}>
                <DataGridPaper>
                    <DataGridSGS
                        sx={{
                            '& .MuiDataGrid-columnHeader, .MuiDataGrid-cell': {
                                borderRight: `3px solid ${
                                    '#f4f4f4'
                                }`,
                            },
                        }}
                        experimentalFeatures={{ columnGrouping: true }}
                        columnGroupingModel={columnGroupingModel}
                        queryKey={"BEHAVIOUR"}
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
            <ConfirmationModal
                open={openRequestModal}
                title={"ნიშნის ცვლილება"}
                onSubmit={
                    async (options) => {
                        await axios.post("/change-request/create-change-request", newRowToSave)
                    }}
                onClose={() => (setOpenRequestModal(false))}
            />
        </div>
    )
}

export default BehaviourDashBoard;