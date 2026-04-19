import {useQuery} from "react-query";
import axios from "../utils/axios";

export const fetchMonthly = async (filters) => {
    if (!filters.year) {
        return [];
    }
    const params = {
        trimester: filters.trimester,
        year: filters.year
    }
    console.log("AAAA")

    const {data} = await axios.get("/client/grade/get-trimester-for-student-by-subject", {params});
    data.map((row, index) => {
        let copyRow = row;
        copyRow.index = index + 1;
        return copyRow;
    });
    return data;
}

const useGradesForMonth = (filterData) => useQuery(["TRIMESTER_SUM", filterData],
    () => fetchMonthly(filterData));

export default useGradesForMonth;