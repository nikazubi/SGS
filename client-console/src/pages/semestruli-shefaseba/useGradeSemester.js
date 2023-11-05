import {useQuery} from "react-query";
import axios from "../utils/axios";

export const fetchGradesSemester = async (filters) => {
    const params = {
        yearRange: filters.year,
        component: filters.semesterN.value
    }
    const {data} = await axios.get("/client/grade/get-grades-by-semester", {params});
    return data;
}

const useGradeSemester = (filterData) => useQuery(["SEMESTER_GRADE", filterData],
    () => fetchGradesSemester(filterData));

export default useGradeSemester;