import {useQuery} from "react-query";
import axios from "../utils/axios";
import {ABSENCE_SUBJECT_ID, BEHAVIOUR_SUBJECT_ID} from "../utils/gradeContract";

export const fetchGradesAnual = async (filters) => {

    const params = {
        yearRange: filters && filters.yearRange ? filters.yearRange[0].toString() : null,
    }
    const {data} = await axios.get("/client/grade/get-grades-year", {params});
    const result = data ? data[0] : {gradeList: []};
    const bla = result.gradeList.filter(a => a.subject.id !== ABSENCE_SUBJECT_ID && a.subject.id !== BEHAVIOUR_SUBJECT_ID)
    return [{student: data.student, gradeList: bla}];
}

const useGradeAnual = (filterData) => useQuery(["ANNUAL_GRADE", filterData],
    () => fetchGradesAnual(filterData));

export default useGradeAnual;