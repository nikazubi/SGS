import React, {useState} from "react";
import {Grid} from "@material-ui/core";
import FlexBox from "../../../components/FlexBox";
import FormikTextField from "../../components/formik/FormikTextField";
import FormikAutocomplete from "../../components/formik/FormikAutocomplete";
import {useFormikContext} from "formik";
import Switch from "@mui/material/Switch";
import useSubjectsForAcademyClass from "./useSubjectsForAcademyClass";
import useFetchStudentsQuerykey from "../../../hooks/useStudentsWithQuerykey";

const AcademyClassForm = ({modalOpenMode}) => {
    const {mutateAsync: onFetchSubjects} = useSubjectsForAcademyClass();
    const {mutateAsync: onFetchStudents} = useFetchStudentsQuerykey();
    const {setFieldValue, values} = useFormikContext();
    const [checked, setChecked] = useState(values.isTransit);

    return (
        <Grid>
            <FlexBox pl={0.5} pr={0.5}>
                <Grid container spacing={1}>
                    <Grid container spacing={1}>
                        <Grid item xs={12} sm={12}>
                            <Grid container spacing={1}>
                                <Grid item xs={12} sm={12}>
                                    <FormikTextField
                                        name={"className"}
                                        // variant={"standard"}
                                        label={"კლასის სახელი"}
                                    />
                                </Grid>
                                <Grid item xs={12} sm={12}>
                                    <FormikAutocomplete name="subjectList"
                                                        multiple={true}
                                                        label={"საგანი"}
                                                        onFetch={onFetchSubjects}
                                                        getOptionSelected={(option, value) => option.id === value.id}
                                                        getOptionLabel={(option) => option.name}
                                                        hideTagsOnOverflow={true}

                                    />
                                </Grid>
                                <Grid item xs={12} sm={12}>
                                    <FormikAutocomplete name="studentList"
                                                        multiple={true}
                                                        label={"მოსწავლე"}
                                                        onFetch={onFetchStudents}
                                                        getOptionSelected={(option, value) => option.id === value.id}
                                                        hideTagsOnOverflow={true}
                                                        getOptionLabel={(option) => option.firstName + " " + option.lastName}
                                    />
                                </Grid>
                                <Grid item xs={12} sm={12}>
                                    <FormikTextField
                                        name={"classLevel"}
                                        type={"number"}
                                        // variant={"standard"}
                                        label={"კლასის საფეხური"}
                                    />
                                </Grid>
                                <Grid item xs={12} sm={12}>
                                    <div style={{display: 'flex', flexDirection: 'row', alignItems: 'center'}}>
                                        <div>
                                            {"ტრანზიტული კლასი"}
                                        </div>
                                        <Switch
                                            checked={checked}
                                            onChange={() => {
                                                setFieldValue("isTransit", !checked);
                                                setChecked(!checked);
                                            }}
                                            inputProps={{'aria-label': 'controlled'}}
                                        />
                                    </div>
                                </Grid>
                            </Grid>
                        </Grid>
                    </Grid>
                </Grid>
            </FlexBox>
        </Grid>
    );
};

export default AcademyClassForm;