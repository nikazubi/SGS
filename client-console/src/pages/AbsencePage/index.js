import CustomBar from "./BarChart";
import { useState, useEffect } from "react";
import Dropdown from "./Dropdown";
import Button from '@mui/material/Button';
const AbsencePage = () => {


  const subjectsData = ['მათემატიკა', 'ქართული', 'მუსიკა']
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
  const yearsData = [2021 ,2022, 2023]
  
  const [isButtonActive, setIsButtonActive] = useState(true)

  const [selectedMonth, setSelectedMonth] = useState('');
  const [selectedYear, setSelectedYear] = useState('');

  const [subject, setSubject] = useState(subjectsData)
  const [month, setMonth] = useState(monthData)
  const [year, setYear] = useState(yearsData)

  useEffect(()=>{

    if (selectedMonth && selectedYear) {
        setIsButtonActive(false)
    }

    else {

        //es udna eweros handleSearch magram satestoa da ak magitom weria

        setIsButtonActive(true)
    }

},[selectedMonth, selectedYear])



//API CALL
    const [attendMax, setAttendMAx] = useState(120) //chatarebuli saatebi
    const [absence, setAbsence] = useState(60) //gaacdina sul

    const allAbsenceData = [
        {
          name: 'გაცდენა',
          არა: absence, 
        },
      ];
    //60 miviget jamshi anu sul gacdenebis raodenoba rac werie "absence"
    const absenceBySubject = [
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

    return ( 
        <>
            <div className="ib__center column">
                <div className="pageName">მოსწავლის მიერ გაცდენილი საათები</div>
                <div className="absenceDropdown">
                <Dropdown data={month} select={setSelectedMonth} label={'თვე'}/>
                <Dropdown data={year}  select={setSelectedYear} label={'წელი'}/>
                </div>
                <Button onClick={()=>console.log('1')} disabled={isButtonActive} style={{ fontWeight: 'bold'}} variant="contained">ძიება</Button>
            </div>
        <div className="absenceMain">
            <CustomBar color={'#01619b'} attend={absence} attendMax={attendMax} layout={'vertical'} data={allAbsenceData}/>
        </div>
        <div className="absenceMain horizontal">
            <CustomBar color={'#FF5722'} attendMax={absence} keyLabel={'არა'} layout={'horizontal'} data={absenceBySubject} />
        </div>
        </>
     );
}
 
export default AbsencePage;