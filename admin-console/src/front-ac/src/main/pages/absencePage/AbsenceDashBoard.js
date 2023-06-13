import {useEffect, useState} from "react";
import AbsenceTableToolbar from "./AbsenceTableToolbar";

const AbsenceDashBoard = () => {
    const [filters, setFilters] = useState({});
    useEffect(() =>{
        console.log(filters)
    },[filters])
    return (
        <div>
            <AbsenceTableToolbar filters={filters} setFilters={setFilters}/>
        </div>
    )
}

export default AbsenceDashBoard;