import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import SearchIcon from '@mui/icons-material/Search';
import Button from '@mui/material/Button';
import {useCallback, useEffect, useState} from "react";
import CustomShefasebaBar from "../../components/CustomShefasebaBar";
import SemesterGradeDashBoard from "./SemesterGradeDashBoard";
import useFetchYear from "./useYear";
import {getSemesterOfMonth} from "../utils/date";

const SemestruliShefaseba = () => {
    const {data: yearData, isLoading: isYearLoading} = useFetchYear();
    const [chosenYear, setChosenYear] = useState();
    const [currentData, setCurrentData] = useState([]);
    const [currentDataTerm2, setCurrentDataTerm2] = useState([]);

    const semesterOptions = [
        {value: 'firstSemester', label: 'I სემესტრი'},
        {value: 'secondSemester', label: 'II სემესტრი'},
    ];

    const getInitialSemester = () => {
        return getSemesterOfMonth(new Date().getUTCMonth())
    }

    const [chosenSemester, setChosenSemester] = useState(getInitialSemester())
    const [initialYear, setInitialYear] = useState(getInitialSemester())


    const handleChange = (event) => {
        setChosenYear(event.target.value);
    };

    const handleChangeSemester = (event) => {
        setChosenSemester(semesterOptions.filter(semester => semester.value === event.target.value)[0]);
    };

    useEffect(() => {
        handleSearch();
    }, [])


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

    const getSemesterIndexDropdown = useCallback(
        () => {
            if (!semesterOptions) {
                return <div></div>
            }

            return (
                <div className="yearDropwdown" style={{marginLeft: 10}}>
                    <TextField
                        select
                        label="აირჩიეთ სემესტრი"
                        value={chosenSemester?.value}
                        onChange={handleChangeSemester}
                        variant="outlined"
                    >
                        {semesterOptions.map((m) => (
                            <MenuItem key={m.value} value={m.value}>
                                {m.label}
                            </MenuItem>
                        ))}
                    </TextField>
                </div>
            );
        },
        [chosenSemester, semesterOptions, handleChangeSemester],
    );


    const handleSearch = () => {
        // if (!!chosenYear) {
        //     const studentTermGrades = [
        //         {
        //             name: 'ქართული ენა და ლიტერატურა',
        //             ქულა: 6,
        //         },
        //
        //         {
        //             name: 'მათემატიკა',
        //             ქულა: 5,
        //         },
        //
        //         {
        //             name: 'ინგლისური',
        //             ქულა: 7,
        //         },
        //         {
        //             name: 'ისტორია',
        //             ქულა: 5,
        //         },
        //         {
        //             name: 'გეოგრაფია',
        //             ქულა: 7,
        //         }]
        //
        //     setCurrentData(studentTermGrades)
        //
        //     //tu ar arsebobs - [] => da ar gamoitans meore semestrs
        //
        //     const studentTermGrades2 = [
        //         {
        //             name: 'ქართული ენა და ლიტერატურა',
        //             ქულა: 7,
        //         },
        //
        //         {
        //             name: 'მათემატიკა',
        //             ქულა: 6,
        //         },
        //
        //         {
        //             name: 'ინგლისური',
        //             ქულა: 6,
        //         },
        //         {
        //             name: 'ისტორია',
        //             ქულა: 6,
        //         },
        //         {
        //             name: 'გეოგრაფია',
        //             ქულა: 7,
        //         }]
        //
        //     setCurrentDataTerm2(studentTermGrades2)
        //
        // }
    }


    return (
        <>
            <div className="ib__center column">
                <div className="pageName">მოსწავლის სემესტრული შეფასება</div>
                <div style={{display: 'flex', alignItems: 'center', marginTop: '25px'}}>
                    {getYearDropdown()}
                    {getSemesterIndexDropdown()}
                    <div style={{marginLeft: '10px'}}>
                        <Button onClick={handleSearch} disabled={!chosenYear}
                                style={{fontWeight: 'bold', height: '50px'}}
                                variant="contained">ძიება<SearchIcon/></Button>
                    </div>
                </div>
            </div>


            <SemesterGradeDashBoard filters={{
                year: chosenYear || yearData ? yearData[yearData.length - 1] : undefined,
                semesterN: chosenSemester
            }}/>

        </>
    );
}

export default SemestruliShefaseba;