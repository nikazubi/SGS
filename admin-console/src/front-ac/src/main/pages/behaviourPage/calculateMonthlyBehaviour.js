import axios from "../../../utils/axios";
import useMutationWithInvalidation from "../../../hooks/useMutationWithInvalidation";

export const calculateMonthlyBehaviour = async (data) => {
    const params = {
        academyClassId: data.academyClassId,
        subjectId: data.subjectId,
        date: data.date,
    }

    axios.get("calculate-grade/behaviour-monthly", {params})
        .then(() => {
            data.setNotification({
                message: 'თვის ნიშანი წარმატებით დაითვალა',
                severity: 'success'
            });
        })
        .catch((error) => {
            data.setErrorMessage(error);
        });
}

const useCalculateMonthlyBehaviour = () => useMutationWithInvalidation(calculateMonthlyBehaviour, "BEHAVIOUR");

export default useCalculateMonthlyBehaviour;