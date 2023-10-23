import {useQuery} from "react-query";
import axios from "../utils/axios";

export const fetchMonthly = async (filters) => {
    if (!filters.month) {
        return [];
    }
    const params = {
        month: filters.month,
        year: filters.year
    }

    const {data} = await axios.get("/client/grade/get-grades-for-month", {params});
    return data;
}

const useGradesForMonth = (filterData) => useQuery(["GRADES_MONTHLY", filterData],
    () => fetchMonthly(filterData));

export default useGradesForMonth;