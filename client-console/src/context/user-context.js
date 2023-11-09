import React, {createContext, useContext, useEffect, useState} from "react";
import useAxios from "../hooks/useAxios";
import {LOGIN_ENDPOINT} from "../pages/utils/constants";
import secureLocalStorage from "react-secure-storage";
import {useHistory} from "react-router-dom";

const UserContext = createContext(null);

export const useUserContext = () => {
    const context = useContext(UserContext);

    if (!context) {
        return {};
    }

    return context;
};

export const UserContextProvider = props => {
    const [loggedIn, setLoggedIn] = useState(false);
    const [user, setUser] = useState();
    const {axios} = useAxios();
    const history = useHistory();

    useEffect(() => {
        if (secureLocalStorage.getItem("jwtToken")) {
            setLoggedIn(true)
            setUser(secureLocalStorage.getItem("student"))
        }
    }, [])

    const login = async ({username, password}) => {
        const params = {username: username, password: password};
        const {data} = await axios.post(LOGIN_ENDPOINT, params);
        await secureLocalStorage.setItem("jwtToken", data?.jwtToken);
        await secureLocalStorage.setItem("student", data?.student);
        setLoggedIn(true)
        setUser(data?.student)
        // localStorage.setItem("loginTime", new Date().toString())
    };

    const logout = () => {
        secureLocalStorage.clear();
        history.push('/');
        setLoggedIn(false)
        // refetch();
    };

    //
    // if (isLoading) {
    //   return <Progress/>;
    // } else if (isError) {
    //   return <ErrorPage error={error} logout={logout} onReload={refetch}/>;
    // } else {
    return <UserContext.Provider
        value={{
            user: user,
            login,
            logout,
            loggedIn: loggedIn
        }} {...props}/>;

};
