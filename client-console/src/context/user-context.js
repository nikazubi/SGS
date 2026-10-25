import React, {createContext, useContext, useEffect, useState} from "react";
import useAxios from "../hooks/useAxios";
import {LOGIN_ENDPOINT} from "../pages/utils/constants";
import secureLocalStorage from "react-secure-storage";
import {useHistory} from "react-router-dom";
import {useQueryClient} from "react-query";

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
    const queryClient = useQueryClient();

    useEffect(() => {
        if (secureLocalStorage.getItem("jwtToken")) {
            setLoggedIn(true)
            setUser(secureLocalStorage.getItem("student"))
        }
    }, [])

    const login = async ({username, password}) => {
        const {data} = await axios.post(LOGIN_ENDPOINT, {username, password});
        // The child's name comes back with the token so the portal can greet
        // them without a second call; everything else is fetched per journal.
        const student = {firstName: data?.firstName, lastName: data?.lastName};
        // Nothing cached may outlive the child it was fetched for. Siblings
        // share a username, so the next login on this tab is often the other
        // child - who would otherwise be shown the first one's marks, module
        // list and school until each query refetched.
        queryClient.clear();
        await secureLocalStorage.setItem("jwtToken", data?.token);
        await secureLocalStorage.setItem("student", student);
        setLoggedIn(true);
        setUser(student);
    };

    const logout = () => {
        secureLocalStorage.clear();
        queryClient.clear();
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
