import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import SearchIcon from '@mui/icons-material/Search';
import Button from '@mui/material/Button';
import React, {useCallback, useEffect, useMemo, useState} from "react";
import CustomShefasebaBar from "../../components/CustomShefasebaBar";
import useFetchYear from "../semestruli-shefaseba/useYear";
import useGradeAnual from "./useGradeAnual";

const TsliuriShefaseba = () => {


    const {data: yearData, isLoading: isYearLoading} = useFetchYear();
    const [chosenYear, setChosenYear] = useState();

    const [selectedData, setSelectedData] = useState('2022-2023');
    const [currentData, setCurrentData] = useState([]);

    const {data, isLoading, isError, error, isSuccess} = useGradeAnual({yearRange: yearData});

    const parsedData = useMemo(() => {
        const result = [];
        if (!data) {
            return result
        }
        return data[0]?.gradeList?.map((grade) => ({
            name: grade.subject.name || "",
            value: grade.value["4"] ? grade.value["4"] < 0 ? 0 : grade.value["4"] : 0
        }))
    }, [data])


    const handleChange = (event) => {
        setChosenYear(event.target.value);
    };

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
    
    
        const handleSearch = async () =>{
            if(!!selectedData) {
                const studentYearlyGrades = [
                    {
                        name: 'ქართული ენა და ლიტერატურა',
                        ქულა: 7,
                    },

                    {
                        name: 'მათემატიკა',
                        ქულა: 6,
                    },

                    {
                        name: 'ინგლისური',
                        ქულა: 7,
                    },
                    {
                        name: 'ისტორია',
                        ქულა: 6,
                    },
                    {
                        name: 'გეოგრაფია',
                        ქულა: 7,
                    }]
                
                setCurrentData(studentYearlyGrades)
            }
        }
    
    
        return ( 
            <>
                <div className="ib__center column">
                    <div className="pageName">მოსწავლის წლიური შეფასება</div>
                    <div style={{display: 'flex', alignItems: 'center', marginTop: '25px'}}>
                        {getYearDropdown()}
                        <div style={{marginLeft: '10px'}}>
                            <Button onClick={handleSearch} disabled={!selectedData}
                                    style={{fontWeight: 'bold', height: '50px'}} variant="contained">ძიება<SearchIcon/></Button>
                        </div>
                    </div>
                </div>

                {!!parsedData && parsedData.length > 0 && <div style={{marginTop: 30}}>
                    <CustomShefasebaBar color={'#45c1a4'} data={parsedData} height={450} width={window.innerWidth - 30}
                                        left={0}/>
                </div>}

            </>
         );
}
 
export default TsliuriShefaseba;