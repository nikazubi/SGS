import {useMutation} from "react-query";
import axios from "../../../utils/axios";

/**
 * The classes a user can be limited to: this year's class groups.
 *
 * Not the legacy academy-class list, which reads dbo.academy_class and so never
 * contains a class created through the roster screen.
 */
export const fetchClassOptions = async () => {
    const {data} = await axios.get("system-user/class-options");
    return data;
};

/** A past year's class is named with its year, so a stale grant is visible. */
export const classOptionLabel = (option) =>
    `${option.name} - ${option.schoolName}${option.currentYear ? "" : ` (${option.yearCode})`}`;

const useClassOptions = () => useMutation(fetchClassOptions);

export default useClassOptions;
