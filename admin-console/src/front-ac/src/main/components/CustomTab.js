import React, { useState } from 'react';
import { IconButton, Tab } from '@material-ui/core';
import { Close as CloseIcon } from '@material-ui/icons';
import TabContextMenu from './TabContextMenu';
import { makeStyles } from '@material-ui/core/styles';
import { theme } from "../../assets/theme";

const useStyles = makeStyles({
  root: {
    '& .MuiTab-wrapper': {
      whiteSpace: 'nowrap',
      overflow: 'hidden',
      textOverflow: 'ellipsis',
      display: "flex",
      flexDirection: "row",
      alignItems: "center",
      marginLeft: "-2px",

      '& .MuiSvgIcon-root': {
        marginRight: "6px",
        marginTop: "6px"
      }
    },
    '& svg': {
      fontSize: 20,
      color: theme.navigation.text.main
    },
    '& > *:not(.Mui-selected) svg': {
      fontSize: 20,
      color: theme.navigation.text.secondary
    },
    '& .MuiTab-root': {
      color: theme.navigation.text.secondary,
      backgroundColor: theme.navigation.background.secondary,
      minHeight: 50
    },
    '& .Mui-selected': {
      backgroundColor: theme.navigation.background.main,
      color: theme.navigation.text.main,
      fontWeight: 'bold',
    },
    '& + .page-tab .MuiTab-root': {
      boxShadow: '1px 0 0 0 #0365a5ad inset'
    },
  }
});

// custom tab component
const TabComponent = React.forwardRef((props, ref) => {
  const classes = useStyles();
  const [closeIconVisibility, setCloseIconVisibility] = useState("hidden")
  return (
    <div className={`page-tab ${classes.root}`}
      onMouseOut={e => {
        setCloseIconVisibility("hidden");
      }}
      onMouseOver={e => {
        setCloseIconVisibility("visible")
      }}>
      <div ref={ref} {...props}>
        {props.children}
        <IconButton
          style={{ marginLeft: 3, visibility: closeIconVisibility}}
          size='small'
          onClick={e => {
            e.stopPropagation();
            props.onClose();
          }}
        >
          {props.onClose && <CloseIcon/>}
        </IconButton>
      </div>
    </div>
  );
});

const initialState = {
  mouseX: null,
  mouseY: null,
};

export const CustomTabComponent = ({ tab, value, onClose, onCloseAll, onCloseOthers, ...rest }) => {
  const [menuState, setMenuState] = useState(initialState);
  const handleClose = tab.id === 'GRADES' || tab.id === 'BEHAVIOUR' || tab.id === 'ABSENCE' ? null : onClose;

  const handleMenuOpen = (event) => {
    event.preventDefault();
    setMenuState({
      mouseX: event.clientX - 2,
      mouseY: event.clientY - 4,
    });
  };

  const handleMenuClose = () => {
    setMenuState(initialState);
  };

  return (
    <>
      <Tab
        label={tab.label}
        component={TabComponent}
        onClose={handleClose}
        value={value}
        onContextMenu={handleMenuOpen}
        {...rest}
      />
      <TabContextMenu
        onClose={handleClose}
        onCloseAll={onCloseAll}
        onCloseOthers={onCloseOthers}
        onCloseMenu={handleMenuClose}
        menuState={menuState}
      />
    </>
  );
};
