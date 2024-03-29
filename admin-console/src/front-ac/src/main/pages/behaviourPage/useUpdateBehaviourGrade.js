import axios from "../../../utils/axios";
import useMutationWithUpdate from "../../../hooks/useMutationWithUpdate";
import useMutationWithInvalidation from "../../../hooks/useMutationWithInvalidation";


export const updateGrade = async grade => {
    const gradeType = Object.keys(grade).filter(field => field.startsWith("BEHAVIOUR") || field.startsWith("GENERAL"))[0];
    const request = {
        // id: grade.grades.filter(g => g.gradeType === gradeType)[0].id,
        value: grade[gradeType],
        gradeType: gradeType,
        student: grade.student,
        subject: grade.grades[0].subject,
        exactMonth: grade.exactMonth// TODO should be grade.subject because on no grades this will fail
    }
    const {data} = await axios.post("/grade/insert-student-grade", request);
    return data;
};

const useUpdateBehaviourGrade = () => useMutationWithInvalidation(updateGrade, "BEHAVIOUR");

export default useUpdateBehaviourGrade
