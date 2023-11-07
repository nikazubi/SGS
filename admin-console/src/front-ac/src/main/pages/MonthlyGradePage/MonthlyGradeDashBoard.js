import React, {useCallback, useState} from "react";
import DataGridPaper from "../../components/grid/DataGridPaper";
import DataGridSGS from "../../components/grid/DataGrid";
import MonthlyGradeToolbar from "./MonthlyGradeToolbar";
import "./header.css"
import {getFiltersOfPage} from "../../../utils/filters";
import useGradeMonthly from "./useGradeMonthly";
import {subjectPattern} from "./Helper";

const MonthlyGradeDashBoard = () => {
    const [filters, setFilters] = useState({...getFiltersOfPage("MONTHLY_GRADE")});
    const [checked, setChecked] = useState(false);
    const {data, isLoading, isError, error, isSuccess} = useGradeMonthly(filters);
    const gradeColumns = [
        {
            headerName: "მოსწავლე",
            renderCell: ({row}) => {
                return ( <div  >
                    {row.name}
                </div>);
            },
            field: 'name',
            sortable: false,
            align: 'center',
            headerAlign: 'center',
            width: 300,
            maxWidth: 300
        },
        {
            headerName: "ქართული ლიტ",
            renderCell: ({row}) => {
                return (<div  >
                    {row.geo}
                </div>);
            },

            field: 'geo',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },
        {
            headerName: "ქართული ენა",
            renderCell: ({row}) => {
                return (<div  >
                    {row.geolang}
                </div>);
            },

            field: 'geolang',
            sortable: false,
            align: 'center',
            headerAlign: 'center'

        },
        {
            headerName: "ქართული წერა",
            renderCell: ({row}) => {
                return (<div  >
                    {row.write}
                </div>);
            },

            field: 'write',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },
        {
            headerName: "მათემატიკა",
            renderCell: ({row}) => {
                return (<div  >
                    {row.math}
                </div>);
            },

            field: 'math',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },
        {
            headerName: "ინგლისური",
            renderCell: ({row}) => {
                return (<div  >
                    {row.eng}
                </div>);
            },

            field: 'eng',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },
        {
            headerName: "ინგლისური ლიტ",
            renderCell: ({row}) => {
                return (<div  >
                    {row.englit}
                </div>);
            },

            field: 'englit',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },
        {
            headerName: "გერმანული",
            renderCell: ({row}) => {
                return (<div  >
                    {row.german}
                </div>);
            },

            field: 'german',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },
        {
            headerName: "რუსული ენა",
            renderCell: ({row}) => {
                return (<div  >
                    {row.russia}
                </div>);
            },

            field: 'russia',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },
        {
            headerName: "ბიოლოგია",
            renderCell: ({row}) => {
                return (<div  >
                    {row.bio}
                </div>);
            },

            field: 'bio',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },
        {
            headerName: "ქიმია",
            renderCell: ({row}) => {
                return (<div  >
                    {row.chemistry}
                </div>);
            },

            field: 'chemistry',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },
        {
            headerName: "ფიზიკა",
            renderCell: ({row}) => {
                return (<div  >
                    {row.phisic}
                </div>);
            },

            field: 'physic',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },
        {
            headerName: "ისტორია",
            renderCell: ({row}) => {
                return (<div  >
                    {row.history}
                </div>);
            },

            field: 'history',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },
        {
            headerName: "გეოგრაფია",
            renderCell: ({row}) => {
                return (<div  >
                    {row.geography}
                </div>);
            },

            field: 'geography',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },
        {
            headerName: "მოქალაქეობა",
            renderCell: ({row}) => {
                return (<div  >
                    {row.nationaly}
                </div>);
            },

            field: 'nationaly',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },
        {
            headerName: "ჰუმ. აზროვნება",
            renderCell: ({row}) => {
                return (<div  >
                    {row.hum}
                </div>);
            },

            field: 'hum',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },
        {
            headerName: "სპორტი",
            renderCell: ({row}) => {
                return (<div  >
                    {row.sport}
                </div>);
            },

            field: 'sport',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },
        {
            headerName: "რეიტინგი",
            renderCell: ({row}) => {
                return (<div>
                    {row.rating}
                </div>);
            },

            field: 'rating',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },
        {
            headerName: "ეთიკური ნორმა",
            renderCell: ({row}) => {
                return (<div>
                    {row.ethic}
                </div>);
            },

            field: 'ethic',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },
        {
            headerName: "გაცდენილი საათი",
            renderCell: ({row}) => {
                return (<div  >
                    {row.absent}
                </div>);
            },

            field: 'absent',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },
        {
            headerName: "შენიშვნა",
            renderCell: ({row}) => {
                return (<div  >
                    {row.mistake}
                </div>);
            },


            field: 'mistake',
            sortable: false,
            align: 'center',
            headerAlign: 'center'
        },

    ];

    let gradeClomuns2 = []

    const getGradeColumns = useCallback(() =>{
        if(data && data.length > 0){
            gradeClomuns2 = [        {
                headerName: "მოსწავლე",
                renderCell: ({row}) => {
                    return ( <div>
                        {row.index + ". " + row.student.lastName + " " + row.student.firstName}
                    </div>);
                },
                field: 'name',
                sortable: false,
                align: 'center',
                headerAlign: 'center',
                width: 300,
                maxWidth: 300,
            }];
            let absenceIndex = -10;
            let behaviourIndex = -10;
            let ratingIndex = -10;
            data[0].gradeList.forEach((o, index) => {
                if (o.subject.name === 'absence') {
                    absenceIndex = index;
                } else if (o.subject.name === 'behaviour') {
                    behaviourIndex = index;
                } else if (o.subject.name === 'rating') {
                    ratingIndex = index;
                } else {
                    gradeClomuns2 = [ ...gradeClomuns2, {
                        headerName: o.subject.name,
                        renderCell: ({row}) => {
                            if (row.student.id === -6) {
                                return (<div>
                                    {!row.gradeList[index] || !row.gradeList[index].subject ? '' : row.gradeList[index].subject.teacher}
                                </div>)
                            }
                            return (<div>
                                {!row.gradeList[index] || row.gradeList[index]?.value === 0 ? '' : row.gradeList[index].value === -50 ? 'ჩთ' :
                                    checked? row.gradeList[index].value + 3 : row.gradeList[index].value}
                            </div>);
                        },
                        field: '' + o.subject.name,
                        sortable: false,
                        align: 'center',
                        headerAlign: 'center',
                        width: 200,
                        maxWidth: 200,
                    }]
                }
            })
            gradeClomuns2.push(
                {
                    headerName: "რეიტინგი",
                    renderCell: ({row}) => {
                        return (<div>
                            {!row.gradeList[ratingIndex] || row.gradeList[ratingIndex]?.value === 0 ? '' : row.gradeList[ratingIndex].value}
                        </div>);
                    },

                    field: 'rating',
                    sortable: false,
                    align: 'center',
                    headerAlign: 'center',
                    width: 200,
                    maxWidth: 200,
                },
                {
                    headerName: "ეთიკური ნორმა",
                    renderCell: ({row}) => {
                        return (<div>
                            {!row.gradeList[behaviourIndex] || row.gradeList[behaviourIndex]?.value === 0 ? '' : row.gradeList[behaviourIndex].value}
                        </div>);
                    },

                    field: 'ethic',
                    sortable: false,
                    align: 'center',
                    headerAlign: 'center',
                    width: 200,
                    maxWidth: 200,
                },
                {
                    headerName: "გაცდენილი საათი",
                    renderCell: ({row}) => {
                        return (<div>
                            {!row.gradeList[absenceIndex] || row.gradeList[absenceIndex]?.value === 0 ? '' : row.gradeList[absenceIndex].value}
                        </div>);
                    },

                    field: 'absent',
                    sortable: false,
                    align: 'center',
                    headerAlign: 'center',
                    width: 200,
                    maxWidth: 200
                },)
            return gradeClomuns2
        }
        return gradeColumns
    }, [data, checked])



    // if (data !== undefined && data.length > 0) {
    //     data[0].gradeList.forEach(o => {
    //         gradeClomuns2 = [ ...gradeClomuns2, {
    //             headerName: o.subject.name,
    //             renderCell: ({o}) => {
    //                 return ( <div>
    //                     {o.value}
    //                 </div>);
    //             },
    //             renderHeader: (params) => (
    //                 <div style={{writingMode: "vertical-rl", height:150,textAlign:'center', fontSize:16}}>
    //                     {o.subject.name}
    //                 </div>
    //             ),
    //             field: '' + o.subject.name,
    //             sortable: false,
    //             align: 'center',
    //             headerAlign: 'center',
    //         }]
    //     })
    //     setColumn(gradeClomuns2);
    // }

    return (
        <div className={"monthlyGradeCnt"}>
            <MonthlyGradeToolbar filters={filters} setFilters={setFilters} checked={checked} setChecked={setChecked}/>
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
                        queryKey={"MONTHLY_GRADE"}
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

export default MonthlyGradeDashBoard;