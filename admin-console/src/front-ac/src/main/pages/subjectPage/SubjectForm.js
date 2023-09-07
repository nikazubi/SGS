import React from "react";
import {Grid} from "@material-ui/core";
import FlexBox from "../../../components/FlexBox";
import FormikTextField from "../../components/formik/FormikTextField";

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
                                    <FormikTextField
                                        name={"name"}
                                        // variant={"standard"}
                                        label={"მასწავლებლის სახელი და გვარი"}
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