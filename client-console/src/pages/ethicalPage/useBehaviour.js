import {useQuery} from "react-query";
import axios from "../utils/axios";

export const fetchGradesGrouped = async (filters) => {
    const params = {
        month: filters.month,
    }
    const {data} = await axios.get("/client/grade/get-grades-behaviour", {params});
    return data;
}

const useFetchBehaviour = (filterData) => useQuery(["BEHAVIOUR", filterData],
    () => fetchGradesGrouped(filterData));

export default useFetchBehaviour;