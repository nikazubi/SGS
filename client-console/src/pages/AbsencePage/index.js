import CustomBar from "./BarChart";
import { useState, useEffect } from "react";
import Dropdown from "./Dropdown";
import Button from '@mui/material/Button';
import SearchIcon from '@mui/icons-material/Search';
const AbsencePage = () => {

  useEffect(()=>{
    const monthData = [
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
    setMonth(monthData)

    const yearsData = [2021 ,2022, 2023]
    setYear(yearsData)
  },[])


  const [absenceBySubject, setAbsenceBySubject] = useState([]);

  const [selectedMonth, setSelectedMonth] = useState('');
  const [selectedYear, setSelectedYear] = useState('');


  const [month, setMonth] = useState([])
  const [year, setYear] = useState([])
  //API CALL
  const [attendMax, setAttendMAx] = useState(0) //chatarebuli saatebi
  const [absence, setAbsence] = useState(0) //gaacdina sul

    const allAbsenceData = [
        {
          name: 'გაცდენა',
          არა: absence, 
        },
      ];

    const handleSearch = async () =>{
      if(!!selectedMonth && !!selectedYear) {
    //60 miviget jamshi anu sul gacdenebis raodenoba rac werie "absence"

    const subjectAbsence = [
        {
          name: 'მუსიკა',
          არა: 1, 
        },

        {
          name: 'მათემატიკა',
          არა: 17,
        },

        {
          name: 'მუსიკა',
          არა: 1, 
        },

        {
          name: 'მათემატიკა',
          არა: 5,
        },
        {
          name: 'მუსიკა',
          არა: 5, 
        },

        {
          name: 'მათემატიკა',
          არა: 5,
        },

        {
          name: 'ქართული',
          არა: 5, 
        },

        {
          name: 'მათემატიკა',
          არა: 5,
        },

        {
          name: 'ქართული',
          არა: 5, 
        },

        {
          name: 'მათემატიკა',
          არა: 1,
        },

        {
          name: 'ქართული',
          არა: 5, 
        },

        {
          name: 'ქართული',
          არა: 5, 
        },
      ];
          setAbsenceBySubject(subjectAbsence)
          setAbsence(60)
          setAttendMAx(120)
      }
  }

    return ( 
        <>
        <div className="ib__center column">
            <div className="pageName">მოსწავლის მიერ გაცდენილი საათები</div>
            <div className="absenceDropdown">
            <Dropdown data={month} select={setSelectedMonth} label={'თვე'}/>
            <Dropdown data={year}  select={setSelectedYear} label={'წელი'}/>
            <div style={{marginLeft:'10px'}}>
              <Button onClick={handleSearch} disabled={!selectedMonth || !selectedYear} style={{ fontWeight: 'bold', height: '40px'}} variant="contained">ძიება<SearchIcon/></Button>
            </div>
            </div>

        </div>
        {!!absence && <div className="absenceMain">
            <CustomBar color={'#01619b'} attend={absence} attendMax={attendMax} layout={'vertical'} data={allAbsenceData}/>
        </div>}
        {!!absenceBySubject.length && <div className="absenceMain horizontal">
            <CustomBar color={'#FF5722'} attendMax={absence} keyLabel={'არა'} layout={'horizontal'} data={absenceBySubject} />
        </div>}
        </>
     );
}
 
export default AbsencePage;