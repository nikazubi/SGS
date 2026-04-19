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
        {
            headerName: "I",
            renderCell: ({row}) => {

            },
            field: 'TRIMESTER_ONGOING_GRADE_1',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 50,
            maxWidth: 50,
        },
        {
            headerName: "II",
            renderCell: ({row}) => {

            },
            field: 'TRIMESTER_ONGOING_GRADE_2',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 50,
            maxWidth: 50,
        },
        {
            headerName: "III",
            renderCell: ({row}) => {

            },
            field: 'TRIMESTER_ONGOING_GRADE_3',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 50,
            maxWidth: 50,
        },
        {
            headerName: "IV",
            renderCell: ({row}) => {

            },
            field: 'TRIMESTER_ONGOING_GRADE_4',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 50,
            maxWidth: 50,
        },
        {
            headerName: "V",
            renderCell: ({row}) => {

            },
            field: 'TRIMESTER_ONGOING_GRADE_5',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 50,
            maxWidth: 50,
        },
        {
            headerName: "VI",
            renderCell: ({row}) => {

            },
            field: 'TRIMESTER_ONGOING_GRADE_6',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 50,
            maxWidth: 50,
        },
        {
            headerName: "VII",
            renderCell: ({row}) => {

            },
            field: 'TRIMESTER_ONGOING_GRADE_7',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 50,
            maxWidth: 50,
        },
        {
            headerName: "საწყისი ცოდნის განსმაზღვრელი ტესტი",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "TRIMESTER_INITIAL_KNOWLEDGE_GRADE");
                if (summary2.length === 0) {
                    return ""
                }
                return summary2[0].value;
            },
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
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "TRIMESTER_PROGRESS_GRADE");
                if (summary2.length === 0) {
                    return ""
                }
                return summary2[0].value;
            },
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
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "TRIMESTER_FINAL_EXAM_GRADE");
                if (summary2.length === 0) {
                    return ""
                }
                return summary2[0].value;
            },
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
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "TRIMESTER_GRADE");
                if (summary2.length === 0) {
                    return ""
                }
                return summary2[0].value;
            },
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
        async (newRow) => {
            const gradeType = Object.keys(newRow).filter(field => field.startsWith('GENERAL_') || field.startsWith('TRANSIT'))[0]
            const gradesOfType = newRow.grades?.filter(g => g.gradeType === gradeType);
            newRow.exactMonth = newRow.exactMonth ? newRow.exactMonth : Date.parse(filters.date);
            //TODO this if is if close period has passed which will be implemented not today :)
            if (gradesOfType.length > 0 && gradesOfType[0].value !== undefined && gradesOfType[0].value !== null && gradesOfType[0].value !== 0) {
                const params = {
                    academyClassId: gradesOfType[0].academyClass.id,
                    gradePrefix: "GENERAL",
                    gradeId: gradesOfType[0].id
                };
                const {data} = await axios.get("/close-period/get-period-by-class", {params});

                if (data) {
                    setNewRowToSave({
                            newValue: newRow[gradeType],
                            id: gradesOfType[0].id,
                            description: descriptionText
                        }
                    )
                    setOpenRequestModal(true);
                } else {
                    newRow.subject = filters.subject
                    newRow.exact = filters.date
                    return await mutateRow(newRow);
                }
            } else {
                newRow.subject = filters.subject;
                newRow.trimester = filters.trimesterN.value;
                return await mutateRow(newRow);
            }
        },
        [mutateRow, filters],
    );

    if (isLoading) {
        return <div></div>
    }

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