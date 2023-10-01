import {useQuery} from "react-query";
import axios from "../../../utils/axios";

export const fetchGradesMonthly = async (filters) => {
    if (!filters.academyClass) {
        return [];
    }
    const params = {
        classId: filters?.academyClass.id,
        studentId: filters?.student.id,
        createDate: Date.parse(filters.date),
        component: "monthly"
    }
    const {data} = await axios.get("/grade/get-grades-by-component", {params});
    data.sort((a, b) => {
        if (a.student?.id < b.student.id)
            return 1
        if (a.student?.id === b.student.id)
            return 0
        else return -1
    })
    return data;
}

const useGradeMonthly = (filterData) => useQuery(["MONTHLY_GRADE",filterData],
    () => fetchGradesMonthly(filterData));

export default useGradeMonthly;