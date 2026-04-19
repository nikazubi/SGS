import React, {useCallback, useEffect, useState} from "react";
import DataGridSGS from "../../components/grid/DataGrid";
import DataGridPaper from "../../components/grid/DataGridPaper";
import axios from "../../../utils/axios";
import Modal from "../../../components/modals/Modal";
import TextField from "../../components/formik/TextField";
import {getFiltersOfPage} from "../../../utils/filters";
import {useNotification} from "../../../contexts/notification-context";
import TrimesterGradeToolbar from "./TrimesterGradeToolbar";
import useTrimesterGrades from "./useTrimesterGrades";
import useUpdateTrimesterGrade from "./useUpdateTrimesterGrade";

const TrimesterDashBoard = () => {
    const [filters, setFilters] = useState({...getFiltersOfPage("TRIMESTER")});
    const {data, isLoading, isError, error, isSuccess} = useTrimesterGrades(filters);
    const {setErrorMessage} = useNotification();
    const [openRequestModal, setOpenRequestModal] = useState(false);
    const [newRowToSave, setNewRowToSave] = useState({});
    const [descriptionText, setDescriptionText] = useState('');
    const {mutateAsync: mutateRow} = useUpdateTrimesterGrade();

    useEffect(() => {
        if (isError && error) {
            setErrorMessage(error);
        }
    }, [isError, error])

    const renderGradeCell = (gradeType) => ({row}) => {
        const found = row.grades?.find(g => g.gradeType === gradeType);
        if (!found || found.value === null || found.value === undefined || found.value === 0) return "";
        return found.value;
    };

    const valueGetterForGrade = (gradeType) => ({row}) => {
        const found = row.grades?.find(g => g.gradeType === gradeType);
        return found?.value ?? "";
    };

    const ongoingCol = (roman, gradeType) => ({
        headerName: roman,
        renderCell: renderGradeCell(gradeType),
        valueGetter: valueGetterForGrade(gradeType),
        field: gradeType,
        sortable: false,
        align: 'center',
        headerAlign: 'center',
        editable: true,
        type: "number",
        width: 60,
        maxWidth: 60,
    });

    const gradeColumns = [
        {
            headerName: "მოსწავლის გვარი, სახელი",
            renderCell: ({row}) => {
                return <div style={{height: 50, justifyContent: 'center', alignItems: 'center', display: 'flex'}}>
                    {row.index + ". " + row.student.lastName + " " + row.student.firstName}</div>
            },
            field: 'firstName',
            sortable: false,
            headerAlign: 'center',
            align: 'center',
            width: 200,
            maxWidth: 200,
        },
        ongoingCol("I", 'TRIMESTER_ONGOING_GRADE_1'),
        ongoingCol("II", 'TRIMESTER_ONGOING_GRADE_2'),
        ongoingCol("III", 'TRIMESTER_ONGOING_GRADE_3'),
        ongoingCol("IV", 'TRIMESTER_ONGOING_GRADE_4'),
        ongoingCol("V", 'TRIMESTER_ONGOING_GRADE_5'),
        ongoingCol("VI", 'TRIMESTER_ONGOING_GRADE_6'),
        ongoingCol("VII", 'TRIMESTER_ONGOING_GRADE_7'),
        {
            headerName: "საწყისი ცოდნის განსმაზღვრელი ტესტი",
            renderCell: renderGradeCell("TRIMESTER_INITIAL_KNOWLEDGE_GRADE"),
            valueGetter: valueGetterForGrade("TRIMESTER_INITIAL_KNOWLEDGE_GRADE"),
            field: 'TRIMESTER_INITIAL_KNOWLEDGE_GRADE',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 240,
            maxWidth: 240,
        },
        {
            headerName: "პროგრეს ტესტი",
            renderCell: renderGradeCell("TRIMESTER_PROGRESS_GRADE"),
            valueGetter: valueGetterForGrade("TRIMESTER_PROGRESS_GRADE"),
            field: 'TRIMESTER_PROGRESS_GRADE',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 130,
            maxWidth: 130,
        },
        {
            headerName: "ფინალური ტესტი",
            renderCell: renderGradeCell("TRIMESTER_FINAL_EXAM_GRADE"),
            valueGetter: valueGetterForGrade("TRIMESTER_FINAL_EXAM_GRADE"),
            field: 'TRIMESTER_FINAL_EXAM_GRADE',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 150,
            maxWidth: 150,
        },
        {
            headerName: "ტრიმესტრის შეფასება",
            renderCell: renderGradeCell("TRIMESTER_GRADE"),
            valueGetter: valueGetterForGrade("TRIMESTER_GRADE"),
            field: 'TRIMESTER_GRADE',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 180,
            maxWidth: 180,
        },
    ];

    const onProcessRowUpdateError = useCallback(async (newRow) => {

    }, [filters]);

    const columnGroupingModel = [
        {
            groupId: 'student',
            headerName: '',
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
            headerName: 'მიმდინარე შეფასება',
            description: '',
            // renderHeaderGroup: (params) => (
            //     <HeaderWithIcon {...params} icon={<BuildIcon fontSize="small" />} />
            // ),
            children: [{field: 'TRIMESTER_ONGOING_GRADE_1'}, {field: 'TRIMESTER_ONGOING_GRADE_2'},
                {field: 'TRIMESTER_ONGOING_GRADE_3'}, {field: 'TRIMESTER_ONGOING_GRADE_4'},
                {field: 'TRIMESTER_ONGOING_GRADE_5'}, {field: 'TRIMESTER_ONGOING_GRADE_6'},
                {field: 'TRIMESTER_ONGOING_GRADE_7'}],
            align: 'center',
            headerAlign: 'center'
        }
    ];

    const processRowUpdate = useCallback(
        async (newRow, oldRow) => {
            const changedField = Object.keys(newRow).find(
                key => key.startsWith("TRIMESTER") && newRow[key] !== oldRow[key]
            );
            if (!changedField) return oldRow;

            await mutateRow({
                ...newRow,
                subject: filters.subject,
                trimester: filters.trimesterN.value,
                changedField,
            });
            return newRow;
        },
        [mutateRow, filters],
    );

    return (
        <div>
            <TrimesterGradeToolbar filters={filters} setFilters={setFilters}/>
            <div style={{height: `calc(100vh - ${130}px)`, width: '98%', marginLeft: 15, marginRight: 15}}>
                <DataGridPaper>
                    {filters.subject ?
                        <div style={{
                            textAlign: 'center',
                            marginTop: 10,
                            marginBottom: 10,
                            width: '100%',
                            backgroundColor: 'white',
                            fontSize: 20,
                            fontWeight: 'bold'
                        }}>
                            <div> {filters.subject.name + " - " + (filters.subject.teacher ? filters.subject.teacher : "")}</div>
                        </div>
                        :
                        null}
                    <DataGridSGS
                        sx={{
                            '& .MuiDataGrid-columnHeader, .MuiDataGrid-cell': {
                                border: `1px solid ${
                                    '#cce1ea'
                                }`,
                            },
                            '& .MuiDataGrid-editInputCell input[type=number]::-webkit-outer-spin-button, & .MuiDataGrid-editInputCell input[type=number]::-webkit-inner-spin-button': {
                                WebkitAppearance: 'none',
                                margin: 0,
                            },
                            '& .MuiDataGrid-editInputCell input[type=number]': {
                                MozAppearance: 'textfield',
                                textAlign: 'center',
                                padding: 0,
                            },
                        }}

                        experimentalFeatures={{columnGrouping: true}}
                        columnGroupingModel={columnGroupingModel}
                        queryKey={"GRADES"}
                        columns={gradeColumns}
                        rows={data ? data : []}
                        getRowId={(row) => {
                            return row.student.id;
                        }}
                        processRowUpdate={processRowUpdate}
                        onProcessRowUpdateError={onProcessRowUpdateError}
                        getRowHeight={() => 'auto'}
                        disableColumnMenu
                        filters={filters}
                    />
                </DataGridPaper>
            </div>
            <Modal
                maxWidth={600}
                open={openRequestModal}
                onClose={() => (setOpenRequestModal(false))}
                title={"ნიშნის ცვლილება"}
                onSubmit={
                    async (options) => {
                        await axios.post("/change-request/create-change-request", {
                            ...newRowToSave,
                            description: descriptionText
                        });
                        setOpenRequestModal(false);
                    }}
            >
                <p>თუ გსურთ ნიშნის ცვლილების მოთხოვნა შეავსეთ ახსნა განმარტების ველი:</p>
                <br/>
                <TextField
                    onChange={(e) => {
                        setDescriptionText(e.target.value);
                    }}
                    value={descriptionText}
                    label={"ახსნა-განმარტება"}
                />
            </Modal>
        </div>
    )
}

export default TrimesterDashBoard;