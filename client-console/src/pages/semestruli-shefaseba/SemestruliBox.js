import SemestruliBoxFooter from "./SemestruliBoxFooter";

const SemestruliBox = ({title, number, precent,id, data}) => {

    console.log(data,'DATA')

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
    const renderBackground = () =>{
        switch(id){
            case 1:
                return "#f25d23";
            case 2:
                return "#f79348";
            case 3:
                return "#f8a66a";
            default:
                return "orange"
        }
    }

    return ( 
        <div className="semestruliBox" style={{backgroundColor:renderBackground()}}>
            <div className="semestruliBox__title">{title}</div>
            <div>{number}</div>
            <div className="semestruliBox__precent">{precent}</div>
            <SemestruliBoxFooter data={data} testID={id}/>
        </div>
     );
}
 
export default SemestruliBox;