import './assets/global.css'
import Header from './components/Header'
import AfterLoginPage from './pages/afterLoginPage/AfterLoginPage'
import {BrowserRouter, Route, Switch} from 'react-router-dom';
import Discipline from './pages/Discipline/Discipline';
import EthicPage from './pages/ethicalPage';
import AbsencePage from './pages/AbsencePage';
import SemestruliShefaseba from './pages/semestruli-shefaseba';
import TsliuriShefaseba from './pages/TsliuriShefaseba';
import MonthlyGrade from './pages/MonthlyGrade';
import LoginPage from "./pages/loginPage/LoginForm";
import {useUserContext} from "./context/user-context";

export const App = () => {
    const {loggedIn} = useUserContext();

    return (
        <div>
            {
                loggedIn ?
                    <BrowserRouter>
                        <Header/>
                        <Switch>
                            <Route exact path="/" component={AfterLoginPage}/>
                            <Route path="/semestruli-shefaseba" component={SemestruliShefaseba}/>
                            <Route path="/ethicalPage" component={EthicPage}/>
                            <Route path="/absence-page" component={AbsencePage}/>
                            {/* <Route exact path="/shefaseba-akademiuri-sagnobrivi-disciplinis-mixedvit" component={Discipline}/> */}
                            <Route path="/grades/:id" component={Discipline}/>
                            <Route path="/tsliuri-shefaseba" component={TsliuriShefaseba}/>
                            <Route path="/tvis-reitingi" component={MonthlyGrade}/>
                        </Switch>
                    </BrowserRouter>
                    :
                    <LoginPage/>
            }
        </div>
    )
}