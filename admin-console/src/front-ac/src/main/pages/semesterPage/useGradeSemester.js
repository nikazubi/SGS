import {useQuery} from "react-query";
import axios from "../../../utils/axios";

export const fetchGradesSemester = async (filters) => {
    if (!filters.academyClass) {
        return [];
    }
    const params = {
        classId: filters.academyClass.id,
        studentId: filters.student.id,
        yearRange: filters.yearRange,
        component: filters.semesterN.value
    }
    const {data} = await axios.get("/grade/get-grades-by-semester", {params});
    data.sort((a, b) => {
        if (a.student?.lastName < b.student.lastName)
            return -1
        if (a.student?.lastName > b.student.lastName)
            return 1
        else return 0
    })
    data.map((row, index) => {
        let copyRow = row;
        copyRow.index = index + 1;
        return copyRow;
    });
    return data;
}

const useGradeSemester = (filterData) => useQuery(["SEMESTER_GRADE",filterData],
    () => fetchGradesSemester(filterData));

export default useGradeSemester;