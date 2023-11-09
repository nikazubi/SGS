import {useQuery} from "react-query";
import axios from "../utils/axios";

export const fetchGradesAnual = async (filters) => {

    const params = {
        yearRange: filters && filters.yearRange ? filters.yearRange[0].toString() : null,
    }
    const {data} = await axios.get("/client/grade/get-grades-year", {params});
    return data;
}

const useGradeAnual = (filterData) => useQuery(["ANNUAL_GRADE", filterData],
    () => fetchGradesAnual(filterData));

export default useGradeAnual;