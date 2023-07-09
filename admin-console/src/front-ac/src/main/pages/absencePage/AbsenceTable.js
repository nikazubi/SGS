import { useEffect, useState } from "react";

const CustomTable = () => {

    const [dateTitleSpan, SetDateTitleSpan] = useState(1)

    const monthDateArray = ['13-02-23', '13-02-23', '13-02-23', '13-02-23', '13-02-23','13-02-23', '13-02-23', '13-02-23', '13-02-23', '13-02-23','13-02-23', '13-02-23', '13-02-23', '13-02-23', '13-02-23','13-02-23', '13-02-23', '13-02-23', '13-02-23', '13-02-23','13-02-23', '13-02-23', '13-02-23', '13-02-23', '13-02-23', '13-02-23', '13-02-23', '13-02-23', '13-02-23', '13-02-23', '13-02-23']
    
    const user = [
        {name:'ზუბიაშვილი სალომე', id: '1'},
        {name:'ზუბიაშვილი ნიკა', id: '2'},
        {name:'ზუბიაშვილი ვასო', id: '3'},
        {name:'ზუბიაშვილი ანა', id: '4'},
        {name:'ზუბიაშვილი ნინო', id: '5'},
        {name:'ზუბიაშვილი სოსო', id: '55'},
        {name:'ზუბიაშვილი ლანა', id: '11'},
        {name:'ზუბიაშვილი ელენე', id: '22'},
        {name:'ზუბიაშვილი დემე', id: '33'},
        {name:'ზუბიაშვილი დათო', id: '44'},
        {name:'ზუბიაშვილი ლუკა', id: '555'},
        {name:'ზუბიაშვილი ზაზა', id: '111'},
        {name:'ზუბიაშვილი ნიკოლოზი', id: '222'},
        {name:'ზუბიაშვილი გუგა', id: '333'},
        {name:'ზუბიაშვილი კოტე', id: '444'},
        {name:'ზუბიაშვილი ნინა', id: '6'},
        {name:'ზუბიაშვილი ლიზა', id: '7'},
        {name:'ზუბიაშვილი თამთა', id: '8'},
        {name:'ზუბიაშვილი თატული', id: '9'},
        {name:'ზუბიაშვილი თათია', id: '10'},
        {name:'ზუბიაშვილი თამარ', id: '12'},
        {name:'ზუბიაშვილი გიგი', id: '14'},
        {name:'ზუბიაშვილი გოგა', id: '13'},
        {name:'ზუბიაშვილი გეგა', id: '15'},
        {name:'ზუბიაშვილი შოთა', id: '16'},
        {name:'ზუბიაშვილი ლიკა', id: '17'}
        
    ]

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