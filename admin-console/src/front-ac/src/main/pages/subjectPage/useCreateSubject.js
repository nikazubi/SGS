import useMutationWithInvalidation from "../../../hooks/useMutationWithInvalidation";
import axios from "../../../utils/axios";


export const createSubject = async subject => {
    const {data} = await axios.post("subjects/create-subject", subject);
    return data;
};

const useCreateSubject = () => useMutationWithInvalidation(createSubject, "SUBJECTS");

export default useCreateSubject
