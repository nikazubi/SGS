import makeStyles from "@material-ui/core/styles/makeStyles";
import {Link, useLocation} from "react-router-dom";
import React from 'react';
import UserBar from './UserBar';
import {useQuery} from "react-query";
import {fetchParentModules} from "../pages/journal/parentApi";


const Header = () => {
    const location = useLocation();
    const isNotLoginPage = location.pathname !== '/login'

    // Header mounts only once logged in, so this token-bearing call is always
    // safe here. Shares its cache with AfterLoginPage's identical query key -
    // this never triggers a second request on its own.
    const {data: modules} = useQuery(
        ["PARENT_MODULES"], fetchParentModules, {refetchOnWindowFocus: false});
    const isPrimary = (modules || []).includes("SCHEDULE");
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


    return !isNotLoginPage ? "" : (
        <header className={isPrimary ? 'header--primary' : undefined}>
            <div className={`headerCnt ${!isNotLoginPage ? '' : 'home'}`}>
                <div style={{display: 'flex'}}>
                    <Link className="headerCnt__aTag" to="/">
                        {/* <ArrowBackIcon className='headerCnt__arrow'/> */}
                        <div className='mtavari' style={{marginLeft:'15px'}}>მთავარი</div>
                    </Link>
                    <Link to="/"><div className='headerLogoImg'></div></Link>
                </div>
                <div>
                    <Link className="headerCnt__aTag" to="" >
                        <UserBar />
                    </Link>
                </div>
            </div>
        </header>
     );
}
 
export default Header;