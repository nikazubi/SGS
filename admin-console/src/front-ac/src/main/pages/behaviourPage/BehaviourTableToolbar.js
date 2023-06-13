import FlexBox from "../../../components/FlexBox";
import {Formik} from "formik";
import FormikAutocomplete from "../../components/formik/FormikAutocomplete";
import useAcademyClass from "../../../hooks/useAcademyClass";
import useFetchStudents from "../../../hooks/useStudents";
import {FormikDatePickerField} from "../../components/formik/FormikDatePickerField";
import {useState} from "react";

const BehaviourTableToolbar = ({setFilters, filters}) => {
    const {mutateAsync: onFetchAcademyClass} = useAcademyClass();
    const {mutateAsync: onFetchStudents} = useFetchStudents();
    const [date, setDate] = useState(new Date())

    return (
        <div>
            <FlexBox justifyContent='space-between'>
                <Formik
                    initialValues={
                        {
                            student: '',
                            academyClass: '',
                            date: date,
                            groupBy: 'STUDENT',
                        }
                    }
                    onSubmit={() => {
                    }}
                    //validationSchema={}
                    enableReinitialize
                >
                    {({ values, setFieldValue }) => (
                    <div style={{display: "flex", flexDirection: 'row', marginTop: 50}}>
                        <div style={{marginLeft: 50, width: 300}}>
                            <FormikAutocomplete name="academyClass"
                                                multiple={false}
                                                label={"კლასი"}
                                // resolveData={resolveCardTypeAutocompleteData}
                                                onFetch={onFetchAcademyClass}
                                                getOptionSelected={(option, value) => option.id === value.id}
                                                getOptionLabel={(option) => option.className}
                                                onBlur={()=> setFilters(values)}/>
                        </div>
                        <div style={{marginLeft: 50, width: 300}}>
                            <FormikAutocomplete name="student"
                                                multiple={false}
                                                label={"მოსწავლე"}
                                // resolveData={resolveCardTypeAutocompleteData}
                                                onFetch={onFetchStudents}
                                                getOptionSelected={(option, value) => option.id === value.id}
                                                getOptionLabel={(option) => option.firstName + " " + option.lastName}
                                                onBlur={()=> setFilters(values)}/>
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
                    </div>)}
                </Formik>
            </FlexBox>
        </div>
    )
}

export default BehaviourTableToolbar;