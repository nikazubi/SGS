import axios from "../../../utils/axios";
import useMutationWithInvalidation from "../../../hooks/useMutationWithInvalidation";


export const updateSystemUser = async systemUser => {
    const groupIdList = systemUser.systemGroup.map(o => o.id);
    const classIdList = systemUser.academyClasses.map(o => o.id);

    const {data} = await axios.put("system-user/update", {
        systemUserDTO: {
            username: systemUser.username,
            password: systemUser.password,
            name: systemUser.name,
            email: systemUser.email,
            active: systemUser.active
        },
        groupIdList: groupIdList,
        classIdList: classIdList
    });
    return data;
};

const useUpdateSystemUser = () => useMutationWithInvalidation(updateSystemUser(), "SYSTEM_USER");

export default useUpdateSystemUser
