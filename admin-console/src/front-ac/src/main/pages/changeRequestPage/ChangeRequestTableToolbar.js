import FlexBox from "../../../components/FlexBox";
import {Formik} from "formik";
import FormikAutocomplete from "../../components/formik/FormikAutocomplete";
import useAcademyClassGeneral from "../../../hooks/useAcademyClassGeneral";
import useFetchStudents from "../../../hooks/useStudents";
import {FormikDatePickerField} from "../../components/formik/FormikDatePickerField";
import {useState} from "react";
import IconButton from "../../../components/buttons/IconButton";
import {Search} from "@material-ui/icons";
import Button from "../../../components/buttons/Button";
import {closePeriod} from "./useClosePeriod";
import {useUserContext} from "../../../contexts/user-context";

const ChangeRequestTableToolbar = ({setFilters, filters}) => {
    const {mutateAsync: onFetchAcademyClass} = useAcademyClassGeneral();
    const {mutateAsync: onFetchStudents} = useFetchStudents();
    const [date, setDate] = useState(new Date());
    const {hasPermission} = useUserContext();

    const hasManageClosePeriodPermission = hasPermission("MANAGE_CLOSED_PERIOD")


    return (
        <div style={{width:'100%'}}>
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
                    <div style={{display: "flex", flexDirection: 'row', marginTop: 50,}}>
                        <div style={{marginLeft: 50, width: 300}}>
                            <FormikAutocomplete name="academyClass"
                                                multiple={false}
                                                label={"კლასი"}
                                             // resolveData={resolveCardTypeAutocompleteData}
                                                onFetch={onFetchAcademyClass}
                                                getOptionSelected={(option, value) => option.id === value.id}
                                                getOptionLabel={(option) => option.className}
                                                // onBlur={()=> setFilters(values)}
                                                />
                        </div>
                        <div style={{marginLeft: 50, width: 300}}>
                            <FormikAutocomplete name="student"
                                                multiple={false}
                                                label={"მოსწავლე"}
                                // resolveData={resolveCardTypeAutocompleteData}
                                                onFetch={onFetchStudents}
                                                getOptionSelected={(option, value) => option.id === value.id}
                                                getOptionLabel={(option) => option.firstName + " " + option.lastName}
                                                //onBlur={()=> setFilters(values)}
                             />
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
                                onClick={() => setFilters(values)}
                            />
                        </div>
                        {hasManageClosePeriodPermission &&
                            <div style={{position: 'absolute', right: '5%'}}>
                            <Button style={{backgroundColor: !filters || !filters.academyClass || ! filters.academyClass.id? "#b3aabf":"#e46c0a", color: "#fff", marginBottom: -30, fontSize: 16}}
                                    disabled={!filters || !filters.academyClass || ! filters.academyClass.id}
                                    onClick={async () => {
                                        const params = {
                                            academyClassId: filters.academyClass.id
                                        }
                                        await closePeriod(params)
                                    }}>
                                {"პერიოდის დახურვა"}
                            </Button>
                        </div>}
                    </div>)}
                </Formik>
            </FlexBox>
        </div>
    )
}

export default ChangeRequestTableToolbar;