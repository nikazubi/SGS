import useMutationWithInvalidation from "../../../hooks/useMutationWithInvalidation";
import axios from "../../../utils/axios";


export const updateAcademyClass = async academyClass => {
    const params = {
        id: academyClass.id,
        name: academyClass.name,
        active: academyClass.active,
        permissions: academyClass.permission.map(v => v.value).join(",")
    }
    const {data} = await axios.post("system-user-group/edit", params);
    return data;
};

const useUpdateSystemUserGroup = () => useMutationWithInvalidation(updateAcademyClass, "SYSTEM_USER_GROUP");

export default useUpdateSystemUserGroup
