import { useEffect, useState } from "react";

const CustomTable = () => {

    const [dateTitleSpan, SetDateTitleSpan] = useState(1)

    const monthDateArray = ['13-02-23', '13-02-23', '13-02-23', '13-02-23', '13-02-23','13-02-23', '13-02-23', '13-02-23', '13-02-23', '13-02-23','13-02-23', '13-02-23', '13-02-23', '13-02-23', '13-02-23','13-02-23', '13-02-23', '13-02-23', '13-02-23', '13-02-23','13-02-23', '13-02-23', '13-02-23', '13-02-23', '13-02-23', '13-02-23', '13-02-23', '13-02-23', '13-02-23', '13-02-23', '13-02-23']
    
    const user = [{name:'Giorgi Gudadze', id: 'gdsgtergdf'}, {name:'Zubi Nika', id: 'weasd'}]

    function handleKeyPress(event) {
        if (event.key === 'Enter') {
            event.target.blur()
        }
      }

    function handleBlur(event) {
        console.log(event.target.value)
    }

    useEffect(()=>{
        const count = document.querySelectorAll('.absenceTable__date').length
        SetDateTitleSpan(count)
    },[dateTitleSpan])

    return ( 
<div className="absenceMain__cnt">        
<table className="absenceTable">
    <tr>
    <th className="absenceTable__nameSurname" colSpan={4} rowSpan={5}>მოსწავლის გვარი,<br></br>სახელი</th>
        <th style={{width:'100%'}} colSpan={dateTitleSpan}>თარიღი</th>
    </tr>

    <tr>
        {monthDateArray.map(m=><td colSpan={1} className="absenceTable__date" rowSpan={4}><div>{m}</div></td>)}
    </tr>

        {user.map(user=>{
            return(
                <tr style={{display:'table-footer-group'}}>
                <td colSpan={4}>{user.name}</td>
                {monthDateArray.map(m=>
                <td>
                    <input className="absenceTable__attend" onKeyDown={handleKeyPress} onBlur={handleBlur} type="text" maxLength={3}/>
                </td>)}
                </tr>
            )
        })}

    </table>
    </div>
);
}
 
export default CustomTable;