import { Link } from 'react-router-dom';
import { useEffect, useRef } from "react";

const Box = ({text, link}) => {

    const ref = useRef()

    useEffect(()=>{
        let temp = 0;
        if(ref.current)
        {
            temp = ref.current
            ref.current.classList.add('loaded')
        }

        if(!!temp)
        {
            return () =>{
                temp.classList.remove('loaded')
            }
        }
    },[ref])

    return ( 
        <Link ref={ref} to={link} className="box">
        <div>
            <div>{text}</div>
        </div>
        </Link>
     );
}
 
export default Box;