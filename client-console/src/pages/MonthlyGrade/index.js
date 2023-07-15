import ShefasebaTable from "../../components/ShefasebaTable";
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import { useState } from "react";

const MonthlyGrade = () => {

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

  const [selectedYear, setSelectedYear] = useState('');

  const handleYearChange = (event) => {
    setSelectedYear(event.target.value);
  };
    function dropdown(){
        return (
            <div className="yearDropwdown">
                <TextField
                select
                label="აირჩიე თვე"
                value={selectedYear}
                onChange={handleYearChange}
                variant="outlined"
                >
                {month.map((year) => (
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
 
export default MonthlyGrade;