import React, {useMemo, useState} from "react";
import DisciplineBox from "./DisciplineBox";
import TextField from "@mui/material/TextField";
import MenuItem from "@mui/material/MenuItem";
import {getRomanByInt, MONTHS, MONTHS_SCHOOL} from "../utils/date";
import useFetchBehaviour from "./useBehaviour";

const EthicPage = () => {
    const [selectedData, setSelectedData] = useState(MONTHS[new Date().getUTCMonth()]);
    const chosenMonth = useMemo(
        () => selectedData ? MONTHS.filter((month) => month.value === selectedData.value)[0] : new Date().getUTCMonth(),
        [selectedData]
    );

    const {data: gradeData, isLoading, isError, error, isSuccess} = useFetchBehaviour({month: chosenMonth.key});
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
    const handleChange = (event) => {
        setSelectedData(MONTHS.filter(month => month.value === event.target.value)[0]);
    };

    const parsedData = useMemo(() => {

        if (!gradeData) {
            return []
        }
        const uniformBoxDetails = gradeData?.filter((grade) => grade.gradeType?.toString().includes("BEHAVIOUR_APPEARING_IN_UNIFORM"))
            .sort((a, b) => {
                if (a.gradeType?.toString().includes("MONTH")) {
                    return 1;
                }
                if (b.gradeType?.toString().includes("MONTH")) {
                    return -1;
                }
                const lastNumberA = parseInt(a.gradeType?.toString().match(/\d+$/)[0]);
                const lastNumberB = parseInt(b.gradeType?.toString().match(/\d+$/)[0]);
                return lastNumberA - lastNumberB;
            })
            .map((grade, index) => {
                if (grade.gradeType.toString().includes("MONTH")) {
                    return ({
                        label: `თვის ქულა`,
                        grade: grade.value
                    })
                }
                return ({
                    label: `${getRomanByInt(grade.gradeType.toString().charAt(grade.gradeType.toString().length - 1))} კვირა`,
                    grade: grade.value
                })
            })
        const delaysBoxDetails = gradeData?.filter((grade) => grade.gradeType?.toString().includes("BEHAVIOUR_STUDENT_DELAYS"))
            .sort((a, b) => {
                if (a.gradeType?.toString().includes("MONTH")) {
                    return 1;
                }
                if (b.gradeType?.toString().includes("MONTH")) {
                    return -1;
                }
                const lastNumberA = parseInt(a.gradeType?.toString().match(/\d+$/)[0]);
                const lastNumberB = parseInt(b.gradeType?.toString().match(/\d+$/)[0]);
                return lastNumberA - lastNumberB;
            })
            .map((grade, index) => {
                if (grade.gradeType.toString().includes("MONTH")) {
                    return ({
                        label: `თვის ქულა`,
                        grade: grade.value
                    })
                }
                return ({
                    label: `${getRomanByInt(grade.gradeType.toString().charAt(grade.gradeType.toString().length - 1))} კვირა`,
                    grade: grade.value
                })
            })
        const inventoryBoxDetails = gradeData?.filter((grade) => grade.gradeType?.toString().includes("BEHAVIOUR_CLASSROOM_INVENTORY"))
            .sort((a, b) => {
                if (a.gradeType?.toString().includes("MONTH")) {
                    return 1;
                }
                if (b.gradeType?.toString().includes("MONTH")) {
                    return -1;
                }
                const lastNumberA = parseInt(a.gradeType?.toString().match(/\d+$/)[0]);
                const lastNumberB = parseInt(b.gradeType?.toString().match(/\d+$/)[0]);
                return lastNumberA - lastNumberB;
            })
            .map((grade, index) => {
                if (grade.gradeType.toString().includes("MONTH")) {
                    return ({
                        label: `თვის ქულა`,
                        grade: grade.value
                    })
                }
                return ({
                    label: `${getRomanByInt(grade.gradeType.toString().charAt(grade.gradeType.toString().length - 1))} კვირა`,
                    grade: grade.value
                })
            })
        const hygeneBoxDetails = gradeData?.filter((grade) => grade.gradeType?.toString().includes("BEHAVIOUR_STUDENT_HYGIENE"))
            .sort((a, b) => {
                if (a.gradeType?.toString().includes("MONTH")) {
                    return 1;
                }
                if (b.gradeType?.toString().includes("MONTH")) {
                    return -1;
                }
                const lastNumberA = parseInt(a.gradeType?.toString().match(/\d+$/)[0]);
                const lastNumberB = parseInt(b.gradeType?.toString().match(/\d+$/)[0]);
                return lastNumberA - lastNumberB;
            })
            .map((grade, index) => {
                if (grade.gradeType.toString().includes("MONTH")) {
                    return ({
                        label: `თვის ქულა`,
                        grade: grade.value
                    })
                }
                return ({
                    label: `${getRomanByInt(grade.gradeType.toString().charAt(grade.gradeType.toString().length - 1))} კვირა`,
                    grade: grade.value
                })
            })
        const behaviourBoxDetails = gradeData?.filter((grade) => grade.gradeType?.toString().includes("BEHAVIOUR_STUDENT_BEHAVIOR"))
            .sort((a, b) => {
                if (a.gradeType?.toString().includes("MONTH")) {
                    return 1;
                }
                if (b.gradeType?.toString().includes("MONTH")) {
                    return -1;
                }
                const lastNumberA = parseInt(a.gradeType?.toString().match(/\d+$/)[0]);
                const lastNumberB = parseInt(b.gradeType?.toString().match(/\d+$/)[0]);
                return lastNumberA - lastNumberB;
            })
            .map((grade, index) => {
                if (grade.gradeType.toString().includes("MONTH")) {
                    return ({
                        label: `თვის ქულა`,
                        grade: grade.value
                    })
                }
                return ({
                    label: `${getRomanByInt(grade.gradeType.toString().charAt(grade.gradeType.toString().length - 1))} კვირა`,
                    grade: grade.value
                })
            })
        return [
            {
                name: 'მოსწავლის ფორმითი გამოცხადება',
                testNumber: null,
                precent: null,
                month: null,
                monthGrade: null,
                absence: null,
                absenceGrade: null,

                boxdetails: uniformBoxDetails
            },
            {
                name: 'მოსწავლის დაგვიანება',
                testNumber: null,
                precent: null,
                month: null,
                monthGrade: null,
                absence: null,
                absenceGrade: null,

                boxdetails: delaysBoxDetails
            },
            {
                name: 'საკლასო ინვენტარის მოვლა',
                testNumber: null,
                precent: null,
                month: null,
                monthGrade: null,
                absence: null,
                absenceGrade: null,

                boxdetails: inventoryBoxDetails
            },
            {
                name: 'მოსწავლის მიერ ჰიგიენური ნორმების დაცვა',
                testNumber: null,
                precent: null,
                month: null,
                monthGrade: null,
                absence: null,
                absenceGrade: null,

                boxdetails: hygeneBoxDetails
            },
            {
                name: 'მოსწავლის ყოფაქცევა',
                testNumber: null,
                precent: null,
                month: null,
                monthGrade: null,
                absence: null,
                absenceGrade: null,

                boxdetails: behaviourBoxDetails
            },

        ]

    }, [gradeData])


    function dropdown() {
        return (
            <div className="yearDropwdown">
                <TextField
                    select
                    label="აირჩიე თვე"
                    value={selectedData.value}
                    onChange={handleChange}
                    variant="outlined"
                >
                    {MONTHS_SCHOOL.map((m) => (
                        <MenuItem key={m.key} value={m.value}>
                            {m.value}
                        </MenuItem>
                    ))}
                </TextField>
            </div>
        );
    }

    return (
        <>
            <div className="pageName">მოსწავლის შეფასება ეთიკური ნორმების მიხედვით</div>
            <div className="ibCnt">
                {dropdown()}
                <div className="termEstCnt">
                    <DisciplineBox data={parsedData}/>
                </div>
            </div>
        </>
    );
}

export default EthicPage;