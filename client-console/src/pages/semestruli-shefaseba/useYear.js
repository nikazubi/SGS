import {useMutation, useQuery} from "react-query";
import axios from "../utils/axios";

export const fetchYear = async (params) => {
    const {data} = await axios.get("/client/grade/get-grades-years-grouped", {});
    return data;
}

const useFetchYear = () => useQuery("AVALAIBLE_YEARS", fetchYear);

export default useFetchYear;