import FlexBox from "../../../components/FlexBox";
import {Formik} from "formik";
import FormikAutocomplete from "../../components/formik/FormikAutocomplete";
import useSubjects from "../../../hooks/useSubjects";
import useAcademyClassGeneral from "../../../hooks/useAcademyClassGeneral";
import useFetchStudents from "../../../hooks/useStudents";
import {FormikDatePickerField} from "../../components/formik/FormikDatePickerField";
import {useState} from "react";
import Button from "@material-ui/core/Button";
import IconButton from "../../../components/buttons/IconButton";
import {Search} from "@material-ui/icons";
import {closePeriod} from "../changeRequestPage/useClosePeriod";
import useCalculateGeneralGrade, {calculateMonthlyGrade} from "./calculateMonthlyGrade";
import {setFiltersOfPage} from "../../../utils/filters";
import {useNotification} from "../../../contexts/notification-context";

const GradeTableToolbar = ({setFilters, filters}) => {
    const {mutateAsync: onFetchSubjects} = useSubjects();
    const {mutateAsync: onFetchAcademyClass} = useAcademyClassGeneral();
    const {mutateAsync: onFetchStudents} = useFetchStudents();
    const {mutateAsync: calculateGeneralGrade} = useCalculateGeneralGrade();
    const [date, setDate] = useState(new Date());
    const {setNotification, setErrorMessage} = useNotification();


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
                    {({ values, setFieldValue }) => (
                    <div style={{display: "flex", flexDirection: 'row', marginTop: 50, marginBottom:25}}>
                            <div style={{marginLeft: 15, width: 200}}>
                                <FormikAutocomplete name="academyClass"
                                                    multiple={false}
                                                    label={"კლასი"}
                                                    onFetch={onFetchAcademyClass}
                                                    getOptionSelected={(option, value) => option.id === value.id}
                                                    getOptionLabel={(option) => option.className}
                                                    setInitialVulue={(options) =>{
                                                        if(options.length === 1){
                                                          return options[0]
                                                        }
                                                    }}
                                />
                            </div>
                        <div style={{marginLeft: 15, width: 200}}>
                            <FormikAutocomplete name="subject"
                                                multiple={false}
                                                label={"საგანი"}
                                                onFetch={onFetchSubjects}
                                                getOptionSelected={(option, value) => option.id === value.id}
                                                getOptionLabel={(option) => option.name}
                                                setInitialVulue={(options) =>{
                                                    if(options.length === 1){
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
                                                setInitialVulue={(options) =>{
                                                    if(options.length === 1){
                                                        return options[0]
                                                    }
                                                }}
                            />
                        </div>

                        <div style={{marginLeft: 15, width: 200}}>
                            <FormikDatePickerField name="date"
                                                   label={"თვე"}
                                                   onChange={(event, value)=> {
                                                      setFieldValue("date", value)
                                                      // setFilters(prevState => {
                                                      //     const copied = prevState;
                                                      //     copied.date = value
                                                      //     return copied
                                                      // })
                                                   }}/>
                        </div>
                        <div style={{marginLeft: 15, width: 100}}>
                            <IconButton
                                icon={<Search/>}
                                onClick={() => {
                                    setFiltersOfPage("GRADES", values)
                                    setFilters(values)
                                }}
                                />
                        </div>
                        <div style={{marginLeft: 15, width: 250}}>
                            <Button style={{backgroundColor: "#45c1a4", color: "#fff", marginBottom: -30, fontSize: 16}}
                                    disabled={!filters.academyClass || !filters.subject}
                                    onClick={async () => {
                                        const params = {
                                            academyClassId: filters.academyClass.id,
                                            subjectId: filters.subject.id,
                                            date: new Date(filters.date).getTime(),
                                        }
                                        calculateGeneralGrade(params).then(() =>{
                                            setNotification({
                                                message: 'თვის ნიშანი წარმატებით დაითვალა',
                                                severity: 'success'
                                            });
                                        }).catch((error) => {
                                            setErrorMessage(error);
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