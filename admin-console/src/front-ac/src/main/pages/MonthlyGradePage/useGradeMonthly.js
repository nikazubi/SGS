import {useQuery} from "react-query";
import axios from "../../../utils/axios";

export const fetchGradesMonthly = async (filters) => {
    console.log("filteeers:", filters)
    if (!filters.academyClass) {
        console.log("aaaaaaaaaaaaaaaaaaaa");
        return [];
    }
    const params = {
        classId: filters.academyClass.id,
        studentId: filters.student.id,
        createDate: Date.parse(filters.date),
        component: "monthly"
    }
    const {data} = await axios.get("/grade/get-grades-by-component", {params});
    return data;
}

const useGradeMonthly = (filterData) => useQuery(["MONTHLY_GRADE",filterData],
    () => fetchGradesMonthly(filterData));

export default useGradeMonthly;