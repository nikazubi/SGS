import React from "react";
import ReactDOM from 'react-dom';
import {App} from './App';
import './aaaa.css';
import UserDataProvider from "./context/userDataContext";

const app = (
    <div className="diagonal-lines">
        <UserDataProvider>
            <App/>
        </UserDataProvider>
    </div>
);

ReactDOM.render(app, document.getElementById('root'));
