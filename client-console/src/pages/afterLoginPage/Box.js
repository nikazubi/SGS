import { Link } from 'react-router-dom';

const Box = ({text, color, link}) => {
    return ( 
        <Link style={{borderColor:color}} to={link} className="box">
        <div>
            <div>{text}</div>
        </div>
        </Link>
     );
}
 
export default Box;