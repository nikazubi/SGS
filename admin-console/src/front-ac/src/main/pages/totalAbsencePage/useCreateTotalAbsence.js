import useMutationWithInvalidation from "../../../hooks/useMutationWithInvalidation";
import axios from "../../../utils/axios";


export const createTotalAbsence = async totalAbsence => {
    const {data} = await axios.post("absence/create", totalAbsence);
    return data;
};

const useCreateTotalAbsence = () => useMutationWithInvalidation(createTotalAbsence, "TOTAL_ABSENCE");

export default useCreateTotalAbsence
