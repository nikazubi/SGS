import {useMutation} from "react-query";
import axios from "../../../utils/axios";

export const fetchSubjects = async ({queryKey}) => {
    const params = {name: queryKey};
    const {data} = await axios.get("subjects/get-subjects", {params});
    return data;
}

const useSubjects = () => useMutation(fetchSubjects);

export default useSubjects;