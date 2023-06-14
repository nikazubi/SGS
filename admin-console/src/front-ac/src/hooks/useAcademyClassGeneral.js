import {useMutation} from "react-query";
import axios from "../utils/axios";

export const fetchAcademyClass = async (params) => {
    const {data} = await axios.get("academy-class/get-academy-classes", {
        queryKey: params,
        gradeTypePrefix: "GENERAL_"
    });
    return data;
}

const useAcademyClassGeneral = () => useMutation(fetchAcademyClass);

export default useAcademyClassGeneral;