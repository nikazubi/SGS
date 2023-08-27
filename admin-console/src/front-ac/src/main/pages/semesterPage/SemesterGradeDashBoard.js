import React, {useCallback, useState} from "react";
import DataGridPaper from "../../components/grid/DataGridPaper";
import DataGridSGS from "../../components/grid/DataGrid";
import SemesterGradeToolbar from "./SemesterGradeToolbar";
import "./header.css"
import useGradeSemester from "./useGradeSemester";
import {getFiltersOfPage} from "../../../utils/filters";
import {getFirstSemestMonths, MONTHS} from "../../../utils/months";

const SemesterGradeDashBoard = () => {
    const [filters, setFilters] = useState({...getFiltersOfPage("SEMESTER_GRADE")});

    const {data, isLoading, isError, error, isSuccess} = useGradeSemester(filters);

    const getMonthFields = useCallback(() => {
        if (filters.semesterN?.value === 'firstSemester') {
            return [{
                headerName: "მოსწავლის გვარი, სახელი",
                renderCell: ({row}) => {
                    return <div style={{height: 50, justifyContent: 'center', alignItems: 'center', display: 'flex'}}>
                        {row.student.lastName + " " + row.student.firstName}</div>
                },
                field: 'firstName',
                sortable: false,
                headerAlign: 'center',
                align: 'center',
                width: 200,
                maxWidth: 200,
            },
            // getFirstSemestMonths.map((key, keyIndex)
            {
                headerName: 'სექტემბერი-ოქტომბერი',
                description: '',
                field: "9",
                sortable: false,
                align: 'center',
                headerAlign: 'center'
            },
            {
                headerName: 'ნოემბერი',
                description: '',
                // renderHeaderGroup: (params) => (
                //     <HeaderWithIcon {...params} icon={<BuildIcon fontSize="small" />} />
                // ),
                field: "11",
                sortable: false,
                align: 'center',
                headerAlign: 'center'
            },
            {
                headerName: 'დეკემბერი',
                description: '',
                // renderHeaderGroup: (params) => (
                //     <HeaderWithIcon {...params} icon={<BuildIcon fontSize="small" />} />
                // ),
                field: "12",
                sortable: false,
                align: 'center',
                headerAlign: 'center'
            }]
        } else {
            return [        {
                headerName: "მოსწავლის გვარი, სახელი",
                renderCell: ({row}) => {
                    return <div style={{height: 50, justifyContent: 'center', alignItems: 'center', display: 'flex'}}>
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
                headerName: 'იანვარი-თებერვალი',
                description: '',
                // renderHeaderGroup: (params) => (
                //     <HeaderWithIcon {...params} icon={<BuildIcon fontSize="small" />} />
                // ),
                field: "0",
                sortable: false,
                align: 'center',
                headerAlign: 'center'
             },
                {
                    headerName: 'მარტი',
                    description: '',
                    // renderHeaderGroup: (params) => (
                    //     <HeaderWithIcon {...params} icon={<BuildIcon fontSize="small" />} />
                    // ),
                    field: "3",
                    sortable: false,
                    align: 'center',
                    headerAlign: 'center'
                },
                {
                    headerName: 'აპრილი',
                    description: '',
                    // renderHeaderGroup: (params) => (
                    //     <HeaderWithIcon {...params} icon={<BuildIcon fontSize="small" />} />
                    // ),
                    field: "4",
                    sortable: false,
                    align: 'center',
                    headerAlign: 'center'
                },
                {
                    headerName: 'მაისი',
                    description: '',
                    // renderHeaderGroup: (params) => (
                    //     <HeaderWithIcon {...params} icon={<BuildIcon fontSize="small" />} />
                    // ),
                    field: "5",
                    sortable: false,
                    align: 'center',
                    headerAlign: 'center'
                },
                {
                    headerName: 'ივნისი',
                    description: '',
                    // renderHeaderGroup: (params) => (
                    //     <HeaderWithIcon {...params} icon={<BuildIcon fontSize="small" />} />
                    // ),
                    field: "6",
                    sortable: false,
                    align: 'center',
                    headerAlign: 'center'
                },
                {
                    headerName: 'ივლისი',
                    description: '',
                    // renderHeaderGroup: (params) => (
                    //     <HeaderWithIcon {...params} icon={<BuildIcon fontSize="small" />} />
                    // ),
                    field: "7",
                    sortable: false,
                    align: 'center',
                    headerAlign: 'center'
                },
                {
                    headerName: 'აგვისტო',
                    description: '',
                    // renderHeaderGroup: (params) => (
                    //     <HeaderWithIcon {...params} icon={<BuildIcon fontSize="small" />} />
                    // ),
                    field: "8",
                    sortable: false,
                    align: 'center',
                    headerAlign: 'center'
                }]
        }
    }, [data])
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
    const getName = (o) =>{
        console.log("+++++++++++++++++++++++++++++++++++", o.subject.name)
        return o.subject.name
    }

    const getFieldName = (o, num) =>{
        return num + o.subject.name
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
            data[0].gradeList.forEach((o, index) => {
                gradeClomuns2 = [...gradeClomuns2, {
                    groupId: o.subject.name,
                    headerName: o.subject.name,
                    renderCell: ({row}) => {
                        return (<div>
                            {row.gradeList[index].value === 0 ? '' : row.gradeList[index].value}
                        </div>);
                        // return (<div style={{display:'flex', flexDirection:'column'}}>
                        //     {row.gradeList[index].value?
                        //         Object.keys(row.gradeList[index].value).map((key, keyIndex) =>{
                        //             return ( <>
                        //                 <div>
                        //                     {key.toString() === '-1'? 'სემესტრული ნიშანი' : MONTHS[key]}
                        //                 </div>
                        //                 <div>
                        //                     {row.gradeList[index].value[key]}
                        //                 </div>
                        //             </>)
                        //         })
                        //         :
                        //         0
                        //     }
                        // </div>);
                    },
                    renderHeader: (params) => (
                        <div style={{writingMode: "vertical-rl", height: 150, textAlign: 'center', fontSize: 16}}>
                            {getName(o)}
                        </div>
                    ),
                    // field: '' + o.subject.name,
                    children: [{field: getFieldName(o, "9")},
                        {field: getFieldName(o, "11")},
                        {field: getFieldName(o, "12")},
                        {field: getFieldName(o, "0")},
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
            return gradeClomuns2
        }
        return gradeColumns
    }, [data])

    let columnGroupingModel = [];

console.log("hello",columnGroupingModel)

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