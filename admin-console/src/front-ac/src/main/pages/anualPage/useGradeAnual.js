import {useQuery} from "react-query";
import axios from "../../../utils/axios";

export const fetchGradesAnual = async (filters) => {
    if (!filters.academyClass) {
        return [];
    }
    const params = {
        classId: filters.academyClass.id,
        studentId: filters.student.id,
        yearRange: filters.yearRange,
        component: "anual"
    }
    const {data} = await axios.get("/grade/get-grades-by-component", {params});
    data.map((row, index) => {
        let copyRow = row;
        copyRow.index = index + 1;
        return copyRow;
    });
    return data;
}

const useGradeAnual = (filterData) => useQuery(["ANNUAL_GRADE",filterData],
    () => fetchGradesAnual(filterData));

export default useGradeAnual;