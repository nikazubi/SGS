import FlexBox from "../../../components/FlexBox";
import {Formik} from "formik";
import {useState} from "react";
import {Add, Search} from "@material-ui/icons";
import FormikTextField from "../../components/formik/FormikTextField";
import StudentModal from "./StudentModal";
import {ModalOpenMode} from "../../../utils/constants";
import IconButtonWithTooltip from "../../../components/buttons/IconButtonWithTooltip";

const StudentTableToolbar = ({setFilters, filters}) => {
    const [open, setOpen] = useState(false);


    return (
        <div>
            <FlexBox justifyContent='space-between'>
                <Formik
                    initialValues={
                        {
                            firstName: '',
                            lastName: '',
                            personalNumber: ''
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
                                <FormikTextField name="firstName" label={"მოსწავლის სახელი"}/>
                            </div>
                            <div style={{marginLeft: 20, width: 250}}>
                                <FormikTextField name="lastName" label={"მოსწავლის გვარი"}/>
                            </div>
                            <div style={{marginLeft: 20, width: 250}}>
                                <FormikTextField name="personalNumber" label={"მოსწავლის პირადი ნომერი"}/>
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
                <StudentModal
                    open={open}
                    onClose={() => setOpen(false)}
                    modalOpenMode={ModalOpenMode.add}
                />
            )}
        </div>
    )
}

export default StudentTableToolbar;