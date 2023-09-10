import FlexBox from "../../../components/FlexBox";
import {Formik} from "formik";
import {useState} from "react";
import {Add, Search} from "@material-ui/icons";
import FormikTextField from "../../components/formik/FormikTextField";
import AcademyClassModal from "./SystemUserGroupModal";
import {ModalOpenMode} from "../../../utils/constants";
import IconButtonWithTooltip from "../../../components/buttons/IconButtonWithTooltip";
import FormikAutocomplete from "../../components/formik/FormikAutocomplete";
import {PERMISSION_OPTIONS} from "./permissions";

const SystemUserGroupTableToolbar = ({setFilters, filters}) => {
    const [open, setOpen] = useState(false);


    return (
        <div>
            <FlexBox justifyContent='space-between'>
                <Formik
                    initialValues={
                        {
                            name: "",
                            permission: ""
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
                            <div style={{marginLeft: 15, width: 250}}>
                                <FormikAutocomplete name="permission"
                                                    multiple={false}
                                                    label={"უფლება"}
                                                    minLengthForSearch={300}
                                                    onFetch={() => {
                                                    }}
                                                    options={PERMISSION_OPTIONS}
                                                    getOptionLabel={(option) => option.label}
                                />
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
                <AcademyClassModal
                    open={open}
                    onClose={() => setOpen(false)}
                    modalOpenMode={ModalOpenMode.add}
                />
            )}
        </div>
    )
}

export default SystemUserGroupTableToolbar;