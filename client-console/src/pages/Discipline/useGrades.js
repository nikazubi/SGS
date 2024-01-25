import {useQuery} from "react-query";
import axios from "../utils/axios";

export const fetchGrades = async (filters) => {
    if (! filters.subject || !filters.year) {
        return [];
    }
    const params = {
        subjectId: filters.subject.id,
        month: filters.month,
        year: filters.year,

    }
    const {data} = await axios.get("/client/grade/get-grades-for-student", {params});
    return data;
}

const useGrades = (filterData) => useQuery(["GRADES", filterData],
    () => fetchGrades(filterData));

export default useGrades;