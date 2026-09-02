import React, {createContext, useCallback, useContext, useEffect, useMemo, useState} from 'react';
import useNavigationData from "../app/useNavigationData";
import {useHistory, useLocation} from "react-router-dom";
import {createAndDispatchEventOfType, FILTER_REMOVAL} from "../utils/events";

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

    // Nothing open to begin with; the effect below opens the landing page.
    //
    // This used to hard-code four tabs by id - TRIMESTER, BEHAVIOUR, ABSENCE and
    // CHANGE_REQUEST - which were the legacy grade screens. When those pages were
    // deleted the filter matched nothing, and the console opened to a blank panel
    // with no error anywhere. Page ids are not a stable thing to name from here:
    // the journal tabs do not even have fixed ones, since they are keyed by uuid.
    const [tabList, setTabList] = useState([]);

  const setDocumentTitle = useCallback((pageId) => {
    const newPage = pages.find((currPage) => currPage.id === pageId);
      // "Close all tabs" calls changeActiveTab('') and no page has that id, so
      // this threw on a null page every time somebody used it. Named rather than
      // found means the bare school name, which is the right title for a console
      // showing nothing.
      if (!newPage) {
          document.title = "აიბი მთიები";
          return;
      }
    document.title = "აიბი მთიები " +
            (newPage.label.length ? ` - ${newPage.label}` : '');
  }, [pages]);

  useEffect(() => {
    const {pathname} = location;
    let page = pages.find(page => !!pathname
      && (pathname.endsWith(page.id)
        || pathname.includes(`${page.id}/`))
    );
      // The landing page is simply the first one this user may see. `pages` is
      // already filtered by permission, so this cannot land on a tab they are not
      // allowed to open, and it cannot go stale when a page is added or removed.
      //
      // Guarded on activeTab so it happens once: the journal tabs arrive
      // asynchronously and reorder `pages` when they do, which would otherwise
      // open a second tab underneath the user.
      if (pathname === '/' && !activeTab) {
          page = pages[0];
    }
    if (!!page) {
      setTabList(prevState => {
        if (!prevState.some(tab => tab.id === page.id)) {
          return [...prevState, page];
        }
        return prevState;
      });
      setDocumentTitle(page.id);
      setActiveTab(page.id);
    }
  }, [location, pages, activeTab, setDocumentTitle]);

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