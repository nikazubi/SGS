import axios from "../../../utils/axios";
import useMutationWithInvalidation from "../../../hooks/useMutationWithInvalidation";

const deleteSystemUser = async absenceId => {
    const {data} = await axios.delete(`system-user/delete/${absenceId}`);
    return data;
}

const useDeleteSystemUser = () => useMutationWithInvalidation(deleteSystemUser, "SYSTEM_USER");

export default useDeleteSystemUser