import React, { memo } from 'react';
import Content from './pages/HomePage/Content';
import Sidebar from "./components/Sidebar";
import { SidebarContextProvider } from "../contexts/sidebar-context";

const MainContainer = () => {
  return (
    <div className="main-container">
      <SidebarContextProvider >
        <Sidebar/>
      </SidebarContextProvider>
      <Content/>
    </div>
  );
};

export default memo(MainContainer);
