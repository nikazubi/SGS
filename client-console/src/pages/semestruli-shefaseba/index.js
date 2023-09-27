import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import SearchIcon from '@mui/icons-material/Search';
import Button from '@mui/material/Button';
import {useEffect, useState} from "react";
import CustomShefasebaBar from "../../components/CustomShefasebaBar";

const SemestruliShefaseba = () => {

    const term = [
        '2021-2022',
        '2022-2023'
    ];
    
      const [selectedData, setSelectedData] = useState('2022-2023');
    const [currentData, setCurrentData] = useState([]);
      const [currentDataTerm2, setCurrentDataTerm2] = useState([]);
    
      const handleChange = (event) => {
        setSelectedData(event.target.value);
      };

      useEffect(()=> {
          handleSearch();
      }, [])
    
        function dropdown(){
            return (
                <div className="yearDropwdown">
                    <TextField
                    select
                    label="აირჩიეთ სასწავლო წელი"
                    value={selectedData}
                    onChange={handleChange}
                    variant="outlined"
                    >
                    {term.map((m) => (
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
                const studentTermGrades = [
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
                
                setCurrentData(studentTermGrades)

                //tu ar arsebobs - [] => da ar gamoitans meore semestrs

                const studentTermGrades2 = [
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
                        ქულა: 6,
                    },
                    {
                        name: 'ისტორია',
                        ქულა: 6,
                    },
                    {
                        name: 'გეოგრაფია',
                        ქულა: 7,
                    }]
                
                setCurrentDataTerm2(studentTermGrades2)

            }
        }
    
    
        return ( 
            <>
                <div className="ib__center column">
                    <div className="pageName">მოსწავლის სემესტრული შეფასება</div>
                    <div style={{display: 'flex', alignItems: 'center', marginTop: '25px'}}>
                        {dropdown()}
                        <div style={{marginLeft: '10px'}}>
                            <Button onClick={handleSearch} disabled={!selectedData}
                                    style={{fontWeight: 'bold', height: '50px'}} variant="contained">ძიება<SearchIcon/></Button>
                        </div>
                    </div>
                </div>
                {/*<div>*/}
                {/*    <DataGrid*/}
                {/*        columns={[{ field: 'username' }, { field: 'age' }]}*/}
                {/*        rows={[*/}
                {/*            {*/}
                {/*                id: 1,*/}
                {/*                username: '@MUI',*/}
                {/*                age: 20,*/}
                {/*            },*/}
                {/*        ]}*/}
                {/*        sx={DataGridStyles()}*/}
                {/*    />*/}
                {/*</div>*/}
                {!!currentData.length && <div className="absenceMain shefaseba term horizontal">
                    <div style={{fontWeight: 'unset', marginBottom: 10, marginTop: -25}} className="pageName">I სემესტრი
                    </div>
                    <CustomShefasebaBar color={'#45c1a4'} data={currentData}/>
                </div>}

                {!!currentDataTerm2.length && <div className="absenceMain shefaseba term horizontal">
                    <div style={{fontWeight: 'unset', marginBottom: 10, marginTop: -25}} className="pageName">II
                        სემესტრი
                    </div>

                    <CustomShefasebaBar color={'#45c1a4'} data={currentDataTerm2}/>
                </div>}
                <div style={{height: 100}}>

                </div>
            </>
         );
}
 
export default SemestruliShefaseba;