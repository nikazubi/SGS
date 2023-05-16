import React, { useState } from "react";
import IconButton from "../../components/buttons/IconButton";
import { ExitToApp, Person } from "@material-ui/icons";
import FlexBox from "../../components/FlexBox";
import Menu from "@material-ui/core/Menu";
import MenuItem from "@material-ui/core/MenuItem";
import ListItemIcon from "@material-ui/core/ListItemIcon";
import ListItemText from "@material-ui/core/ListItemText";
import Avatar from "@material-ui/core/Avatar";
import makeStyles from "@material-ui/core/styles/makeStyles";
import AvatarWithLabel from "../../components/avatar/AvatarWithLabel";
// import UserFormModal from "../pages/Users/UserFormModal";
import BadgeWithImageUpload from "../../components/badge/BadgeWithImageUpload";
import { useUserContext } from "../../contexts/user-context";
import { compressImage } from "../../utils/helpers";
import { useToggle } from "../../hooks/useToggle";
// import useUpdateUser from "../pages/Users/hooks/useUpdateUser";
// import { modalOpenModes } from "../../utils/enums";


const UserBar = () => {
  const useStyles = makeStyles((theme) => ({
    avatar: {
      width: theme.spacing(4),
      height: theme.spacing(4),
    },
    bigAvatar: {
      width: theme.spacing(15),
      height: theme.spacing(15),
    },
    text: {
      color: '#000000',
      fontSize: 15,
      marginTop: 'auto',
      marginBottom: 'auto',
      marginLeft: 5,
      marginRight: 5,
    },
    menuitem: {
      '&:focus': {
        backgroundColor: theme.palette.common.white,
        '& .MuiListItemIcon-root': {
          color: theme.palette.primary.main,
        },
      },
      justifyContent: "center",
      minWidth: "250px"
    }
  }));

  const classes = useStyles();
  // const {mutateAsync: onUpdate} = useUpdateUser();
  const {user, logout, userUpdated} = useUserContext();
  const avatar = user.avatarUrl;
  const {open: modalOpen, handleOpen: handleModalOpen, handleClose: handleModalClose} = useToggle();
  const [anchorEl, setAnchorEl] = useState(null);
  const imgZipOpts = {
    maxSizeMB: 0.1,
    maxWidthOrHeight: 400,
    useWebWorker: true
  };

  const handleToggle = (event) => {
    setAnchorEl(event.currentTarget);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
  };


  return (
    <FlexBox>
      <IconButton color="secondary"
                  component="span"
                  size="small"
                  aria-controls="user-bar-menu"
                  aria-haspopup="true"
                  onClick={handleToggle}
                  icon={<AvatarWithLabel avatar={avatar} label={user.name} classes={classes}/>}/>
      <Menu
        id="user-bar-menu"
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={handleMenuClose}
      >
        <MenuItem classes={{root: classes.menuitem}} disableTouchRipple={true}>
          <BadgeWithImageUpload inputId="user-bar-image-upload"
                                resolveImageUpload={(img) => compressImage(img, imgZipOpts)}
                                onImageUpload={(avatar) => {
                                  // user.avatar = avatar;
                                  // onUpdate(user).then(value => {
                                  //   userUpdated(value);
                                  // });
                                }}>
            <Avatar classes={{root: classes.bigAvatar}} src={avatar}/>
          </BadgeWithImageUpload>
        </MenuItem>
        <MenuItem classes={{root: classes.menuitem}} onClick={() => {
          handleMenuClose();
          handleModalOpen();
        }}>
          <ListItemIcon>
            <Person/>
          </ListItemIcon>
          <ListItemText primary={"პროფილის რედაქტირება"}/>
        </MenuItem>
        <MenuItem classes={{root: classes.menuitem}} onClick={logout}>
          <ListItemIcon>
            <ExitToApp/>
          </ListItemIcon>
          <ListItemText primary={"გამოსვლა"}/>
        </MenuItem>
      </Menu>
      {/*{modalOpen && (*/}
      {/*  <UserFormModal*/}
      {/*    open={modalOpen}*/}
      {/*    data={user}*/}
      {/*    modalOpenMode={modalOpenModes.EDIT_MODE}*/}
      {/*    isDataTableModal={false}*/}
      {/*    onClose={handleModalClose}*/}
      {/*  />*/}
      {/*)}*/}
    </FlexBox>
  );
};

export default UserBar;
