import {useQuery} from "react-query";
import axios from "../../../utils/axios";

export const fetchGradesGrouped = async (filters) => {
    if (!filters.academyClass || ! filters.subject ||
        filters.academyClass.length === 0 || filters.subject.length ===0) {
        return [];
    }
    const params = {
        classId: filters.academyClass.id,
        subjectId: filters.subject.id,
        studentId: filters.student?.id,
        date: Date.parse(filters.date),
        groupByClause: filters.groupByClause,
        gradeTypePrefix: filters.academyClass.isTransit? "TRANSIT" : "GENERAL"
    }
    const {data} = await axios.get("grade/get-grades-grouped", {params});
    data.map((row, index) => {
        let copyRow = row;
        copyRow.index = index + 1;
        return copyRow;
    });
    return data;
}

const useFetchGrade = (filterData) => useQuery(["GRADES",filterData],
    () => fetchGradesGrouped(filterData));

export default useFetchGrade;