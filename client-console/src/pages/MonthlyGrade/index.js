import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import React, {useCallback, useMemo, useState} from "react";
import CustomShefasebaBar from "../../components/CustomShefasebaBar";
import {YEAR} from "../utils/date";
import useGradesForMonth from "./useGradesForMonth";
import DataGridPaper from "../../components/datagrid/DataGridPaper";
import DataGridSGS from "../../components/datagrid/DataGrid";

const MonthlyGrade = () => {
    const [selectedData, setSelectedData] = useState({key: 1, value: "I"});
    const [chosenYear, setChosenYear] = useState(new Date().getUTCFullYear());

    const trimester = [
        {key: 1, value: "I"},
        {key: 2, value: "II"},
        {key: 3, value: "III"}
    ]

    const choosenYeeear = useMemo(
        () => chosenYear? YEAR.filter((year) => year.value === chosenYear)[0] : new Date().getUTCFullYear(),
        [chosenYear]
    );
    const {data: monthData} = useGradesForMonth({trimester: selectedData.key, year: choosenYeeear.key});

    const handleChange = (event) => {
        setSelectedData(trimester.filter(month => month.value === event.target.value)[0]);
    };

    const handleYearChange = (event) => {
        setChosenYear(event.target.value);
    };

    function dropdown() {
        return (
            <div className="yearDropwdown">
                <TextField
                    select
                    label="აირჩიე ტრიმესტრი"
                    value={selectedData.value}
                    onChange={handleChange}
                    variant="outlined"
                >
                    {trimester.map((m) => (
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
            return monthData.filter(grade => grade.subject.name !== 'rating'
                && grade.subject.name !== 'behaviour'
                && grade.subject.name !== 'absence').map(grade => {
                return {
                    name: grade.subject.name,
                    value: grade.value ? grade.value === -50 ? 0 : grade.value : 0
                }
            })
        }, [monthData]
    )


    const getGradeColumns = useCallback(() => {
        return [
            {
                headerName: "საგანი",
                renderCell: ({row}) => {
                    return (<div>
                        {row.subject.name}
                    </div>);
                },
                field: 'name',
                sortable: false,
                align: 'center',
                headerAlign: 'center',
                width: 200,
                maxWidth: 200
            },
            {
                headerName: "ნიშანი",
                renderCell: ({row}) => {
                    return (<div>
                        {row.value ? row.value : ""}
                    </div>);
                },

                field: 'geo',
                sortable: false,
                align: 'center',
                headerAlign: 'center'
            },
        ]
    }, [monthData])


    return (
        <>
            <div className="ib__center column">
                <div className="pageName">მოსწავლის შემაჯამებელი ტრიმესტრული შეფასება</div>
                <div style={{display: 'flex', alignItems: 'center', marginTop: '25px'}}>
                    <div style={{display: 'flex'}}>
                        {dropdown()}
                    </div>
                </div>
            </div>
            <div style={{display: 'flex'}}>
                <div style={{height: `65vh`, width: '30%', marginTop: 30, paddingLeft: 40, paddingRight: 40}}>

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
                        queryKey={"TRIMESTER_SUM"}
                        columns={getGradeColumns()}
                        rows={monthData ? monthData : []}
                        getRowId={(row) => {
                            return row.subject.id;
                        }}
                        headerHeight={400}
                        getRowHeight={() => 50}
                        disableColumnMenu
                        filters={{
                            trimester: selectedData.key,
                            year: choosenYeeear.key
                        }}
                    />
                </DataGridPaper>
            </div>
                {parsedMonthlyData && parsedMonthlyData.length > 0 &&
                    <div style={{marginTop: 10}}>
                        <CustomShefasebaBar color={'#45c1a4'} data={parsedMonthlyData} height={window.innerHeight - 250}
                                            width={window.innerWidth - 460} left={5}/>
            </div>}
            </div>
        </>
    );
}

export default MonthlyGrade;