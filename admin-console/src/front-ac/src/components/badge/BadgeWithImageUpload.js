import React from "react";
import Badge from "@material-ui/core/Badge";
import { PhotoCamera } from "@material-ui/icons";
import IconButton from "../buttons/IconButton";
import makeStyles from "@material-ui/core/styles/makeStyles";

const useStyles = makeStyles((theme) => ({
  input: {
    display: 'none'
  },
  button: {
    backgroundColor: "#FFFFFF"
  }
}));

const BadgeWithImageUpload = ({children, inputId, resolveImageUpload, onImageUpload}) => {
  const classes = useStyles();

  const handleChange = async event => {
    const reader = new FileReader();
    let imageFile = event.target.files[0];

    if (resolveImageUpload) {
      imageFile = await resolveImageUpload(imageFile);
    }
    reader.readAsDataURL(imageFile);
    reader.onloadend = () => onImageUpload(reader.result.split(',')[1]);
  };

  return (
    <Badge
      overlap="circle"
      anchorOrigin={{
        vertical: 'bottom',
        horizontal: 'right',
      }}
      badgeContent={<div>
        <input accept="image/*" className={classes.input} id={inputId} type="file" onChange={handleChange}/>
        <label htmlFor={inputId}>
          <IconButton component="span"
                      color="primary"
                      className={classes.button}
                      icon={<PhotoCamera/>}/>
        </label>
      </div>}>
        {children}
    </Badge>
  );
};

export default BadgeWithImageUpload;
