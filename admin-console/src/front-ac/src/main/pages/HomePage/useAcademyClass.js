import {useMutation} from "react-query";
import axios from "../../../utils/axios";

export const fetchAcademyClass = async (params) => {
    const {data} = await axios.get("academy-class/get-academy-classes", {params});
    return data;
}

const useAcademyClass = () => useMutation(fetchAcademyClass);

export default useAcademyClass;