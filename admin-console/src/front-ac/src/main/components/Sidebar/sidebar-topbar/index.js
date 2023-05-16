import React from 'react';
import SidebarToggleButton from './SidebarToggleButton';
import styled from '@material-ui/core/styles/styled';
import { useSidebarContext } from "../../../../contexts/sidebar-context";
import FlexBox from "../../../../components/FlexBox";
// import { ENDPOINT_ICON } from "../../../../constants/endpoints";

export const Wrapper = styled(FlexBox)(({theme, open}) => ({
  alignItems: 'flex-start',
  justifyContent: 'center',
  position: "relative",
  paddingTop: 10,
  height: 60,
  ".MuiDrawer-root:hover & img": {
    height: 60
  },
  "& img": {
    height: open ? 60 : 30,
    position: "relative",
    zIndex: 1
  },
  marginBottom: 25
}));


const SidebarTopbar = ({
                         onDrawerOpen,
                         onDrawerClose,
                       }) => {
  const {isSidebarExpanded} = useSidebarContext();

  return (
    <Wrapper variant="dense" open={isSidebarExpanded}>
      {/*<img src={ENDPOINT_ICON} alt={""}/>*/}
      <SidebarToggleButton
        style={{position: "absolute", left: 'calc(100% - 55px)'}}
        open={isSidebarExpanded}
        onDrawerClose={onDrawerClose}
        onDrawerOpen={onDrawerOpen}
      />
    </Wrapper>
  );
};

export default SidebarTopbar;
