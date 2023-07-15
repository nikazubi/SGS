import useCreateTotalAbsence from "./useCreateTotalAbsence";
import FormModal from "../../../components/modal/FormModal";
import TotalAbsenceForm from "./TotalAbsenceForm";
import {useState} from "react";


const initialValues = {
    activePeriod: new Date(),
    academyClasses: [],
    totalAcademyHour: 0
};

const TotalAbsenceModal = ({open, news, onClose, modalOpenMode, submitButton, ...props}) => {
    const {mutate: onCreate} = useCreateTotalAbsence();
    const [date, setDate] = useState(new Date());

    return (
        <FormModal
            open={open}
            title={"დამატება"}
            cancelText={'დახურვა'}
            saveText={'შენახვა'}
            width={700}
            height={250}
            maxWidth={700}
            maxHeight={250}
            initialValues={

                {
                    ...initialValues,
                    ...news,
                }
            }
            formProps={{
                modalOpenMode
            }}
            form={TotalAbsenceForm}
            // validationSchema={validateNewsFormData(!!news, t)}
            onSubmit={onCreate}
            onClose={onClose}
            submitButton={submitButton}
            {...props}
        />
    );
};

export default TotalAbsenceModal;