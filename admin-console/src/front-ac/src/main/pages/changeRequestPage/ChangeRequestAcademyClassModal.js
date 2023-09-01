import FormikAutocomplete from "../../components/formik/FormikAutocomplete";
import {Grid} from "@material-ui/core";
import React from "react";
import useAcademyClassGeneral from "../../../hooks/useAcademyClassGeneral";


const ChangeRequestAcademyClassModal = () => {
    const {mutateAsync: onFetchAcademyClass} = useAcademyClassGeneral();

    return (
        <Grid container spacing={2}>
            <Grid item xs={12} sm={12}>
                {"მიუთითეთ სასურველი კლასი. არცერთის მითითება გულისხმობს ყველას მონიშვნას"}
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
        </Grid>
    )
}

export default ChangeRequestAcademyClassModal;