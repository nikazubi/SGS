import { useEffect, useState } from "react";

const SmallBox = ({testID, boxLabel, data}) => {

    const [studentResult, setStudentResult] = useState([])
    const [grade, setGrade] = useState(0)
    useEffect(()=>{
        //api
        //it should come from Api that it's 'შემაჯამებელი წერა' or other if not testID=1 => შემაჯამებელი წერა
        //tu boxLabel anu shemajamebeli werebis raodenoba araa fiksirebuli mashin API gvinda
        //data prop example => {selectedSubject: 'ქართული', selectedMonth: 'სექტემბერი', selectedYear: 2023}

        if(testID === 1){
            //axios
            //boxLabel -> 1 ანუ -> პირველი შემაჯამებელი წერა

            //API SHOULD LOOK LIKE THIS
            //  ArrayOfobj = [{
            //     boxLabel: 1 | 2 | 3 | 'აღდ' | 'თვე' | '%',
            //     mark: number | string
            // }]
        }

        //Tvis kula da gacdena 
        else{
            //API SHOULD LOOK LIKE THIS
            //  ArrayOfobj = [{
            //     boxLabel: თვის ქულა || გაცდენების რაოდენობა,
            //     monthlyGrade | GACDENA: number 
            // }]
        }

    },[])

    return ( 
        <div className="ib__center column" style={{margin:"unset"}}>
            {boxLabel !== 'თვის ქულა' && boxLabel !== 'გაცდენილი საათები' && <div>{boxLabel}</div>}
            {/* write retrived grades from API here */}
            <div className="grade__data__api">{grade}</div>

            {/* {studentResult.map(data=>{
                if(data.boxLabel !== 'თვის ქულა' && data.boxLabel !== 'გაცდენილი საათები'){
                    return(
                        <>
                        <div>{data.boxLabel}</div>
                        <div className="grade__data__api">{data.mark}</div>
                        </>
                    )
                }
                else{
                    <div className="grade__data__api">{data.boxLabel === 'თვის ქულა' ? data.monthlyGrade : data.GACDENA}</div>
                }
            })} */}
        </div>
     );
}
 
export default SmallBox;