import useMutationWithInvalidation from "../../../hooks/useMutationWithInvalidation";
import axios from "../../../utils/axios";


export const updateSubject = async subject => {
    let teach = subject.teacher;
    const obj = {
        name: subject.name,
        teacher: teach,
        id: subject.id
    }
    const {data} = await axios.put("subjects/update-subject", obj);
    return data;
};

const useUpdateSubject = () => useMutationWithInvalidation(updateSubject, "SUBJECTS");

export default useUpdateSubject
