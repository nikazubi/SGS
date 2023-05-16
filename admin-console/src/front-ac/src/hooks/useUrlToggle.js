import { useCallback, useEffect, useMemo, useState } from "react";
import { useHistory, useLocation } from "react-router-dom";
import { addToPath, removeSlashes, removeTrailingSlashes } from "../utils/helpers";
import { useTableRef } from "../contexts/table-ref-context";
import { useNavigate } from "../contexts/navigation-context";

const baseUrl = (url, key) => {
  const upTo = url.indexOf(key) !== -1 ? url.indexOf(key) : url.length;
  return removeTrailingSlashes(url.substring(0, upTo));
}

function getResourceName(key, value) {
  key = removeSlashes(key)
  value = removeSlashes(value);
  return `${key}/${value}`;
}

export const useUrlToggle = (key, value = '') => {
  const [open, setOpen] = useState(false);
  const { pathname } = useLocation();
  const history = useHistory();
  const { pageId: usedOnTab } = useTableRef();
  const { activeTab } = useNavigate();

  useEffect(() => {
    const resourceName = getResourceName(key, value);
    const open = pathname.endsWith(resourceName) || pathname.includes(`${resourceName}/`);
    if (usedOnTab === activeTab) { //if component is not rendered right now
      setOpen(open);
    } else {
      setOpen(false);
    }
  }, [pathname, key, value, usedOnTab, activeTab]);

  const handleClose = useCallback(() => {
    const newPath = baseUrl(pathname, key);
    history.push(newPath);
  }, [pathname, history, key]);

  const handleOpen = useCallback(() => {
    const base = baseUrl(pathname, key);
    history.push(addToPath(base, getResourceName(key, value)));
  }, [pathname, history, key, value]);

  return useMemo(() => ({ open, handleOpen, handleClose }), [open, handleOpen, handleClose]);
};