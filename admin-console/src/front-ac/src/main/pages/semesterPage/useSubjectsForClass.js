import {useMutation} from "react-query";
import axios from "../../../utils/axios";

export const fetchSubjectsForClass = async ({classId}) => {
    const params = {classId: classId};
    const {data} = await axios.get("subjects/get-subjects-for-class", {params});
    return data;
}

const useSubjectsForClass = () => useMutation(fetchSubjectsForClass);

export default useSubjectsForClass;