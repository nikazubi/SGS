import {useQuery} from "react-query";
import axios from "../../../utils/axios";

export const fetchTotalAbsences = async (filters) => {
    const params = {
        academyClassId: filters?.academyClass?.id,
        activePeriod: Date.parse(filters?.date),
    }
    const {data} = await axios.get("absence/filter", {params});
    return data;
}

const useFetchTotalAbsences = (filterData) => useQuery(["TOTAL_ABSENCE", filterData],
    () => fetchTotalAbsences(filterData));

export default useFetchTotalAbsences;