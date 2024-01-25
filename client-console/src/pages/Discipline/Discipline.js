import React, {useCallback, useMemo, useRef, useState} from "react";
import Chart from "./Chart";
import {NavLink} from 'react-router-dom'
import {MyContext} from "../../context/userDataContext";
import HomeIcon from '@mui/icons-material/Home';
import {Box, List, ListItem, ListItemIcon} from '@mui/material';
import Sidebar from "../../utils/Sidebar";
import DisciplineBox from "../ethicalPage/DisciplineBox";
import TextField from "@mui/material/TextField";
import MenuItem from "@mui/material/MenuItem";
import Button from "@mui/material/Button";
import SearchIcon from "@mui/icons-material/Search";
import {MONTHS, YEAR} from "../utils/date";
import useSubjects from "./useSubjects";
import useGrades from "./useGrades";
import useMonthlyGradesOfSubjectAndStudent from "./useMonthlyGradesOfSubjectAndStudent";
import {useUserContext} from "../../context/user-context";
import useTransit from "./useTransit";
import useFetchYear from "../semestruli-shefaseba/useYear";


const Discipline = ({match}) => {
    const id = match.params.id
    const [sidebarOpen, setSidebarOpen] = useState(false);
    const [selectedData, setSelectedData] = useState(MONTHS[new Date().getUTCMonth()]);
    const [chosenYear, setChosenYear] = useState(new Date().getUTCFullYear());
    const {user, isTransit} = useUserContext();
    const {data: subjectData, isLoading, isError, error, isSuccess} = useSubjects();
    const chosenSubject = useMemo(
        () => id && subjectData? subjectData.filter((data) => data.name === id)[0] : {},
        [id, subjectData]
    );
    const chosenMonth = useMemo(
        () => selectedData? MONTHS.filter((month) => month.value === selectedData.value)[0] : new Date().getUTCMonth(),
        [selectedData]
    );

    const choosenYeeear = useMemo(
        () => chosenYear? YEAR.filter((year) => year.value === chosenYear)[0] : new Date().getUTCFullYear(),
        [chosenYear]
    );
    const {data: gradeData, isLoading: isGradesLoading} = useGrades({month: chosenMonth.key, subject: chosenSubject, year: choosenYeeear.key});
    const {data: monthData, isLoading: isMonthLoading} = useMonthlyGradesOfSubjectAndStudent({subject: chosenSubject});
    const {data: transit} = useTransit();

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

    const year = [
        2023,
        2024,
        2025,
        2026,
        2027,
        2028,
        2029,
        2030,
        2031
    ];

    const handleResize = () => {
        asideRef.current.classList.add('open')
    };

    const columns = useMemo(
        () => {
            if(!gradeData){
                return []
            }
            const generalSummaryAssignmentGrades = gradeData?.filter((grade) => grade.gradeType?.toString().includes("GENERAL_SUMMARY_ASSIGMENT"))
            const generalSchoolWorkGrades = gradeData?.filter((grade) => grade.gradeType?.toString().includes("GENERAL_SCHOOL_WORK"))
            const generalHomeWorkGrades = gradeData?.filter((grade) => grade.gradeType?.toString().includes("GENERAL_HOMEWORK_"))
            const completeMonthly = gradeData?.filter((grade) => grade.gradeType?.toString().includes("GENERAL_COMPLETE_MONTHLY"))
            return [
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
                            grade: generalSummaryAssignmentGrades?.filter((grade) => grade.gradeType.toString() === "GENERAL_SUMMARY_ASSIGMENT_1")[0]?.value || "",
                        },

                        {
                            label: '2',
                            grade: generalSummaryAssignmentGrades?.filter((grade) => grade.gradeType.toString() === "GENERAL_SUMMARY_ASSIGMENT_2")[0]?.value || "",
                        },

                        { label: 'აღდ',
                            grade: generalSummaryAssignmentGrades?.filter((grade) => grade.gradeType.toString() === "GENERAL_SUMMARY_ASSIGMENT_RESTORATION")[0]?.value || "",
                        },

                        {
                            label: 'თვე',
                            grade: generalSummaryAssignmentGrades?.filter((grade) => grade.gradeType.toString() === "GENERAL_SUMMARY_ASSIGMENT_MONTH")[0]?.value || "",
                        },
                        {
                            label: '%',
                            grade: generalSummaryAssignmentGrades?.filter((grade) => grade.gradeType.toString() === "GENERAL_SUMMARY_ASSIGMENT_PERCENT")[0]?.value || "",
                        },

                    ]
                },
                {
                    name: 'შემოქმედებითობა II - 30%',
                    testNumber: null,
                    precent: null,
                    month: null,
                    monthGrade: null,
                    absence: null,
                    absenceGrade: null,
                    boxdetails: [
                        {
                            label: '1',
                            grade: generalSchoolWorkGrades?.filter((grade) => grade.gradeType.toString() === "GENERAL_SCHOOL_WORK_1")[0]?.value || "",
                        },

                        {
                            label: '2',
                            grade: generalSchoolWorkGrades?.filter((grade) => grade.gradeType.toString() === "GENERAL_SCHOOL_WORK_2")[0]?.value || "",
                        },
                        {
                            label: '3',
                            grade: generalSchoolWorkGrades?.filter((grade) => grade.gradeType.toString() === "GENERAL_SCHOOL_WORK_3")[0]?.value || "",
                        },

                        {
                            label: '4',
                            grade: generalSchoolWorkGrades?.filter((grade) => grade.gradeType.toString() === "GENERAL_SCHOOL_WORK_4")[0]?.value || "",
                        },

                        {
                            label: '5',
                            grade: generalSchoolWorkGrades?.filter((grade) => grade.gradeType.toString() === "GENERAL_SCHOOL_WORK_5")[0]?.value || "",
                        },
                        {
                            label: 'თვე',
                            grade: generalSchoolWorkGrades?.filter((grade) => grade.gradeType.toString() === "GENERAL_SCHOOL_WORK_MONTH")[0]?.value || "",
                        },
                        {
                            label: '%',
                            grade: generalSchoolWorkGrades?.filter((grade) => grade.gradeType.toString() === "GENERAL_SCHOOL_WORK_PERCENT")[0]?.value || "",
                        },

                    ]
                },
                {
                    name: 'საშინაო დავალება III - 20%',
                    testNumber: null,
                    precent: null,
                    month: null,
                    monthGrade: null,
                    absence: null,
                    absenceGrade: null,
                    boxdetails: [
                        {
                            label: '1',
                            grade: generalHomeWorkGrades?.filter((grade) => grade.gradeType.toString() === "GENERAL_HOMEWORK_WRITE_ASSIGMENT_1")[0]?.value || "",
                        },

                        {
                            label: '2',
                            grade: generalHomeWorkGrades?.filter((grade) => grade.gradeType.toString() === "GENERAL_HOMEWORK_WRITE_ASSIGMENT_2")[0]?.value || "",
                        },
                        {
                            label: '3',
                            grade: generalHomeWorkGrades?.filter((grade) => grade.gradeType.toString() === "GENERAL_HOMEWORK_WRITE_ASSIGMENT_3")[0]?.value || "",
                        },

                        {
                            label: '4',
                            grade: generalHomeWorkGrades?.filter((grade) => grade.gradeType.toString() === "GENERAL_HOMEWORK_WRITE_ASSIGMENT_4")[0]?.value || "",
                        },
                        {
                            label: 'თვე',
                            grade: generalHomeWorkGrades?.filter((grade) => grade.gradeType.toString() === "GENERAL_HOMEWORK_MONTHLY")[0]?.value || "",
                        },
                        {
                            label: '%',
                            grade: generalHomeWorkGrades?.filter((grade) => grade.gradeType.toString() === "GENERAL_HOMEWORK_WRITE_ASSIGMENT_PERCENT")[0]?.value || "",
                        },

                    ]
                },
                {
                    name: 'თვის ქულა',
                    testNumber: null,
                    precent: null,
                    month: null,
                    monthGrade: null,
                    absence: null,
                    absenceGrade: null,
                    boxdetails: [
                        {
                            label: '',
                            grade: completeMonthly[0]?.value ? completeMonthly[0]?.value === -50 ? 'ჩთ' : completeMonthly[0]?.value : "",
                        },
                    ]
                }
            ]
            // GENERAL_SUMMARY_ASSIGMENT
        },
        [gradeData]
    )
    console.log("transittransittransit", transit)
    const columnsTransit = useMemo(
        () => {
            if(!gradeData){
                return []
            }

            const generalSummaryAssignmentGrades = gradeData?.filter((grade) => grade.gradeType?.toString().includes("TRANSIT_SUMMARY_ASSIGMENT"))
            const generalSchoolWorkGrades = gradeData?.filter((grade) => grade.gradeType?.toString().includes("TRANSIT_SCHOOL_WORK"))
            const completeMonthly = gradeData?.filter((grade) => grade.gradeType?.toString().includes("TRANSIT_SCHOOL_COMPLETE_MONTHLY"))
            return [
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
                            grade: generalSummaryAssignmentGrades?.filter((grade) => grade.gradeType.toString() === "TRANSIT_SUMMARY_ASSIGMENT_1")[0]?.value || "",
                        },

                        {
                            label: '2',
                            grade: generalSummaryAssignmentGrades?.filter((grade) => grade.gradeType.toString() === "TRANSIT_SUMMARY_ASSIGMENT_2")[0]?.value || "",
                        },

                        { label: 'აღდ',
                            grade: generalSummaryAssignmentGrades?.filter((grade) => grade.gradeType.toString() === "TRANSIT_SUMMARY_ASSIGMENT_RESTORATION")[0]?.value || "",
                        },

                        {
                            label: 'თვე',
                            grade: generalSummaryAssignmentGrades?.filter((grade) => grade.gradeType.toString() === "TRANSIT_SUMMARY_ASSIGMENT_MONTH")[0]?.value || "",
                        },
                        {
                            label: '%',
                            grade: generalSummaryAssignmentGrades?.filter((grade) => grade.gradeType.toString() === "TRANSIT_SUMMARY_ASSIGMENT_PERCENT")[0]?.value || "",
                        },

                    ]
                },
                {
                    name: 'საკლასო სამუშაო II - 30%',
                    testNumber: null,
                    precent: null,
                    month: null,
                    monthGrade: null,
                    absence: null,
                    absenceGrade: null,
                    boxdetails: [
                        {
                            label: '1',
                            grade: generalSchoolWorkGrades?.filter((grade) => grade.gradeType.toString() === "TRANSIT_SCHOOL_WORK_1")[0]?.value || "",
                        },

                        {
                            label: '2',
                            grade: generalSchoolWorkGrades?.filter((grade) => grade.gradeType.toString() === "TRANSIT_SCHOOL_WORK_2")[0]?.value || "",
                        },
                        {
                            label: '3',
                            grade: generalSchoolWorkGrades?.filter((grade) => grade.gradeType.toString() === "TRANSIT_SCHOOL_WORK_3")[0]?.value || "",
                        },

                        {
                            label: '4',
                            grade: generalSchoolWorkGrades?.filter((grade) => grade.gradeType.toString() === "TRANSIT_SCHOOL_WORK_4")[0]?.value || "",
                        },

                        {
                            label: '5',
                            grade: generalSchoolWorkGrades?.filter((grade) => grade.gradeType.toString() === "TRANSIT_SCHOOL_WORK_5")[0]?.value || "",
                        },
                        {
                            label: '6',
                            grade: generalSchoolWorkGrades?.filter((grade) => grade.gradeType.toString() === "TRANSIT_SCHOOL_WORK_6")[0]?.value || "",
                        },
                        {
                            label: '7',
                            grade: generalSchoolWorkGrades?.filter((grade) => grade.gradeType.toString() === "TRANSIT_SCHOOL_WORK_7")[0]?.value || "",
                        },
                        {
                            label: '8',
                            grade: generalSchoolWorkGrades?.filter((grade) => grade.gradeType.toString() === "TRANSIT_SCHOOL_WORK_8")[0]?.value || "",
                        },
                        {
                            label: 'თვე',
                            grade: generalSchoolWorkGrades?.filter((grade) => grade.gradeType.toString() === "TRANSIT_SCHOOL_WORK_MONTH")[0]?.value || "",
                        },
                        {
                            label: '%',
                            grade: generalSchoolWorkGrades?.filter((grade) => grade.gradeType.toString() === "TRANSIT_SCHOOL_WORK_MONTH_PERCENT")[0]?.value || "",
                        },

                    ]
                },
                {
                    name: 'თვის ქულა',
                    testNumber: null,
                    precent: null,
                    month: null,
                    monthGrade: null,
                    absence: null,
                    absenceGrade: null,
                    boxdetails: [
                        {
                            label: '',
                            grade: completeMonthly[0]?.value ? completeMonthly[0]?.value === -50 ? 'ჩთ' : completeMonthly[0]?.value : "",
                        },
                    ]
                }
            ]
            // GENERAL_SUMMARY_ASSIGMENT
        },
        [gradeData]
    )


    const toggleSidebar = () => {
      hamburgerIcon.current.classList.toggle("open")
      backgroundRef.current.classList.toggle("open")
      asideRef.current.classList.toggle('open')
      setSidebarOpen(!sidebarOpen);
    };

    const handleYearChange = (event) => {
        setChosenYear(event.target.value);
    };

    const handleChange = (event) => {
        setSelectedData(MONTHS.filter(month => month.value === event.target.value)[0]);
    };

    function dropdown(){
        return (
            <div className="yearDropwdown">
                <TextField
                    select
                    label="აირჩიე თვე"
                    value={selectedData.value}
                    onChange={handleChange}
                    variant="outlined"
                >
                    {month.map((m) => (
                        <MenuItem key={m} value={m}>
                            {m}
                        </MenuItem>
                    ))}
                </TextField>&nbsp;&nbsp;&nbsp;
                <TextField
                    select
                    label="აირჩიე წელი"
                    value={chosenYear}
                    onChange={handleYearChange}
                    variant="outlined"
                >
                    {year.map((m) => (
                        <MenuItem key={m} value={m}>
                            {m}
                        </MenuItem>
                    ))}
                </TextField>&nbsp;&nbsp;&nbsp;
                {/*{getYearDropdown()}*/}
                <Button onClick={()=>{}} disabled={!selectedData} style={{ fontWeight: 'bold', height: '50px'}} variant="contained">ძიება<SearchIcon/></Button>
            </div>
        );
    }


    const renderSubjects = () => {

      return subjectData.map(subject=>(
        <NavLink exact onClick={toggleSidebar} className="aside_aTag" to={`/grades/${subject.name}`} activeClassName="active">
          <ListItem button>
          <div>{subject.name}</div>
          </ListItem>
        </NavLink>
      ))

    }

    if (isLoading || isGradesLoading) {
        return <></>
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
                  <DisciplineBox data={transit? columnsTransit : columns}/>
              </div>
              <div className="ibChart" style={{marginLeft: -50, marginTop:65}}>
                  <Chart id={id} monthData={monthData}/>
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