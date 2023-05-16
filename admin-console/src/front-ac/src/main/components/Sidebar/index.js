import React, {useState} from 'react';
import * as S from './styles';
import SidebarTopbar from './sidebar-topbar';
import {List, Typography} from "@material-ui/core";
import Navigation from "../Navigation";
import SimpleBarReact from "simplebar-react";
import "simplebar/src/simplebar.css";
import useNavigationData from "../../../app/useNavigationData";
import {useSidebarContext} from "../../../contexts/sidebar-context";
import {makeStyles} from "@material-ui/core/styles";
import {AzRyLogo} from "../../../assets/images";
import FlexBox from "../../../components/FlexBox";
import {useInitialDataContext} from "../../../contexts/initial-data-context";

const useStyles = makeStyles({
  simpleBar: {
    height: 'calc(100vh - 140px)',
    width: 300,
    '& .simplebar-scrollbar::before': {
      backgroundColor: '#0e7fb7'
    },
    overflowX: 'hidden'
  },
  list: {
    height: 'calc(100vh - 140px)',
    width: 300,
  },
  logo: {
    position: 'fixed',
    bottom: 10,
    left: 7
  },
  copyright: {
    color: '#0e7fb7',
    marginRight: 5,
  }
});


const Sidebar = () => {
  const {isSidebarExpanded, handleDrawerExpand, handleDrawerClose} = useSidebarContext();

  const pages = useNavigationData();
  const {appVersion} = useInitialDataContext();
  const [expandedItems, setExpandedItems] = useState([]);
  const classes = useStyles();

  const handleExpandToggle = (id) => {
    if (expandedItems.includes(id)) {
      setExpandedItems((prevItems) =>
        prevItems.filter((item) => item !== id)
      );
    } else {
      setExpandedItems((prevItems) => [...prevItems, id]);
    }
  };

  return (
    <S.Sidebar
      variant="permanent"
      open={isSidebarExpanded}
      sidebarwidth={300}
    >
      <div style={{width: "auto"}}>
        <SidebarTopbar
          onDrawerClose={handleDrawerClose}
          onDrawerOpen={handleDrawerExpand}
        />
        <SimpleBarReact className={classes.simpleBar}>
          <List classes={{root: classes.list}}>
            <Navigation
              pages={pages}
              expandedItems={expandedItems}
              onExpandToggle={handleExpandToggle}
            />
          </List>
        </SimpleBarReact>
        <br/>
        {isSidebarExpanded && <FlexBox flexDirection="row-reverse">
          {!!appVersion && <Typography className={classes.copyright}>v{appVersion}</Typography>}
        </FlexBox>}
      </div>
    </S.Sidebar>
  );
};

export default Sidebar;
