import axios from "../../../utils/axios";
import useMutationWithInvalidation from "../../../hooks/useMutationWithInvalidation";

export const closePeriod = async (academyClassDTOS) => {

    const {data} = await axios.post("close-period/create-closed-period", {academyClassDTOS});
    return data;
}

const useCreateClosePeriod = () => useMutationWithInvalidation(closePeriod, "CLOSE_PERIOD");

export default useCreateClosePeriod;
