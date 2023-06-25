import './assets/global.css'
import Header from './components/Header'
import AfterLoginPage from './pages/afterLoginPage/AfterLoginPage'
import { BrowserRouter, Route, Switch } from 'react-router-dom';
import SemestruliShefaseba from './pages/semestruli-shefaseba/SemestruliShefaseba';

export const App = () =>{
    return(
        <BrowserRouter>
            <Header/>
            <Switch>
                <Route exact path="/" component={AfterLoginPage}/>
                <Route path="/dato" component={SemestruliShefaseba}/>
            </Switch>
        </BrowserRouter>
    )
}