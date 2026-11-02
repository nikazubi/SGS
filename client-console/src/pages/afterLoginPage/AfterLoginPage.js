import PrimaryLandingGrid from "./PrimaryLandingGrid";
import StandardLandingGrid from "./StandardLandingGrid";
import {useQuery} from "react-query";
import {fetchParentJournals, fetchParentModules, fetchParentViews} from "../journal/parentApi";
import usePrimarySchool from "../../hooks/usePrimarySchool";

/**
 * The landing page.
 *
 * Two grids, one decision: which school the child is in. Which modules a class
 * shows is decided on the server, from rows (sgs.parent_module_grant), so the
 * console maps a name to a route and does not decide.
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

    // Not "PARENT_MENU" - that is the meals page's key. Sharing it served this
    // list to the meals page from cache, which then crashed on `doc.days`.
    const {data: views, isLoading: viewsLoading} = useQuery(
        ["PARENT_VIEWS"], fetchParentViews, {refetchOnWindowFocus: false});

    const {primary, isLoading: schoolLoading} = usePrimarySchool();

    if (isLoading || modulesLoading || viewsLoading || schoolLoading) {
        return <></>;
    }

    // Primary has its own visual, sent by the school. Keyed on the school
    // itself: this used to read "SCHEDULE is in the list" as "primary", which
    // stopped being true once the module list became configuration.
    if (primary) {
        return <PrimaryLandingGrid modules={modules} journals={journals} views={views}/>;
    }

    return <StandardLandingGrid modules={modules} views={views}/>;
};

export default AfterLoginPage;
