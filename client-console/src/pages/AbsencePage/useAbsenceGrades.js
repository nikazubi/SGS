import {useQuery} from "react-query";
import axios from "../utils/axios";

export const fetchAbsenceGrades = async (filters) => {
    const params = {
        month: filters.month,
        yearRange: filters.yearRange,
    }
    const {data} = await axios.get("/client/grade/get-grades-absence", {params});
    return data;
}

const useAbsenceGrade = (filterData) => useQuery(["ABSENCE_GRADES", filterData],
    () => fetchAbsenceGrades(filterData));

export default useAbsenceGrade;