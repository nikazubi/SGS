import {NotificationProvider} from './notification-context';
import {MuiThemeProvider} from '@material-ui/core/styles';
import {theme} from '../assets/theme';
import React from 'react';
import {BackdropProvider} from './backdrop-context';
import {NavigationProvider} from './navigation-context';
import {UserContextProvider} from './user-context';
import {InitialDataProvider} from "./initial-data-context";
import {ConvertErrorProvider} from "../hooks/convertError";
import {HashRouter} from "react-router-dom";
import {ReactQueryProvider} from "./react-query-context";

export const AppProviders = ({children}) => {
    return (
        <HashRouter>
            <ReactQueryProvider>
                <ConvertErrorProvider>
                    <UserContextProvider>
                        <InitialDataProvider>
                            <NavigationProvider>
                                <NotificationProvider>
                                    <BackdropProvider>
                                        <MuiThemeProvider theme={theme}>
                                            {children}
                                        </MuiThemeProvider>
                                    </BackdropProvider>
                                </NotificationProvider>
                            </NavigationProvider>
                        </InitialDataProvider>
                    </UserContextProvider>
                </ConvertErrorProvider>
            </ReactQueryProvider>
        </HashRouter>
    );
};
