import FlexBox from "../../../components/FlexBox";
import {Formik} from "formik";
import FormikAutocomplete from "../../components/formik/FormikAutocomplete";
import useAcademyClassGeneral from "../../../hooks/useAcademyClassGeneral";
import {FormikDatePickerField} from "../../components/formik/FormikDatePickerField";
import {useEffect, useState} from "react";
import {Search} from "@material-ui/icons";
import Button from "../../../components/buttons/Button";
import {useUserContext} from "../../../contexts/user-context";
import axios from "../../../utils/axios";
import moment from "moment";
import {setFiltersOfPage} from "../../../utils/filters";
import {useNotification} from "../../../contexts/notification-context";
import ChangeRequestAcademyClassFormModal from "./ChangeRequestAcademyClassFormModal";
import IconButtonWithTooltip from "../../../components/buttons/IconButtonWithTooltip";

const ClosePeriodTableToolbar = ({setFilters, filters}) => {
    const {mutateAsync: onFetchAcademyClass} = useAcademyClassGeneral();
    const [date, setDate] = useState(new Date());
    const {hasPermission} = useUserContext();
    const [lastCloseDate, setLastCloseDate] = useState("");
    const [lastCloseDateInDateFormat, setLastCloseDateInDateFormat] = useState();
    const hasManageClosePeriodPermission = hasPermission("MANAGE_CLOSED_PERIOD")
    const {setNotification, setErrorMessage} = useNotification();
    const [modalOpen, setOpenModal] = useState(false);


    useEffect(() => {
        axios.get("change-request/get-last-update-time")
            .then(({data}) => {
                setLastCloseDateInDateFormat(new Date(data))
                setLastCloseDate(data ? moment.utc(data).local().format("DD-MM-YYYY") : "")
            })
    }, []);


    return (
        <div style={{width: '100%'}}>
            <FlexBox justifyContent='space-between'>
                <Formik
                    initialValues={
                        {
                            academyClass: filters.academyClass || '',
                            dateFrom: filters.dateFrom || new Date(date.getFullYear(), date.getMonth() - 1, date.getDay()),
                            dateTo: filters.dateTo || date,
                        }
                    }
                    onSubmit={() => {
                    }}
                    //validationSchema={}
                    enableReinitialize
                >
                    {({values, setFieldValue}) => (
                        <div style={{display: "flex", flexDirection: 'row', marginTop: 50,}}>
                            <div style={{marginLeft: 15, width: 250}}>
                                <FormikAutocomplete name="academyClass"
                                                    multiple={false}
                                                    label={"კლასი"}
                                    // resolveData={resolveCardTypeAutocompleteData}
                                                    onFetch={onFetchAcademyClass}
                                                    getOptionSelected={(option, value) => option.id === value.id}
                                                    getOptionLabel={(option) => option.className}
                                    // onBlur={()=> setFilters(values)}
                                />
                            </div>

                            <div style={{marginLeft: 20, width: 250}}>
                                <FormikDatePickerField name="dateFrom"
                                                       label={"შექმნის თარიღი -დან"}
                                                       onChange={(event, value) => {
                                                           setFieldValue("dateFrom", value)
                                                           setFilters(prevState => {
                                                               const copied = prevState;
                                                               copied.dateFrom = value
                                                               return copied
                                                           })
                                                       }}/>
                            </div>
                            <div style={{marginLeft: 20, width: 250}}>
                                <FormikDatePickerField name="dateTo"
                                                       label={"შექმნის თარიღი -მდე"}
                                                       onChange={(event, value) => {
                                                           setFieldValue("dateTo", value)
                                                           setFilters(prevState => {
                                                               const copied = prevState;
                                                               copied.dateTo = value
                                                               return copied
                                                           })
                                                       }}/>
                            </div>
                            <div style={{marginLeft: 10, width: 50}}>
                                <IconButtonWithTooltip
                                    tooltip={"ძიება"}
                                    icon={<Search/>}
                                    onClick={() => {
                                        setFiltersOfPage("CHANGE_REQUEST", values)
                                        setFilters(values)
                                    }}
                                />
                            </div>
                            {hasManageClosePeriodPermission &&
                                <div style={{marginLeft: 15, display: 'flex', flexDirection: 'row'}}>
                                    <div style={{display: 'flex', flexDirection: 'column', alignItems: 'center'}}>
                                        <div>{"დახურვის თარიღი: "}</div>
                                        <div style={{color: "#e46c0a", fontWeight: 'bold'}}>{lastCloseDate}</div>
                                    </div>
                                    <div>
                                        <Button style={{
                                            backgroundColor: new Date().getUTCFullYear() === new Date(new Date(lastCloseDateInDateFormat)).getUTCFullYear() &&
                                            new Date().getUTCMonth() === new Date(new Date(lastCloseDateInDateFormat)).getUTCMonth() &&
                                            new Date().getUTCDate() === new Date(new Date(lastCloseDateInDateFormat)).getUTCDate() ?
                                                "grey" :
                                                "#e46c0a",
                                            color: "#fff",
                                            marginLeft: 15,
                                            marginBottom: -20,
                                            fontSize: 16
                                        }}
                                                disabled={false}
                                                onClick={async () => {
                                                    setOpenModal(true)
                                                    // closePeriod().then(() =>{
                                                    //     setNotification({
                                                    //         message: 'თვის ნიშანი წარმატებით დაითვალა',
                                                    //         severity: 'success'
                                                    //     });
                                                    // }).catch((error) => {
                                                    //     setErrorMessage(error);
                                                    // });
                                                }}>
                                            {"პერიოდის დახურვა"}
                                        </Button>
                                    </div>
                                </div>}
                        </div>)}
                </Formik>
            </FlexBox>
            {modalOpen && (
                <ChangeRequestAcademyClassFormModal
                    open={modalOpen}
                    onClose={() => setOpenModal(false)}
                />
            )}
        </div>
    )
}

export default ClosePeriodTableToolbar;