import {useEffect, useState} from "react";
import ChangeRequestTableToolbar from "./ChangeRequestTableToolbar";

const ChangeRequestDashBoard = () => {
    const [filters, setFilters] = useState({});
    useEffect(() =>{
        console.log(filters)
    },[filters])
    return (
        <div>
            <ChangeRequestTableToolbar filters={filters} setFilters={setFilters}/>
        </div>
    )
}

export default ChangeRequestDashBoard;