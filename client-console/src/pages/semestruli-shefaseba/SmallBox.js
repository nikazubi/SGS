const SmallBox = ({testID, boxLabel}) => {
    return ( 
        <div className="ib__center column" style={{margin:"unset"}}>
            {boxLabel && <div>{boxLabel}</div>}
            {/* write retrived grades from API here */}
            <div className="grade__data__api">0</div>
        </div>
     );
}
 
export default SmallBox;