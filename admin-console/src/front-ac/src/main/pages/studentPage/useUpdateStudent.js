import useMutationWithInvalidation from "../../../hooks/useMutationWithInvalidation";
import axios from "../../../utils/axios";


export const updateStudent = async student => {
    const {data} = await axios.put("students/update-student", student);
    return data;
};

const useUpdateStudent = () => useMutationWithInvalidation(updateStudent, "STUDENTS");

export default useUpdateStudent
