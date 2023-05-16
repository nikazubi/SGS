import React, { createContext, useContext } from 'react';
import ErrorPage from "../components/ErrorPage";
import Progress from "../components/Progress";

const InitialDataContext = createContext(null);

export const useInitialDataContext = () => {
  const context = useContext(InitialDataContext);

  if (!context) {
    throw new Error('You forgot to wrap your component with InitialDataProvider');
  }

  return context;
};

const createRegexes = (data) => {
  if (!data || !data.regexes) {
    return;
  }
  Object.keys(data.regexes).forEach(key => {
    data.regexes[key] = new RegExp(data.regexes[key]);
  })
}

export const InitialDataProvider = (props) => {
  const data = {}
  // const {data, isLoading, isError, error, refetch} = useInitialData();
  //
  // if (isLoading) {
  //   return <Progress />;
  // } else if (isError) {
  //   return <ErrorPage error={error} onReload={refetch}/>;
  // } else {
  //   createRegexes(data);
    return <InitialDataContext.Provider value={data} {...props} />;
  // }
};