import FlexBox from "../../../components/FlexBox";
import {Formik} from "formik";
import FormikAutocomplete from "../../components/formik/FormikAutocomplete";
import useAcademyClassGeneral from "../../../hooks/useAcademyClassGeneral";
import useFetchStudents from "../../../hooks/useStudents";
import {FormikDatePickerField} from "../../components/formik/FormikDatePickerField";
import {useState} from "react";
import IconButton from "../../../components/buttons/IconButton";
import {Add, Search} from "@material-ui/icons";
import TotalAbsenceModal from "./TotalAbsenceFormModal";
import IconButtonWithTooltip from "../../../components/buttons/IconButtonWithTooltip";

const TotalAbsenceTableToolbar = ({setFilters, filters}) => {
    const {mutateAsync: onFetchAcademyClass} = useAcademyClassGeneral({});
    const [date, setDate] = useState(new Date());
    const [open, setOpen] = useState(false);


    return (
        <div>
            <FlexBox justifyContent='space-between'>
                <Formik
                    initialValues={
                        {
                            academyClassId: null,
                            activePeriod: date,
                        }
                    }
                    onSubmit={() => {
                    }}
                    //validationSchema={}
                    enableReinitialize
                >
                    {({ values, setFieldValue }) => (
                    <div style={{display: "flex", flexDirection: 'row', marginTop: 50}}>
                        <div style={{marginLeft: 15, width: 250}}>
                            <FormikAutocomplete name="academyClass"
                                                multiple={false}
                                                label={"კლასი"}
                                // resolveData={resolveCardTypeAutocompleteData}
                                                onFetch={onFetchAcademyClass}
                                                getOptionSelected={(option, value) => option.id === value.id}
                                                getOptionLabel={(option) => option.className}
                                                onBlur={()=> setFilters(values)}/>
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
                                onClick={() => setFilters(values)}
                            />
                        </div>
                        <div style={{marginLeft: 10, width: 50}}>
                            <IconButtonWithTooltip
                                tooltip={"დამატება"}
                                icon={<Add/>}
                                onClick={() => setOpen(true)}
                            />
                        </div>
                    </div>)}
                </Formik>
            </FlexBox>
            {open && (
                <TotalAbsenceModal
                    open={open}
                    onClose={() => setOpen(false)}
                />
            )}
        </div>
    )
}

export default TotalAbsenceTableToolbar;