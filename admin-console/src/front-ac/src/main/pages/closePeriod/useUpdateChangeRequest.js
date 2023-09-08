import axios from "../../../utils/axios";
import useMutationWithInvalidation from "../../../hooks/useMutationWithInvalidation";

export const changeRequestStatus = async (filters) => {
    // if (filters.academyClass.length === 0 || filters.subject.length ===0) {
    //     return [];
    // }
    console.log(filters)
    const params = {
        changeRequestId: filters?.changeRequestId,
        changeRequestStatus: filters?.changeRequestStatus,
    }
    const {data} = await axios.put("change-request/change-request-status", params);
    return data;
}

const useChangeRequestStatus = () => useMutationWithInvalidation(changeRequestStatus, "CHANGE_REQUEST");

export default useChangeRequestStatus;