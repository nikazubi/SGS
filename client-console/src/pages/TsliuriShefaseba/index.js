import ShefasebaTable from "../../components/ShefasebaTable";
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import { useState } from "react";

const TsliuriShefaseba = () => {

    const data = [
        {
            subject: 'მუსიკა',
            grade: 7
        },

        {
            subject: 'მათემატიკა',
            grade: 7
        },

        {
            subject: 'ქართული',
            grade: 7
        },
]



const years = [
  2020,
  2021,
  2022,
  2023,
  // Add more years as needed
];

  const [selectedYear, setSelectedYear] = useState('');

  const handleYearChange = (event) => {
    setSelectedYear(event.target.value);
  };
    function dropdown(){
        return (
            <div className="yearDropwdown">
                <TextField
                select
                label="აირჩიე წელი"
                value={selectedYear}
                onChange={handleYearChange}
                variant="outlined"
                >
                {years.map((year) => (
                    <MenuItem key={year} value={year}>
                    {year}
                    </MenuItem>
                ))}
                </TextField>
            </div>
        );
    }



    return ( 
        <>
        {dropdown()}
        {selectedYear && <ShefasebaTable data={data}/>}
        </>
     );
}
 
export default TsliuriShefaseba;