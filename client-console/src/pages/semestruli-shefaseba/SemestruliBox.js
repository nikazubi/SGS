import SemestruliBoxFooter from "./SemestruliBoxFooter";

const SemestruliBox = ({title, number, precent,id}) => {

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
            <SemestruliBoxFooter testID={id}/>
        </div>
     );
}
 
export default SemestruliBox;