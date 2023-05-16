import React from "react";
import Avatar from "@material-ui/core/Avatar";
import makeStyles from "@material-ui/core/styles/makeStyles";
import FlexBox from "../FlexBox";

const useStyles = makeStyles((theme) => ({
  text: {
    fontSize: theme.fontSize,
    marginTop: 'auto',
    marginBottom: 'auto',
    marginLeft: 5,
    marginRight: 5
  }
}));

const AvatarWithLabel = ({avatar, imagePlacement, classes, label, labelColor, alt, ...rest}) => {
  const innerClasses = useStyles();
  const imagePlacementStyles = imagePlacement === "end" ? {justifyContent: "space-between", width: "96%"} : {};
  return (
    <FlexBox sx={{...imagePlacementStyles}}>
      {imagePlacement !== "end" && (
        <Avatar classes={{root: classes ? classes.avatar : ''}} src={avatar} {...rest} >
          {alt?.toUpperCase() || label?.charAt(0)?.toUpperCase()}
        </Avatar>
      )}
      <div className={classes ? classes.text : innerClasses.text}>
        {label}
      </div>
      {imagePlacement === "end" && (
        <Avatar classes={{root: classes ? classes.avatar : ''}} src={avatar} {...rest} >
          {alt?.toUpperCase() || label?.charAt(0)?.toUpperCase()}
        </Avatar>
      )}
    </FlexBox>
  );
};

export default AvatarWithLabel;