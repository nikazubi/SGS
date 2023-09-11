import {useMutation} from "react-query";
import axios from "../../../utils/axios";

export const fetchYear = async (params) => {
    const {data} = await axios.get("grade/get-grades-years-grouped", {});
    return data;
}

const useFetchYear = () => useMutation(fetchYear);

export default useFetchYear;