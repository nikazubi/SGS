import FlexBox from "../../../components/FlexBox";
import {Formik} from "formik";
import FormikAutocomplete from "../../components/formik/FormikAutocomplete";
import useAcademyClassGeneral from "../../../hooks/useAcademyClassGeneral";
import useFetchStudents from "../../../hooks/useStudents";
import {Search} from "@material-ui/icons";
import {setFiltersOfPage} from "../../../utils/filters";
import IconButtonWithTooltip from "../../../components/buttons/IconButtonWithTooltip";
import useFetchYear from "../anualPage/useYear";

const AbsenceTableToolbar = ({setFilters, filters, setData, checked, setChecked}) => {
    const {mutateAsync: onFetchAcademyClass} = useAcademyClassGeneral();
    const {mutateAsync: onFetchStudents} = useFetchStudents();
    const {mutateAsync: onFetchYear} = useFetchYear();


    return (
        <div>
            <FlexBox justifyContent='space-between'>
                <Formik
                    initialValues={
                        {
                            student: '',
                            academyClass: '',
                            yearRange: '',
                        }
                    }
                    onSubmit={() => {
                    }}
                    //validationSchema={}
                    enableReinitialize
                >
                    {({ values, setFieldValue }) => (
                        <div style={{display: "flex", flexDirection: 'row', marginTop: 25, marginBottom: 25}}>
                            <div style={{marginLeft: 0, width: 250}}>
                                <FormikAutocomplete name="academyClass"
                                                    multiple={false}
                                                    label={"კლასი"}
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
                            <div style={{marginLeft: 20, width: 250}}>
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
                                                    }}/>
                            </div>

                            <div style={{marginLeft: 20, width: 250}}>
                                <FormikAutocomplete name="yearRange"
                                                    multiple={false}
                                                    label={"წელი"}
                                    // resolveData={resolveCardTypeAutocompleteData}
                                                    onFetch={onFetchYear}
                                                    getOptionSelected={(option, value) => option.id === value.id}
                                                    getOptionLabel={(option) => option}
                                                    setInitialVulue={(options) => {
                                                        if (options.length === 1) {
                                                            return options[0]
                                                        }
                                                    }}/>
                            </div>
                            <div style={{
                                marginLeft: 15,
                                width: 100,
                                display: 'flex',
                                flexDirection: 'row',
                                alignItems: 'center'
                            }}>
                                <IconButtonWithTooltip
                                    tooltip={"ძიება"}
                                    icon={<Search/>}
                                    onClick={() => {
                                        setFiltersOfPage("ANNUAL_GRADE", values)
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

export default AbsenceTableToolbar;