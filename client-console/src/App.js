import './assets/global.css'
import Header from './components/Header'
import JournalPage from './pages/journal/JournalPage';
import HomeworkPage from './pages/homework/HomeworkPage';
import NewsPage from './pages/news/NewsPage';
import NewsArticlePage from './pages/news/NewsArticlePage';
import {
    fetchDescriptionDay,
    fetchDescriptionMonth,
    fetchHomeworkDay,
    fetchHomeworkMonth,
    fetchMenu,
    fetchSchedule
} from './pages/journal/parentApi';
import StandingDocPage from './pages/standing/StandingDocPage';
import AfterLoginPage from './pages/afterLoginPage/AfterLoginPage'
import {BrowserRouter, Route, Switch} from 'react-router-dom';
import LoginPage from "./pages/loginPage/LoginForm";
import {useUserContext} from "./context/user-context";
import {useEffect} from "react";

export const App = () => {
    const {loggedIn} = useUserContext();

    useEffect(() => {

    }, [loggedIn])

    return (
        <div>
            {
                loggedIn ?
                    <BrowserRouter>
                        <Header/>
                        <Switch>
                            <Route exact path="/" component={AfterLoginPage}/>
                            {/* One route for every journal, including the ones
                                the school has not created yet. */}
                            <Route path="/journal/:uuid" component={JournalPage}/>

                            {/* Shared by every school. The modules that are
                                primary-only - meals, the daily schedule, the
                                child's description - land next. */}
                            <Route path="/homework" render={() => (
                                <HomeworkPage
                                    title="საშინაო დავალებები"
                                    path="/homework"
                                    queryKey="PARENT_HOMEWORK"
                                    fetchMonth={fetchHomeworkMonth}
                                    fetchDay={fetchHomeworkDay}/>
                            )}/>
                            {/* Before the list, so /news/<uuid> is an article rather than the
                                list with a stray segment. */}
                            <Route path="/news/:uuid" component={NewsArticlePage}/>
                            <Route path="/news" component={NewsPage}/>

                            {/* Primary-school modules. The route exists for
                                everyone; whether the box appears comes from
                                /modules, which knows the child's school. */}
                            <Route path="/schedule" render={() => (
                                <StandingDocPage
                                    queryKey="PARENT_SCHEDULE"
                                    title="დღის რეჟიმი"
                                    fetcher={fetchSchedule}
                                    withTime
                                />
                            )}/>
                            <Route path="/menu" render={() => (
                                <StandingDocPage
                                    queryKey="PARENT_MENU"
                                    title="კვება"
                                    fetcher={fetchMenu}
                                />
                            )}/>
                            {/* The same calendar as homework, over the
                                descriptions written about this child. */}
                            <Route path="/description" render={() => (
                                <HomeworkPage
                                    title="მოსწავლის დახასიათება"
                                    path="/description"
                                    queryKey="PARENT_DESCRIPTION"
                                    fetchMonth={fetchDescriptionMonth}
                                    fetchDay={fetchDescriptionDay}/>
                            )}/>
                            {/* The five hardcoded pages that used to live here -
                                ethics, discipline, annual, trimester, absence -
                                are gone. The journal boxes replaced them and
                                nothing had linked to them since; they read the
                                dbo tables through /client/**, which the rewrite
                                does not use. Recoverable from git if a screen
                                turns out to be missed. */}
                        </Switch>
                    </BrowserRouter>
                    :
                    <LoginPage/>
            }
        </div>
    )
}