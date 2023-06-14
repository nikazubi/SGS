import React, { createContext, useCallback, useContext } from 'react';
import { useToggle } from "../hooks/useToggle";

const SidebarContext = createContext();

export const useSidebarContext = () => {
  const context = useContext(SidebarContext);

  if (!context) {
    throw new Error('You forgot to wrap your component with SidebarContextProvider');
  }

  return context;
};

export const SidebarContextProvider = ({...rest}) => {
  const sideBarStateKey = 'sideBarState';

  const getState = () => {
    const state = localStorage.getItem(sideBarStateKey);
    if (!state) {
      setState(false)
      return false;
    }
    return JSON.parse(state)?.isSidebarExpanded;
  };

  const setState = (isSidebarExpanded) => {
    localStorage.setItem(sideBarStateKey, JSON.stringify({isSidebarExpanded}));
  };

  const { open, handleOpen, handleClose } = useToggle(getState());


  const handleDrawerClose = useCallback(() => {
    handleClose();
    setState(false);
  }, [handleClose]);

  const handleDrawerExpand = useCallback(() => {
    handleOpen();
    setState(true);
  }, [handleOpen]);

  return <SidebarContext.Provider value={{ isSidebarExpanded: open, handleDrawerClose, handleDrawerExpand  }} {...rest} />;
};
