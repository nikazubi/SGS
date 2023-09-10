import useMutationWithInvalidation from "../../../hooks/useMutationWithInvalidation";
import axios from "../../../utils/axios";


export const createAcademyClass = async academyClass => {
    const params = {
        name: academyClass.name,
        active: academyClass.active,
        permissions: academyClass.permission.map(v => v.value).join(",")
    }
    const {data} = await axios.post("system-user-group/create", params);
    return data;
};

const useCreateSystemUserGroup = () => useMutationWithInvalidation(createAcademyClass, "SYSTEM_USER_GROUP");

export default useCreateSystemUserGroup
