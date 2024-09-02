import {useQuery} from "react-query";
import axios from "../utils/axios";

export const fetchGradesAnual = async (filters) => {

    const params = {
        yearRange: filters && filters.yearRange ? filters.yearRange[0].toString() : null,
    }
    const {data} = await axios.get("/client/grade/get-grades-year", {params});
    const result = data ? data[0] : {gradeList: []};
    const bla = result.gradeList.filter(a => a.subject.id !== 8888 && a.subject.id !== 9999)
    return [{student: data.student, gradeList: bla}];
}

const useGradeAnual = (filterData) => useQuery(["ANNUAL_GRADE", filterData],
    () => fetchGradesAnual(filterData));

export default useGradeAnual;