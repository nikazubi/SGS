import axios from "../../../utils/axios";
import useMutationWithInvalidation from "../../../hooks/useMutationWithInvalidation";


export const updateTrimesterGrade = async grade => {
    const gradeType = Object.keys(grade).filter(field => field.startsWith("TRIMESTER"))[0];
    const request = {
        // id: grade.grades.filter(g => g.gradeType === gradeType)[0].id,
        value: Number(grade[gradeType]),
        gradeType: gradeType,
        student: grade.student,
        subject: grade.grades[0].subject, // TODO should be grade.subject because on no grades this will fail
        identifier: grade.trimester
    }
    const {data} = await axios.post("/grade/insert-student-grade", request);
    return data;
};

const useUpdateTrimesterGrade = () => useMutationWithInvalidation(updateTrimesterGrade, "TRIMESTER");

export default useUpdateTrimesterGrade;
