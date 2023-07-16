import React from "react";
import { Grid } from "@material-ui/core";
import FlexBox from "../../../components/FlexBox";
import FormikTextField from "../../components/formik/FormikTextField";
import FormikAutocomplete from "../../components/formik/FormikAutocomplete";
import useAcademyClassGeneral from "../../../hooks/useAcademyClassGeneral";
import useSystemGroup from "./useSystemGroup";


const SystemUserForm = ({modalOpenMode}) => {
    const {mutateAsync: onFetchAcademyClass} = useAcademyClassGeneral();
    const {mutateAsync: onFetchSystemGroup} = useSystemGroup();

    return (
        <div>
            <FlexBox pl={0.5} pr={0.5}>
                <Grid container spacing={1}>
                    <Grid container spacing={1}>
                        <Grid item xs={12} sm={12}>
                            <Grid container spacing={1}>
                                <Grid item xs={12} sm={12}>
                                    <FormikTextField
                                        name={"username"}
                                        type={"text"}
                                        // variant={"standard"}
                                        label={"მომხმარებლის სახელი"}
                                    />
                                </Grid>
                                <Grid item xs={12} sm={12}>
                                    <FormikTextField
                                        name={"name"}
                                        type={"text"}
                                        // variant={"standard"}
                                        label={"სახელი"}
                                    />
                                </Grid>
                                <Grid item xs={12} sm={12}>
                                    <FormikTextField
                                        name={"email"}
                                        type={"text"}
                                        // variant={"standard"}
                                        label={"ელ. ფოსტა"}
                                    />
                                </Grid>
                                <Grid item xs={12} sm={12}>
                                    <FormikTextField
                                        name={"password"}
                                        type={"password"}
                                        // variant={"standard"}
                                        label={"პაროლი"}
                                    />
                                </Grid>
                                <Grid item xs={12} sm={12}>
                                    <FormikAutocomplete name="academyClasses"
                                                        multiple={true}
                                                        label={"კლასები"}
                                                        onFetch={onFetchAcademyClass}
                                                        getOptionSelected={(option, value) => option.id === value.id}
                                                        getOptionLabel={(option) => option.className}
                                    />
                                </Grid>
                                <Grid item xs={12} sm={12}>
                                    <FormikAutocomplete name="systemGroup"
                                                        multiple={true}
                                                        label={"სუსყემური ჯგუფები"}
                                                        onFetch={onFetchSystemGroup}
                                                        getOptionSelected={(option, value) => option.id === value.id}
                                                        getOptionLabel={(option) => option.name}
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

export default SystemUserForm;