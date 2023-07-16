import useMutationWithInvalidation from "../../../hooks/useMutationWithInvalidation";
import axios from "../../../utils/axios";


export const createSystemUser = async totalAbsence => {
    console.log("looook at hereeeeeeeeeeeeee", totalAbsence)
    const groupIdList = totalAbsence.systemGroup.map(o => o.id);
    const classIdList = totalAbsence.academyClasses.map(o => o.id);
    const {data} = await axios.post("system-user/add-User", {
        systemUserDTO: {
            username: totalAbsence.username,
            password: totalAbsence.password,
            name: totalAbsence.name,
            email: totalAbsence.email,
            active: true
        },
        groupIdList: groupIdList,
        classIdList: classIdList
    });
    return data;
};

const useCreateSystemUser = () => useMutationWithInvalidation(createSystemUser, "SYSTEM_USER");

export default useCreateSystemUser
