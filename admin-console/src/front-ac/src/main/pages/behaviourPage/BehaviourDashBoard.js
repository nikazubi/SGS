import {useEffect, useState} from "react";
import BehaviourTableToolbar from "./BehaviourTableToolbar";

const BehaviourDashBoard = () => {
    const [filters, setFilters] = useState({});
    useEffect(() =>{
        console.log(filters)
    },[filters])
    return (
        <div>
            <BehaviourTableToolbar filters={filters} setFilters={setFilters}/>
        </div>
    )
}

export default BehaviourDashBoard;