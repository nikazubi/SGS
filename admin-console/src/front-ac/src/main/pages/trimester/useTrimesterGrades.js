import {useQuery} from "react-query";
import axios from "../../../utils/axios";

export const fetchTrimesterGrades = async (filters) => {
    if (!filters.academyClass || !filters.subject ||
        filters.academyClass.length === 0 || filters.subject.length === 0) {
        return [];
    }
    const params = {
        classId: filters.academyClass.id,
        subjectId: filters.subject.id,
        studentId: filters.student?.id,
        trimester: filters.trimesterN.value
    }
    const {data} = await axios.get("grade/get-grades-by-trimester", {params});
    data.map((row, index) => {
        let copyRow = row;
        copyRow.index = index + 1;
        return copyRow;
    });
    return data;
}

const useFetchTrimesterGrade = (filterData) => useQuery(["TRIMESTER", filterData],
    () => fetchTrimesterGrades(filterData));

export default useFetchTrimesterGrade;