import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import useNavigationData from "../app/useNavigationData";
import { useHistory, useLocation } from "react-router-dom";
import { createAndDispatchEventOfType, FILTER_REMOVAL } from "../utils/events";

const NavigationContext = createContext(null);

export const useNavigate = () => {
  const context = useContext(NavigationContext);

  if (!context) {
    throw new Error('You forgot to wrap your component with NavigationProvider');
  }

  return context;
};

export const NavigationProvider = (props) => {
  const location = useLocation();
  const history = useHistory();
  const [activeTab, setActiveTab] = useState(null);
  const [locationCache, setLocationCache] = useState({});
  const pageArray = useNavigationData();
  const pages = useMemo(() => {
    const result = [];

    const addPage = (page) => {
      if (page.collapsable) {
        Object.values(page.options).forEach(option => {
          if (option.collapsable) {
            addPage(option);
          } else {
            result.push(option);
          }
        });
      } else {
        result.push(page);
      }
    };

    pageArray.forEach(page => addPage(page));
    return result;
  }, [pageArray]);

  const [tabList, setTabList] = useState([...pages.filter(page => page.id === 'GRADES' ||
      page.id === 'BEHAVIOUR' || page.id === 'ABSENCE')]);

  const setDocumentTitle = useCallback((pageId) => {
    const newPage = pages.find((currPage) => currPage.id === pageId);
    document.title = "აიბი მთიები " +
            (newPage.label.length ? ` - ${newPage.label}` : '');
  }, [pages]);

  useEffect(() => {
    const {pathname} = location;
    console.log(pathname)
    const page = pages.find(page => !!pathname
      && (pathname.endsWith(page.id)
        || pathname.includes(`${page.id}/`))
    );
    console.log(page)
    if (!!page) {
      console.log('amhere')
      setTabList(prevState => {
        if (!prevState.some(tab => tab.id === page.id)) {
          return [...prevState, page];
        }
        return prevState;
      });
      setDocumentTitle(page.id);
      setActiveTab(page.id);
    }
  }, [location, pages, setDocumentTitle]);

  const changeActiveTab = useCallback(pageId => {
    if (pageId === activeTab) {
      return;
    }
    setLocationCache(pervLocationCache => {
      const newLocationCache = {...pervLocationCache};
      newLocationCache[activeTab] = history.location;
      return newLocationCache;
    })

    setDocumentTitle(pageId);
    const nextLocation = locationCache[pageId]?.pathname ?? `/${pageId}`;
    history.push(nextLocation);
  }, [activeTab, history, locationCache, setDocumentTitle]);


  const handleCloseTab = useCallback(index => {
    let tabId = tabList[index].id;
    if (tabId === '') {
      return;
    }

    createAndDispatchEventOfType(FILTER_REMOVAL, {id: tabId});
    if(tabList[index].subTabs){
      tabList[index].subTabs.map((val, ind) =>{
          createAndDispatchEventOfType(FILTER_REMOVAL, {id: tabList[index].subTabs[ind]})
          return val;
        }
      )
    }

    const newTabList = [...tabList];
    // if active tab
    if (tabId === activeTab) {
      let nextActiveTab = newTabList[index - 1] || newTabList[index + 1] || null;
      if (!!nextActiveTab) {
        changeActiveTab(nextActiveTab.id);
      } else {
        history.push('/');
      }
    }
    setLocationCache(pervLocationCache => {
      pervLocationCache[tabId] = null;
      return pervLocationCache;
    })
    // remove tab
    newTabList.splice(index, 1);
    setTabList(newTabList);
  }, [tabList, setTabList, activeTab, changeActiveTab, history]);

  for (let i = 0; i < tabList.length; i++) {
    let tab = tabList[i];
    if (!pages.some(page => page.id === tab.id)) {
      handleCloseTab(i);
      break;
    }
  }

  const value = useMemo(() => ({
    tabList,
    activeTab,
    locationCache,
    setTabList,
    changeActiveTab,
    handleCloseTab,
  }), [
    tabList,
    activeTab,
    locationCache,
    setTabList,
    changeActiveTab,
    handleCloseTab,
  ]);

  return <NavigationContext.Provider value={value} {...props}/>;
};