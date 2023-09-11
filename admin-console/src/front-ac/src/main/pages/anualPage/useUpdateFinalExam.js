import axios from "../../../utils/axios";
import useMutationWithUpdate from "../../../hooks/useMutationWithUpdate";
import useMutationWithInvalidation from "../../../hooks/useMutationWithInvalidation";


export const updateGrade = async grade => {
    const gradeType = "FINAL_EXAM";
    const key = Object.keys(grade).filter(key => key.toString().includes("-3"))
    const subjectName = key.toString().split('-')[0]
    const subjectList = grade.gradeList.filter(gradeL => gradeL.subject.name === subjectName)
    const request = {
        // id: grade.grades.filter(g => g.gradeType === gradeType)[0].id,
        value: grade[key],
        gradeType: gradeType,
        student: grade.student,
        subject: subjectList? subjectList[0]?.subject : null,
        exactMonth: new Date()
    }
    const {data} = await axios.post("/grade/insert-student-grade", request);
    return data;
};

const useUpdateFinalExamGrade = () => useMutationWithInvalidation(updateGrade, "ANNUAL_GRADE");

export default useUpdateFinalExamGrade
