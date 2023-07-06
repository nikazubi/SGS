import React, {Suspense} from "react";
import ReactDOM from 'react-dom';
import './assets/index.css';
import App from './app/App';
import Progress from "./components/Progress";
import {AppProviders} from "./contexts";
const logToConsole = (message) => {
    console.log('[Custom Log]:', message);
  };

const app = (
    <Suspense fallback={<Progress/>}>
        <AppProviders>
            <App/>
        </AppProviders>
    </Suspense>
);
logToConsole('Starting the app...');
ReactDOM.render(app, document.getElementById('root'));
