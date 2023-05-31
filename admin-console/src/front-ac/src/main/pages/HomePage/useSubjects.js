import {useMutation} from "react-query";
import axios from "../../../utils/axios";

export const fetchSubjects = async params => {
    const {data} = await axios.get("subjects/get-subjects", {params});
    return data;
}

const useCardTypes = () => useMutation(fetchSubjects);

export default useCardTypes;