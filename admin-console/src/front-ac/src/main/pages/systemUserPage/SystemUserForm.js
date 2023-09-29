import React, {useState} from "react";
import {Grid} from "@material-ui/core";
import FlexBox from "../../../components/FlexBox";
import FormikTextField from "../../components/formik/FormikTextField";
import FormikAutocomplete from "../../components/formik/FormikAutocomplete";
import useAcademyClassGeneral from "../../../hooks/useAcademyClassGeneral";
import useSystemGroup from "./useSystemGroup";
import {Tooltip} from "@mui/material";
import MTIconButton from "@material-ui/core/IconButton";
import {Visibility, VisibilityOff} from "@mui/icons-material";


const SystemUserForm = ({modalOpenMode}) => {
    const {mutateAsync: onFetchAcademyClass} = useAcademyClassGeneral();
    const {mutateAsync: onFetchSystemGroup} = useSystemGroup();
    const [isVisible, setIsVisible] = useState(false);

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
                                    <div style={{display: "flex", flexDirection: 'row'}}>
                                        <FormikTextField
                                            name={"password"}
                                            type={isVisible ? "text" : "password"}
                                            // variant={"standard"}
                                            label={"პაროლი"}
                                            style={{width: 630}}
                                        />
                                        <Tooltip title={isVisible ? "დამალვა" : "გამოჩენა"}>
                                            <MTIconButton
                                                color="primary"
                                                onClick={() => setIsVisible(!isVisible)}
                                            >
                                                {isVisible ? <VisibilityOff/> : <Visibility/>}
                                            </MTIconButton>
                                        </Tooltip>
                                    </div>
                                </Grid>
                                <Grid item xs={12} sm={12}>
                                    <FormikAutocomplete name="academyClasses"
                                                        multiple={true}
                                                        label={"კლასები"}
                                                        onFetch={onFetchAcademyClass}
                                                        getOptionSelected={(option, value) => option.id === value.id}
                                                        getOptionLabel={(option) => option.className}
                                                        hideTagsOnOverflow={true}
                                    />
                                </Grid>
                                <Grid item xs={12} sm={12}>
                                    <FormikAutocomplete name="systemGroup"
                                                        multiple={true}
                                                        label={"სისტემური ჯგუფები"}
                                                        onFetch={onFetchSystemGroup}
                                                        getOptionSelected={(option, value) => option.id === value.id}
                                                        getOptionLabel={(option) => option.name}
                                                        hideTagsOnOverflow={true}
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