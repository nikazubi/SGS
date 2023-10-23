import {useQuery} from "react-query";
import axios from "../utils/axios";

export const fetchSubjects = async () => {
    const {data} = await axios.get("/client/subjects/get-subjects-for-student");
    return data;
}

const useSubjects = () => useQuery(["SUBJECTS"], fetchSubjects);

export default useSubjects;