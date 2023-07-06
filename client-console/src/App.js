import './assets/global.css'
import Header from './components/Header'
import AfterLoginPage from './pages/afterLoginPage/AfterLoginPage'
import { BrowserRouter, Route, Switch } from 'react-router-dom';
import SemestruliShefaseba from './pages/semestruli-shefaseba/SemestruliShefaseba';
import EthicPage from './pages/ethicalPage';

export const App = () =>{
    return(
        <BrowserRouter>
            <Header/>
            <Switch>
                <Route exact path="/" component={AfterLoginPage}/>
                <Route path="/semestruli-shefaseba" component={SemestruliShefaseba}/>
                <Route path="/ethicalPage" component={EthicPage}/>
            </Switch>
        </BrowserRouter>
    )
}