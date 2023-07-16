import useMutationWithInvalidation from "../../../hooks/useMutationWithInvalidation";
import axios from "../../../utils/axios";


export const updateSubject = async subject => {
    const {data} = await axios.put("subjects/update-subject", subject);
    return data;
};

const useUpdateSubject = () => useMutationWithInvalidation(updateSubject, "SUBJECTS");

export default useUpdateSubject
