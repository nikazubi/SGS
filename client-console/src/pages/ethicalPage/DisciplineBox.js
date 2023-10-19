import FooterBox from "./FooterBox";
// {title, number, precent,id, data}=argumentebia ->DisciplineBox
const DisciplineBox = ({data}) => {
    console.log(data)
    const allData = data;

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

    // /min-width: 310px;
    //     height: 230px;
    //     border-radius: 0px 30px 30px 30px;
    //     background-color: #01619b;
    //     text-align: center;
    //     font-size: 20px;
    //     padding: 0 15px;
    //     flex-grow: 1;
    return (
        allData.map(m =>
            
          (m.boxdetails[0] !== 'თვის ქულა' && m.boxdetails[0] !== 'გაცდენილი საათები') ? 
          (
            <div style={{minWidth: "310px",
                height: m.month || m.absence ? 150: 200,
                // maxHeight: m.month === "თვის ქულა" || m.month === "გაცდენილი საათები"? 100: 200,
                borderRadius: "0px 30px 30px 30px",backgroundColor:'#01619b', textAlign:'center', fontSize: '20px', flexGrow:1}} key={m.name} >
              <div className="ethical__title">{m.name}</div>
              <div>{m.testNumber}</div>
              <div className="ethical__precent">{m.precent}</div>
                {m.month &&
                    <div style={{display:'flex', alignItems:'center', justifyContent: 'space-evenly'}}>
                        <div className="modified__title">{m.month}</div>
                        <div className="grade__data__api ethical">{m.monthGrade}</div>
                    </div>
                }
                { m.absence  &&
                    <div style={{display:'flex', alignItems:'center', justifyContent: 'space-evenly'}}>
                        <div className="modified__title">{m.absence}</div>
                        <div className="grade__data__api ethical">{m.absenceGrade}</div>
                    </div>
                }
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