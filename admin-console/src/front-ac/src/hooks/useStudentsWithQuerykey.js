import axios from "../utils/axios";
import {useMutation} from "react-query";

export const fetchStudentsWithQuerykey = async (params) => {
    console.log(params)
    const {data} = await axios.get("students/get-students-by-name-without-validation", {params});
    return data;
}

const useFetchStudentsQuerykey = () => useMutation(fetchStudentsWithQuerykey);

export default useFetchStudentsQuerykey;