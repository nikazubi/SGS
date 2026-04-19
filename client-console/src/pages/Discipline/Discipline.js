import React, {useMemo, useRef, useState} from "react";
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
import {YEAR} from "../utils/date";
import useSubjects from "./useSubjects";
import useGrades from "./useGrades";
import {useUserContext} from "../../context/user-context";
import useTransit from "./useTransit";


const Discipline = ({match}) => {
    const id = match.params.id
    const [sidebarOpen, setSidebarOpen] = useState(false);

    const [selectedData, setSelectedData] = useState({key: 1, value: "I"});
    const [chosenYear, setChosenYear] = useState(new Date().getUTCFullYear());
    const {user, isTransit} = useUserContext();
    const {data: subjectData, isLoading, isError, error, isSuccess} = useSubjects();
    const chosenSubject = useMemo(
        () => id && subjectData ? subjectData.filter((data) => data.name === id)[0] : {},
        [id, subjectData]
    );

    const choosenYeeear = useMemo(
        () => chosenYear ? YEAR.filter((year) => year.value === chosenYear)[0] : new Date().getUTCFullYear(),
        [chosenYear]
    );
    const {data: gradeData, isLoading: isGradesLoading} = useGrades({
        trimester: selectedData,
        subject: chosenSubject,
        year: choosenYeeear.key
    });
    const {data: transit} = useTransit();

    const asideRef = useRef();
    const backgroundRef = useRef();
    const hamburgerIcon = useRef();

    const trimester = [
        {key: 1, value: "I"},
        {key: 2, value: "II"},
        {key: 3, value: "III"}
    ]

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
            if (!gradeData) {
                return []
            }
            const trimesterOngoingGrades = gradeData?.filter((grade) => grade.gradeType?.toString().includes("TRIMESTER_ONGOING_GRADE"))
            const trimesterInitialKnowledge = gradeData?.filter((grade) => grade.gradeType?.toString().includes("TRIMESTER_INITIAL_KNOWLEDGE_GRADE"))
            const progressGrade = gradeData?.filter((grade) => grade.gradeType?.toString().includes("TRIMESTER_PROGRESS_GRADE"))
            const finalExam = gradeData?.filter((grade) => grade.gradeType?.toString().includes("TRIMESTER_FINAL_EXAM_GRADE"))
            const fullGrade = gradeData?.filter((grade) => grade.gradeType?.toString().includes("TRIMESTER_GRADE"))
            return [
                {
                    name: 'მიმდინარე შეფასება',
                    testNumber: null,
                    precent: null,
                    month: null,
                    monthGrade: null,
                    absence: null,
                    absenceGrade: null,
                    boxdetails: [
                        {
                            label: '1',
                            grade: trimesterOngoingGrades?.filter((grade) => grade.gradeType.toString() === "TRIMESTER_ONGOING_GRADE_1")[0]?.value || "",
                        },
                        {
                            label: '2',
                            grade: trimesterOngoingGrades?.filter((grade) => grade.gradeType.toString() === "TRIMESTER_ONGOING_GRADE_2")[0]?.value || "",
                        },

                        {
                            label: '3',
                            grade: trimesterOngoingGrades?.filter((grade) => grade.gradeType.toString() === "TRIMESTER_ONGOING_GRADE_3")[0]?.value || "",
                        },
                        {
                            label: '4',
                            grade: trimesterOngoingGrades?.filter((grade) => grade.gradeType.toString() === "TRIMESTER_ONGOING_GRADE_4")[0]?.value || "",
                        },
                        {
                            label: '5',
                            grade: trimesterOngoingGrades?.filter((grade) => grade.gradeType.toString() === "TRIMESTER_ONGOING_GRADE_5")[0]?.value || "",
                        },
                        {
                            label: '6',
                            grade: trimesterOngoingGrades?.filter((grade) => grade.gradeType.toString() === "TRIMESTER_ONGOING_GRADE_6")[0]?.value || "",
                        },
                        {
                            label: '7',
                            grade: trimesterOngoingGrades?.filter((grade) => grade.gradeType.toString() === "TRIMESTER_ONGOING_GRADE_7")[0]?.value || "",
                        }
                    ]
                },
                {
                    name: 'საწყისი ცოდნის განმსაზღვრელი ტესტი',
                    testNumber: null,
                    precent: null,
                    month: null,
                    monthGrade: null,
                    absence: null,
                    absenceGrade: null,
                    boxdetails: [
                        {
                            label: '',
                            grade: trimesterInitialKnowledge[0]?.value ? trimesterInitialKnowledge[0]?.value === -50 ? 'ჩთ' : trimesterInitialKnowledge[0]?.value : "",
                        }
                    ]
                },
                {
                    name: 'პროგრეს ტესტი',
                    testNumber: null,
                    precent: null,
                    month: null,
                    monthGrade: null,
                    absence: null,
                    absenceGrade: null,
                    boxdetails: [
                        {
                            label: '',
                            grade: progressGrade[0]?.value ? progressGrade[0]?.value === -50 ? 'ჩთ' : progressGrade[0]?.value : "",
                        }
                    ]
                },
                {
                    name: 'ფინალური ქულა',
                    testNumber: null,
                    precent: null,
                    month: null,
                    monthGrade: null,
                    absence: null,
                    absenceGrade: null,
                    boxdetails: [
                        {
                            label: '',
                            grade: finalExam[0]?.value ? fullGrade[0]?.value === -50 ? 'ჩთ' : fullGrade[0]?.value : "",
                        },
                    ]
                },
                {
                    name: 'ტრიმესტრის შეფასება',
                    testNumber: null,
                    precent: null,
                    month: null,
                    monthGrade: null,
                    absence: null,
                    absenceGrade: null,
                    boxdetails: [
                        {
                            label: '',
                            grade: fullGrade[0]?.value ? fullGrade[0]?.value === -50 ? 'ჩთ' : fullGrade[0]?.value : "",
                        },
                    ]
                }
            ]
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
        setSelectedData(trimester.filter(month => month.value === event.target.value)[0]);
    };

    function dropdown() {
        return (
            <div className="yearDropwdown">
                <TextField
                    select
                    label="აირჩიე ტრიმესტრი"
                    value={selectedData.value}
                    onChange={handleChange}
                    variant="outlined"
                >
                    {trimester.map((m) => (
                        <MenuItem key={m.key} value={m.value}>
                            {m.value}
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
                <Button onClick={() => {
                }} disabled={!selectedData} style={{fontWeight: 'bold', height: '50px'}}
                        variant="contained">ძიება<SearchIcon/></Button>
            </div>
        );
    }


    const renderSubjects = () => {

        return subjectData.map(subject => (
            <NavLink exact onClick={toggleSidebar} className="aside_aTag" to={`/grades/${subject.name}`}
                     activeClassName="active">
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
        <Box sx={{display: 'flex'}}>
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
                                <HomeIcon/>
                                <div>მთავარი</div>
                            </ListItemIcon>
                        </ListItem>
                    </NavLink>
                    {/* Add your sidebar items */}
                    {renderSubjects()}
                </List>
            </Sidebar>
            <Box component="main" sx={{flexGrow: 1, p: 3}}>

                <div className="ibCnt">
                    <div className="ib__center column">
                        <div className="pageName">მოსწავლის ტრიმესტრული შეფასება აკადემიური დისციპლინების მიხედვით</div>
                        <div className="pageName">{id}</div>
                    </div>
                    <div style={{display: "flex"}}>
                        {dropdown()}
                    </div>
                    <div className="termEstCnt">
                        <DisciplineBox data={columns}/>
                    </div>
                    {/*<div className="ibChart" style={{marginLeft: -50, marginTop: 65}}>*/}
                    {/*    <Chart id={id} monthData={monthData}/>*/}
                    {/*</div>*/}
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