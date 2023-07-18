import React, {useMemo} from 'react';
import {useUserContext} from "../contexts/user-context";
import {Grading, Home, SwitchAccount} from "@mui/icons-material";
import DashBoard from "../main/pages/HomePage/DashBoard";
import {
  AssignmentLate,
  ChangeHistory, Class,
  DiscFull,
  EventAvailable,
  EventNote,
  ExitToApp,
  Grade, MenuBook, Person,
  Today
} from "@material-ui/icons";
import BehaviourDashBoard from "../main/pages/behaviourPage/BehaviourDashBoard";
import AbsenceDashBoard from "../main/pages/absencePage/AbsenceDashBoard";
import ChangeRequestDashBoard from "../main/pages/changeRequestPage/ChangeRequestDashBoard";
import MonthlyGradeDashBoard from "../main/pages/MonthlyGradePage/MonthlyGradeDashBoard";
import SemesterGradeDashBoard from "../main/pages/semesterPage/SemesterGradeDashBoard";
import AnualGradeDashBoard from "../main/pages/anualPage/AnualGradeDashBoard";
import TotalAbsenceDashBoard from "../main/pages/totalAbsencePage/TotalAbsenceDashBoard";
import SubjectDashBoard from "../main/pages/subjectPage/SubjectDashBoard";
import StudentDashBoard from "../main/pages/studentPage/StudentDashBoard";
import AcademyClassDashBoard from "../main/pages/academyClassPage/AcademyClassDashBoard";
import SystemUserDashBoard from "../main/pages/systemUserPage/SystemUserDashBoard";


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
      name: 'ეთიკური ნორმა',
      component: <BehaviourDashBoard/>,
      icon: <Grade/>,
      show: false,
      permissions: ["ADD_GRADES", "MANAGE_GRADES"],
      collapsible: false
    },
    // ABSENCE: {
    //   id: 'ABSENCE',
    //   name: 'გაცდენების ჟურნალი',
    //   component: <TotalAbsenceDashBoard/>,
    //   icon: <ExitToApp/>,
    //   show: false,
    //   permissions: ["ADD_GRADES", "MANAGE_GRADES"],
    //   collapsible: false
    // },
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
      icon: <Today/>,
      show: false,
      permissions: ["MANAGE_CHANGE_REQUESTS"], //TODO
      collapsible: false
    },
    SEMESTER_GRADE: {
      id: 'SEMESTER_GRADE',
      name: 'სემესტრის ნიშნები',
      component: <SemesterGradeDashBoard/>,
      icon: <EventNote/>,
      show: false,
      permissions: ["MANAGE_CHANGE_REQUESTS"], //TODO
      collapsible: false
    },
    ANNUAL_GRADE: {
      id: 'ANNUAL_GRADE',
      name: 'წლიური ნიშნები',
      component: <AnualGradeDashBoard/>,
      icon: <EventAvailable/>,
      show: false,
      permissions: ["MANAGE_CHANGE_REQUESTS"], //TODO
      collapsible: false
    },
    TOTAL_ABSENCE: {
      id: 'TOTAL_ABSENCE',
      name: 'ჯამური გაცდენები',
      component: <TotalAbsenceDashBoard/>,
      icon: <AssignmentLate/>,
      show: false,
      permissions: ["MANAGE_CHANGE_REQUESTS"], //TODO
      collapsible: false
    },
    SYSTEM_USER: {
      id: 'SYSTEM_USER',
      name: 'სისტემური მომხმარებელი',
      component: <SystemUserDashBoard/>,
      icon: <Person/>,
      show: false,
      permissions: ["MANAGE_CHANGE_REQUESTS"], //TODO
      collapsible: false
    },
    SUBJECTS: {
      id: 'SUBJECTS',
      name: 'საგანები',
      component: <SubjectDashBoard/>,
      icon: <MenuBook/>,
      show: false,
      permissions: ["MANAGE_CHANGE_REQUESTS"], //TODO
      collapsible: false
    },
    STUDENTS: {
      id: 'STUDENTS',
      name: 'მოსწავლეები',
      component: <StudentDashBoard/>,
      icon: <SwitchAccount/>,
      show: false,
      permissions: ["MANAGE_CHANGE_REQUESTS"], //TODO
      collapsible: false
    },
    ACADEMY_CLASS: {
      id: 'ACADEMY_CLASS',
      name: 'კლასები',
      component: <AcademyClassDashBoard/>,
      icon: <Class/>,
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
