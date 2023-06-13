import GradeTableToolbar from "./GradeTableToolbar";
import {useEffect, useState} from "react";

const DashBoard = () => {
    const [filters, setFilters] = useState({});
    useEffect(() =>{
        console.log(filters)
    },[filters])
    return (
        <div>
            <GradeTableToolbar filters={filters} setFilters={setFilters}/>
        </div>
    )
}

export default DashBoard;