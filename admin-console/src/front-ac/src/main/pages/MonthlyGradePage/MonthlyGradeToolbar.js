import FlexBox from "../../../components/FlexBox";
import {Formik} from "formik";
import FormikAutocomplete from "../../components/formik/FormikAutocomplete";
import useAcademyClassGeneral from "../../../hooks/useAcademyClassGeneral";
import useFetchStudents from "../../../hooks/useStudents";
import {FormikDatePickerField} from "../../components/formik/FormikDatePickerField";
import {useState} from "react";
import IconButton from "../../../components/buttons/IconButton";
import {Search} from "@material-ui/icons";
import {setFiltersOfPage} from "../../../utils/filters";
import IconButtonWithTooltip from "../../../components/buttons/IconButtonWithTooltip";
import Switch from "@mui/material/Switch";
import {Tooltip} from "@mui/material";
import {FileDownload} from "@mui/icons-material";
import {fetchGradeMonthly, fetchGradesSemester} from "../semesterPage/useExportWord";

const MonthlyGradeToolbar = ({setFilters, filters, checked, setChecked}) => {
    const {mutateAsync: onFetchAcademyClass} = useAcademyClassGeneral();
    const {mutateAsync: onFetchStudents} = useFetchStudents();
    const [date, setDate] = useState(new Date())


    return (
        <div>
            <FlexBox justifyContent='space-between'>
                <Formik
                    initialValues={
                        {
                            student: filters.student || '',
                            academyClass: filters.academyClass || '',
                            date: filters.date || date,
                            groupBy: 'STUDENT',
                        }
                    }
                    onSubmit={() => {
                    }}
                    //validationSchema={}
                    enableReinitialize
                >
                    {({ values, setFieldValue }) => (
                    <div style={{display: "flex", flexDirection: 'row', marginTop: 25, marginBottom:25}}>
                        <div style={{marginLeft: 0, width: 250}}>
                            <FormikAutocomplete name="academyClass"
                                                multiple={false}
                                                label={"კლასი"}
                                // resolveData={resolveCardTypeAutocompleteData}
                                                onFetch={onFetchAcademyClass}
                                                getOptionSelected={(option, value) => {

                                                    return option.id === value.id
                                                }}
                                                getOptionLabel={(option) => option.className}
                                                setInitialVulue={(options) =>{
                                                    if(options.length === 1){
                                                        return options[0]
                                                    }
                                                }}/>
                        </div>
                        <div style={{marginLeft: 20, width: 250}}>
                            <FormikAutocomplete name="student"
                                                multiple={false}
                                                label={"მოსწავლე"}
                                // resolveData={resolveCardTypeAutocompleteData}
                                                onFetch={onFetchStudents}
                                                getOptionSelected={(option, value) => option.id === value.id}
                                                getOptionLabel={(option) => option.firstName + " " + option.lastName}
                                                setInitialVulue={(options) =>{
                                                    if(options.length === 1 && options[0] !== 'undefined undefined'){
                                                        return options[0]
                                                    }
                                                }}/>
                        </div>

                        <div style={{marginLeft: 20, width: 250}}>
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
                        <div style={{marginLeft: 10, width: 50}}>
                            <IconButtonWithTooltip
                                tooltip={"ძიება"}
                                icon={<Search/>}
                                onClick={() => {
                                    setFiltersOfPage("MONTHLY_GRADE", values)
                                    setFilters(values)
                                }}
                            />
                        </div>
                        <Tooltip title={checked? "10 ბალიანი სისტემა" : "7 ბალიანი სისტემა"}>
                            <div style={{marginLeft: 0, width: 50}}>
                                <Switch
                                    checked={checked}
                                    onChange={()=> setChecked(!checked)}
                                    inputProps={{ 'aria-label': 'controlled' }}
                                />
                            </div>
                        </Tooltip>
                        <div style={{marginLeft: 0, width: 40}}>
                            <IconButtonWithTooltip
                                icon={<FileDownload/>}
                                tooltip={"ექსპორტი"}
                                onClick={async () => {
                                    await fetchGradeMonthly(filters, checked)
                                }}
                            />
                        </div>
                    </div>)}
                </Formik>
            </FlexBox>
        </div>
    )
}

export default MonthlyGradeToolbar;