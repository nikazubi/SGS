import { useContext, useEffect, useState } from "react";
import Chart from "./Chart";
import Dropdown from "./Dropdown";
import SemestruliBox from "./SemestruliBox";
import SmallBox from "./SmallBox";
import Button from '@mui/material/Button';
import userDataContext from "../../context/userDataContext";

const SemestruliShefaseba = () => {

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

    const [selectedSubject, setSelectedSubject] = useState('');
    const [selectedMonth, setSelectedMonth] = useState('');
    const [selectedYear, setSelectedYear] = useState('');
    const [allSelectedData, setAllSelectedData] = useState({});

    const [subject, setSubject] = useState(subjectsData)
    const [month, setMonth] = useState(monthData)
    const [year, setYear] = useState(yearsData)
    
    console.log(selectedSubject, selectedMonth, selectedYear)
    const test123 = useContext(userDataContext)
    console.log(test123,'GUDA')
    useEffect(()=>{

        
        //API for Subjects month and year
        
        //with returned data should run setSubject, setMonth setYear

    },[subject, month, year])

    useEffect(()=>{

        if (selectedSubject && selectedMonth && selectedYear) {
            setIsButtonActive(false)
        }
        else {
            setIsButtonActive(true)
        }

    },[selectedSubject, selectedMonth, selectedYear])

    const handleSearch = async () =>{

        if(selectedSubject && selectedMonth && selectedYear) {
            //API AXIOS
            setAllSelectedData({selectedSubject, selectedMonth, selectedYear})
        }
    }

    return ( 
        <div className="ibCnt">
            <div className="ib__center column">
                <div>მოსწავლის შეფასება საგნობრივი დისციპლინების მიხედვით</div>
                <div>
                <Dropdown data={subject} select={setSelectedSubject} label={'საგანი'}/>
                <Dropdown data={month} select={setSelectedMonth} label={'თვე'}/>
                <Dropdown data={year}  select={setSelectedYear} label={'წელი'}/>
                </div>
                <Button onClick={handleSearch} disabled={isButtonActive} style={{ fontWeight: 'bold'}} variant="contained">ძიება</Button>
            </div>

            <div className="termEstCnt">
                {/* id=1 means it's  შემაჯამებელი დავალება*/}
                <SemestruliBox title={'შემაჯამებელი დავალება'} number={'I'} precent={'50%'} id={1} data={allSelectedData}/>
                <SemestruliBox title={'საშინაო დავალება'} number={'II'} precent={'30%'} id={2} data={allSelectedData}/>
                <SemestruliBox title={'საკლასო დავალება'} number={'III'} precent={'20%'} id={3} data={allSelectedData}/>
            </div>

            <div className="ib__center" style={{gap:'30px'}}>
                <div className="avgCnt">
                    <div style={{marginLeft:"20px"}}>თვის ქულა</div>
                    <SmallBox boxLabel={'თვის ქულა'} />
                </div>

                <div className="avgCnt">
                    <div boxLabel={'გაცდენილი საათები'} style={{marginLeft:"20px"}}>გაცდენილი საათები</div>
                    <SmallBox/>
                </div>
            </div>

            <div className="ibChart">
                <Chart/>
            </div>

        </div>
     );
}
 
export default SemestruliShefaseba;