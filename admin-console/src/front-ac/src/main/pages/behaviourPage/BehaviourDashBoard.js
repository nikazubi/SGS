import {useEffect, useState} from "react";
import BehaviourTableToolbar from "./BehaviourTableToolbar";

const BehaviourDashBoard = () => {
    const [filters, setFilters] = useState({});

    return (
        <div>
            <BehaviourTableToolbar filters={filters} setFilters={setFilters}/>
        </div>
    )
}

export default BehaviourDashBoard;