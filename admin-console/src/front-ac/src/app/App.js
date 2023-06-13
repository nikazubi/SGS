import React, { useEffect, useState } from 'react';
import './App.css';
import {Backdrop, CssBaseline} from "@mui/material";
import {ErrorBoundary} from "react-error-boundary";
import {useBackdrop} from "../contexts/backdrop-context";
import {ErrorPage} from "../components/styles";
import MainContainer from "../main/MainContainer";
import Notification from '../components/notifications/Notification';
import LoginPage from "../main/pages/loginPage/LoginForm";
// import MainContainer from '../main/MainContainer';
// import CssBaseline from '@material-ui/core/CssBaseline';
// import Backdrop from '../shared/components/Backdrop';
// import { ErrorBoundary } from 'react-error-boundary';
// import ErrorPage from '../shared/components/ErrorPage';
// import { useBackdrop } from '../contexts/backdrop-context';
// import Login from "../main/pages/Login";
// import { useUserContext } from "../contexts/user-context";
// import i18next from "i18next";
// import setLanguage from "../messages/Language";
// import { useLocalesDataContext } from "../contexts/locale-context";
// import Progress from "../shared/components/Progress";

const loginPageLanguageStorageKey = "loginLanguage"

const App = () => {

  const {isBackdropOpen} = useBackdrop();
  const [loggedIn, setLoggedIn ]= useState(false);


  return (
      <ErrorBoundary FallbackComponent={ErrorPage}>
        <CssBaseline/>
        {(loggedIn ?
                <MainContainer/>
                :
                <LoginPage setLoggedIn={setLoggedIn}/>
        )}
        <Notification/>
        <Backdrop open={isBackdropOpen}/>
      </ErrorBoundary>
  );
};

export default App;
