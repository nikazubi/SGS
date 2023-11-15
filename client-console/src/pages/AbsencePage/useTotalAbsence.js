import {useQuery} from "react-query";
import axios from "../utils/axios";

export const fetchTotalAbsence = async (filters) => {
    const params = {
        month: filters.month,
        year: filters.year,
    }
    const {data} = await axios.get("/client/absence/filter", {params});
    return data;
}

const useTotalAbsence = (filterData) => useQuery(["ABSENCE", filterData],
    () => fetchTotalAbsence(filterData));

export default useTotalAbsence;