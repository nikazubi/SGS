import axios from "../../../utils/axios";
import useMutationWithInvalidation from "../../../hooks/useMutationWithInvalidation";

const deleteTotalAcademicHour = async absenceId => {
    const {data} = await axios.delete(`absence/${absenceId}`);
    return data;
}

const useDeleteTotalAcademicHour = () => useMutationWithInvalidation(deleteTotalAcademicHour, "TOTAL_ABSENCE");

export default useDeleteTotalAcademicHour