//get-students-by-name-without-validation

import axios from "../utils/axios";
import {useMutation} from "react-query";

export const fetchStudentsWithoutValidation = async (params) => {
    const {data} = await axios.get("students/get-students-by-name-without-validation", {params});
    return data;
}

const useFetchStudentsWithoutValidation = () => useMutation(fetchStudentsWithoutValidation);

export default useFetchStudentsWithoutValidation;