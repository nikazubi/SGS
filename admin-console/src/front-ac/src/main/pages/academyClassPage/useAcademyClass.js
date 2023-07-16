import {useQuery} from "react-query";
import axios from "../../../utils/axios";

export const fetchAcademyClass = async config => {
    console.log(config)
    const params = config; //TODO paging
    // const params = {
    //     name: filters?.name,
    //     // activePeriod: Date.parse(filters?.date),
    // }
    const {data} = await axios.get("academy-class/get-academy-classes", {params});
    return data;
}

const useFetchAcademyClass = (filterData) => useQuery(["ACADEMY_CLASS", filterData],
    () => fetchAcademyClass(filterData));

export default useFetchAcademyClass;