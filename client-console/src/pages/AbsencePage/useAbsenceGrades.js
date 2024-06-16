import {useQuery} from "react-query";
import axios from "../utils/axios";

export const fetchAbsenceGrades = async (filters) => {
    const params = {
        yearRange: filters.yearRange,
    }
    const {data} = await axios.get("/client/absence", {params});
    return data;
}

const useAbsenceGrade = (filterData) => useQuery(["ABSENCE_GRADES", filterData],
    () => fetchAbsenceGrades(filterData));

export default useAbsenceGrade;