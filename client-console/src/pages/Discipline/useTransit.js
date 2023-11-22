import {useQuery} from "react-query";
import axios from "../utils/axios";

export const fetchTransit = async () => {
    const {data} = await axios.get("/client/grade/get-transient");
    return data;
}

const useTransit = (filterData) => useQuery(["TRANSIT", filterData],
    () => fetchTransit(filterData));

export default useTransit;