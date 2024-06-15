import axios from "../../../utils/axios";
import useMutationWithInvalidation from "../../../hooks/useMutationWithInvalidation";


export const updateGrade = async grade => {
    let date = new Date();
    let year = 2024;
    let month = 5; // Note: January is 0, February is 1, ..., May is 4, etc.
    if (grade.gradeType === "SEPTEMBER_OCTOBER" ||
        grade.gradeType === "NOVEMBER" || grade.gradeType === "DECEMBER") {
        year = grade.year.toString().split("-")[0]
        month = grade.gradeType === "SEPTEMBER_OCTOBER" ? 9 : grade.gradeType === "NOVEMBER" ? 11 : 12;
    } else {
        year = grade.year.toString().split("-")[1]
        month = grade.gradeType === "JANUARY_FEBRUARY" ? 1 : grade.gradeType === "MARCH" ? 3 : grade.gradeType === "APRIL" ? 4 : 5;

    }
    date.setFullYear(Number(year));
    date.setMonth(month - 1);
    const params = {
        student: grade.student,
        value: grade.value,
        academyClass: grade.academyClass,
        gradeType: grade.gradeType,
        exactMonth: date
    }
    const {data} = await axios.post("/absence/add-absence-grade", params);
    return data;
};

const useUpdateAbsenceGrade = () => useMutationWithInvalidation(updateGrade, "ABSENCE_GRADES");

export default useUpdateAbsenceGrade
