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
        console.log(gradeData)
        //BEHAVIOUR_MONTHLY
        const shouldBeSixWeeks = selectedData.key === 0 || selectedData.key === 8
        let uniformBoxDetails = gradeData?.filter((grade) => grade.gradeType?.toString().includes("BEHAVIOUR_APPEARING_IN_UNIFORM"))
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
        if (!shouldBeSixWeeks) {
            uniformBoxDetails = [...uniformBoxDetails.slice(0, 4), uniformBoxDetails[uniformBoxDetails.length - 1]];
        }
        let delaysBoxDetails = gradeData?.filter((grade) => grade.gradeType?.toString().includes("BEHAVIOUR_STUDENT_DELAYS"))
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
        if (!shouldBeSixWeeks) {
            delaysBoxDetails = [...delaysBoxDetails.slice(0, 4), delaysBoxDetails[delaysBoxDetails.length - 1]];
        }
        let inventoryBoxDetails = gradeData?.filter((grade) => grade.gradeType?.toString().includes("BEHAVIOUR_CLASSROOM_INVENTORY"))
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
        if (!shouldBeSixWeeks) {
            inventoryBoxDetails = [...inventoryBoxDetails.slice(0, 4), inventoryBoxDetails[inventoryBoxDetails.length - 1]];
        }
        let hygeneBoxDetails = gradeData?.filter((grade) => grade.gradeType?.toString().includes("BEHAVIOUR_STUDENT_HYGIENE"))
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
        if (!shouldBeSixWeeks) {
            hygeneBoxDetails = [...hygeneBoxDetails.slice(0, 4), hygeneBoxDetails[hygeneBoxDetails.length - 1]];
        }
        let behaviourBoxDetails = gradeData?.filter((grade) => grade.gradeType?.toString().includes("BEHAVIOUR_STUDENT_BEHAVIOR"))
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
        if (!shouldBeSixWeeks) {
            behaviourBoxDetails = [...behaviourBoxDetails.slice(0, 4), behaviourBoxDetails[behaviourBoxDetails.length - 1]];
        }
        const completeMonthly = gradeData?.filter((grade) => grade.gradeType?.toString().includes("BEHAVIOUR_MONTHLY"))
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
                        grade: completeMonthly[0]?.value || "",
                    },
                ]
            }
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