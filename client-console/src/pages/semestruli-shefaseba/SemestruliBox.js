import { useUserData } from "../../context/userDataContext";
import SemestruliBoxFooter from "./SemestruliBoxFooter";
// {title, number, precent,id, data}=argumentebia ->SemestruliBox
const SemestruliBox = () => {

    const allData = useUserData()
    // ArrayOfObj = [{
    //     name: 'შემაჯამებელი დავალება' | 'საშინაო დავალება' | 'საკლასო დავალება' | null,
    //     number: romauli ricxvebi iyo -> III ან I ა.შ | null,
    //     precent: string -> '50%' | null,
    //     boxLabel: 1 | 2 | 3 | 'აღდ' | 'თვე' | '%', თვის ქულა || გაცდენების რაოდენობა,
    //     monthlyGrade:number | null ,
    //     mark: number | null
    //     GACDENA:  number | null
    // }]

    //meore varianti ori database
        // ArrayOfObj1 = [{
    //     name: 'შემაჯამებელი დავალება' | 'საშინაო დავალება' | 'საკლასო დავალება',
    //     number: romauli ricxvebi iyo -> III ან I ა.შ ,
    //     precent: string -> '50%',
    //     boxLabel: 1 | 2 | 3 | 'აღდ' | 'თვე' | '%',
    //     mark: number | null
    // }]
        // ArrayOfObj2 = [{
    //     boxLabel: თვის ქულა || გაცდენების რაოდენობა,
    //     GACDENA:  number | null,
    // monthlyGrade:number | null ,
    // }]


{/* id=1 means it's  შემაჯამებელი დავალება*/}
    const renderBackground = (id) =>{
        switch(id){
            case 'შემაჯამებელი დავალება':
                return "#f25d23";
            case 'საშინაო დავალება':
                return "#f79348";
            case 'საკლასო დავალება':
                return "#f8a66a";
            default:
                return "orange"
        }
    }
    console.log(allData, 'TEST123 ALL DATA')
    return (
        allData.map(m =>
            
          (m.boxdetails[0] !== 'თვის ქულა' && m.boxdetails[0] !== 'გაცდენილი საათები') ? 
          (
            <div className="semestruliBox" key={m.name} style={{backgroundColor: renderBackground(m.name)}}>
              <div className="semestruliBox__title">{m.name}</div>
              <div>{m.testNumber}</div>
              <div className="semestruliBox__precent">{m.precent}</div>
              <SemestruliBoxFooter boxdetails={m.boxdetails} />
            </div>
          ) : null
        )
      );
      
             // <div className="semestruliBox">
        //     <div className="semestruliBox__title">{title}</div>
        //     <div>{number}</div>
        //     <div className="semestruliBox__precent">{precent}</div>
        //     <SemestruliBoxFooter data={data} testID={id}/>
        // </div>
}
 
export default SemestruliBox;