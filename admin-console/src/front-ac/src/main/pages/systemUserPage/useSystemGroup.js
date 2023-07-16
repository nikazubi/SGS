import {useMutation} from "react-query";
import axios from "../../../utils/axios";

export const fetchSystemGroup = async (params) => {
    const {data} = await axios.get("system-user-group/get-all", {});
    return data;
}

const useSystemGroup = () => useMutation(fetchSystemGroup);

export default useSystemGroup;