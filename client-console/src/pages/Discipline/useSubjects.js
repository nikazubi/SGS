import {useQuery} from "react-query";
import axios from "../utils/axios";

export const fetchSubjects = async () => {
    console.log(await axios.get("/subjects/get-subjects-for-student"));
    // return data;
}

const useSubjects = () => useQuery(["SUBJECTS"], fetchSubjects);

export default useSubjects;