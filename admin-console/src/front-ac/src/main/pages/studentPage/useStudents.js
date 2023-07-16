import {useQuery} from "react-query";
import axios from "../../../utils/axios";

export const fetchStudents = async config => {
    console.log(config)
    const params = config; //TODO paging
    // const params = {
    //     name: filters?.name,
    //     // activePeriod: Date.parse(filters?.date),
    // }
    const {data} = await axios.get("students/get-students", {params});
    return data;
}

const useFetchSubject = (filterData) => useQuery(["STUDENTS", filterData],
    () => fetchStudents(filterData));

export default useFetchSubject;