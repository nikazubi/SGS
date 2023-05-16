import React, {createContext, useContext} from "react";
import {deleteAuth, setAuth} from "../utils/auth";
import {useLoggedInUser} from "../hooks/useLoggedInUser";
import ErrorPage from "../components/ErrorPage";
import Progress from "../components/Progress";

const UserContext = createContext(null);

export const useUserContext = () => {
  const context = useContext(UserContext);

  if (!context) {
    return {};
  }

  return context;
};

export const UserContextProvider = props => {
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
  // const login = (auth) => {
  //   setAuth({accessToken: auth.accessToken, refreshToken: auth.refreshToken});
  //   refetch();
  // };
  //
  // const logout = () => {
  //   deleteAuth();
  //   refetch();
  // };
  //
  // const userUpdated = ({id}) => {
  //   if (user?.id && user.id === id) {
  //     updateLogin();
  //   }
  // };
  //
  // let hasPermission = () => true;
  // if (!!user) {
  //   user.permissions = user.groups.flatMap(group => group?.systemPermissions ?? []);
  //   hasPermission = (permission) => user.permissions.includes(permission);
  // }
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
          user: {},
          login: () => {},
          logout: () => {},
         hasPermission: () => true,
         userUpdated: () => {},
         userGroupUpdated: () => {},
         loggedIn: true
      }} {...props}/>;
  // }
};
