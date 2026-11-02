import {useQuery} from "react-query";
import {fetchParentSchool} from "../pages/journal/parentApi";

/**
 * Whether the logged-in child is in the primary school.
 *
 * That decides the console's look - the header, the news colours - and which
 * landing grid is drawn. Asked of the server rather than read off the module
 * list: SCHEDULE used to stand in for it, which held only while the schedule
 * was primary's alone. What each class is shown is configuration now
 * (db/038), so the modules a child has say nothing reliable about their school.
 */
const usePrimarySchool = () => {
    const {data, isLoading} = useQuery(
        ["PARENT_SCHOOL"], fetchParentSchool, {refetchOnWindowFocus: false});
    return {primary: data?.code === "PRIMARY", isLoading};
};

export default usePrimarySchool;
