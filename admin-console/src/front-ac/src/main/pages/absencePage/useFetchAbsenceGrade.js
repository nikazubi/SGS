import {useQuery} from "react-query";
import axios from "../../../utils/axios";

export const fetchAbsenceGrades = async (filters) => {
    if (!filters.academyClass || filters.academyClass.length === 0) {
        return [];
    }
    const params = {
        classId: filters.academyClass.id,
        studentId: filters.student?.id,
        yearRange: filters.yearRange
    }
    const {data} = await axios.get("absence/find-absence-grade", {params});
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

const useFetchAbsenceGrade = (filterData) => useQuery(["ABSENCE_GRADES", filterData],
    () => fetchAbsenceGrades(filterData));

export default useFetchAbsenceGrade;