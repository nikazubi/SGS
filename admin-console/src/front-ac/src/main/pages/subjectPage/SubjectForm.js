import React from "react";
import AddIcon from "@material-ui/icons/Add";
import { Grid } from "@material-ui/core";
import FlexBox from "../../../components/FlexBox";
import FormikTextField from "../../components/formik/FormikTextField";
import FormikAutocomplete from "../../components/formik/FormikAutocomplete";
import useAcademyClassGeneral from "../../../hooks/useAcademyClassGeneral";
import {FormikDatePickerField} from "../../components/formik/FormikDatePickerField";
import {useFormikContext} from "formik";
import useCreateSubject from "./useCreateSubject";
import {updateSubject} from "./useUpdateSubject";

const SubjectForm = ({modalOpenMode}) => {

    return (
        <div>
            <FlexBox pl={0.5} pr={0.5}>
                <Grid container spacing={1}>
                    <Grid container spacing={1}>
                        <Grid item xs={12} sm={12}>
                            <Grid container spacing={1}>
                                <Grid item xs={12} sm={12}>
                                    <FormikTextField
                                        name={"name"}
                                        // variant={"standard"}
                                        label={"საგნის სახელი"}
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

export default SubjectForm;