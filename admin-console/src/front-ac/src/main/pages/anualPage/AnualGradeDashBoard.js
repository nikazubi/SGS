import React, {useCallback, useEffect, useState} from "react";
import DataGridPaper from "../../components/grid/DataGridPaper";
import DataGridSGS from "../../components/grid/DataGrid";
import AnualGradeToolbar from "./AnualGradeToolbar";
import "./header.css"
import useGradeAnual from "./useGradeAnual";
import {getFiltersOfPage} from "../../../utils/filters";
import {fetchSubjects} from "../../../hooks/useSubjects";
import axios from "../../../utils/axios";
import ConfirmationModal from "../../../components/modals/ConfirmationModal";
import useUpdateBehaviourGrade from "../behaviourPage/useUpdateBehaviourGrade";
import useUpdateFinalExamGrade from "./useUpdateFinalExam";

const AnualGradeDashBoard = () => {
    const [filters, setFilters] = useState({...getFiltersOfPage("ANNUAL_GRADE")});
    const [subjects, setSubjects] = useState([]);
    const [openRequestModal, setOpenRequestModal] = useState(false);
    const [newRowToSave, setNewRowToSave] = useState({});
    const {mutateAsync: mutateRow} = useUpdateFinalExamGrade();
    const [checked, setChecked] = useState(false);

    const {data, isLoading, isError, error, isSuccess} = useGradeAnual(filters);

    useEffect(()=>{
        const getSubjects = async () => {
            const param = {queryKey: ""};
            const subjectArr = await fetchSubjects(param);
            setSubjects(subjectArr);
        }
        getSubjects();
    },[])

    const getSemesterFields = useCallback(() => {
        if (!subjects) {
            return [];
        }

        const semesterFields = [];

        const names = [
            "I სემესტრი",
            "II სემესტრი",
            "გამოცდა",
            "წლიური"
        ]

        const semesterNames = [
            {val: "I სემესტრი", ind: 1},
            {val: "II სემესტრი", ind: 2},
            {val: "გამოცდა", ind: 3},
            {val: "წლიური", ind: 4}
        ];

        semesterFields.push({
            headerName: "მოსწავლის გვარი, სახელი",
            renderCell: ({ row }) => {
                return <div style={{ height: 50, justifyContent: 'center', alignItems: 'center', display: 'flex' }}>
                    {row.student.lastName + " " + row.student.firstName}</div>
            },
            field: 'firstName',
            sortable: false,
            headerAlign: 'center',
            align: 'center',
            width: 200,
            maxWidth: 200,
        });

        for (const semester of semesterNames) {
            for (const subject of subjects) {
                semesterFields.push({
                    headerName: names[semester.ind - 1] || '',
                    description: '',
                    renderCell: ({row}) => {
                        const transformedArray = row.gradeList.map(item => ({
                            subjectName: item.subject.name,
                            value: item.value
                        }));
                        const monthValue = transformedArray.find(item => item.subjectName === subject.name)?.value[semester.ind];

                        return <div>{monthValue === 0 ? '' : monthValue === '' || monthValue === undefined || monthValue === null ? '' : checked? Number(monthValue) + 3 : monthValue}</div>;
                        // return <div>{transformedArray.value[month.month] === 0 ? '' : transformedArray.value[month.month]}</div>;
                    },
                    editable: semester.val === "გამოცდა",
                    field: subject.name + "-" + semester.ind,
                    sortable: false,
                    align: 'center',
                    headerAlign: 'center'
                });
            }
        }

        semesterFields.sort((a, b) => {
            const [subjectA, monthA] = a.field.split('-');
            const [subjectB, monthB] = b.field.split('-');

            if (subjectA !== subjectB) {
                return subjectA.localeCompare(subjectB);
            } else {
                return parseInt(monthA) - parseInt(monthB);
            }
        });
        return semesterFields;
    }, [data, subjects, checked]);


    const gradeColumns = [
        {
            headerName: "მოსწავლე",
            renderCell: ({row}) => {
                return ( <div  >
                    {row.name}
                </div>);
            },
            renderHeader: (params) => (
                <div style={{writingMode: "vertical-rl", height:150,textAlign:'center', fontSize:16}}>
                    {'მოსწავლე'}
                </div>
            ),
            field: 'name',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
        },
        {
            headerName: "ქართული ლიტ",
            renderCell: ({row}) => {
                return (<div  >
                    {row.geo}
                </div>);
            },
            renderHeader: (params) => (
                <div style={{writingMode: "vertical-rl", height:150, textAlign:'center', fontSize:16}}>
                    {'ქართული ლიტ '}
                </div>
            ),
            field: 'geo',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header"
        },
        {
            headerName: "ქართული ენა",
            renderCell: ({row}) => {
                return (<div  >
                    {row.geolang}
                </div>);
            },
            renderHeader: (params) => (
                <div style={{writingMode: "vertical-rl", height:150, textAlign:'center', fontSize:16}}>
                    {'ქართული ენა'}
                </div>
            ),
            field: 'geolang',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header"

        },
        {
            headerName: "ქართული წერა",
            renderCell: ({row}) => {
                return (<div  >
                    {row.write}
                </div>);
            },
            renderHeader: (params) => (
                <div style={{writingMode: "vertical-rl", height:150, textAlign:'center', fontSize:16}}>
                    {'ქართული წერა'}
                </div>
            ),
            field: 'write',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header"
        },
        {
            headerName: "მათემატიკა",
            renderCell: ({row}) => {
                return (<div  >
                    {row.math}
                </div>);
            },
            renderHeader: (params) => (
                <div style={{writingMode: "vertical-rl", height:150, textAlign:'center', fontSize:16}}>
                    {'მათემატიკა'}
                </div>
            ),
            field: 'math',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header"
        },
        {
            headerName: "ინგლისური",
            renderCell: ({row}) => {
                return (<div  >
                    {row.eng}
                </div>);
            },
            renderHeader: (params) => (
                <div style={{writingMode: "vertical-rl", height:150, textAlign:'center', fontSize:16}}>
                    {'ინგლისური'}
                </div>
            ),
            field: 'eng',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header"
        },
        {
            headerName: "ინგლისური ლიტ",
            renderCell: ({row}) => {
                return (<div  >
                    {row.englit}
                </div>);
            },
            renderHeader: (params) => (
                <div style={{writingMode: "vertical-rl", height:150, textAlign:'center', fontSize:16}}>
                    {'ინგლისური ლიტ'}
                </div>
            ),
            field: 'englit',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header"
        },
        {
            headerName: "გერმანული",
            renderCell: ({row}) => {
                return (<div  >
                    {row.german}
                </div>);
            },
            renderHeader: (params) => (
                <div style={{writingMode: "vertical-rl", height:150, textAlign:'center', fontSize:16}}>
                    {'გერმანული'}
                </div>
            ),
            field: 'german',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header"
        },
        {
            headerName: "რუსული ენა",
            renderCell: ({row}) => {
                return (<div  >
                    {row.russia}
                </div>);
            },
            renderHeader: (params) => (
                <div style={{writingMode: "vertical-rl", height:150, textAlign:'center', fontSize:16}}>
                    {'რუსული ენა'}
                </div>
            ),
            field: 'russia',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header"
        },
        {
            headerName: "ბიოლოგია",
            renderCell: ({row}) => {
                return (<div  >
                    {row.bio}
                </div>);
            },
            renderHeader: (params) => (
                <div style={{writingMode: "vertical-rl", height:150, textAlign:'center', fontSize:16}}>
                    {'ბიოლოგია'}
                </div>
            ),
            field: 'bio',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header"
        },
        {
            headerName: "ქიმია",
            renderCell: ({row}) => {
                return (<div  >
                    {row.chemistry}
                </div>);
            },
            renderHeader: (params) => (
                <div style={{writingMode: "vertical-rl", height:150, textAlign:'center', fontSize:16}}>
                    {'ქიმია'}
                </div>
            ),
            field: 'chemistry',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header"
        },
        {
            headerName: "ფიზიკა",
            renderCell: ({row}) => {
                return (<div  >
                    {row.phisic}
                </div>);
            },
            renderHeader: (params) => (
                <div style={{writingMode: "vertical-rl", height:150, textAlign:'center', fontSize:16}}>
                    {'ფიზიკა'}
                </div>
            ),
            field: 'physic',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header"
        },
        {
            headerName: "ისტორია",
            renderCell: ({row}) => {
                return (<div  >
                    {row.history}
                </div>);
            },
            renderHeader: (params) => (
                <div style={{writingMode: "vertical-rl", height:150, textAlign:'center', fontSize:16}}>
                    {'ისტორია'}
                </div>
            ),
            field: 'history',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header"
        },
        {
            headerName: "გეოგრაფია",
            renderCell: ({row}) => {
                return (<div  >
                    {row.geography}
                </div>);
            },
            renderHeader: (params) => (
                <div style={{writingMode: "vertical-rl", height:150, textAlign:'center', fontSize:16}}>
                    {'გეოგრაფია'}
                </div>
            ),
            field: 'geography',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header"
        },
        {
            headerName: "მოქალაქეობა",
            renderCell: ({row}) => {
                return (<div  >
                    {row.nationaly}
                </div>);
            },
            renderHeader: (params) => (
                <div style={{writingMode: "vertical-rl", height:150, textAlign:'center', fontSize:16}}>
                    {'მოქალაქეობა'}
                </div>
            ),
            field: 'nationaly',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header"
        },
        {
            headerName: "ჰუმ. აზროვნება",
            renderCell: ({row}) => {
                return (<div  >
                    {row.hum}
                </div>);
            },
            renderHeader: (params) => (
                <div style={{writingMode: "vertical-rl", height:150, textAlign:'center', fontSize:16}}>
                    {'ჰუმ. აზროვნება'}
                </div>
            ),
            field: 'hum',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header"
        },
        {
            headerName: "სპორტი",
            renderCell: ({row}) => {
                return (<div  >
                    {row.sport}
                </div>);
            },
            renderHeader: (params) => (
                <div style={{writingMode: "vertical-rl", height:150, textAlign:'center', fontSize:16}}>
                    {'სპორტი'}
                </div>
            ),
            field: 'sport',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header"
        },
        {
            headerName: "ეთიკური ნორმა",
            renderCell: ({row}) => {
                return (<div  >
                    {row.ethic}
                </div>);
            },
            renderHeader: (params) => (
                <div style={{writingMode: "vertical-rl", height:150, textAlign:'center', fontSize:16}}>
                    {'ეთიკური ნორმა'}
                </div>
            ),
            field: 'ethic',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header"
        },
        {
            headerName: "რეიტინგი",
            renderCell: ({row}) => {
                return (<div  >
                    {row.rating}
                </div>);
            },
            renderHeader: (params) => (
                <div style={{writingMode: "vertical-rl", height:150, textAlign:'center', fontSize:16}}>
                    {'რეიტინგი'}
                </div>
            ),
            field: 'rating',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header"
        },
        {
            headerName: "გაცდენილი საათი",
            renderCell: ({row}) => {
                return (<div  >
                    {row.absent}
                </div>);
            },
            renderHeader: (params) => (
                <div style={{writingMode: "vertical-rl", height:150, textAlign:'center', fontSize:16}}>
                    {'გაცდენილი საათი'}
                </div>
            ),
            field: 'absent',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header"
        },
        {
            headerName: "შენიშვნა",
            renderCell: ({row}) => {
                return (<div  >
                    {row.mistake}
                </div>);
            },
            renderHeader: (params) => (
                <div style={{writingMode: "vertical-rl", height:150,fontSize:16, textAlign:'center'}}>
                    {'შენიშვნა'}
                </div>
            ),
            field: 'mistake',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header"
        },

    ];

    let gradeClomuns2 = []

    const getGradeColumns = useCallback(() =>{
        if(data && data.length > 0){
            gradeClomuns2 = [        {
                headerName: "მოსწავლე",
                renderCell: ({row}) => {
                    return ( <div>
                        {row.student.firstName + ' ' + row.student.lastName}
                    </div>);
                },
                renderHeader: (params) => (
                    <div style={{writingMode: "vertical-rl", height:150,textAlign:'center', fontSize:16}}>
                        {'მოსწავლე'}
                    </div>
                ),
                field: 'name',
                sortable: false,
                align: 'center',
                headerAlign: 'center',
                width: 200,
                maxWidth: 200,
            }];
            data[0].gradeList.forEach((o, index) => {
                gradeClomuns2 = [ ...gradeClomuns2, {
                    groupId: o.subject.name,
                    headerName: o.subject.name,
                    renderHeader: (params) => (
                        <div style={{writingMode: "vertical-rl", height:150,textAlign:'center', fontSize:16}}>
                        </div>
                    ),
                    children: [
                        {field: o.subject.name + "-" + "1"},
                        {field: o.subject.name + "-" + "2"},
                        {field: o.subject.name + "-" + "3"}
                    ],
                    sortable: false,
                    align: 'center',
                    headerAlign: 'center',
                    width: 200,
                    maxWidth: 200,
                }]
            })
            return gradeClomuns2
        }
        return gradeColumns
    }, [data])

    //TODO request change
    const processRowUpdate = useCallback(
        async (newRow) => {
            // const gradeType = "FINAL_EXAM"
            // console.log(newRow[Object.keys(newRow).filter(key => key.toString().includes("-3"))])
            // const gradesOfType = newRow.grades?.filter(g => g.gradeType === gradeType)
            // console.log(newRow)
            // console.log(gradesOfType)
            // if (gradesOfType > 0) {
            //     setNewRowToSave({newValue: newRow[gradeType], gradeId: gradesOfType[0].id})
            //     setOpenRequestModal(true);
            // } else {
                newRow.subject = filters.subject
                newRow.exactMonth = newRow.exactMonth? newRow.exactMonth : Date.parse(filters.date);
                return await mutateRow(newRow);
            // }
        },
        [mutateRow, filters],
    );

    return (
        <div className={"semesterGradeCnt"}>
            <AnualGradeToolbar filters={filters} setFilters={setFilters} checked={checked} setChecked={setChecked}/>
            <div style={{height: `calc(100vh - ${130}px)`, width: '100%'}}>
                <DataGridPaper>
                    <DataGridSGS
                        sx={{
                            overflowX: 'hidden',
                            '& .MuiDataGrid-columnHeader, .MuiDataGrid-cell': {
                                border: `1px solid ${
                                    '#98c9d7'
                                }`,
                            },
                        }}
                        queryKey={"ANNUAL_GRADE"}
                        experimentalFeatures={{columnGrouping: true}}
                        columns={getSemesterFields()}
                        columnGroupingModel={getGradeColumns()}
                        rows={data ? data : []}
                        getRowId={(row) => {
                            return row.student.id;
                        }}
                        processRowUpdate={processRowUpdate}
                        getRowHeight={() => 50}
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

export default AnualGradeDashBoard;