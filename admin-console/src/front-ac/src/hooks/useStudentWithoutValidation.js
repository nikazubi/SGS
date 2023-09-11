import axios from "../utils/axios";
import {useQuery} from "react-query";

export const fetchStudentsWithoutValidation = async (params) => {
    const {data} = await axios.get("students/get-students", {params});
    return data;
}

const useFetchStudentsWithoutValidation = (filterData) => useQuery(["STUDENTS", filterData],
    () => fetchStudentsWithoutValidation(filterData));

export default useFetchStudentsWithoutValidation;


