import FlexBox from "../../../components/FlexBox";
import {Formik} from "formik";
import FormikAutocomplete from "../../components/formik/FormikAutocomplete";
import useAcademyClassGeneral from "../../../hooks/useAcademyClassGeneral";
import useFetchStudents from "../../../hooks/useStudents";
import {FormikDatePickerField} from "../../components/formik/FormikDatePickerField";
import {useState} from "react";
import IconButton from "../../../components/buttons/IconButton";
import {Search} from "@material-ui/icons";
import Button from "@material-ui/core/Button";
import useCalculateGeneralGrade from "../HomePage/calculateMonthlyGrade";
import useCalculateMonthlyBehaviour, {calculateMonthlyBehaviour} from "./calculateMonthlyBehaviour";
import {setFiltersOfPage} from "../../../utils/filters";
import {useNotification} from "../../../contexts/notification-context";

const BehaviourTableToolbar = ({setFilters, filters}) => {
    const {mutateAsync: onFetchAcademyClass} = useAcademyClassGeneral();
    const {mutateAsync: onFetchStudents} = useFetchStudents();
    const [date, setDate] = useState(new Date());
    const {mutateAsync: calculateMonthlyBehaviour} = useCalculateMonthlyBehaviour();
    const {setNotification, setErrorMessage} = useNotification();

    return (
        <div>
            <FlexBox justifyContent='space-between'>
                <Formik
                    initialValues={
                        {
                            student: filters.student || '',
                            academyClass: filters.academyClass || '',
                            date:  filters.date ||date,
                            groupByClause: 'STUDENT'
                        }
                    }
                    onSubmit={() => {
                    }}
                    //validationSchema={}
                    enableReinitialize
                >
                    {({ values, setFieldValue }) => (
                    <div style={{display: "flex", flexDirection: 'row', marginTop: 50, marginBottom:25}}>
                        <div style={{marginLeft: 50, width: 300}}>
                            <FormikAutocomplete name="academyClass"
                                                multiple={false}
                                                label={"კლასი"}
                                // resolveData={resolveCardTypeAutocompleteData}
                                                onFetch={onFetchAcademyClass}
                                                getOptionSelected={(option, value) => option.id === value.id}
                                                getOptionLabel={(option) => option.className}
                                                setInitialVulue={(options) =>{
                                                    if(options.length === 1){
                                                        return options[0]
                                                    }
                                                }}/>
                        </div>
                        <div style={{marginLeft: 50, width: 300}}>
                            <FormikAutocomplete name="student"
                                                multiple={false}
                                                label={"მოსწავლე"}
                                // resolveData={resolveCardTypeAutocompleteData}
                                                onFetch={onFetchStudents}
                                                getOptionSelected={(option, value) => option.id === value.id}
                                                getOptionLabel={(option) => option.firstName + " " + option.lastName}
                                                setInitialVulue={(options) =>{
                                                    if(options.length === 1){
                                                        return options[0]
                                                    }
                                                }}/>
                        </div>

                        <div style={{marginLeft: 50, width: 300}}>
                            <FormikDatePickerField name="date"
                                                   label={"თვე"}
                                                   onChange={(event, value)=> {
                                                      setFieldValue("date", value)
                                                      setFilters(prevState => {
                                                          const copied = prevState;
                                                          copied.date = value
                                                          return copied
                                                      })
                                                   }}/>
                        </div>
                        <div style={{marginLeft: 15, width: 100}}>
                            <IconButton
                                icon={<Search/>}
                                onClick={() => {
                                    setFiltersOfPage("BEHAVIOUR", values)
                                    setFilters(values)
                                }}
                            />
                        </div>
                        <div style={{marginLeft: 15, width: 250}}>
                            <Button style={{backgroundColor: "#45c1a4", color: "#fff", marginBottom: -30, fontSize: 16}}
                                    disabled={!filters.academyClass}
                                    onClick={async () => {
                                        const params = {
                                            academyClassId: filters.academyClass.id,
                                            date: Date.parse(filters.date),
                                        }
                                        calculateMonthlyBehaviour(params).then(() =>{
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

export default BehaviourTableToolbar;