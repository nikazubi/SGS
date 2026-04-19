import {useQuery} from "react-query";
import axios from "../utils/axios";

export const fetchGrades = async (filters) => {
    if (!filters.subject || !filters.year) {
        return [];
    }
    const params = {
        subjectId: filters.subject.id,
        trimester: filters.trimester.key,
        year: filters.year,

    }
    const {data} = await axios.get("/client/grade/get-trimester-for-student", {params});
    return data;
}

const useGrades = (filterData) => useQuery(["GRADES", filterData],
    () => fetchGrades(filterData));

export default useGrades;