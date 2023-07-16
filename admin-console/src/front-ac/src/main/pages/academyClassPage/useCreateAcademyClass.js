import useMutationWithInvalidation from "../../../hooks/useMutationWithInvalidation";
import axios from "../../../utils/axios";


export const createAcademyClass = async academyClass => {
    const {data} = await axios.post("academy-class/create-academy-class", academyClass);
    return data;
};

const useCreateAcademyClass = () => useMutationWithInvalidation(createAcademyClass, "ACADEMY_CLASS");

export default useCreateAcademyClass
