import FlexBox from "../../../components/FlexBox";
import {Formik} from "formik";
import FormikAutocomplete from "../../components/formik/FormikAutocomplete";
import useAcademyClassGeneral from "../../../hooks/useAcademyClassGeneral";
import useFetchStudents from "../../../hooks/useStudents";
import {Search} from "@material-ui/icons";
import {setFiltersOfPage} from "../../../utils/filters";
import IconButtonWithTooltip from "../../../components/buttons/IconButtonWithTooltip";
import useSubjects from "../../../hooks/useSubjects";
import {useCallback} from "react";

const TrimesterGradeToolbar = ({setFilters, filters, setData, checked, setChecked}) => {
    const {mutateAsync: onFetchAcademyClass} = useAcademyClassGeneral();
    const {mutateAsync: onFetchStudents} = useFetchStudents();
    const {mutateAsync: onFetchSubjects} = useSubjects();


    const options = [
        {value: 1, label: 'I ტრიმესტრი'},
        {value: 2, label: 'II ტრიმესტრი'},
        {value: 3, label: 'III ტრიმესტრი'},
    ];

    const resoleSubjectData = useCallback((data, values) => {
        if ((!values.academyClass || !values.academyClass.id)) {
            return data;
        }
        const subjectIds = values.academyClass?.subjectList.map(subject => subject.id)
        return data.filter(subject => subjectIds.includes(subject.id))
    }, [])

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
                            trimesterN: ''
                        }
                    }
                    onSubmit={() => {
                    }}
                    //validationSchema={}
                    enableReinitialize
                >
                    {({values, setFieldValue}) => (
                        <div style={{display: "flex", flexDirection: 'row', marginTop: 25, marginBottom: 25}}>
                            <div style={{width: 200}}>
                                <FormikAutocomplete name="trimesterN"
                                                    multiple={false}
                                                    label={"ტრიმესტრი"}
                                                    minLengthForSearch={300}
                                                    onFetch={() => {
                                                    }}
                                                    options={options}
                                                    getOptionLabel={(option) => option.label}
                                                    getOptionSelected={(option, value) => {
                                                        return option.id === value.id
                                                    }}
                                />
                            </div>
                            <div style={{marginLeft: 20, width: 200}}>
                                <FormikAutocomplete name="academyClass"
                                                    multiple={false}
                                                    label={"კლასი"}
                                    // resolveData={resolveCardTypeAutocompleteData}
                                                    onFetch={onFetchAcademyClass}
                                                    getOptionSelected={(option, value) => {

                                                        return option.id === value.id
                                                    }}
                                                    getOptionLabel={(option) => option.className}
                                                    setInitialVulue={(options) => {
                                                        if (options.length === 1) {
                                                            return options[0]
                                                        }
                                                    }}/>
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
                            <div style={{marginLeft: 20, width: 200}}>
                                <FormikAutocomplete name="student"
                                                    multiple={false}
                                                    label={"მოსწავლე"}
                                    // resolveData={resolveCardTypeAutocompleteData}
                                                    onFetch={onFetchStudents}
                                                    getOptionSelected={(option, value) => option.id === value.id}
                                                    getOptionLabel={(option) => option.firstName + " " + option.lastName}
                                                    setInitialVulue={(options) => {
                                                        if (options.length === 1) {
                                                            return options[0]
                                                        }
                                                    }}/>
                            </div>

                            {/*<div style={{marginLeft: 10, width: 200}}>*/}
                            {/*    <FormikAutocomplete name="yearRange"*/}
                            {/*                        multiple={false}*/}
                            {/*                        label={"წელი"}*/}
                            {/*        // resolveData={resolveCardTypeAutocompleteData}*/}
                            {/*                        onFetch={onFetchYear}*/}
                            {/*                        getOptionSelected={(option, value) => option.id === value.id}*/}
                            {/*                        getOptionLabel={(option) => option}*/}
                            {/*                        setInitialVulue={(options) => {*/}
                            {/*                            if (options.length === 1) {*/}
                            {/*                                return options[0]*/}
                            {/*                            }*/}
                            {/*                        }}/>*/}
                            {/*</div>*/}
                            <div style={{marginLeft: 10, width: 50}}>
                                <IconButtonWithTooltip
                                    icon={<Search/>}
                                    tooltip={"ძიება"}
                                    onClick={() => {
                                        setFiltersOfPage("TRIMESTER", values)
                                        setFilters(values)
                                    }}
                                />
                            </div>
                            {/* <div style={{marginLeft: -10, width: 40}}>*/}
                            {/*         <IconButtonWithTooltip*/}
                            {/*             icon={<FileDownload/>}*/}
                            {/*             tooltip={"ექსპორტი"}*/}
                            {/*             onClick={async () => {*/}
                            {/*                 await fetchGradesSemester(filters, checked)*/}
                            {/*             }}*/}
                            {/*         />*/}
                            {/*</div>*/}
                            {/* <Tooltip title={checked? "10 ბალიანი სისტემა" : "7 ბალიანი სისტემა"}>*/}
                            {/* <div style={{marginLeft: 0, width: 50}}>*/}
                            {/*     <Switch*/}
                            {/*         checked={checked}*/}
                            {/*         onChange={()=> setChecked(!checked)}*/}
                            {/*         inputProps={{ 'aria-label': 'controlled' }}*/}
                            {/*     />*/}
                            {/* </div>*/}
                            {/* </Tooltip>*/}
                        </div>)}
                </Formik>
            </FlexBox>
        </div>
    )
}

export default TrimesterGradeToolbar;