import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import SearchIcon from '@mui/icons-material/Search';
import Button from '@mui/material/Button';
import {useEffect, useState} from "react";
import CustomShefasebaBar from "../../components/CustomShefasebaBar";

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

  const [selectedData, setSelectedData] = useState('ივნისი');
    const [currentData, setCurrentData] = useState([]);

  const handleChange = (event) => {
    setSelectedData(event.target.value);
  };

    useEffect(()=>{
        handleSearch();
    }, []);
    function dropdown(){
        return (
            <div className="yearDropwdown">
                <TextField
                select
                label="აირჩიე თვე"
                value={selectedData}
                onChange={handleChange}
                variant="outlined"
                >
                {month.map((m) => (
                    <MenuItem key={m} value={m}>
                    {m}
                    </MenuItem>
                ))}
                </TextField>
            </div>
        );
    }


    const handleSearch = () =>{
        if(!!selectedData) {
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
            <div style={{display:'flex', alignItems:'center', marginTop:'25px'}}>
            <div style={{display: 'flex'}}>
                {dropdown()}
                <div style={{marginLeft: '10px'}}>
                    <Button onClick={handleSearch} disabled={!selectedData} style={{fontWeight: 'bold', height: '50px'}}
                            variant="contained">ძიება<SearchIcon/></Button>
                </div>
            </div>
            </div>
        </div>

        {!!currentData.length && <div className="absenceMain shefaseba horizontal">
            <CustomShefasebaBar color={'#45c1a4'} data={currentData}/>
        </div>}

        </>
     );
}
 
export default MonthlyGrade;