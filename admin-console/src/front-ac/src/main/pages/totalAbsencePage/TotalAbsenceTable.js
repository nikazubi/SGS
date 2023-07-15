import { useEffect, useState } from "react";

const CustomTable = () => {

    const [dateTitleSpan, SetDateTitleSpan] = useState(1)


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

    </div>
);
}
 
export default CustomTable;