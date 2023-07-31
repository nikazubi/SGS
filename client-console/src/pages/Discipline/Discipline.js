import { useEffect, useState } from "react";
import Chart from "./Chart";
import Dropdown from "./Dropdown";
import DisciplineBox from "./DisciplineBox";
import SmallBox from "./SmallBox";
import Button from '@mui/material/Button';
import {useUserData, MyContext, useUpdate} from "../../context/userDataContext";
import SearchIcon from '@mui/icons-material/Search';
import ResponsiveDrawer from "./Aside";

import { Box, Drawer, List, ListItem, ListItemIcon, ListItemText } from '@mui/material';
import { display, styled } from '@mui/system';

const Sidebar = styled(Drawer)(({ theme }) => ({
  transition:'.5s',
  width: '240px',
  height: '100vh !important',
  top: 'unset !important',
  flexShrink: 0,
  '& .MuiDrawer-paper': {
    width: '240px',
    boxSizing: 'border-box',
  },
  '@media (max-width: 960px)': {
    transform: 'translateX(-100%)'
  },
}));

const Discipline = () => {

    const [sidebarOpen, setSidebarOpen] = useState(false);

    const toggleSidebar = () => {
        setSidebarOpen(!sidebarOpen);
    };

    const updateData = useUpdate()
    const allStudentsData = useUserData()
    console.log(allStudentsData, 'TEST123 allStudentsData')
    const avgMonth = allStudentsData.filter((m)=>m.boxdetails[0] === 'თვის ქულა' || m.boxdetails[0] === 'გაცდენილი საათები')
    console.log(avgMonth,"AVERAGE TEST123")

    const subjectsData = ['მათემატიკა', 'ქართული', 'მუსიკა']
    const monthData = [
        'სექტემბერი',
        'ოქტომბერი',
        'ნოემბერი',
        'დეკემბერი',
        'იანვარი',
        'თებერვალი',
        'მარტი',
        'აპრილი',
        'მაისი',
        'ივნისი'
      ];
    const yearsData = [2021 ,2022, 2023]
    
    const [isButtonActive, setIsButtonActive] = useState(true)

    const [selectedSubject, setSelectedSubject] = useState('');
    const [selectedMonth, setSelectedMonth] = useState('');
    const [selectedYear, setSelectedYear] = useState('');
    const [allSelectedData, setAllSelectedData] = useState({});

    const [subject, setSubject] = useState(subjectsData)
    const [month, setMonth] = useState(monthData)
    const [year, setYear] = useState(yearsData)
    
    useEffect(()=>{

        //API for Subjects month and year
        
        //with returned data should run setSubject, setMonth setYear

    },[subject, month, year])

    useEffect(()=>{

      if(window.innerWidth <= 960){
        setSidebarOpen(false)
      }


    },[])

    const handleSearch = async () =>{

        if(selectedSubject && selectedMonth && selectedYear) {
            //API AXIOS
            const allStudentData = [
                {
                    name:'შემაჯამებელი დავალება',
                    testNumber: 'I',
                    precent: '50%',
                    boxdetails: [
                      { label: '1',
                        grade: 0,
                      },
              
                      { label: '2',
                        grade: 0,
                      },
              
                      { label: 'აღდ',
                        grade: 0,
                      },
              
                      { label: 'თვე',
                        grade: 0,
                      },
              
                      { label: '%',
                        grade: 0,
                      }
                    ]
                },
              
                {
                    name:'საშინაო დავალება',
                    testNumber: 'II',
                    precent: '30%',
                    boxdetails: [
                      { label: '1',
                        grade: 0,
                      },
              
                      { label: '2',
                        grade: 0,
                      },
              
                      { label: '3',
                        grade: 0,
                      },
              
                      { label: 'თვე',
                        grade: 0,
                      },
              
                      { label: '%',
                        grade: 0,
                      }
                    ]
                },
              
                {
                    name:'საკლასო დავალება',
                    testNumber: 'III',
                    precent: '20%',
                    boxtitle: ['1', '2', 'თვე', '%'],
                    boxdetails: [
                      { label: '1',
                        grade: 0,
                      },
                      
                      { label: '2',
                        grade: 0,
                      },
              
                      { label: 'თვე',
                        grade: 0,
                      },
              
                      { label: '%',
                        grade: 0,
                      }
                    ]
                },
              
                {   monthlyGrade: 6,
                    boxdetails: ['თვის ქულა']
                },
              
                {   gacdena: 3,
                    boxdetails: ['გაცდენილი საათები']
                }
              
              ]

                updateData(allStudentData)
            
            setAllSelectedData({selectedSubject, selectedMonth, selectedYear})
        }
    }

    return ( 

      <Box sx={{ display: 'flex' }}>
<Sidebar
// style={{display: sidebarOpen ? 'block' : 'none'}}
        variant="permanent"
        anchor="left"
        className="disciplineAside"
      >
        <List>
          {/* Add your sidebar items */}
          <ListItem button>
            <ListItemIcon>
              <SearchIcon />
            </ListItemIcon>
            <ListItemText primary="Search" />
          </ListItem>
        </List>
      </Sidebar>
      <Box component="main" sx={{ flexGrow: 1, p: 3 }}>

        <div className="ibCnt">
            <div className="ib__center column">
                <div className="pageName">მოსწავლის შეფასება საგნობრივი დისციპლინების მიხედვით</div>
                {/* <div style={{display:'flex', alignItems:'center'}}>
                <Dropdown data={subject} select={setSelectedSubject} label={'საგანი'}/>
                <Dropdown data={month} select={setSelectedMonth} label={'თვე'}/>
                <Dropdown data={year}  select={setSelectedYear} label={'წელი'}/>
                <Button onClick={handleSearch} disabled={isButtonActive} style={{ fontWeight: 'bold', height: '40px'}} variant="contained">ძიება<SearchIcon/></Button>
                </div> */}
            </div>

            {allStudentsData && <div className="termEstCnt">
                {/* id=1 means it's  შემაჯამებელი დავალება*/}
                <DisciplineBox title={'შემაჯამებელი დავალება'} number={'I'} precent={'50%'} id={1} data={allSelectedData}/>
                {/* <DisciplineBox title={'საშინაო დავალება'} number={'II'} precent={'30%'} id={2} data={allSelectedData}/>
                <DisciplineBox title={'საკლასო დავალება'} number={'III'} precent={'20%'} id={3} data={allSelectedData}/> */}
            </div>}

            {allStudentsData && <div className="ib__center" style={{gap:'30px'}}>
                {avgMonth.map(m=>{
                    return(
                    <div className="avgCnt">
                        <div style={{marginLeft:"20px"}}>{m.boxdetails[0]}</div>
                        <SmallBox boxdetails={m.boxdetails[0]} mark={m} />
                    </div>
                    )
                })}
            </div>}

            {allStudentsData && <div className="ibChart">
                <Chart/>
            </div> }

        </div>
        </Box>
        </Box>
     );
}
 
export default Discipline;