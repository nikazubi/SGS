import {useMutation} from "react-query";
import axios from "../../../utils/axios";

export const fetchSubjects = async ({queryKey}) => {
    const params = {name: queryKey};
    const {data} = await axios.get("subjects/get-subjects-without-academy-class-filter", {params});
    return data;
}

const useSubjectsForAcademyClass = () => useMutation(fetchSubjects);

export default useSubjectsForAcademyClass;