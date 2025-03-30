import FlexBox from "../../../components/FlexBox";
import {Formik} from "formik";
import FormikAutocomplete from "../../components/formik/FormikAutocomplete";
import useAcademyClassGeneral from "../../../hooks/useAcademyClassGeneral";
import useFetchStudents from "../../../hooks/useStudents";
import {useState} from "react";
import {Search} from "@material-ui/icons";
import {setFiltersOfPage} from "../../../utils/filters";
import useFetchYear from "./useYear";
import Switch from "@mui/material/Switch";
import {Tooltip} from "@mui/material";
import IconButtonWithTooltip from "../../../components/buttons/IconButtonWithTooltip";
import {FileDownload} from "@mui/icons-material";
import {fetchGradesAnual} from "../semesterPage/useExportWord";

const AnualGradeToolbar = ({setFilters, filters, setData, checked, setChecked}) => {
    const {mutateAsync: onFetchAcademyClass} = useAcademyClassGeneral();
    const {mutateAsync: onFetchStudents} = useFetchStudents();
    const {mutateAsync: onFetchYear} = useFetchYear();
    const [date, setDate] = useState(new Date())


    return (
        <div>
            <FlexBox justifyContent='space-between'>
                <Formik
                    initialValues={
                        {
                            student: '',
                            academyClass: '',
                            yearRange: '',
                            groupBy: 'STUDENT',
                            semesterN: ''
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
                                                    if(options.length === 1){
                                                        return options[0]
                                                    }
                                                }}/>
                        </div>

                        <div style={{marginLeft: 20, width: 250}}>
                            <FormikAutocomplete name="yearRange"
                                                multiple={false}
                                                label={"წელი"}
                                // resolveData={resolveCardTypeAutocompleteData}
                                                onFetch={onFetchYear}
                                                getOptionSelected={(option, value) => option === value}
                                                getOptionLabel={(option) => option}
                                                setInitialVulue={(options) =>{
                                                    if(options.length === 1){
                                                        return options[0]
                                                    }
                                                }}/>
                        </div>
                        <div style={{marginLeft: 15, width: 100, display:'flex', flexDirection:'row', alignItems:'center'}}>
                            <IconButtonWithTooltip
                                tooltip={"ძიება"}
                                icon={<Search/>}
                                onClick={() => {
                                    setFiltersOfPage("ANNUAL_GRADE", values)
                                    setFilters(values)
                                }}
                            />
                            <div style={{marginLeft: 0, width: 40}}>
                                <IconButtonWithTooltip
                                    icon={<FileDownload/>}
                                    tooltip={"ექსპორტი"}
                                    onClick={async () => {
                                        await fetchGradesAnual(filters, checked)
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
                        </div>
                    </div>)}
                </Formik>
            </FlexBox>
        </div>
    )
}

export default AnualGradeToolbar;