import axios from "../../../utils/axios";
import useMutationWithInvalidation from "../../../hooks/useMutationWithInvalidation";

export const calculateMonthlyGrade = async (data) => {
    const params = {
        academyClassId: data.academyClassId,
        subjectId: data.subjectId,
        date: data.date,
    }

    axios.get("calculate-grade/grades-monthly", {params})
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

const useCalculateGeneralGrade = () => useMutationWithInvalidation(calculateMonthlyGrade, "GRADES");

export default useCalculateGeneralGrade;