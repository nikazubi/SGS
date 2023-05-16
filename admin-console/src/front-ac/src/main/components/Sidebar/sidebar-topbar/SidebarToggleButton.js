import React from 'react';
import styled from '@material-ui/core/styles/styled';
import Button from "@material-ui/core/Button";

export const StyledBurger = styled(Button)(({theme, open}) => ({
  display: 'grid',
  width: '1.5rem',
  height: '100%',
  background: 'transparent',
  border: 'none',
  boxShadow: 'none',

  ".MuiDrawer-root:hover &": {
    opacity: '1',
    pointerEvents: 'all'
  },
  ".MuiDrawer-root &": {
    opacity: open ? '1' : '0',
    pointerEvents: open ? 'all' : 'none'
  },

  "& .MuiTouchRipple-root": {
    display: 'none'
  },

  "&:focus": {
    outline: 'none'
  },

  "& div": {
    width: '1.5rem',
    marginTop: '0.125rem',
    marginBottom: '0.125rem',
    height: '0.25rem',
    background: '#C3CED5',
    borderRadius: '10px',
    transition: 'all 0.3s linear',
    position: 'relative',
    transformOrigin: '1px',
  },
  '& div:first-child': {
    transform: open ? 'rotate(45deg)' : 'rotate(0)'
  },

  '& div:nth-child(2)': {
    opacity: open ? '0' : '1',
    transform: open ? 'translateX(20px)' : 'translateX(0)'
  },

  '& div:nth-child(3)': {
    transform: open ? 'rotate(-45deg)' : 'rotate(0)'
  }
}));

const SidebarToggleButton = ({open, onDrawerClose, onDrawerOpen, ...rest}) => {
  return (
    <StyledBurger open={open} onClick={() => {open ? onDrawerClose() : onDrawerOpen()}} {...rest}>
      <div/>
      <div/>
      <div/>
    </StyledBurger>
  );
};

export default SidebarToggleButton;
