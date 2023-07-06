import React, {useMemo} from 'react';
import {useUserContext} from "../contexts/user-context";
import {Grading, Home} from "@mui/icons-material";
import DashBoard from "../main/pages/HomePage/DashBoard";
import {AssignmentLate, ChangeHistory, DiscFull, ExitToApp, Grade} from "@material-ui/icons";
import BehaviourDashBoard from "../main/pages/behaviourPage/BehaviourDashBoard";
import AbsenceDashBoard from "../main/pages/absencePage/AbsenceDashBoard";
import ChangeRequestDashBoard from "../main/pages/changeRequestPage/ChangeRequestDashBoard";
import MonthlyGradeDashBoard from "../main/pages/MonthlyGradePage/MonthlyGradeDashBoard";


const useNavigationData = () => {
  const {hasPermission, user} = useUserContext();

  const pages = useMemo(() => ({
    GRADES: {
      id: 'GRADES',
      name: 'ნიშნების ჟურნალი',
      component: <DashBoard/>,
      icon: <Grading/>,
      show: false,
      permissions: ["ADD_GRADES", "MANAGE_GRADES"],
    },
    BEHAVIOUR: {
      id: 'BEHAVIOUR',
      name: 'ყოფაქცევის ჟურნალი',
      component: <BehaviourDashBoard/>,
      icon: <Grade/>,
      show: false,
      permissions: ["ADD_GRADES", "MANAGE_GRADES"],
      collapsible: false
    },
    ABSENCE: {
      id: 'ABSENCE',
      name: 'გაცდენების ჟურნალი',
      component: <AbsenceDashBoard/>,
      icon: <ExitToApp/>,
      show: false,
      permissions: ["ADD_GRADES", "MANAGE_GRADES"],
      collapsible: false
    },
    CHANGE_REQUEST: {
      id: 'CHANGE_REQUEST',
      name: 'მოთხოვნილი ცვლილებები',
      component: <ChangeRequestDashBoard/>,
      icon: <ChangeHistory/>,
      show: false,
      permissions: ["MANAGE_CHANGE_REQUESTS", "VIEW_CHANGE_REQUESTS"],
      collapsible: false
    },
    MONTHLY_GRADE: {
      id: 'MONTHLY_GRADE',
      name: 'თვის შემაჯამებელი ნიშნები',
      component: <MonthlyGradeDashBoard/>,
      icon: <DiscFull/>,
      show: false,
      permissions: ["MANAGE_CHANGE_REQUESTS"], //TODO
      collapsible: false
    },
  }), [user]);

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
      return requiredPermissions.map(val => {
        return hasPermission(val)
      }).reduce((curr, next) => curr || next, false);
    };
    return Object.values(pages)
      .map(page => {
        if(page.show !== true) {
          const hasPerm =  hasAnyPermission(page, hasPermission);
          page.show = hasPerm? hasPerm : false;
          if (page.collapsable) {
            Object.values(page.options).forEach(page => {
              const hasPerm =  hasAnyPermission(page, hasPermission);
              page.show = hasPerm? hasPerm : false;
              page.show = hasAnyPermission(page, hasPermission);
            });
          }
        }
        createPageLabels(page);
        return page;
      });

  }, [pages]);
};


export default useNavigationData;
