import FormModal from "../../../components/modal/FormModal";
import ChangeRequestAcademyClassModal from "./ChangeRequestAcademyClassModal";
import {closePeriod} from "./useClosePeriod";
import {useNotification} from "../../../contexts/notification-context";

const initialValues = {
    academyClasses: [],
};

const ChangeRequestAcademyClassFormModal = ({open, onClose, modalOpenMode, submitButton, ...props}) => {
    const {setNotification, setErrorMessage} = useNotification();

    const onSubmit = (values) => {
        closePeriod(values.academyClasses).then(() =>{
            onClose();
            setNotification({
                message: 'თვის ნიშანი წარმატებით დაითვალა',
                severity: 'success'
            });
        }).catch((error) => {
            setErrorMessage(error);
        });
    }
    return (
        <FormModal
            open={open}
            title={"პერიოდის დახურვა"}
            cancelText={'დახურვა'}
            saveText={'შენახვა'}
            width={700}
            height={250}
            maxWidth={700}
            maxHeight={250}
            initialValues={

                {
                    ...initialValues,
                }
            }
            formProps={{
                modalOpenMode
            }}
            form={ChangeRequestAcademyClassModal}
            // validationSchema={validateNewsFormData(!!news, t)}
            onSubmit={onSubmit}
            onClose={onClose}
            submitButton={submitButton}
            {...props}
        />
    );
};

export default ChangeRequestAcademyClassFormModal;