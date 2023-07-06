import axios from "../../../utils/axios";
import {useQuery} from "react-query";

export const closePeriod = async () => {
    const {data} = await axios.get("close-period/create-closed-period");
    return data;
}
