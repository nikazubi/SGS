import React, {useMemo} from 'react';
import {useUserContext} from "../contexts/user-context";
import {Grading, Home} from "@mui/icons-material";
import DashBoard from "../main/pages/HomePage/DashBoard";
import {AssignmentLate, Grade} from "@material-ui/icons";


const useNavigationData = () => {
  const {hasPermission} = useUserContext();

  const pages = useMemo(() => ({
    GRADES: {
      id: 'GRADES',
      name: 'ნიშნების ჟურნალი',
      component: <DashBoard/>,
      icon: <Grading/>,
      show: false,
      permissions: [],
    },
    BEHAVIOUR: {
      id: 'BEHAVIOUR',
      name: 'ყოფაქცევის ჟურნალი',
      component: <div>{"hey"}</div>,
      icon: <Grade/>,
      show: false,
      permissions: [],
      collapsible: false
    },
    ABSENCE: {
      id: 'ABSENCE',
      name: 'გაცდენების ჟურნალი',
      component: <div>{"yoo"}</div>,
      icon: <AssignmentLate/>,
      show: false,
      permissions: [],
      collapsible: false
    }
  }), []);

  return useMemo(() => {
    const createPageLabels = (page) => {
      if (page.collapsable) {
        Object.values(page.options).forEach(option => {
          if (option.collapsable) {
            createPageLabels(option);
          } else {
            option["label"] = option.name;
          }
        });
      }
      page["label"] = (page.name);
    };

    const hasAnyPermission = (page, hasPermission) => {
      const requiredPermissions = page.permissions;

      if (!requiredPermissions) {
        return true;
      }

      return requiredPermissions.map(hasPermission).reduce((curr, next) => curr || next, false);
    };

    return Object.values(pages)
      .map(page => {
        page.show = hasAnyPermission(page, hasPermission);
        if (page.collapsable) {
          Object.values(page.options).forEach(page => {
            page.show = hasAnyPermission(page, hasPermission);
          });
        }
        createPageLabels(page);
        return page;
      });

  }, [pages, hasPermission]);
};


export default useNavigationData;
