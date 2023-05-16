import styled from '@material-ui/core/styles/styled';
import React from 'react';
import Drawer from '@material-ui/core/Drawer';

export const Sidebar = styled(React.forwardRef(({className, ...props}, ref) => (
  <Drawer className={className} classes={{paper: className}} {...props} ref={ref}/>
)))(({theme, open, sidebarwidth}) => ({
    flexShrink: 0,
    overflowX: open ? 'initial' : 'hidden',
    backgroundColor: 'rgb(21,36,34)',
    whiteSpace: 'nowrap',
    width: open ? sidebarwidth : 44,
    borderRight: 0,
    height: '100vh',
    '--transition': theme.transitions.create('all', {
      easing: theme.transitions.easing.sharp,
      duration: theme.transitions.duration.enteringScreen,
    }),
    '& *': {
      transition: 'var(--transition)'
    },
    transition: 'var(--transition)',
    '& .MuiPaper-root': {
      overflow: 'hidden'
    },
    '&:hover > *': {
      width: sidebarwidth,
      boxShadow: open ? 'none' : '20px 0px 20px -20px rgb(21,36,34)'
    },
  })
);
