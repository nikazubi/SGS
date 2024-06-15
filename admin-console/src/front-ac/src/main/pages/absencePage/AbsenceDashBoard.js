import React, {useCallback, useEffect, useState} from "react";
import DataGridPaper from "../../components/grid/DataGridPaper";
import DataGridSGS from "../../components/grid/DataGrid";
import {getFiltersOfPage} from "../../../utils/filters";
import AbsenceTableToolbar from "./AbsenceTableToolbar";
import useFetchAbsenceGrade from "./useFetchAbsenceGrade";
import useUpdateAbsenceGrade from "./useUpdateAbsenceGrade";

const AbsenceDashBoard = () => {
    const [filters, setFilters] = useState({...getFiltersOfPage("ABSENCE_GRADES")});
    const {data, isLoading, isError, error, isSuccess} = useFetchAbsenceGrade(filters);
    // const {setErrorMessage} = useNotification();
    const [openRequestModal, setOpenRequestModal] = useState(false);
    const [descriptionText, setDescriptionText] = useState('');
    const [newRowToSave, setNewRowToSave] = useState({});
    const {mutateAsync: mutateRow} = useUpdateAbsenceGrade();

    useEffect(() => {
    }, [filters]);

    // useEffect(() =>{
    //     if(isError && error){
    //         setErrorMessage(error);
    //     }
    // }, [isError, error])

    const getGradeColumns = useCallback(() => {
        return gradeColumnsLong
    }, []);

    const gradeColumnsLong = [
        {
            headerName: "მოსწავლის გვარი, სახელი",
            renderCell: ({row}) => {
                return <div style={{height: 50, justifyContent: 'center', alignItems: 'center',}}>
                    {row.index + ". " + row.student.lastName + " " + row.student.firstName}</div>
            },
            field: 'firstName',
            sortable: false,
            headerAlign: 'center',
            align: 'left',
            width: 200,
            maxWidth: 200,
        },
        {
            headerName: "სულ",
            renderCell: ({row}) => {
                const summary1 = row.gradeList?.filter(grade => grade.gradeType === "SEPTEMBER_OCTOBER");
                if (summary1.length === 0) {
                    return ""
                }
                return summary1[0].value === 0 ? "" : summary1[0].value;
            },
            field: 'SEPTEMBER_OCTOBER',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 70,
            maxWidth: 70,
        },
        {
            headerName: "არა საპ.",
            renderCell: ({row}) => {
                return "";
            },
            field: 'SEP_OCT_NO',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: false,
            type: "number",
            width: 70,
            maxWidth: 70,
        },
        {
            headerName: "სულ",
            renderCell: ({row}) => {
                const summary1 = row.gradeList?.filter(grade => grade.gradeType === "NOVEMBER");
                if (summary1.length === 0) {
                    return ""
                }
                return summary1[0].value === 0 ? "" : summary1[0].value;
            },
            field: 'NOVEMBER',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 70,
            maxWidth: 70,
        },
        {
            headerName: "არა საპ.",
            renderCell: ({row}) => {
                return "";
            },
            field: 'NOV_NO',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: false,
            type: "number",
            width: 70,
            maxWidth: 70,
        },
        {
            headerName: "სულ",
            renderCell: ({row}) => {
                const summary1 = row.gradeList?.filter(grade => grade.gradeType === "DECEMBER");
                if (summary1.length === 0) {
                    return ""
                }
                return summary1[0].value === 0 ? "" : summary1[0].value;
            },
            field: 'DECEMBER',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 70,
            maxWidth: 70,
        },
        {
            headerName: "არა საპ.",
            renderCell: ({row}) => {
                return "";
            },
            field: 'DEC_NO',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: false,
            type: "number",
            width: 70,
            maxWidth: 70,
        },
        {
            headerName: "სულ",
            renderCell: ({row}) => {
                const sum = row.gradeList?.filter(grade => grade.gradeType === "SEPTEMBER_OCTOBER" ||
                    grade.gradeType === "NOVEMBER" || grade.gradeType === "DECEMBER");

                if (sum.length === 0) {
                    return ""
                }
                const res = sum.reduce((acc, obj) => acc + obj.value, 0)
                return res === 0 ? "" : res;
            },
            field: 'FIRST_SEMESTER_TOTAL',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: false,
            type: "number",
            width: 70,
            maxWidth: 70,
        },
        {
            headerName: "არა საპ.",
            renderCell: ({row}) => {
                return "";
            },
            field: 'FIRST_SEMESTER_NO',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: false,
            type: "number",
            width: 70,
            maxWidth: 70,
        },
        {
            headerName: "სულ",
            renderCell: ({row}) => {
                const summary1 = row.gradeList?.filter(grade => grade.gradeType === "JANUARY_FEBRUARY");
                if (summary1.length === 0) {
                    return ""
                }
                return summary1[0].value === 0 ? "" : summary1[0].value;
            },
            field: 'JANUARY_FEBRUARY',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 70,
            maxWidth: 70,
        },
        {
            headerName: "არა საპ.",
            renderCell: ({row}) => {
                return "";
            },
            field: 'JAN_FEB_NO',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: false,
            type: "number",
            width: 70,
            maxWidth: 70,
        },
        {
            headerName: "სულ",
            renderCell: ({row}) => {
                const summary1 = row.gradeList?.filter(grade => grade.gradeType === "MARCH");
                if (summary1.length === 0) {
                    return ""
                }
                return summary1[0].value === 0 ? "" : summary1[0].value;
            },
            field: 'MARCH',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 70,
            maxWidth: 70,
        },
        {
            headerName: "არა საპ.",
            renderCell: ({row}) => {
                return "";
            },
            field: 'MARCH_NO',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: false,
            type: "number",
            width: 70,
            maxWidth: 70,
        },
        {
            headerName: "სულ",
            renderCell: ({row}) => {
                const summary1 = row.gradeList?.filter(grade => grade.gradeType === "APRIL");
                if (summary1.length === 0) {
                    return ""
                }
                return summary1[0].value === 0 ? "" : summary1[0].value;
            },
            field: 'APRIL',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 70,
            maxWidth: 70,
        },
        {
            headerName: "არა საპ.",
            renderCell: ({row}) => {
                return "";
            },
            field: 'APRIL_NO',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: false,
            type: "number",
            width: 70,
            maxWidth: 70,
        },
        {
            headerName: "სულ",
            renderCell: ({row}) => {
                const summary1 = row.gradeList?.filter(grade => grade.gradeType === "MAY");
                if (summary1.length === 0) {
                    return ""
                }
                return summary1[0].value === 0 ? "" : summary1[0].value;
            },
            field: 'MAY',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 70,
            maxWidth: 70,
        },
        {
            headerName: "არა საპ.",
            renderCell: ({row}) => {
                return "";
            },
            field: 'MAY_NO',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: false,
            type: "number",
            width: 70,
            maxWidth: 70,
        },
        {
            headerName: "სულ",
            renderCell: ({row}) => {
                const sum = row.gradeList?.filter(grade => grade.gradeType === "JANUARY_FEBRUARY" ||
                    grade.gradeType === "MARCH" || grade.gradeType === "APRIL" || grade.gradeType === "MAY");

                if (sum.length === 0) {
                    return ""
                }
                const res = sum.reduce((acc, obj) => acc + obj.value, 0)
                return res === 0 ? "" : res;
            },
            field: 'SECOND_SEMESTER_TOTAL',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: false,
            type: "number",
            width: 70,
            maxWidth: 70,
        },
        {
            headerName: "არა საპ.",
            renderCell: ({row}) => {
                return "";
            },
            field: 'SECOND_SEMESTER_NO',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: false,
            type: "number",
            width: 70,
            maxWidth: 70,
        },
        {
            headerName: "სულ",
            renderCell: ({row}) => {
                const res = row.gradeList?.reduce((acc, obj) => acc + obj.value, 0);
                return res === 0 ? "" : res;
            },
            field: 'YEAR_TOTAL',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: false,
            type: "number",
            width: 70,
            maxWidth: 70,
        },
        {
            headerName: "არა საპ.",
            renderCell: ({row}) => {
                return "";
            },
            field: 'YEAR_NO',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: false,
            type: "number",
            width: 70,
            maxWidth: 70,
        },
    ];


    const getGradeColumnGrouping = useCallback(() => {

        return columnGroupingModelLong;
    },);

    const columnGroupingModelLong = [
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
            groupId: 'sep-oct',
            headerName: 'სექტემბერი-ოქტომბერი',
            description: '',
            // renderHeaderGroup: (params) => (
            //     <HeaderWithIcon {...params} icon={<BuildIcon fontSize="small" />} />
            // ),
            children: [{field: 'SEPTEMBER_OCTOBER'}, {field: 'SEP_OCT_NO'}],
            align: 'center',
            headerAlign: 'center'
        },
        {
            groupId: 'nov',
            headerName: 'ნოემბერი',
            description: '',
            // renderHeaderGroup: (params) => (
            //     <HeaderWithIcon {...params} icon={<BuildIcon fontSize="small" />} />
            // ),
            children: [{field: 'NOVEMBER'}, {field: 'NOV_NO'}],
            align: 'center',
            headerAlign: 'center'
        },
        {
            groupId: 'dec',
            headerName: 'დეკემბერი',
            description: '',
            // renderHeaderGroup: (params) => (
            //     <HeaderWithIcon {...params} icon={<BuildIcon fontSize="small" />} />
            // ),
            children: [{field: 'DECEMBER'}, {field: 'DEC_NO'}],
            align: 'center',
            headerAlign: 'center'
        },
        {
            groupId: 'first-semester',
            headerName: 'I სემესტრი',
            description: '',
            // renderHeaderGroup: (params) => (
            //     <HeaderWithIcon {...params} icon={<BuildIcon fontSize="small" />} />
            // ),
            children: [{field: 'FIRST_SEMESTER_TOTAL'}, {field: 'FIRST_SEMESTER_NO'}],
            align: 'center',
            headerAlign: 'center'
        },
        {
            groupId: 'jan-feb',
            headerName: 'იანვარი-თებერვალი',
            description: '',
            // renderHeaderGroup: (params) => (
            //     <HeaderWithIcon {...params} icon={<BuildIcon fontSize="small" />} />
            // ),
            children: [{field: 'JANUARY_FEBRUARY'}, {field: 'JAN_FEB_NO'}],
            align: 'center',
            headerAlign: 'center'
        },
        {
            groupId: 'march',
            headerName: 'მარტი',
            description: '',
            // renderHeaderGroup: (params) => (
            //     <HeaderWithIcon {...params} icon={<BuildIcon fontSize="small" />} />
            // ),
            children: [{field: 'MARCH'}, {field: 'MARCH_NO'}],
            align: 'center',
            headerAlign: 'center'
        },
        {
            groupId: 'april',
            headerName: 'აპრილი',
            description: '',
            // renderHeaderGroup: (params) => (
            //     <HeaderWithIcon {...params} icon={<BuildIcon fontSize="small" />} />
            // ),
            children: [{field: 'APRIL'}, {field: 'APRIL_NO'}],
            align: 'center',
            headerAlign: 'center'
        },
        {
            groupId: 'may',
            headerName: 'მაისი',
            description: '',
            // renderHeaderGroup: (params) => (
            //     <HeaderWithIcon {...params} icon={<BuildIcon fontSize="small" />} />
            // ),
            children: [{field: 'MAY'}, {field: 'MAY_NO'}],
            align: 'center',
            headerAlign: 'center'
        }, {
            groupId: 'second-semester',
            headerName: 'II სემესტრი',
            description: '',
            // renderHeaderGroup: (params) => (
            //     <HeaderWithIcon {...params} icon={<BuildIcon fontSize="small" />} />
            // ),
            children: [{field: 'SECOND_SEMESTER_TOTAL'}, {field: 'SECOND_SEMESTER_NO'}],
            align: 'center',
            headerAlign: 'center'
        }, {
            groupId: 'year',
            headerName: 'წლიური',
            description: '',
            // renderHeaderGroup: (params) => (
            //     <HeaderWithIcon {...params} icon={<BuildIcon fontSize="small" />} />
            // ),
            children: [{field: 'YEAR_TOTAL'}, {field: 'YEAR_NO'}],
            align: 'center',
            headerAlign: 'center'
        },


    ];

    //
    const processRowUpdate = useCallback(
        async (newRow) => {
            const gradeType = Object.keys(newRow).filter(field => field !== "index" &&
                field !== "student" && field !== "gradeList")[0]
            const valueForType = newRow[gradeType]
            const params = {
                student: newRow["student"],
                gradeType: gradeType,
                value: valueForType,
                academyClass: filters.academyClass,
                year: filters.yearRange
            };
            return await mutateRow(params);
            // const {data} = await axios.get("/close-period/get-period-by-class", {params});
            //
            // if (data) {
            //     setNewRowToSave({
            //             newValue: newRow[gradeType],
            //             id: gradesOfType[0].id,
            //             description: descriptionText
            //         }
            //     )
            //     setOpenRequestModal(true);
            // } else {
            //     newRow.subject = filters.subject
            //     newRow.exact = filters.date
            //     return await mutateRow(newRow);
            // }

        },
        [filters],
    );

    // const onProcessRowUpdateError = useCallback(async (newRow) => {
    //
    // }, [filters]);

    // if (isLoading) {
    //     return <div></div>
    // }

    return (
        <div>
            <AbsenceTableToolbar filters={filters} setFilters={setFilters}/>
            <div style={{height: `calc(100vh - ${130}px)`, marginLeft: 15, marginRight: 15,}}>
                <DataGridPaper>
                    <DataGridSGS
                        sx={{
                            '& .MuiDataGrid-columnHeader, .MuiDataGrid-cell': {
                                border: `1px solid ${
                                    '#cce1ea'
                                }`,
                            },
                        }}
                        // colorGroups={{ overflowX: 'scroll', width: "100%"  }}
                        experimentalFeatures={{columnGrouping: true}}
                        columnGroupingModel={getGradeColumnGrouping()}
                        columns={getGradeColumns()}
                        queryKey={"ABSENCE_GRADES"}
                        rows={data ? data : []}
                        getRowId={(row) => {
                            return row.student.id;
                        }}
                        processRowUpdate={processRowUpdate}
                        // onProcessRowUpdateError={onProcessRowUpdateError}
                        getRowHeight={() => 'auto'}
                        disableColumnMenu
                        filters={filters}
                    />
                </DataGridPaper>
            </div>
        </div>
    )
}

export default AbsenceDashBoard;