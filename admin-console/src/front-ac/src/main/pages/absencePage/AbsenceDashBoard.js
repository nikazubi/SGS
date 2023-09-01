import {useState} from "react";
import AbsenceTableToolbar from "./AbsenceTableToolbar";
import CustomTable from "./AbsenceTable";
import useFetchChangeRequest from "../changeRequestPage/useChangeRequests";

const AbsenceDashBoard = () => {
    const [filters, setFilters] = useState({groupByClause: 'STUDENT'});

    const {data, isLoading, isError, error} = useFetchChangeRequest(filters);

    const absenceColumn = []


    return (
        <div className="absenceMain">
            <AbsenceTableToolbar filters={filters} setFilters={setFilters}/>
            <CustomTable/>
        </div>
    )
}

export default AbsenceDashBoard;