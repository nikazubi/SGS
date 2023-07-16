import {useQuery} from "react-query";
import axios from "../../../utils/axios";

export const fetchSubjects = async config => {
    console.log(config)
    const params = config; //TODO paging
    // const params = {
    //     name: filters?.name,
    //     // activePeriod: Date.parse(filters?.date),
    // }
    const {data} = await axios.get("subjects/get-subjects-without-academy-class-filter", {params});
    return data;
}

const useFetchSubject = (filterData) => useQuery(["SUBJECTS", filterData],
    () => fetchSubjects(filterData));

export default useFetchSubject;