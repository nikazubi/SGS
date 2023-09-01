import FlexBox from "../../../components/FlexBox";
import {Formik} from "formik";
import useAcademyClassGeneral from "../../../hooks/useAcademyClassGeneral";
import {useState} from "react";
import IconButton from "../../../components/buttons/IconButton";
import {Add, Search} from "@material-ui/icons";
import FormikTextField from "../../components/formik/FormikTextField";
import SubjectModal from "./SubjectModal";
import {ModalOpenMode} from "../../../utils/constants";
import IconButtonWithTooltip from "../../../components/buttons/IconButtonWithTooltip";

const SubjectTableToolbar = ({setFilters, filters}) => {
    const [open, setOpen] = useState(false);


    return (
        <div>
            <FlexBox justifyContent='space-between'>
                <Formik
                    initialValues={
                        {
                            name: "",
                        }
                    }
                    onSubmit={() => {
                    }}
                    //validationSchema={}
                    enableReinitialize
                >
                    {({values, setFieldValue}) => (
                        <div style={{display: "flex", flexDirection: 'row', marginTop: 50}}>

                            <div style={{marginLeft: 15, width: 250}}>
                                <FormikTextField name="name" label={"სახელი"}/>
                            </div>
                            <div style={{marginLeft: 10, width: 50}}>
                                <IconButtonWithTooltip
                                    tooltip={"ძიება"}
                                    icon={<Search/>}
                                    onClick={() => setFilters(values)}
                                />
                            </div>
                            <div style={{marginLeft: 10, width: 50}}>
                                <IconButtonWithTooltip
                                    tooltip={"დამატება"}
                                    icon={<Add/>}
                                    onClick={() => setOpen(true)}
                                />
                            </div>
                        </div>)}
                </Formik>
            </FlexBox>
            {open && (
                <SubjectModal
                    open={open}
                    onClose={() => setOpen(false)}
                    modalOpenMode={ModalOpenMode.add}
                />
            )}
        </div>
    )
}

export default SubjectTableToolbar;