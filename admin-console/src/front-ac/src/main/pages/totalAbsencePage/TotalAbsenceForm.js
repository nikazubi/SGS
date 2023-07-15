import React from "react";
import AddIcon from "@material-ui/icons/Add";
import { Grid } from "@material-ui/core";
import FlexBox from "../../../components/FlexBox";
import FormikTextField from "../../components/formik/FormikTextField";
import FormikAutocomplete from "../../components/formik/FormikAutocomplete";
import useAcademyClassGeneral from "../../../hooks/useAcademyClassGeneral";
import {FormikDatePickerField} from "../../components/formik/FormikDatePickerField";
import {useFormikContext} from "formik";

const TotalAbsenceFormForm = ({modalOpenMode}) => {
    const {mutateAsync: onFetchAcademyClass} = useAcademyClassGeneral();
    const {setFieldValue} = useFormikContext();

    return (
        <div>
            <FlexBox pl={0.5} pr={0.5}>
                <Grid container spacing={1}>
                    <Grid container spacing={1}>
                        <Grid item xs={12} sm={12}>
                            <Grid container spacing={1}>
                                <Grid item xs={12} sm={12}>
                                    <FormikAutocomplete name="academyClasses"
                                                        multiple={true}
                                                        label={"კლასები"}
                                                        onFetch={onFetchAcademyClass}
                                                        getOptionSelected={(option, value) => option.id === value.id}
                                                        getOptionLabel={(option) => option.className}
                                                        />
                                </Grid>
                                <Grid item xs={8} sm={8}>
                                    <FormikDatePickerField name="activePeriod"
                                                           label={"თვე"}
                                                           format={'yyyy-MM-dd'}
                                                           onChange={(event, value)=> {
                                                               setFieldValue("activePeriod", value)
                                                               // setFilters(prevState => {
                                                               //     const copied = prevState;
                                                               //     copied.date = value
                                                               //     return copied
                                                               // })
                                                           }}/>
                                </Grid>
                                <Grid item xs={4} sm={4}>
                                    <FormikTextField
                                        name={"totalAcademyHour"}
                                        type={"number"}
                                        // variant={"standard"}
                                        label={"თვიური აკადემიური საათი"}
                                    />
                                </Grid>
                            </Grid>
                        </Grid>
                    </Grid>
                </Grid>
            </FlexBox>
        </div>
    );
};

export default TotalAbsenceFormForm;