import axios from "../../../utils/axios";
import useMutationWithInvalidation from "../../../hooks/useMutationWithInvalidation";


export const updateTrimesterGrade = async grade => {
    const gradeType = grade.changedField;
    const rawValue = grade[gradeType];
    const value = rawValue === "" || rawValue === null || rawValue === undefined ? null : Number(rawValue);
    const subject = grade.subject || grade.grades?.find(g => g.subject)?.subject;
    const request = {
        value,
        gradeType,
        student: grade.student,
        subject,
        identifier: grade.trimester
    }
    const {data} = await axios.post("/grade/insert-student-grade", request);
    return data;
};

const useUpdateTrimesterGrade = () => useMutationWithInvalidation(updateTrimesterGrade, "TRIMESTER");

export default useUpdateTrimesterGrade;
