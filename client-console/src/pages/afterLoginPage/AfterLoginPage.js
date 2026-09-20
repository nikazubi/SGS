import PrimaryLandingGrid from "./PrimaryLandingGrid";
import StandardLandingGrid from "./StandardLandingGrid";
import {useQuery} from "react-query";
import {fetchParentJournals, fetchParentMenu, fetchParentModules} from "../journal/parentApi";

/**
 * The landing page.
 *
 * Two grids, one decision. Which modules a school shows is a rule about the
 * school and the school is in the data, so the console maps a name to a route
 * and does not decide; primary gets the meals, the daily schedule and the
 * child's description, and basic and secondary do not.
 *
 * The buttons below them are `parent_view` rows rather than journals, because
 * three of the school's five parent screens are one journal at different
 * settings - per subject, summed across subjects, and the year. A journal
 * nobody has configured stands in for itself, so an unseeded database still
 * gets a button per journal.
 *
 * The five boxes all of this replaces were hardcoded, and the first linked to
 * /grades/<subject NAME> - so the page it opened had to refetch every subject
 * and match on a string, and a rename broke it. Nothing here knows the name of
 * a journal, a period or a subject.
 */
const AfterLoginPage = () => {

    const {data: journals, isLoading} = useQuery(
        ["PARENT_JOURNALS"], fetchParentJournals, {refetchOnWindowFocus: false});

    const {data: modules, isLoading: modulesLoading} = useQuery(
        ["PARENT_MODULES"], fetchParentModules, {refetchOnWindowFocus: false});

    const {data: views, isLoading: viewsLoading} = useQuery(
        ["PARENT_MENU"], fetchParentMenu, {refetchOnWindowFocus: false});

    if (isLoading || modulesLoading || viewsLoading) {
        return <></>;
    }

    // Primary has its own visual, sent by the school. There is no explicit
    // "school" field on this response - SCHEDULE only ever appears for a
    // primary child (ParentContentService.modulesFor), so its presence is the
    // same signal the backend already keys the rule on, not a guess of our own.
    if ((modules || []).includes("SCHEDULE")) {
        return <PrimaryLandingGrid modules={modules} journals={journals}/>;
    }

    return <StandardLandingGrid modules={modules} views={views}/>;
};

export default AfterLoginPage;
