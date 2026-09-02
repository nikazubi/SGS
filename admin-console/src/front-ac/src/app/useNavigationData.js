import React, {useMemo} from 'react';
import {useQuery} from 'react-query';
import {useUserContext} from "../contexts/user-context";
import {Grading, SwitchAccount} from "@mui/icons-material";
import {Description, Feed, Restaurant, Schedule} from "@mui/icons-material";
import {
    Assignment,
    ChangeHistory,
    Class,
    EventAvailable,
    EventBusy,
    MenuBook,
    Person
} from "@material-ui/icons";
import SubjectsPage from "../main/pages/roster/SubjectsPage";
import StudentsPage from "../main/pages/roster/StudentsPage";
import ClassesPage from "../main/pages/roster/ClassesPage";
import SystemUserDashBoard from "../main/pages/systemUserPage/SystemUserDashBoard";
import ClosePeriodDashBoard from "../main/pages/closePeriod/ClosePeriodDashBoard";
import {TimeIcon} from "@material-ui/pickers/_shared/icons/TimeIcon";
import SystemUserGroupDashBoard from "../main/pages/systemUserGroup/SystemUserGroupDashBoard";
import GradebookDashBoard from "../main/pages/gradebook/GradebookDashBoard";
import JournalsDashBoard from "../main/pages/journals/JournalsDashBoard";
import HomeworkDashBoard from '../main/pages/homework/HomeworkDashBoard';
import AbsenceRegisterPage from '../main/pages/absence/AbsenceRegisterPage';
import DailyRegisterPage from '../main/pages/absence/DailyRegisterPage';
import StandingDocPage from '../main/pages/content/StandingDocPage';
import CharacterizationPage from '../main/pages/content/CharacterizationPage';
import NewsPage from '../main/pages/content/NewsPage';
import {fetchJournals} from "../main/pages/journals/journalApi";
import ChangeRequestQueue from "../main/pages/changeRequests/ChangeRequestQueue";
import PublicationLog from "../main/pages/changeRequests/PublicationLog";


/**
 * One menu entry per journal, keyed by uuid.
 *
 * Not by name: the name is the label and is meant to be changed, so a tab keyed
 * by it would break the moment someone renamed a journal.
 */
const journalPages = (journals) => {
    const pages = {};
    (journals || [])
        // A draft is not a tab. The wizard creates the journal before its
        // columns exist, and its grid cannot be drawn until a version is
        // activated - the index page is where that happens.
        .filter(journal => journal.currentVersionStatus === "ACTIVE"
            || journal.currentVersionStatus === "LOCKED")
        .forEach(journal => {
            const id = `JOURNAL_${journal.uuid}`;
            pages[id] = {
                id,
                name: journal.name,
                // A PERIODS journal is drawn transposed - students down, periods
                // across - which is a property of the journal rather than something
                // this menu should know the names of. Only the monthly hours
                // register is one; daily absence is not a journal at all any more
                // and has its own entry below.
                component: journal.gridMode === "PERIODS"
                    ? <AbsenceRegisterPage journalUuid={journal.uuid}
                                           journalName={journal.name}/>
                    : <GradebookDashBoard journalUuid={journal.uuid}
                                          journalName={journal.name}/>,
            icon: <Grading/>,
            show: false,
            permissions: ["ADD_GRADES", "MANAGE_GRADES"],
            collapsible: false
            };
        });
    return pages;
};

const useNavigationData = () => {
    const {hasPermission, user, loggedIn} = useUserContext();

    // The menu is partly data now. A journal is what a tab is; adding a column
    // to one changes what an existing grid shows and adds nothing here.
    // Wrapped, not passed by reference: react-query calls the function with a
    // QueryFunctionContext, which would arrive as the includeArchived argument
    // and be serialised into the query string.
    //
    // `enabled` is load-bearing rather than an optimisation. NavigationProvider
    // wraps the whole application - above the point where App chooses between
    // the login form and the console - so this hook runs on the login screen
    // too, where there is no token. The request 401s; the axios interceptor
    // answers any 401 by clearing the session and assigning
    // window.location.href, which reloads the page, which mounts this hook
    // again. The login screen then reloads forever and nobody can type a
    // password.
    //
    // Gated here rather than by moving the provider: everything below it
    // expects the menu to exist, and a request made while logged out should not
    // be sent whatever the interceptor does with the answer.
    const {data: journals} = useQuery(["JOURNALS"], () => fetchJournals(), {
        enabled: !!loggedIn,
        refetchOnWindowFocus: false,
        staleTime: 5 * 60 * 1000,
        // A failure here must not blank the menu - the static pages still work.
        onError: () => {
        }
    });

    const pages = useMemo(() => ({
        ...journalPages(journals),
        JOURNALS: {
            id: 'JOURNALS',
            name: 'ჟურნალები',
            component: <JournalsDashBoard/>,
            icon: <MenuBook/>,
            show: false,
            permissions: ["MANAGE_TEMPLATES"],
            collapsible: false
        },
        DAILY_ABSENCE: {
            id: 'DAILY_ABSENCE',
            name: 'გაცდენები (დღიური)',
            // Not a journal, so not in journalPages: daily absence is its own
            // table, keyed by student and date. A row means absent and no row
            // means present, which is why it has no publish button - what
            // reaches a parent is the email, the same day.
            component: <DailyRegisterPage/>,
            icon: <EventBusy/>,
            show: false,
            permissions: ["ADD_GRADES", "MANAGE_GRADES"],
            collapsible: false
        },
        HOMEWORK: {
            id: 'HOMEWORK',
            name: 'საშინაო დავალებები',
            component: <HomeworkDashBoard/>,
            icon: <Assignment/>,
            show: false,
            permissions: ["MANAGE_HOMEWORK"],
            collapsible: false
        },
        SCHEDULE: {
            id: 'SCHEDULE',
            name: 'დღის რეჟიმი',
            component: <StandingDocPage kind="schedule" title="დღის რეჟიმი" withTime/>,
            icon: <Schedule/>,
            show: false,
            permissions: ["MANAGE_SCHEDULE"],
            collapsible: false
        },
        MENU: {
            id: 'MENU',
            name: 'კვება',
            component: <StandingDocPage kind="menu" title="კვება"/>,
            icon: <Restaurant/>,
            show: false,
            permissions: ["MANAGE_MENU"],
            collapsible: false
        },
        CHARACTERIZATION: {
            id: 'CHARACTERIZATION',
            name: 'მოსწავლის დახასიათება',
            component: <CharacterizationPage/>,
            icon: <Description/>,
            show: false,
            permissions: ["MANAGE_CHARACTERIZATION"],
            collapsible: false
        },
        NEWS: {
            id: 'NEWS',
            name: 'სიახლეები',
            component: <NewsPage/>,
            icon: <Feed/>,
            show: false,
            permissions: ["MANAGE_NEWS"],
            collapsible: false
        },
        CHANGE_REQUEST_QUEUE: {
            id: 'CHANGE_REQUEST_QUEUE',
            name: 'ცვლილების მოთხოვნები',
            component: <ChangeRequestQueue/>,
            icon: <ChangeHistory/>,
            show: false,
            permissions: ["MANAGE_CHANGE_REQUESTS", "VIEW_CHANGE_REQUESTS"],
            collapsible: false
        },
        PUBLICATION_LOG: {
            id: 'PUBLICATION_LOG',
            name: 'გამოქვეყნების ისტორია',
            component: <PublicationLog/>,
            icon: <EventAvailable/>,
            show: false,
            permissions: ["MANAGE_CLOSED_PERIOD", "MANAGE_GRADES"],
            collapsible: false
        },
        SYSTEM_USER: {
            id: 'SYSTEM_USER',
            name: 'სისტემური მომხმარებელი',
            component: <SystemUserDashBoard/>,
            icon: <Person/>,
            show: false,
            permissions: ["MANAGE_SYSTEM_USER"],
            collapsible: false
        },
        SUBJECTS: {
            id: 'SUBJECTS',
            name: 'საგანები',
            component: <SubjectsPage/>,
            icon: <MenuBook/>,
            show: false,
            // Viewing is enough to open it; the page hides its own write
            // controls without MANAGE_SUBJECT.
            permissions: ["VIEW_SUBJECT", "MANAGE_SUBJECT"],
            collapsible: false
        },
        STUDENTS: {
            id: 'STUDENTS',
            name: 'მოსწავლეები',
            component: <StudentsPage/>,
            icon: <SwitchAccount/>,
            show: false,
            permissions: ["VIEW_STUDENT", "MANAGE_STUDENT"],
            collapsible: false
        },
        ACADEMY_CLASS: {
            id: 'ACADEMY_CLASS',
            name: 'კლასები',
            component: <ClassesPage/>,
            icon: <Class/>,
            show: false,
            permissions: ["VIEW_ACADEMY_CLASS", "MANAGE_ACADEMY_CLASS"],
            collapsible: false
        },
        CLOSE_PERIOD: {
            id: 'CLOSE_PERIOD',
            name: 'ნიშნების ჩაკეტვა',
            component: <ClosePeriodDashBoard/>,
            icon: <TimeIcon/>,
            show: false,
            permissions: ["MANAGE_CLOSED_PERIOD"],
            collapsible: false
        },
        SYSTEM_USER_GROUP: {
            id: 'SYSTEM_USER_GROUP',
            name: 'უფლებათა ჯგუფები',
            component: <SystemUserGroupDashBoard/>,
            icon: <TimeIcon/>,
            show: false,
            permissions: ["VIEW_SYSTEM_USER_GROUP"], //TODO
            collapsible: false
        },
    }), [user, journals]);

    return useMemo(() => {
        const createPageLabels = (page) => {
            if (page.collapsable) {
                Object.values(page.options).forEach(option => {
                    if (option.collapsable) {
                        createPageLabels(option);
                    } else {
                        option["label"] = option.name;
                    }
                });
            }
            page["label"] = (page.name);
        };

        const hasAnyPermission = (page, hasPermission) => {
            const requiredPermissions = page.permissions;

            if (!requiredPermissions) {
                return true;
            }
            return requiredPermissions.map(val => {
                return hasPermission(val)
            }).reduce((curr, next) => curr || next, false);
        };
        return Object.values(pages)
            .map(page => {
                if (page.show !== true) {
                    const hasPerm = hasAnyPermission(page, hasPermission);
                    page.show = hasPerm ? hasPerm : false;
                    if (page.collapsable) {
                        Object.values(page.options).forEach(page => {
                            const hasPerm = hasAnyPermission(page, hasPermission);
                            page.show = hasPerm ? hasPerm : false;
                            page.show = hasAnyPermission(page, hasPermission);
                        });
                    }
                }
                createPageLabels(page);
                return page;
            });

    }, [pages]);
};


export default useNavigationData;
