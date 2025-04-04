import React, {useCallback, useEffect, useState} from "react";
import DataGridPaper from "../../components/grid/DataGridPaper";
import DataGridSGS from "../../components/grid/DataGrid";
import SemesterGradeToolbar from "./SemesterGradeToolbar";
import "./header.css"
import useGradeSemester from "./useGradeSemester";
import {getFiltersOfPage} from "../../../utils/filters";
import useUpdateSemesterDiagnosticGrade from "./useUpdateSemesterDiagnosticGrade";
import {fetchSubjectsForClass} from "./useSubjectsForClass";

const SemesterGradeDashBoard = () => {
    const [filters, setFilters] = useState({...getFiltersOfPage("SEMESTER_GRADE")});
    const [subjects, setSubjects] = useState([]);
    const [checked, setChecked] = useState(false);
    const {data, isLoading, isError, error, isSuccess} = useGradeSemester(filters);
    const {mutateAsync: mutateRow} = useUpdateSemesterDiagnosticGrade();


    useEffect(() => {
        const getSubjects = async () => {
            const param = {classId: filters.academyClass?.id};
            const subjectArr = await fetchSubjectsForClass(param);
            setSubjects(subjectArr);
        }
        getSubjects();
    }, [filters])

    const getMonthFields = useCallback(() => {
        if (!subjects) {
            return [];
        }
        const monthFields = [];
        const monthNames = [
            'იან-თებ',
            'მარტი',
            'აპრილი',
            'მაისი',
            'ივნისი',
            'სექტ-ოქტ',
            'ნოემბ',
            'დეკემბ'
        ];

        const secondSemesterMonths = [
            {month: 1, ind: 0},  // ianvari-tebervali
            {month: 3, ind: 1},  // marti
            {month: 4, ind: 2},  // aprili
            {month: 5, ind: 3}  // maisu
            // {month: 6, ind: 4} // ivnisi
        ];
        const firstSemesterMonths = [
            {month: 9, ind: 5},  // September-October
            {month: 11, ind: 6},  // noemberi
            {month: 12, ind: 7}
        ];

        const selectedMonths = filters.semesterN?.value === 'firstSemester' ? firstSemesterMonths : secondSemesterMonths;
        monthFields.push({
            headerName: "მოსწავლის გვარი, სახელი",
            renderCell: ({row}) => {
                return <div style={{height: 50, justifyContent: 'center', alignItems: 'center', display: 'flex'}}>
                    {row.index + ". " + row.student.lastName + " " + row.student.firstName}</div>
            },
            field: '0-firstName',
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
                        const transformedArray = row.gradeList.map(item => ({
                            subjectName: item.subject.name,
                            value: item.value
                        }));
                        const monthValue = transformedArray.find(item => item.subjectName === subject.name)?.value[month.month];

                        return <div>{monthValue === 0 || !monthValue ? '' : monthValue === -50 ? 'ჩთ' : checked ? Number(monthValue) + 3 : monthValue}</div>;
                        // return <div>{transformedArray.value[month.month] === 0 ? '' : transformedArray.value[month.month]}</div>;
                    },
                    field: subject.id + "-" + month.month,
                    sortable: false,
                    align: 'center',
                    headerAlign: 'center',
                    width: 100,
                    maxWidth: 100,
                });
            }
        }

        for (const subject of subjects) {
            if (filters.semesterN?.value === 'firstSemester') {
                monthFields.push({
                    headerName: 'დიაგნოსტიკური 1',
                    description: '',
                    renderCell: ({row}) => {
                        const transformedArray = row.gradeList.map(item => ({
                            subjectName: item.subject.name,
                            value: item.value
                        }));
                        const monthValue = transformedArray.find(item => item.subjectName === subject.name)?.value[-3];

                        return <div>{monthValue === 0 || !monthValue ? '' : monthValue === -50 ? 'ჩთ' : checked ? Number(monthValue) + 3 : monthValue}</div>;
                        // return <div>{transformedArray.value[month.month] === 0 ? '' : transformedArray.value[month.month]}</div>;
                    },
                    field: subject.id + "--3",
                    sortable: false,
                    align: 'center',
                    editable: true,
                    headerAlign: 'center'
                });
                monthFields.push({
                    headerName: 'დიაგნოსტიკური 2',
                    description: '',
                    renderCell: ({row}) => {
                        const transformedArray = row.gradeList.map(item => ({
                            subjectName: item.subject.name,
                            value: item.value
                        }));
                        const monthValue = transformedArray.find(item => item.subjectName === subject.name)?.value[-4];

                        return <div>{monthValue === 0 || !monthValue ? '' : monthValue === -50 ? 'ჩთ' : checked ? Number(monthValue) + 3 : monthValue}</div>;
                        // return <div>{transformedArray.value[month.month] === 0 ? '' : transformedArray.value[month.month]}</div>;
                    },
                    field: subject.id + "--4",
                    sortable: false,
                    editable: true,
                    align: 'center',
                    headerAlign: 'center'
                });
            }
            if (filters.semesterN?.value === 'secondSemester') {
                monthFields.push({
                    headerName: 'დიაგნოსტიკური 1',
                    description: '',
                    renderCell: ({row}) => {
                        const transformedArray = row.gradeList.map(item => ({
                            subjectName: item.subject.name,
                            value: item.value
                        }));
                        const monthValue = transformedArray.find(item => item.subjectName === subject.name)?.value[-5];

                        return <div>{monthValue === 0 || !monthValue ? '' : monthValue === -50 ? 'ჩთ' : checked ? Number(monthValue) + 3 : monthValue}</div>;
                        // return <div>{transformedArray.value[month.month] === 0 ? '' : transformedArray.value[month.month]}</div>;
                    },
                    field: subject.id + "--5",
                    sortable: false,
                    align: 'center',
                    editable: true,
                    headerAlign: 'center'
                });
                monthFields.push({
                    headerName: 'დიაგნოსტიკური 2',
                    description: '',
                    renderCell: ({row}) => {
                        const transformedArray = row.gradeList.map(item => ({
                            subjectName: item.subject.name,
                            value: item.value
                        }));
                        const monthValue = transformedArray.find(item => item.subjectName === subject.name)?.value[-6];

                        return <div>{monthValue === 0 || !monthValue ? '' : monthValue === -50 ? 'ჩთ' : checked ? Number(monthValue) + 3 : monthValue}</div>;
                        // return <div>{transformedArray.value[month.month] === 0 ? '' : transformedArray.value[month.month]}</div>;
                    },
                    field: subject.id + "--6",
                    sortable: false,
                    editable: true,
                    align: 'center',
                    headerAlign: 'center'
                });
            }
            // monthFields.push({
            //     headerName: 'შემოქმედებითობა (პროექტი)',
            //     description: '',
            //     renderCell: ({row}) => {
            //         const transformedArray = row.gradeList.map(item => ({
            //             subjectName: item.subject.name,
            //             value: item.value
            //         }));
            //         const monthValue = transformedArray.find(item => item.subjectName === subject.name)?.value[-2];
            //
            //         return <div>{monthValue === 0 || !monthValue ? '' : monthValue === -50 ? 'ჩთ' : checked ? Number(monthValue) + 3 : monthValue}</div>;
            //         // return <div>{transformedArray.value[month.month] === 0 ? '' : transformedArray.value[month.month]}</div>;
            //     },
            //     field: subject.id + "--2",
            //     sortable: false,
            //     editable: true,
            //     align: 'center',
            //     headerAlign: 'center'
            // });
            monthFields.push({
                headerName: 'სემესტრული',
                description: '',
                renderCell: ({row}) => {
                    const transformedArray = row.gradeList.map(item => ({
                        subjectName: item.subject.name,
                        value: item.value
                    }));
                    const monthValue = transformedArray.find(item => item.subjectName === subject.name)?.value[-1];

                    return <div>{monthValue === 0 || !monthValue ? '' : monthValue === -50 ? 'ჩთ' : checked ? Number(monthValue) + 3 : monthValue}</div>;
                    // return <div>{transformedArray.value[month.month] === 0 ? '' : transformedArray.value[month.month]}</div>;
                },
                field: subject.id + "--1",
                sortable: false,
                align: 'center',
                headerAlign: 'center'
            });
        }

        monthFields.sort((a, b) => {
            const [subjectA, monthA] = a.field.split('-');

            const [subjectB, monthB] = b.field.split('-');
            if (subjectA !== subjectB) {
                return subjectA.localeCompare(subjectB);
            } else {
                return parseInt(monthA) - parseInt(monthB);
            }
        });

        monthFields.push({
            headerName: 'ეთიკური',
            description: '',
            renderCell: ({row}) => {
                const transformedArray = row.gradeList.map(item => ({
                    subjectName: item.subject.name,
                    value: item.value
                }));
                let monthValue;
                if (filters.semesterN?.value === 'firstSemester') {
                    monthValue = transformedArray.find(item => item.subjectName === 'behaviour1')?.value[-7];
                } else if (filters.semesterN?.value === 'secondSemester') {
                    monthValue = transformedArray.find(item => item.subjectName === 'behaviour2')?.value[-7];
                }


                return <div>{monthValue === 0 || !monthValue ? '' : monthValue === -50 ? 'ჩთ' : monthValue}</div>;
                // return <div>{transformedArray.value[month.month] === 0 ? '' : transformedArray.value[month.month]}</div>;
            },
            field: "behaviour",
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        });
        monthFields.push({
            headerName: 'გაცდენილი საათები',
            description: '',
            renderCell: ({row}) => {
                const transformedArray = row.gradeList.map(item => ({
                    subjectName: item.subject.name,
                    value: item.value
                }));
                let monthValue;
                if (filters.semesterN?.value === 'firstSemester') {
                    monthValue = transformedArray.find(item => item.subjectName === 'absence1')?.value[-9];
                } else if (filters.semesterN?.value === 'secondSemester') {
                    monthValue = transformedArray.find(item => item.subjectName === 'absence2')?.value[-9];
                }


                return <div>{monthValue === 0 || !monthValue ? '' : monthValue === -50 ? 'ჩთ' : monthValue}</div>;
                // return <div>{transformedArray.value[month.month] === 0 ? '' : transformedArray.value[month.month]}</div>;
            },
            field: "absence",
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        });
        return monthFields;
    }, [data, subjects, filters.semesterN?.value, checked]);

    const gradeColumns = [
        {
            headerName: "მოსწავლე",
            renderCell: ({row}) => {
                return (<div>
                    {row.index + ". " + row.student.lastName + " " + row.student.firstName}
                </div>);
            },
            renderHeader: (params) => (
                <div style={{writingMode: "vertical-rl", height: 150, textAlign: 'center', fontSize: 16}}>
                    {'მოსწავლე'}
                </div>
            ),
            field: '0-name',
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

    const getFieldName = (o, num) => {
        return o.subject.id + "-" + num;
    }

    const getGradeColumns = useCallback(() => {
        if (data && data.length > 0) {
            gradeClomuns2 = [{
                groupId: 'student',
                headerName: "მოსწავლე",
                renderCell: ({row}) => {
                    return (<div>
                        {row.index + ". " + row.student.lastName + " " + row.student.firstName}
                    </div>);
                },
                renderHeader: (params) => (
                    <div style={{writingMode: "vertical-rl", height: 150, textAlign: 'center', fontSize: 16}}>
                        {'მოსწავლე'}
                    </div>
                ),
                children: [{field: '0-firstName'}],
                // field: 'name',
                // sortable: false,
                align: 'center',
                headerAlign: 'center',
                width: 200,
                maxWidth: 200,
            }];
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
                        {field: getFieldName(o, "8")},
                        {field: getFieldName(o, "-1")},
                        {field: getFieldName(o, "-2")},
                        {field: getFieldName(o, "-3")},
                        {field: getFieldName(o, "-4")},
                        {field: getFieldName(o, "-5")},
                        {field: getFieldName(o, "-6")},
                    ],
                    sortable: false,
                    align: 'center',
                    headerAlign: 'center',
                    width: 200,
                    maxWidth: 200,

                }]
            })
            gradeClomuns2 = [...gradeClomuns2, {
                    groupId: 'ეთიკური',
                    headerName: 'ეთიკური',
                    children: [{field: 'behaviour'}],
                    sortable: false,
                    align: 'center',
                    headerAlign: 'center',
                    width: 200,
                    maxWidth: 200,
            }]
            gradeClomuns2 = [...gradeClomuns2, {
                groupId: 'გაცდენილი საათები',
                headerName: 'გაცდენილი საათები',
                children: [{field: 'absence'}],
                sortable: false,
                align: 'center',
                headerAlign: 'center',
                width: 200,
                maxWidth: 200,
            }]
            return gradeClomuns2
        }
        return gradeColumns
    }, [data, checked])

    let columnGroupingModel = [];

    const processRowUpdate = useCallback(
        async (newRow) => {
            const gradeType = Object.keys(newRow).filter(field => field.endsWith("--6") ||
                                                                  field.endsWith("--5") ||
                                                                  field.endsWith("--4") ||
                                                                  field.endsWith("--3") ||
                                                                  field.endsWith("--2"))[0]
            const subjectIdAndType = gradeType.split("-", 1);
            newRow.subject = newRow.gradeList.filter(g => g.subject.id == subjectIdAndType[0])[0].subject;
            newRow.exactMonth = newRow.exactMonth ? newRow.exactMonth : Date.parse(new Date());
            newRow.value = newRow[gradeType];
            newRow.semester = filters.semesterN.value;
            return await mutateRow(newRow);
        }, [mutateRow, filters]
    );

    const onProcessRowUpdateError = useCallback(async (newRow) => {

    }, [filters]);

    return (
        <div className={"semesterGradeCnt"}>
            <SemesterGradeToolbar filters={filters} setFilters={setFilters} checked={checked} setChecked={setChecked}/>
            <div style={{height: `calc(100vh - ${130}px)`, width: '100%'}}>
                <DataGridPaper>
                    <DataGridSGS
                        sx={{
                            '& .MuiDataGrid-columnHeader, .MuiDataGrid-cell': {
                                border: `1px solid ${
                                    '#cce1ea'
                                }`,
                            },
                        }}
                        queryKey={"SEMESTER_GRADE"}
                        experimentalFeatures={{columnGrouping: true}}
                        columnGroupingModel={getGradeColumns()}
                        columns={getMonthFields()}
                        rows={data ? data : []}
                        getRowId={(row) => {
                            return row.student.id;
                        }}
                        processRowUpdate={processRowUpdate}
                        onProcessRowUpdateError={onProcessRowUpdateError}
                        headerHeight={400}
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