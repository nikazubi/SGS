import { useEffect, useRef, useState } from "react";
import Chart from "./Chart";
import {NavLink} from 'react-router-dom'
import {useUserData, MyContext, useUpdate} from "../../context/userDataContext";
import HomeIcon from '@mui/icons-material/Home';
import { Box, List, ListItem, ListItemIcon } from '@mui/material';
import Sidebar from "../../utils/Sidebar";


const Discipline = ({ match }) => {
    const id = match.params.id
    const [sidebarOpen, setSidebarOpen] = useState(false);

    const asideRef = useRef();
    const backgroundRef = useRef();
    const hamburgerIcon = useRef();
    const allStudentsData = useUserData()
    const [width, setWidth] = useState(window.innerWidth);
    
      const handleResize = () => {
        setWidth(window.innerWidth)
        if(window.innerWidth <= 960){
          
          if(!!sidebarOpen){
            console.log('a TEST123')
            asideRef.current.classList.add('open');
          }
          else{
            console.log('b TEST123')
            asideRef.current.classList.remove('open');
          }
        }
        else{
          asideRef.current.classList.remove('open');
          hamburgerIcon.current.classList.remove("open")
          if(!!sidebarOpen){
            setSidebarOpen(!sidebarOpen);
          }
          console.log('backgroundRef.current', backgroundRef.current)
          backgroundRef.current.classList.remove("open")
        }
      };
    
    useEffect(() => {
      window.addEventListener('resize', handleResize);
      if(width <= 960){
        document.querySelector('.mtavari').style.display = 'none';
      }
      else{
        document.querySelector('.mtavari').style.display = 'block';
      }
      return () => {
        window.removeEventListener('resize', handleResize);
        
      };
    }, [width]);

    const toggleSidebar = () => {
      hamburgerIcon.current.classList.toggle("open")
      backgroundRef.current.classList.toggle("open")
      asideRef.current.classList.toggle('open')
      setSidebarOpen(!sidebarOpen);
    };

    const subjects = ['ქართული', 'მათემატიკა'];

    const renderSubjects = () => {

      return subjects.map(subject=>(
        <NavLink exact onClick={width <= 960 ? toggleSidebar : ''} className="aside_aTag" to={`/shefaseba-akademiuri-sagnobrivi-disciplinis-mixedvit/${subject}`} activeClassName="active">
          <ListItem button>
          <div>{subject}</div>
          </ListItem>
        </NavLink>
      ))

    }

    return ( 
      <Box sx={{ display: 'flex' }}>
        <Sidebar
          variant="permanent"
          anchor="left"
          className="disciplineAside"
          ref={asideRef}
        >
          <List>
          <NavLink exact onClick={width <= 960 ? toggleSidebar : ''} className="aside_aTag home" to="/">
            <ListItem button>
                <ListItemIcon>
                    <HomeIcon />
                    <div>მთავარი</div>
                </ListItemIcon>
            </ListItem>
            </NavLink>
            {/* Add your sidebar items */}
            {renderSubjects()}
          </List>
        </Sidebar>
        <Box component="main" sx={{ flexGrow: 1, p: 3 }}>

          <div className="ibCnt">
              <div className="ib__center column">
                  <div className="pageName">მოსწავლის შეფასება საგნობრივი დისციპლინების მიხედვით</div>
                  <div className="pageName">{id}</div>
              </div>
              <div className="ibChart">
                  <Chart id={id}/>
              </div>
          </div>
          </Box>
          <div ref={hamburgerIcon} onClick={toggleSidebar} id="nav-icon3">
                  <span></span>
                  <span></span>
                  <span></span>
                  <span></span>
          </div>
          <div onClick={toggleSidebar} ref={backgroundRef} className="ib_opacityBackground"></div>
        </Box>
     );
}
 
export default Discipline;