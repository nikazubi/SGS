import React, {useCallback, useState} from "react";
import DataGridPaper from "../../components/grid/DataGridPaper";
import DataGridSGS from "../../components/grid/DataGrid";
import SemesterGradeToolbar from "./SemesterGradeToolbar";
import "./header.css"
import useGradeSemester from "./useGradeSemester";
import {getFiltersOfPage} from "../../../utils/filters";

const SemesterGradeDashBoard = () => {
    const [filters, setFilters] = useState({...getFiltersOfPage("SEMESTER_GRADE")});

    const {data, isLoading, isError, error, isSuccess} = useGradeSemester(filters);

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

    const getGradeColumns = useCallback(() => {
        if (data && data.length > 0) {
            gradeClomuns2 = [{
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
                field: 'name',
                sortable: false,
                align: 'center',
                headerAlign: 'center',
                width: 200,
                maxWidth: 200,
            }];
            data[0].gradeList.forEach((o, index) => {
                gradeClomuns2 = [...gradeClomuns2, {
                    headerName: o.subject.name,
                    renderCell: ({row}) => {
                        return (<div>
                            {row.gradeList[index].value === 0 ? '' : row.gradeList[index].value}
                        </div>);
                    },
                    renderHeader: (params) => (
                        <div style={{writingMode: "vertical-rl", height: 150, textAlign: 'center', fontSize: 16}}>
                            {o.subject.name}
                        </div>
                    ),
                    field: '' + o.subject.name,
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
                        columns={getGradeColumns()}
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