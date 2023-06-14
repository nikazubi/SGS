import React from 'react';
import { useTheme } from '@material-ui/core';
import Tabs, { tabsClasses } from '@mui/material/Tabs';
import { CustomTabComponent } from './CustomTab';
import AppBar from '@material-ui/core/AppBar';
import { useNavigate } from '../../contexts/navigation-context';
import UserBar from "./UserBar";

const TabNavigation = () => {
  const {tabList, setTabList, activeTab, changeActiveTab, handleCloseTab} = useNavigate();
  const theme = useTheme();

  const handleChange = (event, newValue) => {
    changeActiveTab(newValue);
  };

  const handleCloseAllTabs = () => {
    setTabList(old => [...old.filter(page => page.id === 'GRADES' ||
        page.id === 'BEHAVIOUR' || page.id === 'ABSENCE' || page.id === 'CHANGE_REQUEST')]);
    changeActiveTab('');
  };

  const handleCloseOtherTabs = (tabId) => {
    setTabList(old => old.filter(tab => tab.id === tabId || tab.id === ''));
    changeActiveTab(tabId);
  };

  return (
    <AppBar
      position="static"
      style={{
        display: 'flex',
        flexFlow: 'row',
        justifyContent: 'space-between',
        backgroundColor: theme.navigation.primary.main,
        minHeight: 50,
      }}
      elevation={0}
    >
      <Tabs
        TabIndicatorProps={{
          style: {display: 'none'}
        }}
        style={{minHeight: 40}}
        value={activeTab}
        onChange={handleChange}
        variant="scrollable"
        scrollButtons="auto"
        sx={{
          [`& .${tabsClasses.scrollButtons}`]: {
            color: "#0e7fb7",
          },
        }}
      >
        {tabList.map((tab, i) => (
          <CustomTabComponent
            key={tab.id}
            tab={tab}
            value={tab.id}
            icon={tab.icon}
            onClose={() => handleCloseTab(i)}
            onCloseAll={handleCloseAllTabs}
            onCloseOthers={() => handleCloseOtherTabs(tab.id)}
          />
        ))}
      </Tabs>
      <UserBar/>
    </AppBar>
  );
};

export default TabNavigation;
