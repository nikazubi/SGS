import axios from "../../../utils/axios";
import {useQuery} from "react-query";

export const closePeriod = async (academyClassDTOS) => {

    const {data} = await axios.post("close-period/create-closed-period", {academyClassDTOS});
    return data;
}
