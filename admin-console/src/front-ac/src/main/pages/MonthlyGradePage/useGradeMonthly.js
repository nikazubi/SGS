import {useQuery} from "react-query";
import axios from "../../../utils/axios";
import {subjectPattern} from "./Helper";

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
    const newData = data.filter(item => item.student.firstName !== 'საშუალო' && item.student.firstName !== 'მასწავლებელი')
    newData.sort((a, b) => {
        if (a.student?.lastName < b.student.lastName)
            return -1
        if (a.student?.lastName > b.student.lastName)
            return 1
        else return 0
    })
    newData.forEach(o => {
        o.gradeList.sort((a, b) => {
            const indexA = subjectPattern.indexOf(a.subject.name);
            const indexB = subjectPattern.indexOf(b.subject.name);
            if (indexA === -1 && indexB === -1) {
                return 0;
            }
            if (indexA === -1) {
                return 1;
            }
            if (indexB === -1) {
                return -1;
            }
            return indexA - indexB;
        });
    })

    newData.map((row, index) => {
        let copyRow = row;
        copyRow.index = index + 1;
        return copyRow;
    });
    return newData;
}

const useGradeMonthly = (filterData) => useQuery(["MONTHLY_GRADE",filterData],
    () => fetchGradesMonthly(filterData));

export default useGradeMonthly;