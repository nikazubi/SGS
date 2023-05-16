import makeStyles from "@material-ui/core/styles/makeStyles";
import FlexBox from "../FlexBox";
import { Tooltip } from "@material-ui/core";
import Avatar from "@material-ui/core/Avatar";
import React from "react";

const useStyles = makeStyles((theme) => ({
  title: {
    fontSize: theme.fontSize,
    marginTop: 'auto',
    marginBottom: 'auto',
    marginLeft: 5,
    marginRight: 5
  },
  description: {
    fontSize: theme.fontSize,
    marginTop: 'auto',
    marginBottom: 'auto',
    marginLeft: 5,
    marginRight: 5,
    color: 'black',
    opacity: '.4'
  },
  avatar: {
    marginRight: 7
  },
}));

const AvatarWithTitleAndDescription = ({avatar, classes, title, description, labelColor, alt, ...rest}) => {
  const innerClasses = useStyles();
  return (
    <FlexBox style={{alignItems: 'center'}}>
      {
        <Avatar classes={{root: classes ? classes.avatar : innerClasses.avatar}} src={avatar} {...rest} >
          {alt?.toUpperCase() || title?.charAt(0)?.toUpperCase()}
        </Avatar>
      }
      <div style={{width: '80%', display: 'flex', flexDirection: 'column'}}>
        <Tooltip title={title} style={{whiteSpace: 'nowrap', textOverflow: 'ellipsis', overflow: 'hidden'}}>
          <label className={classes ? classes.text : innerClasses.title}>
            {title}
          </label>
        </Tooltip>

        <Tooltip title={description} style={{whiteSpace: 'nowrap', textOverflow: 'ellipsis', overflow: 'hidden'}}>
          <label className={classes ? classes.text : innerClasses.description}>
            {description}
          </label>
        </Tooltip>
      </div>
    </FlexBox>
  );
};

export default AvatarWithTitleAndDescription;