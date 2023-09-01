import CustomBar from "./BarChart";
import {useEffect, useState} from "react";
import Dropdown from "./Dropdown";
import Button from '@mui/material/Button';
import SearchIcon from '@mui/icons-material/Search';

const AbsencePage = () => {

  // useEffect(()=>{
    //   const monthData = ;
    //   setMonth(monthData)
    //
    //   const yearsData =
    //   setYear(yearsData)
    // },[])


    const [absenceBySubject, setAbsenceBySubject] = useState([]);

    const [selectedMonth, setSelectedMonth] = useState('ივნისი');
    const [selectedYear, setSelectedYear] = useState(2023);


    const [month, setMonth] = useState([
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
  ])
  const [year, setYear] = useState([2021 ,2022, 2023])
  //API CALL
  const [attendMax, setAttendMAx] = useState(0) //chatarebuli saatebi
  const [absence, setAbsence] = useState(0) //gaacdina sul

    const allAbsenceData = [
        {
          name: 'გაცდენა',
          არა: absence, 
        },
      ];

    useEffect(()=> {
        handleSearch();
    }, [])

    const handleSearch = async () =>{
      if(!!selectedMonth && !!selectedYear) {
    //60 miviget jamshi anu sul gacdenebis raodenoba rac werie "absence"

    const subjectAbsence = [
        {
            name: 'ქართული ენა და ლიტერატურა',
            არა: 10,
        },

        {
            name: 'მათემატიკა',
            არა: 8,
        },

        {
            name: 'ინგლისური',
            არა: 7,
        },
        {
            name: 'ისტორია',
            არა: 3,
        },
        {
            name: 'გეოგრაფია',
            არა: 7,
        }
      ];
          setAbsenceBySubject(subjectAbsence)
          setAbsence(35)
          setAttendMAx(120)
      }
  }

    return ( 
        <>
        <div className="ib__center column">
            <div className="pageName">მოსწავლის მიერ გაცდენილი საათები</div>
            <div className="absenceDropdown">
                <Dropdown data={month} value={selectedMonth} select={setSelectedMonth} label={'თვე'}/>
                <Dropdown data={year} value={selectedYear} select={setSelectedYear} label={'წელი'}/>
                <div style={{marginLeft: '10px', marginTop: '6px'}}>
                    <Button onClick={handleSearch} disabled={!selectedMonth || !selectedYear}
                            style={{fontWeight: 'bold', height: '40px'}} variant="contained">ძიება<SearchIcon/></Button>
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