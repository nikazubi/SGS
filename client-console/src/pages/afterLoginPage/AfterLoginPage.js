import Box from "./Box";
import {useQuery} from "react-query";
import {fetchParentJournals, fetchParentModules} from "../journal/parentApi";

/**
 * The landing page.
 *
 * Two kinds of box, and they are different on purpose.
 *
 * The **modules** are a list of names from the server. Which ones a school shows
 * is a rule about the school, and the school is in the data - so the console
 * maps a name to a route and does not decide. Primary gets the meals, the daily
 * schedule and the child's description; basic and secondary do not.
 *
 * The **journals** are data. One box per journal the school has released to
 * parents; creating one and ticking "visible to parents" is all it takes to
 * appear, and nothing has to be deployed. The list is now scoped to the child's
 * school, so a primary parent gets none of them - their school does not grade
 * on the trimester journal, and until this was scoped it was offered to them
 * anyway.
 *
 * The five boxes this replaces were hardcoded, and the first of them linked to
 * /grades/<subject NAME>, so the page it opened had to refetch every subject
 * and match on a string. Journals are addressed by uuid, which does not change
 * when the school renames one.
 */
const AfterLoginPage = () => {

    const {data: journals, isLoading} = useQuery(
        ["PARENT_JOURNALS"], fetchParentJournals, {refetchOnWindowFocus: false});

    const {data: modules, isLoading: modulesLoading} = useQuery(
        ["PARENT_MODULES"], fetchParentModules, {refetchOnWindowFocus: false});

    if (isLoading || modulesLoading) {
        return <></>;
    }

    // Name to box. A module the server names but the console does not know is
    // skipped rather than rendered blank - which is what makes it safe to add
    // one server-side before the page exists.
    const BOXES = {
        HOMEWORK: {text: "საშინაო დავალებები", link: "/homework"},
        NEWS: {text: "სიახლეები", link: "/news"},
        SCHEDULE: {text: "დღის რეჟიმი", link: "/schedule"},
        MENU: {text: "კვება", link: "/menu"},
        CHARACTERIZATION: {text: "მოსწავლის დახასიათება", link: "/description"}
    };

    return (
        <>
            <div className="boxCnt">
                <div className="boxWrap">
                    {(modules || []).map(name => BOXES[name] ? (
                        <div className="boxWrap__div" key={name}>
                            <Box text={BOXES[name].text} link={BOXES[name].link}/>
                        </div>
                    ) : null)}

                    {(journals || []).map(journal => (
                        <div className="boxWrap__div" key={journal.uuid}>
                            <Box text={journal.name} link={`/journal/${journal.uuid}`}/>
                        </div>
                    ))}
                </div>
            </div>
            <div className="body__wallpaper"></div>
        </>
    );
};

export default AfterLoginPage;
