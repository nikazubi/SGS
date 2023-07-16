import useMutationWithInvalidation from "../../../hooks/useMutationWithInvalidation";
import axios from "../../../utils/axios";


export const createStudent = async subject => {
    const {data} = await axios.post("students/create-student", subject);
    return data;
};

const useCreateStudents = () => useMutationWithInvalidation(createStudent, "STUDENTS");

export default useCreateStudents
