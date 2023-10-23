import {useQuery} from "react-query";
import axios from "../utils/axios";

export const fetchMonthly = async (filters) => {
    if (!filters.subject) {
        return [];
    }
    const params = {
        subjectId: filters.subject.id,
        year: filters.year,
    }

    const {data} = await axios.get("/client/grade/get-grades-for-subject-monthly", {params});
    return data;
}

const useMonthlyGradesOfSubjectAndStudent = (filterData) => useQuery(["SUBJECT_GRADES_MONTHLY", filterData],
    () => fetchMonthly(filterData));

export default useMonthlyGradesOfSubjectAndStudent;