import FormModal from "../../../components/modal/FormModal";
import {useNotification} from "../../../contexts/notification-context";
import ApproveChangeRequestForm from "./ApproveChangeRequestForm";

const initialValues = {
    description: "",
};

const ApproveChangeRequestFormModal = ({open, onClose, modalOpenMode, submitButton, submit, ...props}) => {
    const {setNotification, setErrorMessage} = useNotification();

    const onSubmit = (values) => {
        submit(values)
    }
    return (
        <FormModal
            open={open}
            title={"ნიშნის ცვლილების თანხმობა"}
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
            form={ApproveChangeRequestForm}
            // validationSchema={validateNewsFormData(!!news, t)}
            onSubmit={onSubmit}
            onClose={onClose}
            submitButton={submitButton}
            {...props}
        />
    );
};

export default ApproveChangeRequestFormModal;