import {useQuery} from "react-query";
import axios from "../../../utils/axios";

export const fetchSystemUserGroup = async config => {
    const params = {
        name: config.name,
        permission: config.permission?.value
    }
    const {data} = await axios.get("system-user-group/filter", {params});
    return data;
}

const useFetchSystemUserGroup = (filterData) => useQuery(["SYSTEM_USER_GROUP", filterData],
    () => fetchSystemUserGroup(filterData));

export default useFetchSystemUserGroup;