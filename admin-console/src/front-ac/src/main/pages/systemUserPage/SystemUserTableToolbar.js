import FlexBox from "../../../components/FlexBox";
import {Formik} from "formik";
import FormikAutocomplete from "../../components/formik/FormikAutocomplete";
import useAcademyClassGeneral from "../../../hooks/useAcademyClassGeneral";
import {useState} from "react";
import IconButton from "../../../components/buttons/IconButton";
import {Add, Search} from "@material-ui/icons";
import FormikTextField from "../../components/formik/FormikTextField";
import SystemUserModal from "./SystemuserFormModal";
import {setFiltersOfPage} from "../../../utils/filters";
import IconButtonWithTooltip from "../../../components/buttons/IconButtonWithTooltip";
import {ModalOpenMode} from "../../../utils/constants";

const TotalAbsenceTableToolbar = ({setFilters, filters}) => {
    const {mutateAsync: onFetchAcademyClass} = useAcademyClassGeneral({});
    const [date, setDate] = useState(new Date());
    const [open, setOpen] = useState(false);

    const options = [
        { value: 'true', label: 'აქტიური' },
        { value: 'false', label: 'არააქტიური' },
    ];


    return (
        <div>
            <FlexBox justifyContent='space-between'>
                <Formik
                    initialValues={
                        {
                            academyClassId: null,
                            activePeriod: date,
                        }
                    }
                    onSubmit={() => {
                    }}
                    //validationSchema={}
                    enableReinitialize
                >
                    {({ values, setFieldValue }) => (
                    <div style={{display: "flex", flexDirection: 'row', marginTop: 50}}>
                        <div style={{marginLeft: 15, width: 250}}>
                            <FormikTextField
                                name={"username"}
                                type={"text"}
                                // variant={"standard"}
                                label={"მომხმარებლის სახელი"}
                            />
                        </div>
                        <div style={{marginLeft: 20, width: 250}}>
                            <FormikTextField
                                name={"name"}
                                type={"text"}
                                // variant={"standard"}
                                label={"სახელი"}
                            />
                        </div>
                        <div style={{marginLeft: 20, width: 250}}>
                            <FormikAutocomplete name="active"
                                                multiple={false}
                                                label={"სტატუსი"}
                                                minLengthForSearch={300}
                                                onFetch={()=>{}}
                                                options={options}
                                                getOptionLabel={(option) => option.label}
                            />
                        </div>

                        <div style={{marginLeft: 10, width: 50}}>
                            <IconButtonWithTooltip
                                tooltip={"ძიება"}
                                icon={<Search/>}
                                onClick={() => {
                                    setFiltersOfPage("MONTHLY_GRADE", values);
                                    setFilters(values);
                                }}
                            />
                        </div>
                        <div style={{marginLeft: 15, width: 100}}>
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
                <SystemUserModal
                    modalOpenMode={ModalOpenMode.add}
                    open={open}
                    onClose={() => setOpen(false)}
                />
            )}
        </div>
    )
}

export default TotalAbsenceTableToolbar;