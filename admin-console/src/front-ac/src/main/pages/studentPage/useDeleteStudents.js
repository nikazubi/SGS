import axios from "../../../utils/axios";
import useMutationWithInvalidation from "../../../hooks/useMutationWithInvalidation";

const deleteStudents = async subjectId => {
    const {data} = await axios.delete(`students/delete-students/${subjectId}`);
    return data;
}

const useDeleteStudents = () => useMutationWithInvalidation(deleteStudents, "STUDENTS");

export default useDeleteStudents