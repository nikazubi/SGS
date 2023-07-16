import FlexBox from "../../../components/FlexBox";
import {Formik} from "formik";
import useAcademyClassGeneral from "../../../hooks/useAcademyClassGeneral";
import {useState} from "react";
import IconButton from "../../../components/buttons/IconButton";
import {Add, Search} from "@material-ui/icons";
import FormikTextField from "../../components/formik/FormikTextField";
import StudentModal from "./StudentModal";
import {ModalOpenMode} from "../../../utils/constants";

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
                           personalNumber:''
                        }
                    }
                    onSubmit={() => {
                    }}
                    //validationSchema={}
                    enableReinitialize
                >
                    {({values, setFieldValue}) => (
                        <div style={{display: "flex", flexDirection: 'row', marginTop: 50}}>

                            <div style={{marginLeft: 50, width: 300}}>
                                <FormikTextField name="firstName" label={"მოსწავლის სახელი"}/>
                            </div>
                            <div style={{marginLeft: 50, width: 300}}>
                                <FormikTextField name="lastName" label={"მოსწავლის გვარი"}/>
                            </div>
                            <div style={{marginLeft: 50, width: 300}}>
                                <FormikTextField name="personalNumber" label={"მოსწავლის პირადი ნომერი"}/>
                            </div>
                            <div style={{marginLeft: 15, width: 100}}>
                                <IconButton
                                    icon={<Search/>}
                                    onClick={() => setFilters(values)}
                                />
                            </div>
                            <div style={{marginLeft: 15, width: 100}}>
                                <IconButton
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