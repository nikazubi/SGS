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
      component: <BehaviourDashBoard/>,
      icon: <Grade/>,
      show: false,
      permissions: [],
      collapsible: false
    },
    ABSENCE: {
      id: 'ABSENCE',
      name: 'გაცდენების ჟურნალი',
      component: <AbsenceDashBoard/>,
      icon: <ExitToApp/>,
      show: false,
      permissions: [],
      collapsible: false
    },
    CHANGE_REQUEST: {
      id: 'CHANGE_REQUEST',
      name: 'მოთხოვნილი ცვლილებები',
      component: <ChangeRequestDashBoard/>,
      icon: <ChangeHistory/>,
      show: false,
      permissions: ["EDIT_GRADES"],
      collapsible: false
    },
    MONTHLY_GRADE: {
      id: 'MONTHLY_GRADE',
      name: 'თვის შემაჯამებელი ნიშნები',
      component: <MonthlyGradeDashBoard/>,
      icon: <DiscFull/>,
      show: false,
      permissions: [],
      collapsible: false
    },
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
