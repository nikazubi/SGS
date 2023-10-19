import {useEffect, useRef, useState} from "react";
import Chart from "./Chart";
import {NavLink} from 'react-router-dom'
import {MyContext, useUpdate} from "../../context/userDataContext";
import HomeIcon from '@mui/icons-material/Home';
import {Box, List, ListItem, ListItemIcon} from '@mui/material';
import Sidebar from "../../utils/Sidebar";
import DisciplineBox from "../ethicalPage/DisciplineBox";
import TextField from "@mui/material/TextField";
import MenuItem from "@mui/material/MenuItem";
import Button from "@mui/material/Button";
import SearchIcon from "@mui/icons-material/Search";
import {MONTHS} from "../utils/date";
import useSubjects from "./useSubjects";


const Discipline = ({match}) => {
    const id = match.params.id
    const [sidebarOpen, setSidebarOpen] = useState(false);
    const [selectedData, setSelectedData] = useState([MONTHS[new Date().getUTCMonth()].value]);
    const [currentData, setCurrentData] = useState();
    const {data: subjectData, isLoading, isError, error, isSuccess} = useSubjects();
//TODO getGradeForSubject

    useEffect(() => {
        console.log(subjectData);
    }, [subjectData])

    const asideRef = useRef();
    const backgroundRef = useRef();
    const hamburgerIcon = useRef();

    const month = [
        'სექტემბერი-ოქტომბერი',
        'ნოემბერი',
        'დეკემბერი',
        'იანვარი-თებერვალი',
        'მარტი',
        'აპრილი',
        'მაისი',
        'ივნისი'
    ];
    const handleResize = () => {

        // setWidth(window.innerWidth)
        // if(!!sidebarOpen){
        //   asideRef.current.classList.add('open');
        // }
        // else{
        //   asideRef.current.classList.remove('open');
        // }
        asideRef.current.classList.add('open')
        // asideRef.current.classList.remove('open');
        // hamburgerIcon.current.classList.remove("open")
        // if(!!sidebarOpen){
        //   setSidebarOpen(!sidebarOpen);
        // }

        // backgroundRef.current.classList.remove("open")

    };

    const data = [
        {
            name: 'შემაჯამებელი დავალება I - 50%',
            testNumber: null,
            precent: null,
            month: null,
            monthGrade: null,
            absence: null,
            absenceGrade: null,
            boxdetails: [
                {
                        label: '1',
                        grade: 5,
                    },

                    {
                        label: '2',
                        grade: 6,
                    },

                    { label: 'აღდ',
                        grade: 0,
                    },

                    {
                        label: 'თვე',
                        grade: 6,
                    },
                    {
                        label: '%',
                        grade: 3.5,
                    },

                ]
            },

            {
                name:'შემოქმედებითობა II - 20%',
                testNumber: null,
                precent: null,
                month: null,
                monthGrade: null,
                absence: null,
                absenceGrade: null,
                boxdetails: [
                    {
                        label: '1',
                        grade: 7,
                    },

                    {
                        label: '2',
                        grade: 7,
                    },

                    {
                        label: 'თვე',
                        grade: 7,
                    },
                    {
                        label: '%',
                        grade: 1.4,
                    },

                ]
            },

            {
                name:'საშინაო დავალება III - 30%',
                testNumber: null,
                precent: null,
                month: null,
                monthGrade: null,
                absence: null,
                absenceGrade: null,
                boxdetails: [
                    {
                        label: '1',
                        grade: 6,
                    },

                    {
                        label: '2',
                        grade: 6,
                    },

                    {
                        label: 'თვე',
                        grade: 6,
                    },
                    {
                        label: '%',
                        grade: 1.8,
                    },

                ]
            },

            {
                name: '',
                testNumber: null,
                precent: null,
                month: 'თვის ქულა: ',
                monthGrade: 7,
                absence: null,
                absenceGrade: null,
                boxdetails: []
            },
            {
                name: '',
                testNumber: null,
                precent: null,
                month: null,
                monthGrade: null,
                absence: 'გაცდენილი საათები: ',
                absenceGrade: 6,
                boxdetails: []
            },

        ]

    const toggleSidebar = () => {
      hamburgerIcon.current.classList.toggle("open")
      backgroundRef.current.classList.toggle("open")
      asideRef.current.classList.toggle('open')
      setSidebarOpen(!sidebarOpen);
    };

    const handleChange = (event) => {
        setSelectedData(event.target.value);
    };

    function dropdown(){
        return (
            <div className="yearDropwdown">
                <TextField
                    select
                    label="აირჩიე თვე"
                    value={selectedData}
                    onChange={handleChange}
                    variant="outlined"
                >
                    {month.map((m) => (
                        <MenuItem key={m} value={m}>
                            {m}
                        </MenuItem>
                    ))}
                </TextField>&nbsp;&nbsp;&nbsp;
                <Button onClick={()=>{}} disabled={!selectedData} style={{ fontWeight: 'bold', height: '50px'}} variant="contained">ძიება<SearchIcon/></Button>
            </div>
        );
    }


    const subjects = ['ქართული ენა და ლიტერატურა', 'მათემატიკა', 'ინგლისური', 'ისტორია', 'გეოგრაფია'];

    const renderSubjects = () => {

      return subjects.map(subject=>(
        <NavLink exact onClick={toggleSidebar} className="aside_aTag" to={`/shefaseba-akademiuri-sagnobrivi-disciplinis-mixedvit/${subject}`} activeClassName="active">
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
          <NavLink exact onClick={toggleSidebar} className="aside_aTag home" to="/">
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
              <div style={{display: "flex"}}>
                  {dropdown()}
              </div>
              <div className="termEstCnt">
                  <DisciplineBox data={data}/>
              </div>
              <div className="ibChart" style={{marginLeft: -50, marginTop:65}}>
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