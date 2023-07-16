import useMutationWithInvalidation from "../../../hooks/useMutationWithInvalidation";
import axios from "../../../utils/axios";


export const updateAcademyClass = async academyClass => {
    const {data} = await axios.put("academy-class/update-academy-class", academyClass);
    return data;
};

const useUpdateAcademyClass = () => useMutationWithInvalidation(updateAcademyClass, "ACADEMY_CLASS");

export default useUpdateAcademyClass
