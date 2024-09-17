import CustomBar from "./BarChart";
import React, {useCallback, useMemo, useState} from "react";
import useFetchYear from "../semestruli-shefaseba/useYear";
import {MONTHS, MONTHS_SCHOOL} from "../utils/date";
import TextField from "@mui/material/TextField";
import MenuItem from "@mui/material/MenuItem";
import useTotalAbsence from "./useTotalAbsence";
import useAbsenceGrade from "./useAbsenceGrades";
import DataGridPaper from "../../components/datagrid/DataGridPaper";
import DataGridSGS from "../../components/datagrid/DataGrid";
import "./newheader.css"

const AbsencePage = () => {

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
    const [chosenYear, setChosenYear] = useState();

    const monthToSelectInitially = new Date().getUTCMonth() === 5 ||
    new Date().getUTCMonth() === 6 ||
    new Date().getUTCMonth() === 7 ? 4 : new Date().getUTCMonth()

    const [selectedMonth, setSelectedMonth] = useState(MONTHS[monthToSelectInitially]);

    const chosenMonth = useMemo(
        () => selectedMonth ? MONTHS.filter((month) => month.value === selectedMonth.value)[0] : monthToSelectInitially,
        [selectedMonth]
    );

    const {data: totalAbsenceData} = useTotalAbsence({month: chosenMonth.key, year: chosenYear});
    const year = useMemo(
        () => chosenYear ? chosenMonth : yearData ? yearData[yearData.length - 1] : '2023-2024'
            [chosenYear, yearData]
    );
    const {data: absenceGrades} = useAbsenceGrade({yearRange: year});


    const getGradeTypeByMonth = (month) => {
        switch (month) {
            case 0:
                return "JANUARY_FEBRUARY";
            case 1:
                return "JANUARY_FEBRUARY";
            case 2:
                return "MARCH";
            case 3:
                return "APRIL";
            case 4:
                return "MAY";
            case 8:
                return "SEPTEMBER_OCTOBER";
            case 9:
                return "SEPTEMBER_OCTOBER";
            case 10:
                return "NOVEMBER";
            case 11:
                return "DECEMBER";
            default:
                return "";
        }
    }

    const selectedMonthGrade = useMemo(
        () => {
            const type = getGradeTypeByMonth(chosenMonth.key)
            return absenceGrades ? absenceGrades.filter(grade => grade.gradeType === type)[0] : null
        }, [absenceGrades, chosenMonth, getGradeTypeByMonth]
    )

    const sum = useMemo(
        () => absenceGrades ? absenceGrades.reduce((a, b) => {
            return a + Number(b.value)
        }, 0) : 0,
        [absenceGrades]
    );

    const getTranslation = (gradeType) => {
        switch (gradeType) {
            case "SEPTEMBER_OCTOBER":
                return "სექტემბერი-ოქტომბერი";
            case "NOVEMBER":
                return "ნოემბერი";
            case    "DECEMBER":
                return "დეკემბერი";
            case    "JANUARY_FEBRUARY":
                return "იანვარი-თებერვალი";
            case    "MARCH":
                return "მარტი";
            case    "APRIL":
                return "აპრილი"
            case    "MAY":
                return "მაისი"
        }
        return ""
    }

    const monthData = useMemo(
        () => absenceGrades ?
            absenceGrades.map((val) => {
                return {
                    value: val.value,
                    name: getTranslation(val?.gradeType)
                }
            })
            : []
                [absenceGrades]
    );
    const handleChangeMonth = (event) => {
        setSelectedMonth(MONTHS.filter(month => month.value === event.target.value)[0]);
    };

    const handleChangeYear = (event) => {
        setChosenYear(event.target.value);
    };

    function dropdown() {
        return (
            <div className="yearDropwdown" style={{marginRight: 15}}>
                <TextField
                    select
                    label="აირჩიე თვე"
                    value={selectedMonth.value}
                    onChange={handleChangeMonth}
                    variant="outlined"
                >
                    {MONTHS_SCHOOL.map((m) => (
                        <MenuItem key={m.key} value={m.value}>
                            {m.value}
                        </MenuItem>
                    ))}
                </TextField>
            </div>
        );
    }

    const getYearDropdown = useCallback(
        () => {
            if (!yearData) {
                return <div></div>
            }

            return (
                <div className="yearDropwdown">
                    <TextField
                        select
                        label="აირჩიეთ სასწავლო წელი"
                        value={chosenYear || yearData[yearData.length - 1]}
                        onChange={handleChangeYear}
                        variant="outlined"
                    >
                        {yearData.map((m) => (
                            <MenuItem key={m} value={m}>
                                {m}
                            </MenuItem>
                        ))}
                    </TextField>
                </div>
            );
        },
        [yearData, chosenYear, handleChangeYear],
    );
    const gradeColumns = monthData?.map((item, index) => ({
        field: item.name,
        headerName: item.name,
        align: 'center',
        headerAlign: 'center',
        renderCell: ({row}) => row.monthData.filter((curr) => item.name === curr.name)[0].value

    })) || [];

    return (
        <>
            <div className="ib__center column">
                <div className="pageName">მოსწავლის მიერ გაცდენილი საათები</div>
                <div className="absenceDropdown">
                    {/*<Dropdown data={month} value={selectedMonth} select={setSelectedMonth} label={'თვე'}/>*/}
                    {dropdown()}
                    {getYearDropdown()}
                    {/*<div style={{marginLeft: '10px', marginTop: '6px'}}>*/}
                    {/*    <Button onClick={handleSearch} disabled={!selectedMonth || !selectedYear}*/}
                    {/*            style={{fontWeight: 'bold', height: '40px'}}*/}
                    {/*            variant="contained">ძიება<SearchIcon/></Button>*/}
                    {/*</div>*/}
                </div>

            </div>
            {absenceGrades && absenceGrades.length > 0 && totalAbsenceData && sum && selectedMonthGrade &&

                <div style={{
                    width: '100%',
                    alignItems: 'center',
                    display: 'flex',
                    justifyContent: 'center',
                    marginTop: '30px'
                }}>
                    <div style={{
                        minWidth: "310px",
                        maxWidth: "100px",
                        height: 150,
                        borderRadius: "0px 30px 30px 30px",
                        backgroundColor: '#01619b',
                        textAlign: 'center',
                        fontSize: '20px',
                        flexGrow: 1,
                    }} key={'თვის ქულა'}>
                        <div className="ethical__title">{getTranslation(selectedMonthGrade?.gradeType)}</div>
                        <div style={{display: 'flex', alignItems: 'center', justifyContent: 'space-evenly'}}>
                            <div className="modified__title" style={{fontSize: '15px'}}>{'თვის გაცდენილი საათები'}</div>
                            <div className="grade__data__api ethical">{selectedMonthGrade?.value}</div>
                        </div>

                        {/*<FooterBox boxdetails={m.boxdetails}/>*/}
                    </div>
                </div>
            }


            <div>
                {/*<SemesterGradeToolbar filters={filters} setFilters={setFilters} checked={checked} setChecked={setChecked}/>*/}
                <div style={{height: `30%`, width: '100%', marginTop: 30, paddingLeft: '10%', paddingRight: '10%'}}>
                    <DataGridPaper>
                        <DataGridSGS
                            queryKey={"ABSENCE_GRADE"}
                            // experimentalFeatures={{columnGrouping: true}}
                            // columnGroupingModel={getGradeColumns()}
                            columns={gradeColumns}
                            rows={[{monthData: monthData}] || []}
                            getRowId={(row) => {
                                return 1;
                            }}
                            getRowHeight={() => 50}
                            disableColumnMenu
                            // filters={filters}
                        />
                    </DataGridPaper>
                </div>
            </div>


            {absenceGrades && <div className="absenceMain horizontal">
                <CustomBar color={'#FF5722'} attendMax={sum} keyLabel={'value'} layout={'horizontal'}
                           data={monthData}/>
            </div>}
        </>
    );
}

export default AbsencePage;