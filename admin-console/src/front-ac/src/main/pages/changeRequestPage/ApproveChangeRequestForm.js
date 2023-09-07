import {Grid} from "@material-ui/core";
import React from "react";
import FormikTextField from "../../components/formik/FormikTextField";


const ApproveChangeRequestForm = () => {

    return (
        <Grid container spacing={2}>
            <Grid item xs={12} sm={12}>
                {"მიუთითეთ სასურველი ახსნა-განმარტება, რომელიც გაიგზავნება მშობელთან"}
            </Grid>
            <Grid item xs={12} sm={12}>
                <FormikTextField name="description"
                                 label={"ახსნა-განმარტება"}
                                 rowNum={5}
                                 multiline={true}
                />
            </Grid>
        </Grid>
    )
}

export default ApproveChangeRequestForm;