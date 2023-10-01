import React, {createContext, useContext, useState} from "react";
import {deleteAuth} from "../utils/auth";
import useAxios from "../hooks/useAxios";
import axios from "../utils/axios";

const UserContext = createContext(null);

export const useUserContext = () => {
  const context = useContext(UserContext);

  if (!context) {
    return {};
  }

  return context;
};

export const UserContextProvider = props => {
    const [loggedIn, setLoggedIn ]= useState(false);
    const [user, setUser ]= useState(false);
    // const {data, refetch, isLoading, isError, error} = useLoggedInUser();
    // const user = data?.user;
  // const language = data?.user?.systemUserConfig?.languageTag;
  // const updateLogin = () => refetch().then(auth => {
  //   setAuth({
  //     accessToken: auth.data.accessToken,
  //     refreshToken: auth.data.refreshToken
  //   })
  // });
  //
  const login = async (auth) => {
      const {data} = await axios.get("user-and-permissions")
      setUser(data)
      setLoggedIn(true)
      localStorage.setItem("loginTime", new Date().toString())
      // setAuth({accessToken: auth.accessToken, refreshToken: auth.refreshToken});
      // refetch();
  };

  const logout = () => {
      console.log("inlogout")
      deleteAuth();
    setLoggedIn(false)
    // refetch();
  };
  //
  // const userUpdated = ({id}) => {
  //   if (user?.id && user.id === id) {
  //     updateLogin();
  //   }
  // };
  //
  let hasPermission = (permission) => {
      if (!!user) {
          // hasPermission = (permission) => {
              return user.permissionList.includes(permission.toString());
          // }
      }
  }
  //
  // const userGroupUpdated = ({id}) => {
  //   if (user?.groups && user.groups.map(value => value.id).includes(id)) {
  //     updateLogin();
  //   }
  // };
  //
  // if (isLoading) {
  //   return <Progress/>;
  // } else if (isError) {
  //   return <ErrorPage error={error} logout={logout} onReload={refetch}/>;
  // } else {
    return <UserContext.Provider
      value={{
          user: user,
          login: () => login(),
          logout: () => logout(),
          hasPermission: (permission) => hasPermission(permission),
          userUpdated: () => {
          },
          userGroupUpdated: () => {
          },
          loggedIn: loggedIn
      }} {...props}/>;

};
