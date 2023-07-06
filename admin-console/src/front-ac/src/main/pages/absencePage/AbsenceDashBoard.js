import {useEffect, useState} from "react";
import AbsenceTableToolbar from "./AbsenceTableToolbar";
import CustomTable from "./AbsenceTable";

const AbsenceDashBoard = () => {
    const [filters, setFilters] = useState({});



    return (
        <div className="absenceMain">
            <AbsenceTableToolbar filters={filters} setFilters={setFilters}/>
            {/*<CustomTable/>*/}
        </div>
    )
}

export default AbsenceDashBoard;