import GradeTableToolbar from "./GradeTableToolbar";
import React, {useCallback, useEffect, useState} from "react";
import DataGridSGS from "../../components/grid/DataGrid";
import useGrades from "./useGrades";
import DataGridPaper from "../../components/grid/DataGridPaper";
import useUpdateGrade from "./useUpdateGrade";
import axios from "../../../utils/axios";
import Modal from "../../../components/modals/Modal";
import TextField from "../../components/formik/TextField";
import {getFiltersOfPage} from "../../../utils/filters";
import {useNotification} from "../../../contexts/notification-context";

const DashBoard = () => {
    const [filters, setFilters] = useState({groupByClause: 'STUDENT', ...getFiltersOfPage("GRADES")});
    const {data, isLoading, isError, error, isSuccess} = useGrades(filters);
    const {setErrorMessage} = useNotification();
    const [openRequestModal, setOpenRequestModal] = useState(false);
    const [newRowToSave, setNewRowToSave] = useState({});
    const [descriptionText, setDescriptionText] = useState('');
    const {mutateAsync: mutateRow} = useUpdateGrade();

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
            width: 78,
            maxWidth: 78,
        },
        {
            headerName: "2",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "GENERAL_SUMMARY_ASSIGMENT_2");
                if (summary2.length === 0) {
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
            width: 78,
            maxWidth: 78,
        },
        {
            headerName: "აღდგენა",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "GENERAL_SUMMARY_ASSIGMENT_RESTORATION");
                if (summary2.length === 0) {
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
            width: 78,
            maxWidth: 78,
        },
        {
            headerName: "თვის ნიშანი",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "GENERAL_SUMMARY_ASSIGMENT_MONTH");
                if (summary2.length === 0) {
                    return ""
                }
                return summary2[0].value === 0 ? '' : summary2[0].value;
            },
            field: 'GENERAL_SUMMARY_ASSIGMENT_MONTH',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 78,
            maxWidth: 78,
        },
        {
            headerName: "50%",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "GENERAL_SUMMARY_ASSIGMENT_PERCENT");
                if (summary2.length === 0) {
                    return ""
                }
                return summary2[0].value === 0 ? '' : summary2[0].value;
            },
            field: 'GENERAL_SUMMARY_ASSIGMENT_PERCENT',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 60,
            maxWidth: 60,
        },
        {
            headerName: "1",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "GENERAL_SCHOOL_WORK_1");
                if (summary2.length === 0) {
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
            width: 78,
            maxWidth: 78,
        },
        {
            headerName: "2",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "GENERAL_SCHOOL_WORK_2");
                if (summary2.length === 0) {
                    return ""
                }
                return summary2[0].value;
            },
            field: 'GENERAL_SCHOOL_WORK_2',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 78,
            maxWidth: 78,
        },

        {
            headerName: "3",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "GENERAL_SCHOOL_WORK_3");
                if (summary2.length === 0) {
                    return ""
                }
                return summary2[0].value;
            },
            field: 'GENERAL_SCHOOL_WORK_3',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 78,
            maxWidth: 78,
        },
        {
            headerName: "4",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "GENERAL_SCHOOL_WORK_4");
                if (summary2.length === 0) {
                    return ""
                }
                return summary2[0].value;
            },
            field: 'GENERAL_SCHOOL_WORK_4',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 78,
            maxWidth: 78,
        },
        {
            headerName: "5",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "GENERAL_SCHOOL_WORK_5");
                if (summary2.length === 0) {
                    return ""
                }
                return summary2[0].value;
            },
            field: 'GENERAL_SCHOOL_WORK_5',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 78,
            maxWidth: 78,
        },
        {
            headerName: "თვის ნიშანი",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "GENERAL_SCHOOL_WORK_MONTH");
                if (summary2.length === 0) {
                    return ""
                }
                return summary2[0].value === 0 ? '' : summary2[0].value;
            },
            field: 'GENERAL_SCHOOL_WORK_MONTH',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 78,
            maxWidth: 78,
        },
        {
            headerName: "25%",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "GENERAL_SCHOOL_WORK_PERCENT");
                if (summary2.length === 0) {
                    return ""
                }
                return summary2[0].value === 0 ? '' : summary2[0].value;
            },
            field: 'GENERAL_SCHOOL_WORK_PERCENT',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 60,
            maxWidth: 60,
        },
        {
            headerName: "1",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "GENERAL_HOMEWORK_WRITE_ASSIGMENT_1");
                if (summary2.length === 0) {
                    return ""
                }
                return summary2[0].value;
            },
            field: 'GENERAL_HOMEWORK_WRITE_ASSIGMENT_1',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 78,
            maxWidth: 78,
        },
        {
            headerName: "2",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "GENERAL_HOMEWORK_WRITE_ASSIGMENT_2");
                if (summary2.length === 0) {
                    return ""
                }
                return summary2[0].value;
            },
            field: 'GENERAL_HOMEWORK_WRITE_ASSIGMENT_2',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 78,
            maxWidth: 78,
        },
        {
            headerName: "3",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "GENERAL_HOMEWORK_WRITE_ASSIGMENT_3");
                if (summary2.length === 0) {
                    return ""
                }
                return summary2[0].value;
            },
            field: 'GENERAL_HOMEWORK_WRITE_ASSIGMENT_3',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 78,
            maxWidth: 78,
        },
        {
            headerName: "4",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "GENERAL_HOMEWORK_WRITE_ASSIGMENT_4");
                if (summary2.length === 0) {
                    return ""
                }
                return summary2[0].value;
            },
            field: 'GENERAL_HOMEWORK_WRITE_ASSIGMENT_4',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 78,
            maxWidth: 78,
        },
        {
            headerName: "თვის ნიშანი",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "GENERAL_HOMEWORK_MONTHLY");
                if (summary2.length === 0) {
                    return ""
                }
                return summary2[0].value === 0 ? '' : summary2[0].value;
            },
            field: 'GENERAL_HOMEWORK_MONTHLY',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 78,
            maxWidth: 78,
        },
        {
            headerName: "25%",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "GENERAL_HOMEWORK_PERCENT");
                if (summary2.length === 0) {
                    return ""
                }
                return summary2[0].value === 0 ? '' : summary2[0].value;
            },
            field: 'GENERAL_HOMEWORK_PERCENT',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 78,
            maxWidth: 78,
        },
        {
            headerName: "თვის ქულა",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "GENERAL_COMPLETE_MONTHLY");
                if (summary2.length === 0) {
                    return ""
                }
                return summary2[0].value === 0 ? '' : summary2[0].value;
            },
            field: 'GENERAL_COMPLETE_MONTHLY',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 78,
            maxWidth: 78,
        },
        {
            headerName: "გაცდენა",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "GENERAL_ABSENCE_MONTHLY");
                if (summary2.length === 0) {
                    return ""
                }
                return summary2[0].value;
            },
            field: 'GENERAL_ABSENCE_MONTHLY',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 78,
            maxWidth: 78,
        },
    ];

    const gradeColumnsTransit = [
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
            headerName: "1",
            renderCell: ({row}) => {
                const summary1 = row.grades?.filter(grade => grade.gradeType === "TRANSIT_SUMMARY_ASSIGMENT_1");
                if (summary1.length === 0) {
                    return ""
                }
                return summary1[0].value;
            },
            field: 'TRANSIT_SUMMARY_ASSIGMENT_1',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 78,
            maxWidth: 78,
        },
        {
            headerName: "2",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "TRANSIT_SUMMARY_ASSIGMENT_2");
                if (summary2.length === 0) {
                    return ""
                }
                return summary2[0].value;
            },
            field: 'TRANSIT_SUMMARY_ASSIGMENT_2',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 78,
            maxWidth: 78,
        },
        {
            headerName: "აღდგენა",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "TRANSIT_SUMMARY_ASSIGMENT_RESTORATION");
                if (summary2.length === 0) {
                    return ""
                }
                return summary2[0].value;
            },
            field: 'TRANSIT_SUMMARY_ASSIGMENT_RESTORATION',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 78,
            maxWidth: 78,
        },
        {
            headerName: "თვის ნიშანი",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "TRANSIT_SUMMARY_ASSIGMENT_MONTH");
                if (summary2.length === 0) {
                    return ""
                }
                return summary2[0].value;
            },
            field: 'TRANSIT_SUMMARY_ASSIGMENT_MONTH',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 78,
            maxWidth: 78,
        },
        {
            headerName: "%",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "TRANSIT_SUMMARY_ASSIGMENT_PERCENT");
                if (summary2.length === 0) {
                    return ""
                }
                return summary2[0].value;
            },
            field: 'TRANSIT_SUMMARY_ASSIGMENT_PERCENT',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 60,
            maxWidth: 60,
        },
        {
            headerName: "1",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "TRANSIT_SCHOOL_WORK_1");
                if (summary2.length === 0) {
                    return ""
                }
                return summary2[0].value;
            },
            field: 'TRANSIT_SCHOOL_WORK_1',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 78,
            maxWidth: 78,
        },
        {
            headerName: "2",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "TRANSIT_SCHOOL_WORK_2");
                if (summary2.length === 0) {
                    return ""
                }
                return summary2[0].value;
            },
            field: 'TRANSIT_SCHOOL_WORK_2',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 78,
            maxWidth: 78,
        },
        {
            headerName: "3",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "TRANSIT_SCHOOL_WORK_3");
                if (summary2.length === 0) {
                    return ""
                }
                return summary2[0].value;
            },
            field: 'TRANSIT_SCHOOL_WORK_3',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 78,
            maxWidth: 78,
        },
        {
            headerName: "4",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "TRANSIT_SCHOOL_WORK_4");
                if (summary2.length === 0) {
                    return ""
                }
                return summary2[0].value;
            },
            field: 'TRANSIT_SCHOOL_WORK_4',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 78,
            maxWidth: 78,
        },
        {
            headerName: "5",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "TRANSIT_SCHOOL_WORK_5");
                if (summary2.length === 0) {
                    return ""
                }
                return summary2[0].value;
            },
            field: 'TRANSIT_SCHOOL_WORK_5',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 78,
            maxWidth: 78,
        },
        {
            headerName: "6",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "TRANSIT_SCHOOL_WORK_6");
                if (summary2.length === 0) {
                    return ""
                }
                return summary2[0].value;
            },
            field: 'TRANSIT_SCHOOL_WORK_6',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 78,
            maxWidth: 78,
        },
        {
            headerName: "7",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "TRANSIT_SCHOOL_WORK_7");
                if (summary2.length === 0) {
                    return ""
                }
                return summary2[0].value;
            },
            field: 'TRANSIT_SCHOOL_WORK_7',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 78,
            maxWidth: 78,
        },
        {
            headerName: "8",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "TRANSIT_SCHOOL_WORK_8");
                if (summary2.length === 0) {
                    return ""
                }
                return summary2[0].value;
            },
            field: 'TRANSIT_SCHOOL_WORK_8',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 78,
            maxWidth: 78,
        },
        {
            headerName: "თვის ნიშანი",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "TRANSIT_SCHOOL_WORK_MONTH");
                if (summary2.length === 0) {
                    return ""
                }
                return summary2[0].value;
            },
            field: 'TRANSIT_SCHOOL_WORK_MONTH',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 78,
            maxWidth: 78,
        },
        {
            headerName: "%",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "TRANSIT_SCHOOL_WORK_MONTH_PERCENT");
                if (summary2.length === 0) {
                    return ""
                }
                return summary2[0].value;
            },
            field: 'TRANSIT_SCHOOL_WORK_MONTH_PERCENT',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 60,
            maxWidth: 60,
        },
        {
            headerName: "თვის ქულა",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "TRANSIT_SCHOOL_COMPLETE_MONTHLY");
                if (summary2.length === 0) {
                    return ""
                }
                return summary2[0].value;
            },
            field: 'TRANSIT_SCHOOL_COMPLETE_MONTHLY',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 78,
            maxWidth: 78,
        },
        {
            headerName: "გაცდენა",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "GENERAL_ABSENCE_MONTHLY");
                if (summary2.length === 0) {
                    return ""
                }
                return summary2[0].value;
            },
            field: 'GENERAL_ABSENCE_MONTHLY',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 78,
            maxWidth: 78,
        },
    ];

    const getGradeColumns = useCallback(() => {
        if (filters.academyClass && filters.academyClass.isTransit) {
            return gradeColumnsTransit;
        } else {
            return gradeColumns;
        }
    }, [filters]);

    const columnGroupingModelTransit = [
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
            children: [{field:'TRANSIT_SUMMARY_ASSIGMENT_1'}, {field:'TRANSIT_SUMMARY_ASSIGMENT_2'},
                {field:'TRANSIT_SUMMARY_ASSIGMENT_RESTORATION'}, {field:'TRANSIT_SUMMARY_ASSIGMENT_MONTH'},
                {field: 'TRANSIT_SUMMARY_ASSIGMENT_PERCENT'}],
            align: 'center',
            headerAlign: 'center'
        },
        {
            groupId: 'classwork',
            headerName: 'საკლასო სამუშაო II',
            description: '',
            // renderHeaderGroup: (params) => (
            //     <HeaderWithIcon {...params} icon={<BuildIcon fontSize="small" />} />
            // ),
            children: [{field: 'TRANSIT_SCHOOL_WORK_1'}, {field: 'TRANSIT_SCHOOL_WORK_2'}, {field: 'TRANSIT_SCHOOL_WORK_3'},
                {field: 'TRANSIT_SCHOOL_WORK_4'},
                {field: 'TRANSIT_SCHOOL_WORK_5'}, {field: 'TRANSIT_SCHOOL_WORK_6'},
                {field: 'TRANSIT_SCHOOL_WORK_7'}, {field: 'TRANSIT_SCHOOL_WORK_8'},
                {field: 'TRANSIT_SCHOOL_WORK_MONTH'}, {field: 'TRANSIT_SCHOOL_WORK_MONTH_PERCENT'}],
            align: 'center',
            headerAlign: 'center'
        },
    ]

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
            headerName: 'შემაჯამებელი დავალება I',
            description: '',
            // renderHeaderGroup: (params) => (
            //     <HeaderWithIcon {...params} icon={<BuildIcon fontSize="small" />} />
            // ),
            children: [{field: 'GENERAL_SUMMARY_ASSIGMENT_1'}, {field: 'GENERAL_SUMMARY_ASSIGMENT_2'},
                {field: 'GENERAL_SUMMARY_ASSIGMENT_RESTORATION'}, {field: 'GENERAL_SUMMARY_ASSIGMENT_MONTH'},
                {field: 'GENERAL_SUMMARY_ASSIGMENT_PERCENT'}],
            align: 'center',
            headerAlign: 'center'
        },
        {
            groupId: 'homework',
            headerName: 'შემოქმედებითობა II',
            description: '',
            // renderHeaderGroup: (params) => (
            //     <HeaderWithIcon {...params} icon={<BuildIcon fontSize="small" />} />
            // ),
            children: [{field: 'GENERAL_SCHOOL_WORK_1'}, {field: 'GENERAL_SCHOOL_WORK_2'},{field: 'GENERAL_SCHOOL_WORK_3'}, {field: 'GENERAL_SCHOOL_WORK_4'}, {field: 'GENERAL_SCHOOL_WORK_5'}, {field: 'GENERAL_SCHOOL_WORK_MONTH'},
                {field: 'GENERAL_SCHOOL_WORK_PERCENT'}],
            align: 'center',
            headerAlign: 'center'
        },
        {
            groupId: 'classwork',
            headerName: 'საშინაო დავალება III',
            description: '',
            // renderHeaderGroup: (params) => (
            //     <HeaderWithIcon {...params} icon={<BuildIcon fontSize="small" />} />
            // ),
            children: [{field: 'GENERAL_HOMEWORK_WRITE_ASSIGMENT_1'}, {field: 'GENERAL_HOMEWORK_WRITE_ASSIGMENT_2'},
                // {field: 'GENERAL_HOMEWORK_CREATIVE_ASSIGMENT'},
                {field: 'GENERAL_HOMEWORK_MONTHLY'}, {field: 'GENERAL_HOMEWORK_PERCENT'}, {field: 'GENERAL_HOMEWORK_WRITE_ASSIGMENT_3'}, {field: 'GENERAL_HOMEWORK_WRITE_ASSIGMENT_4'}],
            align: 'center',
            headerAlign: 'center'
        },
    ];

    const getGradeColumnModel = useCallback(() => {
        if (filters.academyClass && filters.academyClass.isTransit) {
            return columnGroupingModelTransit;
        } else {
            return columnGroupingModel;
        }
    }, [filters]);


    const processRowUpdate = useCallback(
        async (newRow) => {
            const gradeType = Object.keys(newRow).filter(field => field !== "student" && field !== "grades")[0]
            const gradesOfType = newRow.grades?.filter(g => g.gradeType === gradeType);
            newRow.exactMonth = newRow.exactMonth? newRow.exactMonth : Date.parse(filters.date);
            if (gradesOfType.length > 0 && gradesOfType[0].value !== undefined && gradesOfType[0].value !== null) {
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
                newRow.subject = filters.subject
                newRow.exact = filters.date
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
            <GradeTableToolbar filters={filters} setFilters={setFilters}/>
            <div style={{height: `calc(100vh - ${130}px)`, width: '98%', marginLeft: 15, marginRight: 15}}>
                <DataGridPaper>
                    {filters.subject?
                        <div style={{ textAlign:'center', marginTop: 10,marginBottom: 10, width: '100%',  backgroundColor:'white', fontSize:20, fontWeight:'bold'}}>
                           <div> {filters.subject.name + " - " + (filters.subject.teacher? filters.subject.teacher : "")}</div>
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
                        columnGroupingModel={getGradeColumnModel()}
                        queryKey={"GRADES"}
                        columns={getGradeColumns()}
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

export default DashBoard;