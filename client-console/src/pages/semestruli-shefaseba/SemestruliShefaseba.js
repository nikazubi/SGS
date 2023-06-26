import Chart from "./Chart";
import Dropdown from "./Dropdown";
import SemestruliBox from "./SemestruliBox";
import SmallBox from "./SmallBox";

const SemestruliShefaseba = () => {
    return ( 
        <div className="ibCnt">
            <div className="ib__center column">
                <div>მოსწავლის შეფასება საგნობრივი დისციპლინების მიხედვით</div>
                <Dropdown />
            </div>

            <div className="termEstCnt">
                <SemestruliBox title={'შემაჯამებელი დავალება'} number={'I'} precent={'50%'} id={1}/>
                <SemestruliBox title={'საშინაო დავალება'} number={'II'} precent={'30%'} id={2}/>
                <SemestruliBox title={'საკლასო დავალება'} number={'III'} precent={'20%'} id={3}/>
            </div>

            <div className="ib__center" style={{gap:'30px'}}>
                <div className="avgCnt">
                    <div style={{marginLeft:"20px"}}>თვის ქულა</div>
                    <SmallBox/>
                </div>

                <div className="avgCnt">
                    <div style={{marginLeft:"20px"}}>გაცდენილი საათები</div>
                    <SmallBox/>
                </div>
            </div>

            <Chart/>

        </div>
     );
}
 
export default SemestruliShefaseba;