import React, {useCallback, useEffect, useState} from "react";
import DataGridPaper from "../../components/grid/DataGridPaper";
import DataGridSGS from "../../components/grid/DataGrid";
import SemesterGradeToolbar from "./SemesterGradeToolbar";
import "./header.css"
import useGradeSemester from "./useGradeSemester";
import {getFiltersOfPage} from "../../../utils/filters";
import useSubjects, {fetchSubjects} from "../../../hooks/useSubjects";
import axios from "../../../utils/axios";

const SemesterGradeDashBoard = () => {
    const [filters, setFilters] = useState({...getFiltersOfPage("SEMESTER_GRADE")});
    const [subjects, setSubjects] = useState([]);
    // const param = {queryKey: ""};
    // const subjects = fetchSubjects(param);
    const {data, isLoading, isError, error, isSuccess} = useGradeSemester(filters);

    useEffect(()=>{
       const getSubjects = async () => {
           const param = {queryKey: ""};
           const subjectArr = await fetchSubjects(param);
           setSubjects(subjectArr);
       }
       getSubjects();
    },[])

    const getMonthFields = useCallback(() => {
        if (!subjects) {
            return [];
        }

        const monthFields = [];
        const monthNames = [
            'იანვარი-თებერვალი',
            'მარტი',
            'აპრილი',
            'მაისი',
            'ივნისი',
            'სექტემბერი-ოქტომბერი',
            'ნოემბერი',
            'დეკემბერი'
        ];

        const secondSemesterMonths = [
            { month: 1, ind: 0 },  // ianvari-tebervali
            { month: 3, ind: 1 },  // marti
            { month: 4, ind: 2 },  // aprili
            { month: 5, ind: 3 },  // maisu
            { month: 6, ind: 4 } // ivnisi
        ];
        const firstSemesterMonths = [
            { month: 9, ind: 5 },  // September-October
            { month: 11, ind: 6 },  // noemberi
            { month: 12, ind: 7 }
        ];

        const selectedMonths = filters.semesterN?.value === 'firstSemester' ? firstSemesterMonths : secondSemesterMonths;
        console.log("selectedMonths", selectedMonths)
        monthFields.push({
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

        for (const month of selectedMonths) {
            for (const subject of subjects) {
                monthFields.push({
                    headerName: monthNames[month.ind] || '',
                    description: '',
                    renderCell: ({row}) => {
                        console.log("assssssssssssssssss", row)
                        const transformedArray = row.gradeList.map(item => ({
                            subjectName: item.subject.name,
                            value: item.value
                        }));
                        console.log("transformedArray", transformedArray)
                        console.log(transformedArray.find(item => item.subjectName === subject.name))
                        console.log("subjectName", subject.name)
                        const monthValue = transformedArray.find(item => item.subjectName === subject.name)?.value[month.month];
                        console.log("Month.month", month.month)
                        console.log(monthValue)

                        return <div>{monthValue === 0 ? '' : monthValue}</div>;
                        // return <div>{transformedArray.value[month.month] === 0 ? '' : transformedArray.value[month.month]}</div>;
                    },
                    field: subject.name + "-" + month.month,
                    sortable: false,
                    align: 'center',
                    headerAlign: 'center'
                });
            }
        }

        console.log("before sort", monthFields);
        monthFields.sort((a, b) => {
            const [subjectA, monthA] = a.field.split('-');
            const [subjectB, monthB] = b.field.split('-');

            if (subjectA !== subjectB) {
                return subjectA.localeCompare(subjectB);
            } else {
                return parseInt(monthA) - parseInt(monthB);
            }
        });
        console.log("after sort", monthFields);
        return monthFields;
    }, [data, subjects, filters.semesterN?.value]);

    // const getMonthFields = useCallback(() => {
    //     if(!subjects) {
    //         return [];
    //     }
    //     if (filters.semesterN?.value === 'firstSemester') {
    //         return [{
    //             headerName: "მოსწავლის გვარი, სახელი",
    //             renderCell: ({row}) => {
    //                 return <div style={{height: 50, justifyContent: 'center', alignItems: 'center', display: 'flex'}}>
    //                     {row.student.lastName + " " + row.student.firstName}</div>
    //             },
    //             field: 'firstName',
    //             sortable: false,
    //             headerAlign: 'center',
    //             align: 'center',
    //             width: 200,
    //             maxWidth: 200,
    //         },
    //         subjects.map((o) => {
    //             return {
    //                 headerName: 'სექტემბერი-ოქტომბერი',
    //                 description: '',
    //                 field: "9-" + o.name,
    //                 sortable: false,
    //                 align: 'center',
    //                 headerAlign: 'center'
    //             }
    //         }),
    //         subjects.map((o) => {
    //             return {
    //                 headerName: 'ნოემბერი',
    //                 description: '',
    //                 field: "11-" + o.name,
    //                 sortable: false,
    //                 align: 'center',
    //                 headerAlign: 'center'
    //             }
    //         }),
    //         subjects.map((o) => {
    //             return {
    //                 headerName: 'დეკემბერი',
    //                 description: '',
    //                 field: "12-" + o.name,
    //                 sortable: false,
    //                 align: 'center',
    //                 headerAlign: 'center'
    //             }
    //         }),
    //         ]
    //     } else {
    //         return [        {
    //             headerName: "მოსწავლის გვარი, სახელი",
    //             renderCell: ({row}) => {
    //                 return <div style={{height: 50, justifyContent: 'center', alignItems: 'center', display: 'flex'}}>
    //                     {row.student.lastName + " " + row.student.firstName}</div>
    //             },
    //             field: 'firstName',
    //             sortable: false,
    //             headerAlign: 'center',
    //             align: 'center',
    //             width: 200,
    //             maxWidth: 200,
    //         },
    //         subjects.map((o) => {
    //             return {
    //                 headerName: 'იანვარი-თებერვალი',
    //                 description: '',
    //                 field: "0-" + o.name,
    //                 sortable: false,
    //                 align: 'center',
    //                 headerAlign: 'center'
    //             }
    //         }),
    //         subjects.map((o) => {
    //             return {
    //                 headerName: 'მარტი',
    //                 description: '',
    //                 field: "3-" + o.name,
    //                 sortable: false,
    //                 align: 'center',
    //                 headerAlign: 'center'
    //             }
    //         }),
    //         subjects.map((o) => {
    //             return {
    //                 headerName: 'აპრილი',
    //                 description: '',
    //                 field: "4-" + o.name,
    //                 sortable: false,
    //                 align: 'center',
    //                 headerAlign: 'center'
    //             }
    //         }),
    //         subjects.map((o) => {
    //             return {
    //                 headerName: 'მაისი',
    //                 description: '',
    //                 field: "5-" + o.name,
    //                 sortable: false,
    //                 align: 'center',
    //                 headerAlign: 'center'
    //             }
    //         }),
    //         subjects.map((o) => {
    //             return {
    //                 headerName: 'ივნისი',
    //                 description: '',
    //                 field: "6-" + o.name,
    //                 sortable: false,
    //                 align: 'center',
    //                 headerAlign: 'center'
    //             }
    //         })
    //         ]
    //     }
    // }, [data, subjects])

    const gradeColumns = [
        {
            headerName: "მოსწავლე",
            renderCell: ({row}) => {
                return (<div>
                    {row.name}
                </div>);
            },
            renderHeader: (params) => (
                <div style={{writingMode: "vertical-rl", height: 150, textAlign: 'center', fontSize: 16}}>
                    {'მოსწავლე'}
                </div>
            ),
            field: 'name',
            sortable: false,
            align: 'center',
            headerAlign: 'center',

            width: 90,
            maxWidth: 90,
        },
        {
            headerName: "ქართული ლიტ",
            renderCell: ({row}) => {
                return (<div>
                    {row.geo}
                </div>);
            },
            renderHeader: (params) => (
                <div style={{writingMode: "vertical-rl", height: 150, textAlign: 'center', fontSize: 16}}>
                    {'ქართული ლიტ '}
                </div>
            ),
            field: 'geo',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header",

            width: 90,
            maxWidth: 90,
        },
        {
            headerName: "ქართული ენა",
            renderCell: ({row}) => {
                return (<div>
                    {row.geolang}
                </div>);
            },
            renderHeader: (params) => (
                <div style={{writingMode: "vertical-rl", height: 150, textAlign: 'center', fontSize: 16}}>
                    {'ქართული ენა'}
                </div>
            ),
            field: 'geolang',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header",

            width: 90,
            maxWidth: 90,

        },
        {
            headerName: "ქართული წერა",
            renderCell: ({row}) => {
                return (<div>
                    {row.write}
                </div>);
            },
            renderHeader: (params) => (
                <div style={{writingMode: "vertical-rl", height: 150, textAlign: 'center', fontSize: 16}}>
                    {'ქართული წერა'}
                </div>
            ),
            field: 'write',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header",

            width: 90,
            maxWidth: 90,
        },
        {
            headerName: "მათემატიკა",
            renderCell: ({row}) => {
                return (<div>
                    {row.math}
                </div>);
            },
            renderHeader: (params) => (
                <div style={{writingMode: "vertical-rl", height: 150, textAlign: 'center', fontSize: 16}}>
                    {'მათემატიკა'}
                </div>
            ),
            field: 'math',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header",

            width: 90,
            maxWidth: 90,
        },
        {
            headerName: "ინგლისური",
            renderCell: ({row}) => {
                return (<div>
                    {row.eng}
                </div>);
            },
            renderHeader: (params) => (
                <div style={{writingMode: "vertical-rl", height: 150, textAlign: 'center', fontSize: 16}}>
                    {'ინგლისური'}
                </div>
            ),
            field: 'eng',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header",

            width: 90,
            maxWidth: 90,
        },
        {
            headerName: "ინგლისური ლიტ",
            renderCell: ({row}) => {
                return (<div>
                    {row.englit}
                </div>);
            },
            renderHeader: (params) => (
                <div style={{writingMode: "vertical-rl", height: 150, textAlign: 'center', fontSize: 16}}>
                    {'ინგლისური ლიტ'}
                </div>
            ),
            field: 'englit',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header",

            width: 90,
            maxWidth: 90,
        },
        {
            headerName: "გერმანული",
            renderCell: ({row}) => {
                return (<div>
                    {row.german}
                </div>);
            },
            renderHeader: (params) => (
                <div style={{writingMode: "vertical-rl", height: 150, textAlign: 'center', fontSize: 16}}>
                    {'გერმანული'}
                </div>
            ),
            field: 'german',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header",

            width: 90,
            maxWidth: 90,

        },
        {
            headerName: "რუსული ენა",
            renderCell: ({row}) => {
                return (<div>
                    {row.russia}
                </div>);
            },
            renderHeader: (params) => (
                <div style={{writingMode: "vertical-rl", height: 150, textAlign: 'center', fontSize: 16}}>
                    {'რუსული ენა'}
                </div>
            ),
            field: 'russia',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header",

            width: 90,
            maxWidth: 90,
        },
        {
            headerName: "ბიოლოგია",
            renderCell: ({row}) => {
                return (<div>
                    {row.bio}
                </div>);
            },
            renderHeader: (params) => (
                <div style={{writingMode: "vertical-rl", height: 150, textAlign: 'center', fontSize: 16}}>
                    {'ბიოლოგია'}
                </div>
            ),
            field: 'bio',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header",

            width: 90,
            maxWidth: 90,
        },
        {
            headerName: "ქიმია",
            renderCell: ({row}) => {
                return (<div>
                    {row.chemistry}
                </div>);
            },
            renderHeader: (params) => (
                <div style={{writingMode: "vertical-rl", height: 150, textAlign: 'center', fontSize: 16}}>
                    {'ქიმია'}
                </div>
            ),
            field: 'chemistry',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header",

            width: 90,
            maxWidth: 90,
        },
        {
            headerName: "ფიზიკა",
            renderCell: ({row}) => {
                return (<div>
                    {row.phisic}
                </div>);
            },
            renderHeader: (params) => (
                <div style={{writingMode: "vertical-rl", height: 150, textAlign: 'center', fontSize: 16}}>
                    {'ფიზიკა'}
                </div>
            ),
            field: 'physic',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header",

            width: 90,
            maxWidth: 90,
        },
        {
            headerName: "ისტორია",
            renderCell: ({row}) => {
                return (<div>
                    {row.history}
                </div>);
            },
            renderHeader: (params) => (
                <div style={{writingMode: "vertical-rl", height: 150, textAlign: 'center', fontSize: 16}}>
                    {'ისტორია'}
                </div>
            ),
            field: 'history',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header",

            width: 90,
            maxWidth: 90,
        },
        {
            headerName: "გეოგრაფია",
            renderCell: ({row}) => {
                return (<div>
                    {row.geography}
                </div>);
            },
            renderHeader: (params) => (
                <div style={{writingMode: "vertical-rl", height: 150, textAlign: 'center', fontSize: 16}}>
                    {'გეოგრაფია'}
                </div>
            ),
            field: 'geography',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header",

            width: 90,
            maxWidth: 90,
        },
        {
            headerName: "მოქალაქეობა",
            renderCell: ({row}) => {
                return (<div>
                    {row.nationaly}
                </div>);
            },
            renderHeader: (params) => (
                <div style={{writingMode: "vertical-rl", height: 150, textAlign: 'center', fontSize: 16}}>
                    {'მოქალაქეობა'}
                </div>
            ),
            field: 'nationaly',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header",

            width: 90,
            maxWidth: 90,
        },
        {
            headerName: "ჰუმ. აზროვნება",
            renderCell: ({row}) => {
                return (<div>
                    {row.hum}
                </div>);
            },
            renderHeader: (params) => (
                <div style={{writingMode: "vertical-rl", height: 150, textAlign: 'center', fontSize: 16}}>
                    {'ჰუმ. აზროვნება'}
                </div>
            ),
            field: 'hum',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header",

            width: 90,
            maxWidth: 90,
        },
        {
            headerName: "სპორტი",
            renderCell: ({row}) => {
                return (<div>
                    {row.sport}
                </div>);
            },
            renderHeader: (params) => (
                <div style={{writingMode: "vertical-rl", height: 150, textAlign: 'center', fontSize: 16}}>
                    {'სპორტი'}
                </div>
            ),
            field: 'sport',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header",

            width: 90,
            maxWidth: 90,
        },
        {
            headerName: "ეთიკური ნორმა",
            renderCell: ({row}) => {
                return (<div>
                    {row.ethic}
                </div>);
            },
            renderHeader: (params) => (
                <div style={{writingMode: "vertical-rl", height: 150, textAlign: 'center', fontSize: 16}}>
                    {'ეთიკური ნორმა'}
                </div>
            ),
            field: 'ethic',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header",

            width: 90,
            maxWidth: 90,
        },
        {
            headerName: "რეიტინგი",
            renderCell: ({row}) => {
                return (<div>
                    {row.rating}
                </div>);
            },
            renderHeader: (params) => (
                <div style={{writingMode: "vertical-rl", height: 150, textAlign: 'center', fontSize: 16}}>
                    {'რეიტინგი'}
                </div>
            ),
            field: 'rating',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header",

            width: 90,
            maxWidth: 90,
        },
        {
            headerName: "გაცდენილი საათი",
            renderCell: ({row}) => {
                return (<div>
                    {row.absent}
                </div>);
            },
            renderHeader: (params) => (
                <div style={{writingMode: "vertical-rl", height: 150, textAlign: 'center', fontSize: 16}}>
                    {'გაცდენილი საათი'}
                </div>
            ),
            field: 'absent',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header",

            width: 90,
            maxWidth: 90,
        },
        {
            headerName: "შენიშვნა",
            renderCell: ({row}) => {
                return (<div>
                    {row.mistake}
                </div>);
            },
            renderHeader: (params) => (
                <div style={{writingMode: "vertical-rl", height: 150, fontSize: 16, textAlign: 'center'}}>
                    {'შენიშვნა'}
                </div>
            ),
            field: 'mistake',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header",

            width: 90,
            maxWidth: 90,
        },

    ];

    let gradeClomuns2 = []

    const getFieldName = (o, num) =>{
        return o.subject.name + "-" + num;
    }

    const getGradeColumns = useCallback(() => {
        if (data && data.length > 0) {
            gradeClomuns2 = [{
                groupId: 'student',
                headerName: "მოსწავლე",
                renderCell: ({row}) => {
                    return (<div>
                        {row.student.firstName + ' ' + row.student.lastName}
                    </div>);
                },
                renderHeader: (params) => (
                    <div style={{writingMode: "vertical-rl", height: 150, textAlign: 'center', fontSize: 16}}>
                        {'მოსწავლე'}
                    </div>
                ),
                children: [{field: 'firstName'}],
                // field: 'name',
                // sortable: false,
                align: 'center',
                headerAlign: 'center',
                width: 200,
                maxWidth: 200,
            }];
            console.log("data[0].gradeList", data[0].gradeList)
            data[0].gradeList.forEach((o, index) => {
                gradeClomuns2 = [...gradeClomuns2, {
                    groupId: o.subject.name,
                    headerName: o.subject.name,
                    renderHeader: (params) => (
                        <div style={{writingMode: "vertical-rl", height: 150, textAlign: 'center', fontSize: 16}}>
                        </div>
                    ),
                    // field: '' + o.subject.name,
                    children: [{field: getFieldName(o, "9")},
                        {field: getFieldName(o, "11")},
                        {field: getFieldName(o, "12")},
                        {field: getFieldName(o, "1")},
                        {field: getFieldName(o, "3")},
                        {field: getFieldName(o, "4")},
                        {field: getFieldName(o, "5")},
                        {field: getFieldName(o, "6")},
                        {field: getFieldName(o, "7")},
                        {field: getFieldName(o, "8")}],
                    sortable: false,
                    align: 'center',
                    headerAlign: 'center',
                    width: 200,
                    maxWidth: 200,

                }]
            })
            console.log("gradeClomuns2", gradeClomuns2)

            return gradeClomuns2
        }
        return gradeColumns
    }, [data])

    let columnGroupingModel = [];


    return (
        <div className={"semesterGradeCnt"}>
            <SemesterGradeToolbar filters={filters} setFilters={setFilters}/>
            <div style={{height: `calc(100vh - ${130}px)`, width: '100%'}}>
                <DataGridPaper>
                    <DataGridSGS
                        // sx={{
                        //     overflowX: 'hidden',
                        //     '& .MuiDataGrid-columnHeader, .MuiDataGrid-cell': {
                        //         borderRight: `3px solid ${
                        //             '#f4f4f4'
                        //         }`,
                        //     },
                        // }}
                        queryKey={"SEMESTER_GRADE"}
                        experimentalFeatures={{columnGrouping: true}}
                        columnGroupingModel={getGradeColumns()}
                        columns={getMonthFields()}
                        rows={data ? data : []}
                        getRowId={(row) => {
                            return row.student.id;
                        }}
                        getRowHeight={() => 50}
                        disableColumnMenu
                        filters={filters}
                    />
                </DataGridPaper>
            </div>
        </div>
    )
}

export default SemesterGradeDashBoard;