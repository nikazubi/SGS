import {useEffect, useState} from "react";
import AbsenceTableToolbar from "./AbsenceTableToolbar";
import CustomTable from "./AbsenceTable";
import DataGridPaper from "../../components/grid/DataGridPaper";
import DataGridSGS from "../../components/grid/DataGrid";
import useChangeRequests from "../changeRequestPage/useChangeRequests";

const AbsenceDashBoard = () => {
    const [filters, setFilters] = useState({groupByClause: 'STUDENT'});

    const {data, isLoading, isError, error} = useChangeRequests(filters);

    const absenceColumn = []


    return (
        <div className="absenceMain">
            <AbsenceTableToolbar filters={filters} setFilters={setFilters}/>
            <CustomTable/>
        </div>
    )
}

export default AbsenceDashBoard;