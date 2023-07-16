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
import useFetchYear from "./useYear";

const SemesterGradeToolbar = ({setFilters, filters, setData}) => {
    const {mutateAsync: onFetchAcademyClass} = useAcademyClassGeneral();
    const {mutateAsync: onFetchStudents} = useFetchStudents();
    const {mutateAsync: onFetchYear} = useFetchYear();
    const [date, setDate] = useState(new Date())

    const options = [
        { value: 'firstSemester', label: 'I სემესტრი' },
        { value: 'secondSemester', label: 'II სემესტრი' },
    ];


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
                        <div style={{marginLeft: 50, width: 300}}>
                            <FormikAutocomplete name="semesterN"
                                                multiple={false}
                                                label={"სემესტრი"}
                                                minLengthForSearch={300}
                                                onFetch={()=>{}}
                                                options={options}
                                                getOptionLabel={(option) => option.label}
                            />
                        </div>
                        <div style={{marginLeft: 50, width: 300}}>
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
                            <FormikAutocomplete name="yearRange"
                                                multiple={false}
                                                label={"წელი"}
                                // resolveData={resolveCardTypeAutocompleteData}
                                                onFetch={onFetchYear}
                                                getOptionSelected={(option, value) => option.id === value.id}
                                                getOptionLabel={(option) => option}
                                                setInitialVulue={(options) =>{
                                                    if(options.length === 1){
                                                        return options[0]
                                                    }
                                                }}/>
                        </div>
                        <div style={{marginLeft: 15, width: 100}}>
                            <IconButton
                                icon={<Search/>}
                                onClick={() => {
                                    setFiltersOfPage("SEMESTER_GRADE", values)
                                    setFilters(values)
                                }}
                            />
                        </div>
                    </div>)}
                </Formik>
            </FlexBox>
        </div>
    )
}

export default SemesterGradeToolbar;