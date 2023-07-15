const ShefasebaTable = ({data}) => {
    return ( 
        <div className="shefasebaCnt">
            <table className="shefasebaTable">
                <tr>{data.map(data=><th key={data.subject} className="shefasebaTable__col row">{data.subject}</th>)}</tr>
                <tr>{data.map(data=><td key={data.subject} className="shefasebaTable__col">{data.grade}</td>)}</tr>
            </table>
        </div>
     );
}
 
export default ShefasebaTable;