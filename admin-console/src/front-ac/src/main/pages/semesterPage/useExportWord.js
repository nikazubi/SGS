import {useQuery} from "react-query";
import axios from "../../../utils/axios";

export const fetchGradesSemester = async (filters) => {
    if (!filters.academyClass) {
        return [];
    }
    console.log(filters)
    const params = {
        classId: filters.academyClass.id,
        studentId: filters.student.id,
        yearRange: filters.yearRange,
        component: filters.semesterN.value,
        isDecimal: false
    }
    const {data} = await axios.get("/export/semester-word", {params});
    return data;
}

