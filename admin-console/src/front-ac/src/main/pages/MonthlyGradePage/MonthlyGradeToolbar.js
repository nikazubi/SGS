import FlexBox from "../../../components/FlexBox";
import {Formik} from "formik";
import FormikAutocomplete from "../../components/formik/FormikAutocomplete";
import useAcademyClassGeneral from "../../../hooks/useAcademyClassGeneral";
import useFetchStudents from "../../../hooks/useStudents";
import {FormikDatePickerField} from "../../components/formik/FormikDatePickerField";
import {useState} from "react";
import IconButton from "../../../components/buttons/IconButton";
import {Search} from "@material-ui/icons";

const MonthlyGradeToolbar = ({setFilters, filters, setData}) => {
    const {mutateAsync: onFetchAcademyClass} = useAcademyClassGeneral();
    const {mutateAsync: onFetchStudents} = useFetchStudents();
    const [date, setDate] = useState(new Date())

    let permanentData = [
        {
            name: "ზუბიაშვილი ნიკა",
            geo: 6,
            geolang: 7,
            write: 3,
            math: 7,
            eng: 7,
            englit:5,
            german:7,
            russia:7,
            bio: 10,
            chemistry: 7,
            phisic: 5,
            history: 10,
            geography: 10,
            nationaly: 9,
            hum: 10,
            sport: 10,
            ethic: 9,
            rating: 6,
            absent: 0,
            mistake:0
        },
        {
            name: "ზუბიაშვილი ანა",
            geo: 6,
            geolang: 5,
            write: 3,
            math: 7,
            eng: 7,
            englit:5,
            german:4,
            russia:7,
            bio: 10,
            chemistry: 7,
            phisic: 5,
            history: 8,
            geography: 8,
            nationaly: 9,
            hum: 10,
            sport: 9,
            ethic: 9,
            rating: 5,
            absent: 0,
            mistake:0
        },
        {
            name: "ზუბიაშვილი ვასო",
            geo: 6,
            geolang: 5,
            write: 3,
            math: 7,
            eng: 7,
            englit:5,
            german:7,
            russia:7,
            bio: 10,
            chemistry: 7,
            phisic: 5,
            history: 10,
            geography: 8,
            nationaly: 9,
            hum: 10,
            sport: 10,
            ethic: 9,
            rating: 10,
            absent: 0,
            mistake:0
        },
        {
            name: "ზუბიაშვილი კოტე",
            geo: 6,
            geolang: 5,
            write: 7,
            math: 6,
            eng: 7,
            englit:9,
            german:7,
            russia:7,
            bio: 5,
            chemistry: 7,
            phisic: 9,
            history: 7,
            geography: 8,
            nationaly: 9,
            hum: 1,
            sport: 10,
            ethic: 9,
            rating: 2,
            absent: 0,
            mistake:0
        },
        {
            name: "ზუბიაშვილი ირაკლი",
            geo: 6,
            geolang: 5,
            write: 3,
            math: 7,
            eng: 7,
            englit:5,
            german:7,
            russia:7,
            bio: 10,
            chemistry: 7,
            phisic: 5,
            history: 10,
            geography: 8,
            nationaly: 9,
            hum: 10,
            sport: 10,
            ethic: 9,
            rating: 10,
            absent: 0,
            mistake:0
        },
        {
            name: "ზუბიაშვილი სოსო",
            geo: 6,
            geolang: 5,
            write: 3,
            math: 7,
            eng: 10,
            englit:2,
            german:3,
            russia:7,
            bio: 4,
            chemistry: 7,
            phisic: 5,
            history: 5,
            geography: 8,
            nationaly: 6,
            hum: 10,
            sport: 3,
            ethic: 9,
            rating: 10,
            absent: 4,
            mistake:0
        },
        {
            name: "ზუბიაშვილი ნანა",
            geo: 6,
            geolang: 5,
            write: 3,
            math: 7,
            eng: 7,
            englit:5,
            german:7,
            russia:7,
            bio: 10,
            chemistry: 7,
            phisic: 5,
            history: 10,
            geography: 8,
            nationaly: 9,
            hum: 10,
            sport: 10,
            ethic: 9,
            rating: 10,
            absent: 0,
            mistake:0
        },
        {
            name: "ზუბიაშვილი ნინო",
            geo: 6,
            geolang: 5,
            write: 3,
            math: 7,
            eng: 7,
            englit:5,
            german:7,
            russia:7,
            bio: 10,
            chemistry: 7,
            phisic: 5,
            history: 10,
            geography: 8,
            nationaly: 9,
            hum: 10,
            sport: 10,
            ethic: 9,
            rating: 10,
            absent: 0,
            mistake:0
        },
        {
            name: "ზუბიაშვილი სალომე",
            geo: 6,
            geolang: 5,
            write: 3,
            math: 7,
            eng: 7,
            englit:5,
            german:7,
            russia:7,
            bio: 10,
            chemistry: 7,
            phisic: 5,
            history: 10,
            geography: 8,
            nationaly: 9,
            hum: 10,
            sport: 10,
            ethic: 9,
            rating: 10,
            absent: 0,
            mistake:0
        },
        {
            name: "ზუბიაშვილი ნიკა",
            geo: 6,
            geolang: 7,
            write: 3,
            math: 7,
            eng: 7,
            englit:5,
            german:7,
            russia:7,
            bio: 10,
            chemistry: 7,
            phisic: 5,
            history: 10,
            geography: 10,
            nationaly: 9,
            hum: 10,
            sport: 10,
            ethic: 9,
            rating: 6,
            absent: 0,
            mistake:0
        },
        {
            name: "ზუბიაშვილი ანა",
            geo: 6,
            geolang: 5,
            write: 3,
            math: 7,
            eng: 7,
            englit:5,
            german:4,
            russia:7,
            bio: 10,
            chemistry: 7,
            phisic: 5,
            history: 8,
            geography: 8,
            nationaly: 9,
            hum: 10,
            sport: 9,
            ethic: 9,
            rating: 5,
            absent: 0,
            mistake:0
        },
        {
            name: "ზუბიაშვილი ვასო",
            geo: 6,
            geolang: 5,
            write: 3,
            math: 7,
            eng: 7,
            englit:5,
            german:7,
            russia:7,
            bio: 10,
            chemistry: 7,
            phisic: 5,
            history: 10,
            geography: 8,
            nationaly: 9,
            hum: 10,
            sport: 10,
            ethic: 9,
            rating: 10,
            absent: 0,
            mistake:0
        },
        {
            name: "ზუბიაშვილი კოტე",
            geo: 6,
            geolang: 5,
            write: 7,
            math: 6,
            eng: 7,
            englit:9,
            german:7,
            russia:7,
            bio: 5,
            chemistry: 7,
            phisic: 9,
            history: 7,
            geography: 8,
            nationaly: 9,
            hum: 1,
            sport: 10,
            ethic: 9,
            rating: 2,
            absent: 0,
            mistake:0
        },
        {
            name: "ზუბიაშვილი ირაკლი",
            geo: 6,
            geolang: 5,
            write: 3,
            math: 7,
            eng: 7,
            englit:5,
            german:7,
            russia:7,
            bio: 10,
            chemistry: 7,
            phisic: 5,
            history: 10,
            geography: 8,
            nationaly: 9,
            hum: 10,
            sport: 10,
            ethic: 9,
            rating: 10,
            absent: 0,
            mistake:0
        },
        {
            name: "ზუბიაშვილი სოსო",
            geo: 6,
            geolang: 5,
            write: 3,
            math: 7,
            eng: 10,
            englit:2,
            german:3,
            russia:7,
            bio: 4,
            chemistry: 7,
            phisic: 5,
            history: 5,
            geography: 8,
            nationaly: 6,
            hum: 10,
            sport: 3,
            ethic: 9,
            rating: 10,
            absent: 4,
            mistake:0
        },
        {
            name: "ზუბიაშვილი ნანა",
            geo: 6,
            geolang: 5,
            write: 3,
            math: 7,
            eng: 7,
            englit:5,
            german:7,
            russia:7,
            bio: 10,
            chemistry: 7,
            phisic: 5,
            history: 10,
            geography: 8,
            nationaly: 9,
            hum: 10,
            sport: 10,
            ethic: 9,
            rating: 10,
            absent: 0,
            mistake:0
        },
        {
            name: "ზუბიაშვილი ნინო",
            geo: 6,
            geolang: 5,
            write: 3,
            math: 7,
            eng: 7,
            englit:5,
            german:7,
            russia:7,
            bio: 10,
            chemistry: 7,
            phisic: 5,
            history: 10,
            geography: 8,
            nationaly: 9,
            hum: 10,
            sport: 10,
            ethic: 9,
            rating: 10,
            absent: 0,
            mistake:0
        },
        {
            name: "ზუბიაშვილი სალომე",
            geo: 6,
            geolang: 5,
            write: 3,
            math: 7,
            eng: 7,
            englit:5,
            german:7,
            russia:7,
            bio: 10,
            chemistry: 7,
            phisic: 5,
            history: 10,
            geography: 8,
            nationaly: 9,
            hum: 10,
            sport: 10,
            ethic: 9,
            rating: 10,
            absent: 0,
            mistake:0
        }
    ]

    return (
        <div>
            <FlexBox justifyContent='space-between'>
                <Formik
                    initialValues={
                        {
                            student: '',
                            academyClass: '',
                            date: date,
                            groupBy: 'STUDENT',
                        }
                    }
                    onSubmit={() => {
                    }}
                    //validationSchema={}
                    enableReinitialize
                >
                    {({ values, setFieldValue }) => (
                    <div style={{display: "flex", flexDirection: 'row', marginTop: 25, marginBottom:25}}>
                        <div style={{marginLeft: 50, width: 300}}>
                            <FormikAutocomplete name="academyClass"
                                                multiple={false}
                                                label={"კლასი"}
                                // resolveData={resolveCardTypeAutocompleteData}
                                                onFetch={onFetchAcademyClass}
                                                getOptionSelected={(option, value) => {

                                                    return option.id === value.id
                                                }}
                                                getOptionLabel={(option) => option.className}
                                                setInitialVulue={(options) =>{
                                                    if(options.length === 1){
                                                        return options[0]
                                                    }
                                                }}/>
                        </div>
                        <div style={{marginLeft: 50, width: 300}}>
                            <FormikAutocomplete name="student"
                                                multiple={false}
                                                label={"მოსწავლე"}
                                // resolveData={resolveCardTypeAutocompleteData}
                                                onFetch={onFetchStudents}
                                                getOptionSelected={(option, value) => option.id === value.id}
                                                getOptionLabel={(option) => option.firstName + " " + option.lastName}
                                                setInitialVulue={(options) =>{
                                                    if(options.length === 1){
                                                        return options[0]
                                                    }
                                                }}/>
                        </div>

                        <div style={{marginLeft: 50, width: 300}}>
                            <FormikDatePickerField name="date"
                                                   label={"თვე"}
                                                   onChange={(event, value)=> {
                                                      setFieldValue("date", value)
                                                      setFilters(prevState => {
                                                          const copied = prevState;
                                                          copied.date = value
                                                          return copied
                                                      })
                                                   }}/>
                        </div>
                        <div style={{marginLeft: 15, width: 100}}>
                            <IconButton
                                icon={<Search/>}
                                onClick={() => setData(permanentData)}
                            />
                        </div>
                    </div>)}
                </Formik>
            </FlexBox>
        </div>
    )
}

export default MonthlyGradeToolbar;