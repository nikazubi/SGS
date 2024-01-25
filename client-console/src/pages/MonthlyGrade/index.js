import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import SearchIcon from '@mui/icons-material/Search';
import Button from '@mui/material/Button';
import React, {useCallback, useEffect, useMemo, useState} from "react";
import CustomShefasebaBar from "../../components/CustomShefasebaBar";
import {MONTHS, MONTHS_SCHOOL, YEAR} from "../utils/date";
import useGradesForMonth from "./useGradesForMonth";
import useFetchYear from "../semestruli-shefaseba/useYear";
import useSubjects from "../Discipline/useSubjects";
import DataGridPaper from "../../components/datagrid/DataGridPaper";
import DataGridSGS from "../../components/datagrid/DataGrid";
import "./header.css"

const MonthlyGrade = () => {

    const month = [
        'სექტემბერი-ოქტომბერი',
        'ნოემბერი',
        'დეკემბერი',
        'იანვარი-თებერვალი',
        'მარტი',
        'აპრილი',
        'მაისი',
        'ივნისი'
    ];

    const {data: yearData, isLoading: isYearLoading} = useFetchYear();

    const [selectedData, setSelectedData] = useState(MONTHS[new Date().getUTCMonth()]);
    const [chosenYear, setChosenYear] = useState(new Date().getUTCFullYear());
    const {data: subjects, isLoading, isError, error, isSuccess} = useSubjects();

    const [currentData, setCurrentData] = useState([]);
    const chosenMonth = useMemo(
        () => selectedData ? MONTHS.filter((month) => month.value === selectedData.value)[0] : new Date().getUTCMonth(),
        [selectedData]
    );
    const choosenYeeear = useMemo(
        () => chosenYear? YEAR.filter((year) => year.value === chosenYear)[0] : new Date().getUTCFullYear(),
        [chosenYear]
    );
    const {data: monthData, isLoading: isMonthLoading} = useGradesForMonth({month: chosenMonth.key, year: choosenYeeear.key});

    const handleChange = (event) => {
        setSelectedData(MONTHS.filter(month => month.value === event.target.value)[0]);
    };

    const handleYearChange = (event) => {
        setChosenYear(event.target.value);
    };

    useEffect(() => {
        handleSearch();
    }, []);

    function dropdown() {
        return (
            <div className="yearDropwdown">
                <TextField
                    select
                    label="აირჩიე თვე"
                    value={selectedData.value}
                    onChange={handleChange}
                    variant="outlined"
                >
                    {MONTHS_SCHOOL.map((m) => (
                        <MenuItem key={m.key} value={m.value}>
                            {m.value}
                        </MenuItem>
                    ))}
                </TextField>
                <TextField
                    select
                    label="აირჩიე წელი"
                    value={chosenYear}
                    onChange={handleYearChange}
                    variant="outlined"
                >
                    {YEAR.map((m) => (
                        <MenuItem key={m.key} value={m.value}>
                            {m.value}
                        </MenuItem>
                    ))}
                </TextField>
            </div>
        );
    }

    const parsedMonthlyData = useMemo(
        () => {
            if (!monthData) {
                return []
            }
            return monthData[0]?.gradeList?.filter(grade => grade.subject.name !== 'rating'
                && grade.subject.name !== 'behaviour'
                && grade.subject.name !== 'absence').map(grade => {
                return {
                    name: grade.subject.name,
                    value: grade.value ? grade.value === -50 ? 0 : grade.value : 0
                }
            })
        }, [monthData]
    )

    const gradeColumns = [
        {
            headerName: "მოსწავლე",
            renderCell: ({row}) => {
                return (<div>
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
                return (<div>
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
                return (<div>
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
                return (<div>
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
                return (<div>
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
                return (<div>
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
                return (<div>
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
                return (<div>
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
                return (<div>
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
                return (<div>
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
                return (<div>
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
                return (<div>
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
                return (<div>
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
                return (<div>
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
                return (<div>
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
                return (<div>
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
                return (<div>
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
                return (<div>
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
                return (<div>
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

    const getGradeColumns = useCallback(() => {
        if (monthData && monthData.length > 0) {
            gradeClomuns2 = [{
                headerName: "მოსწავლე",
                renderCell: ({row}) => {
                    return (<div>
                        {row.student.lastName + " " + row.student.firstName}
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
            monthData[0].gradeList.forEach((o, index) => {
                if (o.subject.name === 'absence') {
                    absenceIndex = index;
                } else if (o.subject.name === 'behaviour') {
                    behaviourIndex = index;
                } else if (o.subject.name === 'rating') {
                    ratingIndex = index;
                } else {
                    gradeClomuns2 = [...gradeClomuns2, {
                        headerName: o.subject.name,
                        renderCell: ({row}) => {
                            if (row.student.id === -6) {
                                return (<div>
                                    {!row.gradeList[index] || !row.gradeList[index].subject ? '' : row.gradeList[index].subject.teacher}
                                </div>)
                            }
                            return (<div>
                                {!row.gradeList[index] || row.gradeList[index]?.value === 0 ? '' : row.gradeList[index].value === -50 ? 'ჩთ' : row.gradeList[index].value}
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
    }, [monthData])


    const handleSearch = () => {
        if (!!selectedData || !!chosenYear) {
            const studentMonthlyGrades = [
                {
                    name: 'ქართული ენა და ლიტერატურა',
                    ქულა: 6,
                },

                {
                    name: 'მათემატიკა',
                    ქულა: 5,
                },

                {
                    name: 'ინგლისური',
                    ქულა: 7,
                },
                {
                    name: 'ისტორია',
                    ქულა: 5,
                },
                {
                    name: 'გეოგრაფია',
                    ქულა: 7,
                }]

            setCurrentData(studentMonthlyGrades)
        }
    }

    return (
        <>
            <div className="ib__center column">
                <div className="pageName">მოსწავლის შეფასება თვის რეიტინგების მიხედვით</div>
                <div style={{display: 'flex', alignItems: 'center', marginTop: '25px'}}>
                    <div style={{display: 'flex'}}>
                        {dropdown()}
                        <div style={{marginLeft: '10px'}}>
                            <Button onClick={handleSearch} disabled={!selectedData || !chosenYear}
                                    style={{fontWeight: 'bold', height: '50px'}}
                                    variant="contained">ძიება<SearchIcon/></Button>
                        </div>
                    </div>
                </div>
            </div>
            <div style={{height: `30vh`, width: '100%', marginTop: 30, paddingLeft: 40, paddingRight: 40}}>

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
                        rows={monthData ? monthData : []}
                        getRowId={(row) => {
                            return row.student.id;
                        }}
                        headerHeight={400}
                        getRowHeight={() => 50}
                        disableColumnMenu
                        filters={{
                            month: chosenMonth.key,
                            year: choosenYeeear.key
                        }}
                    />
                </DataGridPaper>
            </div>
            {parsedMonthlyData && parsedMonthlyData.length > 0 && <div>
                <CustomShefasebaBar color={'#45c1a4'} data={parsedMonthlyData} height={350}
                                    width={window.innerWidth - 30} left={0}/>
            </div>}

        </>
    );
}

export default MonthlyGrade;