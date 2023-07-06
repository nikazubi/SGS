import React, {useEffect, useState} from "react";
import DataGridPaper from "../../components/grid/DataGridPaper";
import DataGridSGS from "../../components/grid/DataGrid";
import MonthlyGradeToolbar from "./MonthlyGradeToolbar";
import "./header.css"

const MonthlyGradeDashBoard = () => {
    const [filters, setFilters] = useState({});

    const [data, setData] = useState([]);

    useEffect(()=>{
        setTimeout((function (){
            document.querySelectorAll('.header-class').forEach((e,index)=>{
                e.style = "left: " + (103 + (index * 60)) + "px";
                // e.style.left = (103 + (index * 57));
                // e.style.color = "red";
                console.log('test123 GUDAAAA', e)
            })
        }),1000)

        console.log('test123 ZUBII')
    },[])

    const gradeColumns = [
        {
            headerName: "მოსწავლე",
            renderCell: ({row}) => {
                return row.name;
            },
            field: 'name',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
        },
        {
            headerName: "ქართული ლიტ",
            renderCell: ({row}) => {
                return row.geo;
            },

            field: 'geo',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header"
        },
        {
            headerName: "ქართული ენა",
            renderCell: ({row}) => {
                return row.geolang;
            },
            field: 'geolang',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header"
            
        },
        {
            headerName: "ქართული წერა",
            renderCell: ({row}) => {
                return row.write;
            },
            field: 'write',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header"
        },
        {
            headerName: "მათემატიკა",
            renderCell: ({row}) => {
                return row.math;
            },
            field: 'math',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header"
        },
        {
            headerName: "ინგლისური",
            renderCell: ({row}) => {
                return row.eng;
            },
            field: 'eng',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header"
        },
        {
            headerName: "ინგლისური ლიტ",
            renderCell: ({row}) => {
                return row.englit;
            },
            field: 'englit',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header"
        },
        {
            headerName: "გერმანული",
            renderCell: ({row}) => {
                return row.german;
            },
            field: 'german',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header"
        },
        {
            headerName: "რუსული ენა",
            renderCell: ({row}) => {
                return row.russia;
            },
            field: 'russia',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header"
        },
        {
            headerName: "ბიოლოგია",
            renderCell: ({row}) => {
                return row.bio;
            },
            field: 'bio',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header"
        },
        {
            headerName: "ქიმია",
            renderCell: ({row}) => {
                return row.chemistry;
            },
            field: 'chemistry',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header"
        },
        {
            headerName: "ფიზიკა",
            renderCell: ({row}) => {
                return row.physic;
            },
            field: 'physic',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header"
        },
        {
            headerName: "ისტორია",
            renderCell: ({row}) => {
                return row.history;
            },
            field: 'history',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header"
        },
        {
            headerName: "გეოგრაფია",
            renderCell: ({row}) => {
                return row.geography;
            },
            field: 'geography',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header"
        },
        {
            headerName: "მოქალაქეობა",
            renderCell: ({row}) => {
                return row.nationaly;
            },
            field: 'nationaly',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header"
        },
        {
            headerName: "ჰუმ. აზროვნება",
            renderCell: ({row}) => {
                return row.hum;
            },
            field: 'hum',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header"
        },
        {
            headerName: "სპორტი",
            renderCell: ({row}) => {
                return row.sport;
            },
            field: 'sport',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header"
        },
        {
            headerName: "ეთიკური ნორმა",
            renderCell: ({row}) => {
                return row.ethic;
            },
            field: 'ethic',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header"
        },
        {
            headerName: "რეიტინგი",
            renderCell: ({row}) => {
                return row.rating;
            },
            field: 'rating',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header"
        },
        {
            headerName: "გაცდენილი საათი",
            renderCell: ({row}) => {
                return row.absent;
            },
            field: 'absent',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header"
        },
        {
            headerName: "შენიშვნა",
            renderCell: ({row}) => {
                return row.mistake;
            },
            field: 'mistake',
            sortable: false,
            align: 'center',
            headerAlign: 'center', headerClassName: "header-class", cellClassName: "cell-header"
        },

    ];

    return (
        <div className={"monthlyGradeCnt"}>
            <MonthlyGradeToolbar filters={filters} setFilters={setFilters} setData={setData}/>
            <div style={{height: 500, width: '100%'}}>
                <DataGridPaper>
                    <DataGridSGS
                        sx={{
                            '& .MuiDataGrid-columnHeader, .MuiDataGrid-cell': {
                                border: `3px solid ${
                                    '#bbbbbb'
                                }`,
                            },
                        }}
                        // queryKey={"GRADES"}
                        columns={gradeColumns}
                        rows={data ? data : []}
                        getRowId={(row) => {
                            return row.name;
                        }}
                        getRowHeight={() => 'auto'}
                        disableColumnMenu
                        filters={filters}
                    />
                </DataGridPaper>
            </div>
        </div>
    )
}

export default MonthlyGradeDashBoard;