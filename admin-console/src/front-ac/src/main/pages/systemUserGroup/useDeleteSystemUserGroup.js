import axios from "../../../utils/axios";
import useMutationWithInvalidation from "../../../hooks/useMutationWithInvalidation";

const deleteAcademyClass = async academyClassId => {
    const {data} = await axios.delete(`system-user-group/delete/${academyClassId}`);
    return data;
}

const useDeleteSystemUserGroup = () => useMutationWithInvalidation(deleteAcademyClass, "SYSTEM_USER_GROUP");

export default useDeleteSystemUserGroup