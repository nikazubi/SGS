import React from "react";
import {Grid} from "@material-ui/core";
import FlexBox from "../../../components/FlexBox";
import FormikTextField from "../../components/formik/FormikTextField";

const StudentForm = ({modalOpenMode}) => {

    return (
        <div>
            <FlexBox pl={0.5} pr={0.5}>
                <Grid container spacing={1}>
                    <Grid container spacing={1}>
                        <Grid item xs={12} sm={12}>
                            <Grid container spacing={1}>
                                <Grid item xs={12} sm={12}>
                                    <FormikTextField
                                        name={"firstName"}
                                        // variant={"standard"}
                                        label={"მოსწავლის სახელი"}
                                    />
                                </Grid>
                                <Grid item xs={12} sm={12}>
                                    <FormikTextField
                                        name={"lastName"}
                                        // variant={"standard"}
                                        label={"მოსწავლის გვარი"}
                                    />
                                </Grid>
                                <Grid item xs={12} sm={12}>
                                    <FormikTextField
                                        name={"personalNumber"}
                                        // variant={"standard"}
                                        label={"მოსწავლის პირადი ნომერი"}
                                    />
                                </Grid>
                                <Grid item xs={12} sm={12}>
                                    <FormikTextField
                                        name={"username"}
                                        // variant={"standard"}
                                        label={"მომხმარებლის სახელი"}
                                    />
                                </Grid>
                                <Grid item xs={12} sm={12}>
                                    <FormikTextField
                                        name={"password"}
                                        type={"password"}
                                        // variant={"standard"}
                                        label={"მომხმარებლის პაროლი"}
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

export default StudentForm;