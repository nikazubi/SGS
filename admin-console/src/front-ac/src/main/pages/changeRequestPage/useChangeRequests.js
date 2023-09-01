import {useQuery} from "react-query";
import axios from "../../../utils/axios";

export const fetchChangeRequest = async (filters) => {
    // if (filters.academyClass.length === 0 || filters.subject.length ===0) {
    //     return [];
    // }
    const params = {
        classId: filters?.academyClass?.id,
        studentId: filters?.student?.id,
        date: Date.parse(filters.date),
    }
    const {data} = await axios.get("change-request/get-change-requests", {params} );
    return data;
}

const useFetchChangeRequest = (filterData) => useQuery([ "CHANGE_REQUEST", filterData],
    () => fetchChangeRequest(filterData));

export default useFetchChangeRequest;