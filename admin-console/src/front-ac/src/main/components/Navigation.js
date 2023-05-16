import React, { Fragment } from 'react';
import { Collapse, Divider, ListItem, ListItemIcon, ListItemText } from "@material-ui/core";
import { ExpandLess, ExpandMore } from "@material-ui/icons";
import { Link } from "react-router-dom";
import { makeStyles } from "@material-ui/core/styles";
import { theme } from "../../assets/theme";
import { useSidebarContext } from "../../contexts/sidebar-context";


const useStyles = makeStyles({
  listItem: {
    color: '#0e7fb7',
    textDecoration: 'none',
    paddingLeft: ({indent, offset, level, isSidebarExpanded}) => isSidebarExpanded
                                                                 ? indent + offset * level
                                                                 : indent,
    "&:hover": {
      backgroundColor: theme.navigation.background.main,
    },
    ".MuiDrawer-root:hover &": {
      paddingLeft: ({indent, offset, level}) => indent + offset * level
    },

  },
  listIcon: {
    color: '#0e7fb7',
    minWidth: 35
  },
  divider: {
    backgroundColor: '#0e7fb7',
    ".MuiDrawer-root:hover &": {
      marginLeft: ({indent, offset, level}) => indent + offset * level,
    },
    marginLeft: ({indent, offset, level, isSidebarExpanded }) => isSidebarExpanded ? indent + offset * level : 0,

  }
});


const Navigation = ({
                      indent = 10,
                      offset = 10,
                      level = 0,
                      pages,
                      expandedItems,
                      onExpandToggle
                    }) => {

  const {isSidebarExpanded} = useSidebarContext();
  const classes = useStyles({indent, offset, level, isSidebarExpanded});
  let index = -1;
  return (
    <>
      {pages.filter(page => page.show).map(page =>  {
        index = index + 1;

        const child = (
          <div>
            <ListItem classes={{root: classes.listItem}}
                      button
                      key={page.id}
                      onClick={page.collapsable ?
                        () => onExpandToggle(page.id) : null
                      }
            >
              <ListItemIcon classes={{root: classes.listIcon}}>
                {page.icon}
              </ListItemIcon>
              <ListItemText primary={page.label}/>
              {page.collapsable && (expandedItems.includes(page.id) ? <ExpandLess/> : <ExpandMore/>)}
            </ListItem>
          </div>
        );

        return (
          <Fragment key={page.id} >
            {page.collapsable
              ? child
              : (
                <Link
                  key={page.id}
                  to={`/${page.id}`}
                  style={{
                    color: '#0e7fb7',
                    textDecoration: 'none'
                  }}
                >
                  {child}
                </Link>
              )}
            {page.collapsable && (
              <Collapse
                in={expandedItems.includes(page.id)}
                timeout="auto"
              >
                <Divider classes={{root: classes.divider}} />
                <Navigation
                  expandedItems={expandedItems}
                  pages={Object.values(page.options).map(option => option).filter(option => option.show)}
                  onExpandToggle={onExpandToggle}
                  level={level + 1}
                />
              </Collapse>)}
          </Fragment>
        );
      })}
    </>
  );
};

export default Navigation;