import axios from "../../../utils/axios";
import useMutationWithInvalidation from "../../../hooks/useMutationWithInvalidation";

export const calculateMonthlyGrade = async (data) => {
    const params = {
        academyClassId: data.academyClassId,
        subjectId: data.subjectId,
        date: data.date,
    }

    await axios.get("calculate-grade/grades-monthly", {params});
}

const useCalculateGeneralGrade = () => useMutationWithInvalidation(calculateMonthlyGrade, "GRADES");

export default useCalculateGeneralGrade;