import axios from "../../../utils/axios";
import useMutationWithInvalidation from "../../../hooks/useMutationWithInvalidation";

export const calculateMonthlyBehaviour = async (data) => {
    const params = {
        academyClassId: data.academyClassId,
        subjectId: data.subjectId,
        date: data.date,
    }

    await axios.get("calculate-grade/behaviour-monthly", {params});
}

const useCalculateMonthlyBehaviour = () => useMutationWithInvalidation(calculateMonthlyBehaviour, "BEHAVIOUR");

export default useCalculateMonthlyBehaviour;