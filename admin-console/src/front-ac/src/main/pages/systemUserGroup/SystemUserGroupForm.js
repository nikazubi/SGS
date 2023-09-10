import React, {useState} from "react";
import {Grid} from "@material-ui/core";
import FlexBox from "../../../components/FlexBox";
import FormikTextField from "../../components/formik/FormikTextField";
import FormikAutocomplete from "../../components/formik/FormikAutocomplete";
import {useFormikContext} from "formik";
import Switch from "@mui/material/Switch";
import {PERMISSION_OPTIONS} from "./permissions";

const SystemUserGroupForm = ({modalOpenMode}) => {
    const {setFieldValue, values} = useFormikContext();
    const [checked, setChecked] = useState(values.active);

    return (
        <Grid>
            <FlexBox pl={0.5} pr={0.5}>
                <Grid container spacing={1}>
                    <Grid container spacing={1}>
                        <Grid item xs={12} sm={12}>
                            <Grid container spacing={1}>
                                <Grid item xs={12} sm={12}>
                                    <FormikTextField
                                        name={"name"}
                                        // variant={"standard"}
                                        label={"სახელი"}
                                    />
                                </Grid>
                                <Grid item xs={12} sm={12}>
                                    <FormikAutocomplete name="permission"
                                                        multiple={true}
                                                        label={"უფლება"}
                                                        minLengthForSearch={300}
                                                        onFetch={() => {
                                                        }}
                                                        options={PERMISSION_OPTIONS}
                                                        getOptionSelected={(option, value) => {

                                                            return option.value === value.value
                                                        }}
                                                        getOptionLabel={(option) => option.label}
                                                        hideTagsOnOverflow={true}
                                    />
                                </Grid>

                                <Grid item xs={12} sm={12}>
                                    <div style={{display: 'flex', flexDirection: 'row', alignItems: 'center'}}>
                                        <div>
                                            {"აქტიური"}
                                        </div>
                                        <Switch
                                            checked={checked}
                                            onChange={() => {
                                                setFieldValue("active", !checked);
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

export default SystemUserGroupForm;