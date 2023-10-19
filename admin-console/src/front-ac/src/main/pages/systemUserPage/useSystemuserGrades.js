import {useQuery} from "react-query";
import axios from "../../../utils/axios";

export const fetchSystemuser = async (filters) => {
    const params = {
        username: filters.username,
        name: filters.name,
        active: filters.active?.value,
    }
    const {data} = await axios.get("system-user/filter", {params});
    return data;
}

const useFetchSystemuser = (filterData) => useQuery(["SYSTEM_USER", filterData],
    () => fetchSystemuser(filterData));

export default useFetchSystemuser;