import React, {useCallback, useEffect, useState} from "react";
import BehaviourTableToolbar from "./BehaviourTableToolbar";
import DataGridPaper from "../../components/grid/DataGridPaper";
import DataGridSGS from "../../components/grid/DataGrid";
import axios from "../../../utils/axios";
import useFetchBehaviour from "./useBehaviour";
import useUpdateBehaviourGrade from "./useUpdateBehaviourGrade";
import {getFiltersOfPage} from "../../../utils/filters";
import {useNotification} from "../../../contexts/notification-context";
import Modal from "../../../components/modals/Modal";
import TextField from "../../components/formik/TextField";

const BehaviourDashBoard = () => {
    const [filters, setFilters] = useState({groupByClause: 'STUDENT', ...getFiltersOfPage("BEHAVIOUR")});
    const {data, isLoading, isError, error, isSuccess} = useFetchBehaviour(filters);
    const {setErrorMessage} = useNotification();
    const [openRequestModal, setOpenRequestModal] = useState(false);
    const [descriptionText, setDescriptionText] = useState('');
    const [newRowToSave, setNewRowToSave] = useState({});
    const {mutateAsync: mutateRow} = useUpdateBehaviourGrade();

    useEffect(() =>{
        if(isError && error){
            setErrorMessage(error);
        }
    }, [isError, error])

    const getGradeColumns = useCallback(() => {
        if (filters.date) {
            if(new Date(filters.date).getMonth() === 0 || new Date(filters.date).getMonth() === 1 || new Date(filters.date).getMonth() === 8 || new Date(filters.date).getMonth() === 9 )
            return gradeColumnsLong;
        } else {
            return gradeColumns;
        }
        return gradeColumns
    }, [filters]);

    const gradeColumnsLong = [
        {
            headerName: "მოსწავლის გვარი, სახელი",
            renderCell: ({row}) => {
                return <div style={{height:50, justifyContent:'center', alignItems: 'center',}}>
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
        {
            headerName: "V კვირა",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_APPEARING_IN_UNIFORM_5");
                if(summary2.length === 0){
                    return ""
                }
                return summary2[0].value;
            },
            field: 'BEHAVIOUR_APPEARING_IN_UNIFORM_5',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 70,
            maxWidth: 70,
        },
        {
            headerName: "VI კვირა",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_APPEARING_IN_UNIFORM_6");
                if(summary2.length === 0){
                    return ""
                }
                return summary2[0].value;
            },
            field: 'BEHAVIOUR_APPEARING_IN_UNIFORM_6',
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
                const summary2 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_APPEARING_IN_UNIFORM_MONTHLY");
                if(summary2.length === 0){
                    return ""
                }
                return summary2[0].value === 0 ? '' : summary2[0].value;
            },
            field: 'BEHAVIOUR_APPEARING_IN_UNIFORM_MONTHLY',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 70,
            maxWidth: 70,
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
            headerName: "V კვირა",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_STUDENT_DELAYS_5");
                if(summary2.length === 0){
                    return ""
                }
                return summary2[0].value;
            },
            field: 'BEHAVIOUR_STUDENT_DELAYS_5',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 70,
            maxWidth: 70,
        },
        {
            headerName: "VI კვირა",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_STUDENT_DELAYS_6");
                if(summary2.length === 0){
                    return ""
                }
                return summary2[0].value;
            },
            field: 'BEHAVIOUR_STUDENT_DELAYS_6',
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
                return summary2[0].value === 0 ? '' : summary2[0].value;
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
        {
            headerName: "V კვირა",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_CLASSROOM_INVENTORY_5");
                if(summary2.length === 0){
                    return ""
                }
                return summary2[0].value;
            },
            field: 'BEHAVIOUR_CLASSROOM_INVENTORY_5',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 70,
            maxWidth: 70,
        },
        {
            headerName: "VI კვირა",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_CLASSROOM_INVENTORY_6");
                if(summary2.length === 0){
                    return ""
                }
                return summary2[0].value;
            },
            field: 'BEHAVIOUR_CLASSROOM_INVENTORY_6',
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
                const summary2 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_CLASSROOM_INVENTORY_MONTHLY");
                if(summary2.length === 0){
                    return ""
                }
                return summary2[0].value === 0 ? '' : summary2[0].value;
            },
            field: 'BEHAVIOUR_CLASSROOM_INVENTORY_MONTHLY',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 70,
            maxWidth: 70,
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
        {
            headerName: "V კვირა",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_STUDENT_HYGIENE_5");
                if(summary2.length === 0){
                    return ""
                }
                return summary2[0].value;
            },
            field: 'BEHAVIOUR_STUDENT_HYGIENE_5',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 70,
            maxWidth: 70,
        },
        {
            headerName: "VI კვირა",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_STUDENT_HYGIENE_6");
                if(summary2.length === 0){
                    return ""
                }
                return summary2[0].value;
            },
            field: 'BEHAVIOUR_STUDENT_HYGIENE_6',
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
                const summary2 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_STUDENT_HYGIENE_MONTHLY");
                if(summary2.length === 0){
                    return ""
                }
                return summary2[0].value === 0 ? '' : summary2[0].value;
            },
            field: 'BEHAVIOUR_STUDENT_HYGIENE_MONTHLY',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 70,
            maxWidth: 70,
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
        {
            headerName: "V კვირა",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_STUDENT_BEHAVIOR_5");
                if(summary2.length === 0){
                    return ""
                }
                return summary2[0].value;
            },
            field: 'BEHAVIOUR_STUDENT_BEHAVIOR_5',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 70,
            maxWidth: 70,
        },
        {
            headerName: "VI კვირა",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_STUDENT_BEHAVIOR_6");
                if(summary2.length === 0){
                    return ""
                }
                return summary2[0].value;
            },
            field: 'BEHAVIOUR_STUDENT_BEHAVIOR_6',
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
                const summary2 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_STUDENT_BEHAVIOR_MONTHLY");
                if (summary2.length === 0) {
                    return ""
                }
                return summary2[0].value === 0 ? '' : summary2[0].value;
            },
            field: 'BEHAVIOUR_STUDENT_BEHAVIOR_MONTHLY',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 70,
            maxWidth: 70,
        },
        {
            headerName: "I კვირა",
            renderCell: ({row}) => {
                const summary1 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_WEEK_AVERAGE_1");
                if (summary1.length === 0) {
                    return ""
                }
                return summary1[0].value;
            },
            field: 'BEHAVIOUR_WEEK_AVERAGE_1',
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
                const summary2 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_WEEK_AVERAGE_2");
                if (summary2.length === 0) {
                    return ""
                }
                return summary2[0].value;
            },
            field: 'BEHAVIOUR_WEEK_AVERAGE_2',
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
                const summary2 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_WEEK_AVERAGE_3");
                if (summary2.length === 0) {
                    return ""
                }
                return summary2[0].value;
            },
            field: 'BEHAVIOUR_WEEK_AVERAGE_3',
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
                const summary2 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_WEEK_AVERAGE_4");
                if (summary2.length === 0) {
                    return ""
                }
                return summary2[0].value;
            },
            field: 'BEHAVIOUR_WEEK_AVERAGE_4',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 70,
            maxWidth: 70,
        },
        {
            headerName: "V კვირა",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_WEEK_AVERAGE_5");
                if (summary2.length === 0) {
                    return ""
                }
                return summary2[0].value;
            },
            field: 'BEHAVIOUR_WEEK_AVERAGE_5',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 70,
            maxWidth: 70,
        },
        {
            headerName: "VI კვირა",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_WEEK_AVERAGE_6");
                if (summary2.length === 0) {
                    return ""
                }
                return summary2[0].value;
            },
            field: 'BEHAVIOUR_WEEK_AVERAGE_6',
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
                const summary2 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_WEEK_AVERAGE_MONTHLY");
                if (summary2.length === 0) {
                    return ""
                }
                return summary2[0].value === 0 ? '' : summary2[0].value;
            },
            field: 'BEHAVIOUR_WEEK_AVERAGE_MONTHLY',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 70,
            maxWidth: 70,
        },
        {
            headerName: "ქულა",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_MONTHLY");
                if (summary2.length === 0) {
                    return ""
                }
                return summary2[0].value === 0 ? '' : summary2[0].value;
            },
            field: 'BEHAVIOUR_MONTHLY',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 70,
            maxWidth: 70,
        },
    ];

    const gradeColumns = [
        {
            headerName: "მოსწავლის გვარი, სახელი",
            renderCell: ({row}) => {
                return <div style={{height:50, justifyContent:'center', alignItems: 'center',}}>
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
        {
            headerName: "თვე",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_APPEARING_IN_UNIFORM_MONTHLY");
                if(summary2.length === 0){
                    return ""
                }
                return summary2[0].value === 0 ? '' : summary2[0].value;
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
                return summary2[0].value === 0 ? '' : summary2[0].value;
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
                return summary2[0].value === 0 ? '' : summary2[0].value;
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
                return summary2[0].value === 0 ? '' : summary2[0].value;
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
        {
            headerName: "თვე",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_STUDENT_BEHAVIOR_MONTHLY");
                if(summary2.length === 0){
                    return ""
                }
                return summary2[0].value === 0 ? '' : summary2[0].value;
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
        {
            headerName: "I კვირა",
            renderCell: ({row}) => {
                const summary1 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_WEEK_AVERAGE_1");
                if (summary1.length === 0) {
                    return ""
                }
                return summary1[0].value;
            },
            field: 'BEHAVIOUR_WEEK_AVERAGE_1',
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
                const summary2 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_WEEK_AVERAGE_2");
                if (summary2.length === 0) {
                    return ""
                }
                return summary2[0].value;
            },
            field: 'BEHAVIOUR_WEEK_AVERAGE_2',
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
                const summary2 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_WEEK_AVERAGE_3");
                if (summary2.length === 0) {
                    return ""
                }
                return summary2[0].value;
            },
            field: 'BEHAVIOUR_WEEK_AVERAGE_3',
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
                const summary2 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_WEEK_AVERAGE_4");
                if (summary2.length === 0) {
                    return ""
                }
                return summary2[0].value;
            },
            field: 'BEHAVIOUR_WEEK_AVERAGE_4',
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
                const summary2 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_WEEK_AVERAGE_MONTHLY");
                if (summary2.length === 0) {
                    return ""
                }
                return summary2[0].value === 0 ? '' : summary2[0].value;
            },
            field: 'BEHAVIOUR_WEEK_AVERAGE_MONTHLY',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 70,
            maxWidth: 70,
        },
        {
            headerName: "ქულა",
            renderCell: ({row}) => {
                const summary2 = row.grades?.filter(grade => grade.gradeType === "BEHAVIOUR_MONTHLY");
                if(summary2.length === 0){
                    return ""
                }
                return summary2[0].value === 0 ? '' : summary2[0].value;
            },
            field: 'BEHAVIOUR_MONTHLY',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            editable: true,
            type: "number",
            width: 50,
            maxWidth: 50,
        },
    ];

    const getGradeColumnGrouping = useCallback(() => {
        if (filters.date) {
            if(new Date(filters.date).getMonth() === 0 || new Date(filters.date).getMonth() === 1 || new Date(filters.date).getMonth() === 8 || new Date(filters.date).getMonth() === 9 )
                return columnGroupingModelLong;
        } else {
            return columnGroupingModel;
        }
        return columnGroupingModel;
    }, [filters]);

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
            groupId: 'uniform',
            headerName: 'მოსწავლის ფორმით გამოცხადება',
            description: '',
            // renderHeaderGroup: (params) => (
            //     <HeaderWithIcon {...params} icon={<BuildIcon fontSize="small" />} />
            // ),
            children: [{ field: 'BEHAVIOUR_APPEARING_IN_UNIFORM_1' }, {field: 'BEHAVIOUR_APPEARING_IN_UNIFORM_2'},
                {field: 'BEHAVIOUR_APPEARING_IN_UNIFORM_3'}, {field: 'BEHAVIOUR_APPEARING_IN_UNIFORM_4'},
                {field: 'BEHAVIOUR_APPEARING_IN_UNIFORM_5'},{field: 'BEHAVIOUR_APPEARING_IN_UNIFORM_6'},{field: 'BEHAVIOUR_APPEARING_IN_UNIFORM_MONTHLY'}],
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
                {field: 'BEHAVIOUR_STUDENT_DELAYS_5'}, {field: 'BEHAVIOUR_STUDENT_DELAYS_6'},{field: 'BEHAVIOUR_STUDENT_DELAYS_MONTHLY'}],
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
                {field: 'BEHAVIOUR_CLASSROOM_INVENTORY_5'},{field: 'BEHAVIOUR_CLASSROOM_INVENTORY_6'},
                {field: 'BEHAVIOUR_CLASSROOM_INVENTORY_MONTHLY'}],
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
                {field: 'BEHAVIOUR_STUDENT_HYGIENE_5'},{field: 'BEHAVIOUR_STUDENT_HYGIENE_6'},{field: 'BEHAVIOUR_STUDENT_HYGIENE_MONTHLY'}],
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
            children: [{field: 'BEHAVIOUR_STUDENT_BEHAVIOR_1'}, {field: 'BEHAVIOUR_STUDENT_BEHAVIOR_2'},
                {field: 'BEHAVIOUR_STUDENT_BEHAVIOR_3'}, {field: 'BEHAVIOUR_STUDENT_BEHAVIOR_4'},
                {field: 'BEHAVIOUR_STUDENT_BEHAVIOR_5'}, {field: 'BEHAVIOUR_STUDENT_BEHAVIOR_6'},
                {field: 'BEHAVIOUR_STUDENT_BEHAVIOR_MONTHLY'}],
            align: 'center',
            headerAlign: 'center'
        },
        {
            groupId: 'weekly',
            headerName: 'ჯამური ქულები',
            description: '',
            // renderHeaderGroup: (params) => (
            //     <HeaderWithIcon {...params} icon={<BuildIcon fontSize="small" />} />
            // ),
            children: [{field: 'BEHAVIOUR_WEEK_AVERAGE_1'}, {field: 'BEHAVIOUR_WEEK_AVERAGE_2'},
                {field: 'BEHAVIOUR_WEEK_AVERAGE_3'}, {field: 'BEHAVIOUR_WEEK_AVERAGE_4'},
                {field: 'BEHAVIOUR_WEEK_AVERAGE_5'}, {field: 'BEHAVIOUR_WEEK_AVERAGE_6'},
                {field: 'BEHAVIOUR_WEEK_AVERAGE_MONTHLY'}],
            align: 'center',
            headerAlign: 'center'
        },

    ];

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
                {field: 'BEHAVIOUR_STUDENT_DELAYS_5'},{field: 'BEHAVIOUR_STUDENT_DELAYS_MONTHLY'}],
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
                {field: 'BEHAVIOUR_CLASSROOM_INVENTORY_5'},{field: 'BEHAVIOUR_CLASSROOM_INVENTORY_MONTHLY'}],
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
                {field: 'BEHAVIOUR_STUDENT_HYGIENE_5'},{field: 'BEHAVIOUR_STUDENT_HYGIENE_MONTHLY'}],
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
                {field: 'BEHAVIOUR_STUDENT_BEHAVIOR_5'},{field: 'BEHAVIOUR_STUDENT_BEHAVIOR_MONTHLY'}],
            align: 'center',
            headerAlign: 'center'
        },
        {
            groupId: 'weekly',
            headerName: 'ჯამური ქულები',
            description: '',
            // renderHeaderGroup: (params) => (
            //     <HeaderWithIcon {...params} icon={<BuildIcon fontSize="small" />} />
            // ),
            children: [{field: 'BEHAVIOUR_WEEK_AVERAGE_1'}, {field: 'BEHAVIOUR_WEEK_AVERAGE_2'},
                {field: 'BEHAVIOUR_WEEK_AVERAGE_3'}, {field: 'BEHAVIOUR_WEEK_AVERAGE_4'},
                {field: 'BEHAVIOUR_WEEK_AVERAGE_MONTHLY'}],
            align: 'center',
            headerAlign: 'center'
        },

    ];

    const processRowUpdate = useCallback(
        async (newRow) => {
            const gradeType = Object.keys(newRow).filter(field => field !== "student" && field !== "grades")[0]
            const gradesOfType = newRow.grades?.filter(g => g.gradeType === gradeType)
            newRow.exactMonth = newRow.exactMonth? newRow.exactMonth : Date.parse(filters.date);
            if (gradesOfType.length > 0 && gradesOfType[0].value !== undefined && gradesOfType[0].value !== null) {
                const params = {
                    academyClassId: gradesOfType[0].academyClass.id,
                    gradePrefix: "BEHAVIOUR",
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


            // if (gradesOfType > 0) {
            //     setNewRowToSave({newValue: newRow[gradeType], gradeId: gradesOfType[0].id})
            //     setOpenRequestModal(true);
            // } else {
            //     newRow.subject = filters.subject
            //     newRow.exactMonth = newRow.exactMonth? newRow.exactMonth : Date.parse(filters.date);
            //     return await mutateRow(newRow);
            // }
        },
        [mutateRow, filters],
    );

    const onProcessRowUpdateError = useCallback(async (newRow) => {

    }, [filters]);

    if (isLoading) {
        return <div></div>
    }

    return (
        <div>
            <BehaviourTableToolbar filters={filters} setFilters={setFilters}/>
            <div style={{height: `calc(100vh - ${130}px)`, marginLeft:15, marginRight:15,}}>
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
                        experimentalFeatures={{ columnGrouping: true }}
                        columnGroupingModel={getGradeColumnGrouping()}
                        columns={getGradeColumns()}
                        queryKey={"BEHAVIOUR"}
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
                <p>თუ გსურთ ნიშნის ცვლილების მოთხოვნა, შეავსეთ ახსნა განმარტების ველი:</p>
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

export default BehaviourDashBoard;