import {useQuery} from "react-query";
import axios from "../../../utils/axios";

export const fetchClosePeriod = async (filters) => {

    const params = {
        academyClassId: filters?.academyClass?.id,
        dateFrom: Date.parse(filters.dateFrom),
        dateTo: Date.parse(filters.dateTo),
    }
    const {data} = await axios.get("close-period/get-closed-period-ordered", {params});
    return data;
}

const useFetchClosePeriod = (filterData) => useQuery(["CLOSE_PERIOD", filterData],
    () => fetchClosePeriod(filterData));

export default useFetchClosePeriod;