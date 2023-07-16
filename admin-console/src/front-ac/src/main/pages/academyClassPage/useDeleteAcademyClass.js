import axios from "../../../utils/axios";
import useMutationWithInvalidation from "../../../hooks/useMutationWithInvalidation";

const deleteAcademyClass = async academyClassId => {
    const {data} = await axios.delete(`academy-class/delete-academy-class/${academyClassId}`);
    return data;
}

const useDeleteAcademyClass = () => useMutationWithInvalidation(deleteAcademyClass, "ACADEMY_CLASS");

export default useDeleteAcademyClass