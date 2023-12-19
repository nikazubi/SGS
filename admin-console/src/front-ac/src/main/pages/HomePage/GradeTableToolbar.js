import FlexBox from "../../../components/FlexBox";
import {Formik} from "formik";
import FormikAutocomplete from "../../components/formik/FormikAutocomplete";
import useSubjects from "../../../hooks/useSubjects";
import useAcademyClassGeneral from "../../../hooks/useAcademyClassGeneral";
import useFetchStudents from "../../../hooks/useStudents";
import {FormikDatePickerField} from "../../components/formik/FormikDatePickerField";
import {useCallback, useState} from "react";
import Button from "@material-ui/core/Button";
import {Search} from "@material-ui/icons";
import useCalculateGeneralGrade from "./calculateMonthlyGrade";
import {setFiltersOfPage} from "../../../utils/filters";
import {useNotification} from "../../../contexts/notification-context";
import IconButtonWithTooltip from "../../../components/buttons/IconButtonWithTooltip";

const GradeTableToolbar = ({setFilters, filters}) => {
    const {mutateAsync: onFetchSubjects} = useSubjects();
    const {mutateAsync: onFetchAcademyClass} = useAcademyClassGeneral();
    const {mutateAsync: onFetchStudents} = useFetchStudents();
    const {mutateAsync: calculateGeneralGrade} = useCalculateGeneralGrade();
    const [date, setDate] = useState(new Date());
    const {setNotification, setErrorMessage} = useNotification();
    const [defaultClass, setDefaultClass] = useState(); // TODO somehow defaultclass never fills

    const resoleSubjectData = useCallback((data, values) => {
        console.log(defaultClass)
        if ((!values.academyClass || !values.academyClass.id)) {
            if (!defaultClass) {
                return data;
            }
        }
        const subjectIds = values.academyClass?.subjectList.map(subject => subject.id)
        return data.filter(subject => subjectIds.includes(subject.id))
    }, [defaultClass])

    const setInitialValue = useCallback((options) => {
        setDefaultClass(options[0])
        return options[0]
    }, [setDefaultClass])


    return (
        <div>
            <FlexBox justifyContent='space-between'>
                <Formik
                    initialValues={
                        {
                            student: filters.student || '',
                            academyClass: filters.academyClass || '',
                            subject: filters.subject || '',
                            date: filters.date || date,
                            groupByClause: 'STUDENT',
                        }
                    }
                    onSubmit={() => {
                    }}
                    //validationSchema={}
                    enableReinitialize
                >
                    {({values, setFieldValue}) => (
                        <div style={{display: "flex", flexDirection: 'row', marginTop: 50, marginBottom: 25}}>
                            <div style={{marginLeft: 15, width: 200}}>
                                <FormikAutocomplete name="academyClass"
                                                    multiple={false}
                                                    label={"კლასი"}
                                                    onFetch={onFetchAcademyClass}
                                                    getOptionSelected={(option, value) => option.id === value.id}
                                                    getOptionLabel={(option) => option.className}
                                                    setInitialVulue={setInitialValue}
                                />
                            </div>
                            <div style={{marginLeft: 15, width: 200}}>
                                <FormikAutocomplete name="subject"
                                                    multiple={false}
                                                    resolveData={(data) => resoleSubjectData(data, values)}
                                                    label={"საგანი"}
                                                    onFetch={onFetchSubjects}
                                                    getOptionSelected={(option, value) => option.id === value.id}
                                                    getOptionLabel={(option) => option.name + " - " + (option.teacher ? option.teacher : "")}
                                                    setInitialVulue={(options) => {
                                                        if (options.length === 1) {
                                                            return options[0]
                                                        }
                                                    }}
                                />
                            </div>
                            <div style={{marginLeft: 15, width: 200}}>
                                <FormikAutocomplete name="student"
                                                    multiple={false}
                                                    label={"მოსწავლე"}
                                                    onFetch={onFetchStudents}
                                                    getOptionSelected={(option, value) => option.id === value.id}
                                                    getOptionLabel={(option) => option.firstName + " " + option.lastName}
                                                    setInitialVulue={(options) => {
                                                        if (options.length === 1) {
                                                            return options[0]
                                                        }
                                                    }}
                                />
                            </div>

                            <div style={{marginLeft: 15, width: 200}}>
                                <FormikDatePickerField name="date"
                                                       label={"თვე"}
                                                       onChange={(event, value) => {
                                                           setFieldValue("date", value)
                                                           // setFilters(prevState => {
                                                           //     const copied = prevState;
                                                           //     copied.date = value
                                                           //     return copied
                                                           // })
                                                       }}/>
                            </div>
                            <div style={{marginLeft: 15, width: 100}}>
                                <IconButtonWithTooltip
                                    tooltip={"ძიება"}
                                    icon={<Search/>}
                                    onClick={() => {
                                        setFiltersOfPage("GRADES", values)
                                        setFilters(values)
                                    }}
                                />
                            </div>
                            <div style={{marginLeft: 15, width: 250}}>
                                <Button
                                    style={{backgroundColor: "#45c1a4", color: "#fff", marginBottom: -30, fontSize: 16}}
                                    disabled={!filters.academyClass || !filters.subject}
                                    onClick={async () => {
                                        const params = {
                                            academyClassId: filters.academyClass.id,
                                            subjectId: filters.subject.id,
                                            date: new Date(filters.date).getTime(),
                                            setErrorMessage: setErrorMessage,
                                            setNotification: setNotification
                                        }
                                        calculateGeneralGrade(params).then(() => {
                                            // setNotification({
                                            //     message: 'თვის ნიშანი წარმატებით დაითვალა',
                                            //     severity: 'success'
                                            // });
                                        }).catch((error) => {
                                            setErrorMessage(error);
                                            setNotification({
                                                message: 'ნიშნის კალკულაცია ვერ მოხერხდა გთხოვთ შეავსოთ ყველა ნიშანი და სცადოთ მოგვიანებით',
                                                severity: 'error'
                                            });
                                        });
                                    }}>
                                    {"თვის ქულის დათვლა"}
                                </Button>
                            </div>

                        </div>)}
                </Formik>
            </FlexBox>
        </div>
    )
}

export default GradeTableToolbar;