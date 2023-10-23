import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import SearchIcon from '@mui/icons-material/Search';
import Button from '@mui/material/Button';
import {useEffect, useMemo, useState} from "react";
import CustomShefasebaBar from "../../components/CustomShefasebaBar";
import {MONTHS, MONTHS_SCHOOL} from "../utils/date";
import useGradesForMonth from "./useGradesForMonth";

const MonthlyGrade = () => {

    const month = [
        'სექტემბერი',
        'ოქტომბერი',
        'ნოემბერი',
        'დეკემბერი',
        'იანვარი',
        'თებერვალი',
        'მარტი',
        'აპრილი',
        'მაისი',
        'ივნისი'
    ];

    const [selectedData, setSelectedData] = useState(MONTHS[new Date().getUTCMonth()]);
    const [currentData, setCurrentData] = useState([]);
    const chosenMonth = useMemo(
        () => selectedData? MONTHS.filter((month) => month.value === selectedData.value)[0] : new Date().getUTCMonth(),
        [selectedData]
    );
    const {data: monthData, isLoading: isMonthLoading} = useGradesForMonth({month: chosenMonth.key});

console.log(monthData)
    const handleChange = (event) => {
        setSelectedData(MONTHS.filter(month => month.value === event.target.value)[0]);
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
            </div>
        );
    }

    const parsedMonthlyData = useMemo(
        () => monthData?.map(grade => ({name: grade.subject.name, value:grade.value})),
        [monthData]
    )


    const handleSearch = () => {
        if (!!selectedData) {
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
                            <Button onClick={handleSearch} disabled={!selectedData}
                                    style={{fontWeight: 'bold', height: '50px'}}
                                    variant="contained">ძიება<SearchIcon/></Button>
                        </div>
                    </div>
                </div>
            </div>

            {parsedMonthlyData && parsedMonthlyData.length && <div className="absenceMain shefaseba horizontal">
                <CustomShefasebaBar color={'#45c1a4'} data={parsedMonthlyData}/>
            </div>}

        </>
    );
}

export default MonthlyGrade;