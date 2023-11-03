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
        if (a.student?.lastName < b.student.lastName)
            return 1
        if (a.student?.lastName === b.student.lastName)
            return 0
        else return -1
    })
    data.map((row, index) => {
        let copyRow = row;
        copyRow.index = index + 1;
        return copyRow;
    });
    return data.filter(item => item.student.firstName !== 'საშუალო' && item.student.firstName !== 'მასწავლებელი');
}

const useGradeMonthly = (filterData) => useQuery(["MONTHLY_GRADE",filterData],
    () => fetchGradesMonthly(filterData));

export default useGradeMonthly;