import axios from "../../../utils/axios";
import useMutationWithInvalidation from "../../../hooks/useMutationWithInvalidation";


export const updateSemesterDiagnosticGrade = async grade => {
    let gradeType = Object.keys(grade).filter(field => field.endsWith("--6") ||
                                                       field.endsWith("--5") ||
                                                       field.endsWith("--4") ||
                                                       field.endsWith("--3") ||
                                                       field.endsWith("--2"))[0];
    if (gradeType.endsWith('--3')) {
        gradeType = 'DIAGNOSTICS_1'
    } else if (gradeType.endsWith('--4')) {
        gradeType = 'DIAGNOSTICS_2'
    } else if (gradeType.endsWith('--2')) {
        gradeType = 'SHEMOKMEDEBITOBA'
    } else if (gradeType.endsWith('--5')) {
        gradeType = 'DIAGNOSTICS_3'
    } else if (gradeType.endsWith('--6')) {
        gradeType = 'DIAGNOSTICS_4'
    }
    let semester = grade.semester;
    const request = {
        // id: grade.grades.filter(g => g.gradeType === gradeType)[0].id,
        value: grade.value,
        gradeType: gradeType,
        student: grade.student,
        subject: grade.subject,
        exactMonth: grade.exactMonth,
        semester: semester
    }
    const {data} = await axios.post("/grade/insert-student-grade", request);
    return data;
};

const useUpdateSemesterDiagnosticGrade = () => useMutationWithInvalidation(updateSemesterDiagnosticGrade, "SEMESTER_GRADE");

export default useUpdateSemesterDiagnosticGrade
