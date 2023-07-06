import React from "react";
import ReactDOM from 'react-dom';
import {App} from './App'
import UserDataProvider from "./context/userDataContext";
const app = (
    <UserDataProvider>
        <App/>
    </UserDataProvider>
);

ReactDOM.render(app, document.getElementById('root'));
