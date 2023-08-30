import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import SearchIcon from '@mui/icons-material/Search';
import Button from '@mui/material/Button';
import { useState } from "react";
import CustomShefasebaBar from "../../components/CustomShefasebaBar";

const TsliuriShefaseba = () => {

    const year = [
        2021,
        2022,
        2023
    ];
    
      const [selectedData, setSelectedData] = useState('');
      const [currentData, setCurrentData] = useState([]);
    
      const handleChange = (event) => {
        setSelectedData(event.target.value);
      };
    
        function dropdown(){
            return (
                <div className="yearDropwdown">
                    <TextField
                    select
                    label="აირჩიე წელი"
                    value={selectedData}
                    onChange={handleChange}
                    variant="outlined"
                    >
                    {year.map((m) => (
                        <MenuItem key={m} value={m}>
                        {m}
                        </MenuItem>
                    ))}
                    </TextField>
                </div>
            );
        }
    
    
        const handleSearch = async () =>{
            if(!!selectedData) {
                const studentYearlyGrades = [
                    {
                      name: 'მუსიკა',
                      ქულა: 1, 
                    },
            
                    {
                      name: 'მათემატიკა',
                      ქულა: 5,
                    },
            
                    {
                      name: 'მუსიკა',
                      ქულა: 7, 
                    }]
                
                setCurrentData(studentYearlyGrades)
            }
        }
    
    
        return ( 
            <>
            <div className="ib__center column">
                <div className="pageName">მოსწავლის წლიური შეფასება</div>
                <div style={{display:'flex', alignItems:'center', marginTop:'25px'}}>
                {dropdown()}
                <div style={{marginLeft:'10px'}}>
                    <Button onClick={handleSearch} disabled={!selectedData} style={{ fontWeight: 'bold', height: '50px'}} variant="contained">ძიება<SearchIcon/></Button>
                </div>
                </div>
            </div>
    
            {!!currentData.length && <div className="absenceMain shefaseba horizontal">
                <CustomShefasebaBar color={'#FF5722'} data={currentData} />
            </div>}
    
            </>
         );
}
 
export default TsliuriShefaseba;