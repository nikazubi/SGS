import {useMutation} from "react-query";
import axios from "../../../utils/axios";

export const fetchStudents = async (params) => {
    const {data} = await axios.get("students/get-students-by-name", {params});
    return data;
}

const useFetchStudents = () => useMutation(fetchStudents);

export default useFetchStudents;