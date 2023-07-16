import axios from "../../../utils/axios";
import useMutationWithInvalidation from "../../../hooks/useMutationWithInvalidation";

const deleteSubject = async subjectId => {
    const {data} = await axios.delete(`subjects/delete-subject/${subjectId}`);
    return data;
}

const useDeleteSubject = () => useMutationWithInvalidation(deleteSubject, "SUBJECTS");

export default useDeleteSubject