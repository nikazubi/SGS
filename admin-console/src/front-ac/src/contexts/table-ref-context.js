import React, { createContext, createRef, useContext, useEffect, useState } from 'react';
import useNavigationData from '../app/useNavigationData';

const TableRefContext = createContext(null);

export const useTableRef = () => {
  const context = useContext(TableRefContext);

  if (!context) {
    return {};
  }

  return context;
};

export const TableRefProvider = ({ pageId, ...rest }) => {
  const [tableRefs, setTableRefs] = useState({})
  const pages = useNavigationData();

  useEffect(() => {
    const createTableReference = (obj, page) => {
      if (page.collapsable) {
        Object.values(page.options).forEach(option => {
          if (option.collapsable) {
            createTableReference(obj, option);
          } else {
            obj[option.id] = createRef();
          }
        })
      } else {
        obj[page.id] = createRef();
      }
    }

    const refsObj = pages.reduce((obj, page) => {
      createTableReference(obj, page);
      return obj;
    }, {})
    
    setTableRefs(refsObj)
  }, [pages])

  const tableRef = !!tableRefs ? tableRefs[pageId] : null;
  
  return <TableRefContext.Provider value={{tableRef, pageId}} {...rest}/>;
};
