import { useUserData } from "../../context/userDataContext";
import FooterBox from "./FooterBox";
// {title, number, precent,id, data}=argumentebia ->DisciplineBox
const DisciplineBox = () => {

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
    return (
        allData.map(m =>
            
          (m.boxdetails[0] !== 'თვის ქულა' && m.boxdetails[0] !== 'გაცდენილი საათები') ? 
          (
            <div className="DisciplineBox" key={m.name} style={{backgroundColor: renderBackground(m.name)}}>
              <div className="DisciplineBox__title">{m.name}</div>
              <div>{m.testNumber}</div>
              <div className="DisciplineBox__precent">{m.precent}</div>
              <FooterBox boxdetails={m.boxdetails} />
            </div>
          ) : null
        )
      );
      
             // <div className="DisciplineBox">
        //     <div className="DisciplineBox__title">{title}</div>
        //     <div>{number}</div>
        //     <div className="DisciplineBox__precent">{precent}</div>
        //     <FooterBox data={data} testID={id}/>
        // </div>
}
 
export default DisciplineBox;