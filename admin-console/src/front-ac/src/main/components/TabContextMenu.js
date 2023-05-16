import React from 'react';
import MenuItem from '@material-ui/core/MenuItem';
import Menu from '@material-ui/core/Menu';


const TabContextMenu = ({ onClose, onCloseAll, onCloseOthers, onCloseMenu, menuState }) => {

  return (
  <Menu
    keepMounted
    open={menuState.mouseY !== null}
    onClose={onCloseMenu}
    anchorReference="anchorPosition"
    anchorPosition={
      menuState.mouseY !== null && menuState.mouseX !== null
        ? { top: menuState.mouseY, left: menuState.mouseX }
        : undefined
    }
  >
    {onClose && <MenuItem onClick={onClose}>{"დახურვა"}</MenuItem>}
    {onClose && <MenuItem onClick={onCloseAll}>{"ყველას დახურვა"}</MenuItem>}
    <MenuItem onClick={() => {
      onCloseOthers();
      onCloseMenu();
    }}
    >
      {"სხვა ტაბების დახურვა"}
    </MenuItem>
  </Menu>);
};

export default TabContextMenu;
