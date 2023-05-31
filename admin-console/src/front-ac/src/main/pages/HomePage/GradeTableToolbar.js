import FlexBox from "../../../components/FlexBox";
import {Formik} from "formik";
import FormikTextField from "../../components/formik/FormikTextField";
import FormikAutocomplete from "../../components/formik/FormikAutocomplete";
import useSubjects from "./useSubjects";

const GradeTableToolbar = () => {
    const {mutateAsync: onFetchSubjects} = useSubjects();

    return (
        <div>
            <FlexBox justifyContent='space-between'>
                <Formik
                    initialValues={
                        {
                            student: '',
                            academyClass: '',
                            subject: '',
                            date: '',
                            groupBy: 'STUDENT',
                        }
                    }
                    onSubmit={() => {}}
                    //validationSchema={}
                    enableReinitialize
                >
                <div>
                    <FormikAutocomplete  name="subject"
                                         multiple={false}
                                         // resolveData={resolveCardTypeAutocompleteData}
                                         onFetch={onFetchSubjects}
                                         getOptionSelected={(option, value) => option.id === value.id}
                                         getOptionLabel={(option) => option.name}/>
                </div>
                </Formik>
            </FlexBox>
        </div>
    )
}

export default GradeTableToolbar;