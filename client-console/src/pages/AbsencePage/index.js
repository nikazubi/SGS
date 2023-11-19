import CustomBar from "./BarChart";
import React, {useCallback, useMemo, useState} from "react";
import useFetchYear from "../semestruli-shefaseba/useYear";
import {MONTHS, MONTHS_SCHOOL} from "../utils/date";
import TextField from "@mui/material/TextField";
import MenuItem from "@mui/material/MenuItem";
import useTotalAbsence from "./useTotalAbsence";
import useAbsenceGrade from "./useAbsenceGrades";

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

    const [selectedMonth, setSelectedMonth] = useState(MONTHS[new Date().getUTCMonth()]);

    const chosenMonth = useMemo(
        () => selectedMonth ? MONTHS.filter((month) => month.value === selectedMonth.value)[0] : new Date().getUTCMonth(),
        [selectedMonth]
    );

    const {data: totalAbsenceData} = useTotalAbsence({month: chosenMonth.key, year: chosenYear});
    const year = useMemo(
        () => chosenYear ? chosenMonth : yearData ? yearData[yearData.length - 1] : '2023-2024'
            [chosenYear, yearData]
    );
    const {data: absenceGrades} = useAbsenceGrade({month: chosenMonth.key, yearRange: year});
    const sum = useMemo(
        () => absenceGrades ? absenceGrades.reduce((a, b) => {
            return a + Number(b.value)
        }, 0) : 0,
        [absenceGrades]
    );

    const subjectData = useMemo(
        () => absenceGrades ?
            absenceGrades.map((val) => {
                return {
                    value: val.value,
                    name: val?.subject?.name
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
            {absenceGrades && absenceGrades.length > 0 && totalAbsenceData && sum &&
                <div className="absenceMain">
                    <CustomBar color={'#01619b'} attend={sum} attendMax={totalAbsenceData[0]?.totalAcademyHour}
                               layout={'vertical'}
                               data={[{name: 'გაცდენა', value: sum}]}/>
                </div>}
            {absenceGrades && <div className="absenceMain horizontal">
                <CustomBar color={'#FF5722'} attendMax={sum} keyLabel={'value'} layout={'horizontal'}
                           data={subjectData}/>
            </div>}
        </>
    );
}

export default AbsencePage;