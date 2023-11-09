import CustomBar from "./BarChart";
import React, {useCallback, useMemo, useState} from "react";
import Dropdown from "./Dropdown";
import Button from '@mui/material/Button';
import SearchIcon from '@mui/icons-material/Search';
import useFetchYear from "../semestruli-shefaseba/useYear";
import {MONTHS, MONTHS_SCHOOL} from "../utils/date";
import TextField from "@mui/material/TextField";
import MenuItem from "@mui/material/MenuItem";

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
    const handleChangeMonth = (event) => {
        setSelectedMonth(MONTHS.filter(month => month.value === event.target.value)[0]);
    };

    const handleChangeYear = (event) => {
        setChosenYear(event.target.value);
    };

    function dropdown() {
        return (
            <div className="yearDropwdown">
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
                        onChange={handleChange}
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
        [yearData, chosenYear, handleChange],
    );

    return (
        <>
            <div className="ib__center column">
                <div className="pageName">მოსწავლის მიერ გაცდენილი საათები</div>
                <div className="absenceDropdown">
                    <Dropdown data={month} value={selectedMonth} select={setSelectedMonth} label={'თვე'}/>
                    {dropdown()}
                    {getYearDropdown()}
                    <div style={{marginLeft: '10px', marginTop: '6px'}}>
                        <Button onClick={handleSearch} disabled={!selectedMonth || !selectedYear}
                                style={{fontWeight: 'bold', height: '40px'}}
                                variant="contained">ძიება<SearchIcon/></Button>
                    </div>
                </div>

            </div>
            {!!absence && <div className="absenceMain">
                <CustomBar color={'#01619b'} attend={absence} attendMax={attendMax} layout={'vertical'}
                           data={allAbsenceData}/>
            </div>}
            {!!absenceBySubject.length && <div className="absenceMain horizontal">
                <CustomBar color={'#FF5722'} attendMax={absence} keyLabel={'არა'} layout={'horizontal'}
                           data={absenceBySubject}/>
            </div>}
        </>
    );
}

export default AbsencePage;